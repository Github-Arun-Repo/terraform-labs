Author: Arunasalam Govindasamy

# Terraform Labs — Architecture Command Center

> A high-clarity, architecture-first repository for AWS infrastructure, Java service design, Kubernetes runtime, and dynamic Jenkins delivery.

<table>
  <tr>
    <td align="center" width="25%">
      <a href="#infrastructure-architecture-default-view">
        <img src="https://img.shields.io/badge/Infrastructure-Default%20View-0B3D91?style=for-the-badge&logo=amazonaws&logoColor=white" alt="Infrastructure" />
      </a>
      <br/>
      <strong>Infrastructure Architecture</strong><br/>
      VPC, EKS, RDS, ALB, state backend
    </td>
    <td align="center" width="25%">
      <a href="#application-design">
        <img src="https://img.shields.io/badge/Application-Design-0E7490?style=for-the-badge&logo=spring&logoColor=white" alt="Application Design" />
      </a>
      <br/>
      <strong>Application Design</strong><br/>
      Service boundaries, domain, API, security
    </td>
    <td align="center" width="25%">
      <a href="#kubernetes-runtime">
        <img src="https://img.shields.io/badge/Kubernetes-Runtime-1D4ED8?style=for-the-badge&logo=kubernetes&logoColor=white" alt="Kubernetes Runtime" />
      </a>
      <br/>
      <strong>Kubernetes Runtime</strong><br/>
      Helm deployment model, ALB ingress
    </td>
    <td align="center" width="25%">
      <a href="#delivery-platform-jenkins">
        <img src="https://img.shields.io/badge/Delivery-Jenkins-9333EA?style=for-the-badge&logo=jenkins&logoColor=white" alt="Delivery Platform" />
      </a>
      <br/>
      <strong>Delivery Platform</strong><br/>
      Dynamic agents, pipeline runtime
    </td>
  </tr>
</table>

## Read by Folder

- Infrastructure details: [terraform/README.md](terraform/README.md)
- Application architecture: [applications/README.md](applications/README.md)
- Kubernetes deployment model: [k8s/README.md](k8s/README.md)
- CI/CD delivery model: [jenkins/README.md](jenkins/README.md)

---

## Infrastructure Architecture (Default View)

This is the default section because platform reliability starts at infrastructure quality.

```mermaid
graph TB
  U[User or Client] --> ALB[Application Load Balancer]

  subgraph VPC[10.0.0.0/16]
    subgraph PUB[Public Subnets]
      ALB
      NAT[NAT Gateway]
    end

    subgraph APP[Private App Subnets]
      EKS[EKS Cluster]
      N1[Node Group api]
      N2[Node Group worker]
      N3[Node Group batch]
    end

    subgraph DB[Private DB Subnets]
      RDS[RDS MySQL]
    end
  end

  EKS --> N1
  EKS --> N2
  EKS --> N3
  N1 --> RDS
  N2 --> RDS
  N3 --> RDS

  subgraph State[Remote Terraform State]
    S3[S3 Bucket]
    DDB[DynamoDB Lock Table]
  end
```

### Why this design is strong

1. Network segmentation by intent: public ingress, private compute, isolated database.
2. Security-group-to-security-group traffic control instead of open CIDR rules.
3. Stateful components isolated from pod churn.
4. Remote Terraform state with locking to prevent destructive concurrency.

### Quick infra outcomes

- Internet entry is only via ALB.
- Workloads run in private subnets.
- Database is private and not internet-addressable.
- State operations are controlled and recoverable.

---

## Application Design

The application is a Spring Boot document-management API with authentication, user lifecycle, and document ingestion/download capability.

Open full design: [applications/README.md](applications/README.md)

```mermaid
flowchart LR
  C[Client] --> AUTH[AuthController]
  C --> DOC[DocumentController]
  C --> USER[UserController]

  AUTH --> AS[AuthService]
  USER --> US[UserService]
  DOC --> DS[DocumentService]

  AS --> JWT[JwtService]
  AS --> REPOU[AppUserRepository]
  US --> REPOU
  DS --> REPOD[DocumentRepository]
  DS --> REPOU

  REPOU --> DB[(MySQL)]
  REPOD --> DB
```

---

## Kubernetes Runtime

The EKS runtime is split into two Helm concerns for scale and ownership clarity.

Open full runtime guide: [k8s/README.md](k8s/README.md)

```mermaid
flowchart LR
  H1[Helm Chart: document-management-service] --> D[Deployment replicas=2]
  H1 --> SVC[ClusterIP Service]

  H2[Helm Chart: document-management-alb] --> ING[Ingress with AWS ALB annotations]

  ING --> SVC
  SVC --> POD1[Pod 1]
  SVC --> POD2[Pod 2]
```

---

## Delivery Platform (Jenkins)

Jenkins is deployed through a dedicated Helm chart and configured to run builds on dynamic Kubernetes agents.

Open full delivery guide: [jenkins/README.md](jenkins/README.md)

```mermaid
sequenceDiagram
  participant Dev as Developer
  participant J as Jenkins Controller
  participant K as Kubernetes Cloud Plugin
  participant A as Dynamic Agent Pod

  Dev->>J: Trigger pipeline
  J->>K: Request agent for label
  K->>A: Create agent pod on demand
  A-->>J: Connect via JNLP
  A->>A: Execute build/test/deploy stage
  A-->>K: Complete and terminate
```

---

## Growth Model

This documentation is intentionally structured to grow:

1. Add a new architecture card in one line at the top table.
2. Add a new section in this README and link to its folder README.
3. Keep detailed implementation in folder-level READMEs.

For reviewers and architects, this root file should remain the main entry point.
