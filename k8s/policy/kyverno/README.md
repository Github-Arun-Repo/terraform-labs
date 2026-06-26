# Kyverno Policies (Audit-first)

This chart deploys cluster-wide Kyverno `ClusterPolicy` resources for baseline runtime and supply-chain enforcement.

```mermaid
%%{init: {'theme':'default','flowchart':{'useMaxWidth':true,'htmlLabels':true}}}%%
flowchart TB
	Request["Kubernetes API request\nPod or ServiceAccount"]:::edge --> Webhook["Kyverno admission webhook"]:::control
	Webhook --> Security["require-security-context\nnon-root, seccomp, no privilege escalation, drop ALL"]:::control
	Webhook --> Resources["require-cpu-memory-requests-limits\nCPU and memory requests/limits"]:::control
	Webhook --> Latest["disallow-latest-tag\nno mutable :latest images"]:::control
	Webhook --> IRSA["require-irsa-annotation\nServiceAccount role ARN"]:::control
	Webhook --> Cosign["verify-images-gha-cosign\nGitHub OIDC/Sigstore identity"]:::control
	Security --> Mode{"policyValidationFailureAction"}:::control
	Resources --> Mode
	Latest --> Mode
	IRSA --> Mode
	Cosign --> Mode
	Mode -->|Audit| Report["PolicyReport / audit finding"]:::data
	Mode -->|Enforce| Block["Admission block"]:::data
	Mode -->|Pass| Admit["Resource admitted"]:::runtime

	classDef edge fill:#EAF4FF,stroke:#1D4ED8,color:#0F172A,stroke-width:1.5px;
	classDef runtime fill:#EFFFF7,stroke:#059669,color:#052E2B,stroke-width:1.5px;
	classDef data fill:#FFF7ED,stroke:#EA580C,color:#431407,stroke-width:1.5px;
	classDef control fill:#F5ECFF,stroke:#7C3AED,color:#2E1065,stroke-width:1.5px;
	classDef legacy fill:#F8FAFC,stroke:#94A3B8,color:#475569,stroke-width:1px,stroke-dasharray: 5 5;
```

*Every policy uses the same validation failure action, so the platform can run audit-first and later flip the pack to enforcement from one values key.*

## Included policies

- Require non-root execution and restricted container security context.
- Require CPU and memory requests and limits.
- Disallow mutable `:latest` image tags.
- Require `eks.amazonaws.com/role-arn` annotation on service accounts in AWS-access namespaces.
- Verify cosign keyless signatures for ECR images (GitHub OIDC/Sigstore identity).

## Policy table

| Policy name | What it checks | Match scope | Default action |
|---|---|---|---|
| `require-security-context` | Pod `runAsNonRoot: true`, `seccompProfile.type: RuntimeDefault`, container `allowPrivilegeEscalation: false`, and `capabilities.drop: ALL`. | `Pod` | `Audit` from `policyValidationFailureAction` |
| `require-cpu-memory-requests-limits` | Every container defines CPU and memory requests and limits. | `Pod` | `Audit` from `policyValidationFailureAction` |
| `disallow-latest-tag` | Container image strings must not contain `:latest`. | `Pod` | `Audit` from `policyValidationFailureAction` |
| `require-irsa-annotation` | ServiceAccounts include `eks.amazonaws.com/role-arn: arn:aws:iam::*:role/*`. | `ServiceAccount` in `document-api`, `document-processing`, `document-review`, `document-processor`, `user-management` namespaces. | `Audit` from `policyValidationFailureAction` |
| `verify-images-gha-cosign` | ECR images match keyless signatures from issuer `https://token.actions.githubusercontent.com` and subject regex `https://github.com/.*/terraform-labs/.github/workflows/ci-services.yml@refs/heads/main`. | `Pod`; `background: false`; ECR image references only. | `Audit` from `policyValidationFailureAction` |

## Audit -> Enforce

Policies start in `Audit` mode by default:

- `policyValidationFailureAction: Audit`

To switch to enforcement, set:

- `policyValidationFailureAction: Enforce`

in [values.yaml](values.yaml) and sync the ArgoCD application.
