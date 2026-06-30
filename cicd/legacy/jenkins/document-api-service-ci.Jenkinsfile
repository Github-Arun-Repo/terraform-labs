pipeline {
  agent none

  options {
    timestamps()
    skipDefaultCheckout(true)
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    string(name: 'AWS_REGION', defaultValue: 'eu-west-1', description: 'AWS region that hosts the ECR repository.')
    string(name: 'ECR_REPOSITORY_URI', defaultValue: '', description: 'Full ECR URI for document-api-service image.')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Explicit image tag. Leave empty to auto-generate.')
    choice(name: 'IMAGE_BUILDER', choices: ['docker', 'jib'], description: 'docker uses Dockerfile; jib is daemonless Maven fallback.')
    booleanParam(name: 'PUSH_LATEST', defaultValue: true, description: 'Also update :latest tag in ECR.')
    string(name: 'GIT_CREDENTIALS_ID', defaultValue: 'github-app', description: 'Jenkins credentials for GitOps write-back.')
    string(name: 'GIT_REPO_URL', defaultValue: '', description: 'HTTPS URL of this repository.')
    string(name: 'GIT_BRANCH', defaultValue: 'main', description: 'Branch that ArgoCD tracks.')
    string(name: 'VALUES_FILE_PATH', defaultValue: 'k8s/eks/document-api-service/values.yaml', description: 'Helm values file to patch.')
  }

  environment {
    APP_PATH = 'applications/document-api-service'
  }

  stages {
    stage('Resolve Source') {
      agent { label 'maven' }
      steps {
        checkout scm
        script {
          def shortSha = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
          env.RESOLVED_IMAGE_TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : "${env.BUILD_NUMBER}-${shortSha}"
          env.ECR_REGISTRY = params.ECR_REPOSITORY_URI.tokenize('/')[0]
        }
        stash name: 'repo-source', includes: '**/*', useDefaultExcludes: false
      }
    }

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

    stage('Build and Push Image - Docker') {
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

    stage('Trivy Image Scan') {
      when {
        expression { params.IMAGE_BUILDER == 'docker' }
      }
      agent { label 'docker' }
      steps {
        unstash 'repo-source'
        container('awscli') {
          sh 'aws ecr get-login-password --region "$AWS_REGION" > ecr-password.txt'
        }
        container('docker') {
          sh '''
            export DOCKER_HOST=tcp://127.0.0.1:2375
            IMAGE_REF="$ECR_REPOSITORY_URI:$RESOLVED_IMAGE_TAG"

            echo "[Trivy] Scanning image: $IMAGE_REF"
            echo "[Trivy] Table output (HIGH and CRITICAL):"
            docker run --rm \
              -e TRIVY_USERNAME=AWS \
              -e TRIVY_PASSWORD="$(cat "$WORKSPACE/ecr-password.txt")" \
              aquasec/trivy:latest image \
              --severity HIGH,CRITICAL \
              --format table \
              --exit-code 0 \
              "$IMAGE_REF"

            JSON_OUTPUT=$(docker run --rm \
              -e TRIVY_USERNAME=AWS \
              -e TRIVY_PASSWORD="$(cat "$WORKSPACE/ecr-password.txt")" \
              aquasec/trivy:latest image \
              --severity HIGH,CRITICAL \
              --format json \
              --exit-code 0 \
              "$IMAGE_REF")

            CRITICAL_COUNT=$(printf '%s' "$JSON_OUTPUT" | grep -o '"Severity":"CRITICAL"' | wc -l | tr -d ' ')
            HIGH_COUNT=$(printf '%s' "$JSON_OUTPUT" | grep -o '"Severity":"HIGH"' | wc -l | tr -d ' ')

            if [ "$HIGH_COUNT" -gt 0 ]; then
              echo "[Trivy][WARNING] HIGH vulnerabilities found: $HIGH_COUNT"
            fi

            if [ "$CRITICAL_COUNT" -gt 0 ]; then
              echo "[Trivy][ERROR] CRITICAL vulnerabilities found: $CRITICAL_COUNT"
              exit 1
            fi

            echo "[Trivy] No CRITICAL vulnerabilities found."
          '''
        }
      }
    }

    stage('Build and Push Image - Jib') {
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

    stage('GitOps - Update Image Tag') {
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
              sed -i "s|^  tag:.*|  tag: \\\"${RESOLVED_IMAGE_TAG}\\\"|" "${VALUES_FILE_PATH}"

              git config user.email "jenkins@ci.local"
              git config user.name "Jenkins CI"

              git add "${VALUES_FILE_PATH}"
              git commit -m "ci: update document-api-service image tag to ${RESOLVED_IMAGE_TAG} [skip ci]"

              AUTHENTICATED_URL=$(echo "${GIT_REPO_URL}" | \
                sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
              git push "${AUTHENTICATED_URL}" "HEAD:${GIT_BRANCH}"
            '''
          }
        }
      }
    }
  }
}
