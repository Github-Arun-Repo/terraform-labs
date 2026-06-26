# Kyverno Policies (Audit-first)

This chart deploys cluster-wide Kyverno `ClusterPolicy` resources for baseline runtime and supply-chain enforcement.

## Included policies

- Require non-root execution and restricted container security context.
- Require CPU and memory requests and limits.
- Disallow mutable `:latest` image tags.
- Require `eks.amazonaws.com/role-arn` annotation on service accounts in AWS-access namespaces.
- Verify cosign keyless signatures for ECR images (GitHub OIDC/Sigstore identity).

## Audit -> Enforce

Policies start in `Audit` mode by default:

- `policyValidationFailureAction: Audit`

To switch to enforcement, set:

- `policyValidationFailureAction: Enforce`

in [k8s/policy/kyverno/values.yaml](k8s/policy/kyverno/values.yaml) and sync the ArgoCD application.
