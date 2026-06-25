// CI pipeline for the Document Management Service.
//
// Responsibilities (CI only):
//   1. Compile, unit-test and integration-test the application.
//   2. Build a container image and push it to ECR.
//   3. Write the new image tag back to the Helm values file in Git so that
//      ArgoCD detects the change and drives the deployment automatically.
//
// Deployment is NOT performed here – it is owned by ArgoCD via the
// GitOps manifest at cicd/argocd/dms-application.yaml.

pipeline {
  agent none

  options {
    timestamps()
    skipDefaultCheckout(true)
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    string(
      name: 'AWS_REGION',
      defaultValue: 'eu-west-1',
      description: 'AWS region that hosts the ECR repository.'
    )
    string(
      name: 'ECR_REPOSITORY_URI',
      defaultValue: '',
      description: 'Full ECR URI, e.g. 123456789012.dkr.ecr.eu-west-1.amazonaws.com/document-management-service.'
    )
    string(
      name: 'IMAGE_TAG',
      defaultValue: '',
      description: 'Explicit image tag. Leave empty to auto-generate as BUILD_NUMBER-shortSHA.'
    )
    choice(
      name: 'IMAGE_BUILDER',
      choices: ['docker', 'jib'],
      description: 'docker uses the Dockerfile; jib is the daemonless Maven fallback.'
    )
    booleanParam(
      name: 'PUSH_LATEST',
      defaultValue: true,
      description: 'Also update the :latest tag in ECR after pushing the build tag.'
    )
    // --- GitOps write-back parameters ----------------------------------------
    string(
      name: 'GIT_CREDENTIALS_ID',
      defaultValue: 'github-app',
      description: 'Jenkins credentials ID (username+password / GitHub App token) for pushing the image-tag commit.'
    )
    string(
      name: 'GIT_REPO_URL',
      defaultValue: '',
      description: 'HTTPS URL of this repository. Required for the write-back commit, e.g. https://github.com/your-org/terraform-labs.git.'
    )
    string(
      name: 'GIT_BRANCH',
      defaultValue: 'main',
      description: 'Branch that ArgoCD tracks. The image-tag commit is pushed here.'
    )
    string(
      name: 'VALUES_FILE_PATH',
      defaultValue: 'k8s/eks/document-management-service/values.yaml',
      description: 'Repo-relative path to the Helm values file where image.tag is updated.'
    )
  }

  environment {
    APP_PATH = 'applications/document-management-service'
  }

  stages {

    // -------------------------------------------------------------------------
    stage('Resolve Source') {
      agent { label 'maven' }
      steps {
        checkout scm
        script {
          def shortSha = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          env.RESOLVED_IMAGE_TAG = params.IMAGE_TAG?.trim()
            ? params.IMAGE_TAG.trim()
            : "${env.BUILD_NUMBER}-${shortSha}"
          env.ECR_REGISTRY = params.ECR_REPOSITORY_URI.tokenize('/')[0]
        }
        stash name: 'repo-source', includes: '**/*', useDefaultExcludes: false
      }
    }

    // -------------------------------------------------------------------------
    stage('Build and Test') {
      agent { label 'maven' }
      steps {
        unstash 'repo-source'
        dir(env.APP_PATH) {
          container('maven') {
            sh 'mvn -B clean verify'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true,
                testResults: "${env.APP_PATH}/**/target/surefire-reports/*.xml"
        }
      }
    }

    // -------------------------------------------------------------------------
    stage('Build and Push Image – Docker') {
      when {
        expression { params.IMAGE_BUILDER == 'docker' }
      }
      agent { label 'docker' }
      steps {
        unstash 'repo-source'
        container('awscli') {
          sh 'aws ecr get-login-password --region "$AWS_REGION" > ecr-password.txt'
        }
        dir(env.APP_PATH) {
          container('docker') {
            sh '''
              export DOCKER_HOST=tcp://127.0.0.1:2375
              cat "$WORKSPACE/ecr-password.txt" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
              docker build -t "$ECR_REPOSITORY_URI:$RESOLVED_IMAGE_TAG" .
              docker push "$ECR_REPOSITORY_URI:$RESOLVED_IMAGE_TAG"

              if [ "$PUSH_LATEST" = "true" ]; then
                docker tag "$ECR_REPOSITORY_URI:$RESOLVED_IMAGE_TAG" "$ECR_REPOSITORY_URI:latest"
                docker push "$ECR_REPOSITORY_URI:latest"
              fi
            '''
          }
        }
      }
    }

    // -------------------------------------------------------------------------
    stage('Build and Push Image – Jib') {
      when {
        expression { params.IMAGE_BUILDER == 'jib' }
      }
      agent { label 'maven' }
      steps {
        unstash 'repo-source'
        container('awscli') {
          sh 'aws ecr get-login-password --region "$AWS_REGION" > ecr-password.txt'
        }
        dir(env.APP_PATH) {
          container('maven') {
            sh '''
              mvn -B -Pjib -DskipTests \
                -Djib.to.image="$ECR_REPOSITORY_URI:$RESOLVED_IMAGE_TAG" \
                -Djib.to.auth.username=AWS \
                -Djib.to.auth.password="$(cat "$WORKSPACE/ecr-password.txt")" \
                jib:build
            '''
          }
        }
      }
    }

    // -------------------------------------------------------------------------
    // GitOps write-back: update image.tag in the Helm values file and push.
    // ArgoCD watches this file and will trigger a sync automatically.
    // -------------------------------------------------------------------------
    stage('GitOps – Update Image Tag') {
      agent { label 'maven' }
      steps {
        unstash 'repo-source'
        container('maven') {
          withCredentials([usernamePassword(
            credentialsId: params.GIT_CREDENTIALS_ID,
            usernameVariable: 'GIT_USER',
            passwordVariable: 'GIT_TOKEN'
          )]) {
            sh '''
              # Patch image.tag in the Helm values file
              sed -i "s|^  tag:.*|  tag: \\"${RESOLVED_IMAGE_TAG}\\"|" "${VALUES_FILE_PATH}"

              git config user.email "jenkins@ci.local"
              git config user.name "Jenkins CI"

              git add "${VALUES_FILE_PATH}"
              git commit -m "ci: update dms image tag to ${RESOLVED_IMAGE_TAG} [skip ci]"

              # Embed credentials in the URL so no interactive prompt is needed
              AUTHENTICATED_URL=$(echo "${GIT_REPO_URL}" | \
                sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
              git push "${AUTHENTICATED_URL}" "HEAD:${GIT_BRANCH}"
            '''
          }
        }
      }
    }

  }

  post {
    success {
      echo "Image pushed and GitOps tag updated: ${env.RESOLVED_IMAGE_TAG}"
      echo "ArgoCD will now sync the deployment automatically."
    }
    always {
      echo "Pipeline finished. Build tag: ${env.RESOLVED_IMAGE_TAG ?: 'not-resolved'}"
    }
  }
}
