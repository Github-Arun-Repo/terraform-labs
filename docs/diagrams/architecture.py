"""Generate architecture diagrams for Terraform Labs.

Outputs (PNG):
- docs/diagrams/system-context.png
- docs/diagrams/event-flow.png
- docs/diagrams/cicd-supply-chain.png
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import ECR, EKS
from diagrams.aws.database import Dynamodb, RDS
from diagrams.aws.devtools import Codebuild
from diagrams.aws.general import Users
from diagrams.aws.integration import SQS
from diagrams.aws.management import Cloudwatch
from diagrams.aws.network import ALB
from diagrams.aws.security import KMS, SecretsManager, WAF
from diagrams.aws.storage import S3

OUT_DIR = Path(__file__).resolve().parent


def _graph_attrs() -> dict[str, str]:
    return {
        "fontsize": "18",
        "pad": "0.35",
        "splines": "spline",
        "nodesep": "0.6",
        "ranksep": "1.0",
        "bgcolor": "white",
        "labelloc": "t",
        "fontname": "Helvetica",
    }


def generate_system_context() -> None:
    with Diagram(
        "Terraform Labs - System Context",
        filename=str(OUT_DIR / "system-context"),
        show=False,
        outformat="png",
        direction="LR",
        graph_attr=_graph_attrs(),
    ):
        users = Users("Platform Users")

        with Cluster("AWS Account"):
            edge = ALB("Public ALB")
            waf = WAF("WAFv2 ACL")
            kms = KMS("CMKs")

            with Cluster("EKS Runtime"):
                ingress = EKS("Ingress + Services")
                k8s_api = EKS("Kubernetes API")
                services = [
                    EKS("document-api"),
                    EKS("document-processing"),
                    EKS("document-review"),
                    EKS("user-management"),
                ]
                workers = EKS("processor pods")

                with Cluster("Platform Controls"):
                    gitops = Users("ArgoCD")
                    helm = Users("Helm Charts")
                    prom = Users("Prometheus")
                    grafana = Users("Grafana")

            with Cluster("Data Plane"):
                s3 = S3("documents bucket")
                sqs = SQS("ingestion queue + DLQ")
                ddb = Dynamodb("DocumentInventory")
                rds = RDS("PostgreSQL")
                secrets = SecretsManager("Secrets Manager")

        users >> Edge(label="HTTPS", color="#1d4ed8", penwidth="2.0") >> edge
        edge >> Edge(label="L7 routing", color="#1d4ed8") >> ingress
        waf >> Edge(style="dashed", label="protect") >> edge

        ingress >> Edge(label="service traffic", color="#059669") >> services
        services >> Edge(label="state", color="#ea580c") >> ddb
        services >> Edge(label="documents", color="#ea580c") >> s3
        services >> Edge(label="workflow", color="#ea580c") >> sqs
        services >> Edge(label="identity/auth", color="#ea580c") >> rds
        services >> Edge(label="credentials", color="#7c3aed") >> secrets

        sqs >> Edge(label="events", color="#059669") >> workers
        workers >> Edge(label="status updates", color="#059669") >> ddb

        gitops >> Edge(style="dashed", label="sync") >> helm
        helm >> Edge(style="dashed", label="release") >> services
        prom >> Edge(style="dashed", label="scrape") >> services
        grafana >> Edge(style="dashed", label="dashboards") >> prom

        kms >> Edge(style="dashed", label="encrypt") >> s3
        kms >> Edge(style="dashed", label="encrypt") >> ddb
        kms >> Edge(style="dashed", label="encrypt") >> sqs
        k8s_api >> Edge(style="dashed", label="envelope secrets") >> kms


def generate_event_flow() -> None:
    with Diagram(
        "Terraform Labs - Event Driven Flow (S3 -> SQS -> Processing)",
        filename=str(OUT_DIR / "event-flow"),
        show=False,
        outformat="png",
        direction="TB",
        graph_attr=_graph_attrs(),
    ):
        supplier = Users("Supplier / Client")
        api = EKS("document-api-service")

        with Cluster("Storage & Event Fabric"):
            raw_bucket = S3("S3 raw objects")
            queue = SQS("SQS ingestion")
            dlq = SQS("SQS dead-letter")

        with Cluster("Processing"):
            processor = EKS("document-processing-service")
            status = Dynamodb("DocumentInventory")
            processed = S3("S3 processed/failed")

        with Cluster("Review"):
            reviewer = EKS("document-review-service")
            finance = Users("Finance reviewer")

        supplier >> Edge(label="request presigned URL", color="#1d4ed8") >> api
        api >> Edge(label="write metadata", color="#ea580c") >> status
        supplier >> Edge(label="PUT object", color="#1d4ed8", penwidth="2.0") >> raw_bucket

        raw_bucket >> Edge(label="ObjectCreated", color="#059669", penwidth="2.0") >> queue
        queue >> Edge(label="poll + ack", color="#059669") >> processor
        queue >> Edge(label="max retry", style="dashed", color="#dc2626") >> dlq

        processor >> Edge(label="extraction + state transitions", color="#ea580c") >> status
        processor >> Edge(label="normalized output", color="#ea580c") >> processed

        finance >> Edge(label="approve/reject", color="#7c3aed") >> reviewer
        reviewer >> Edge(label="read/update", color="#7c3aed") >> status


def generate_cicd_supply_chain() -> None:
    with Diagram(
        "Terraform Labs - CI/CD and Supply Chain",
        filename=str(OUT_DIR / "cicd-supply-chain"),
        show=False,
        outformat="png",
        direction="LR",
        graph_attr=_graph_attrs(),
    ):
        dev = Users("Architect / Developer")
        repo = Users("GitHub Repository")

        with Cluster("CI and Verification"):
            actions = Codebuild("GitHub Actions")
            security_scan = Codebuild("Trivy + CodeQL")
            build = Codebuild("Build/Test Stage")
            tf = Codebuild("Terraform Plan")

        with Cluster("Artifacts"):
            ecr = ECR("Amazon ECR")
            attest = S3("SBOM + Provenance")

        with Cluster("Delivery"):
            gitops = Users("ArgoCD")
            runtime = EKS("EKS workloads")
            metrics = Cloudwatch("Audit/Telemetry")

        dev >> Edge(label="commit / PR", color="#1d4ed8") >> repo
        repo >> Edge(label="trigger workflows", color="#1d4ed8") >> actions

        actions >> Edge(label="SAST/IaC/container", color="#7c3aed") >> security_scan
        actions >> Edge(label="compile/unit/integration", color="#059669") >> build
        actions >> Edge(label="plan + drift", color="#ea580c") >> tf

        build >> Edge(label="push images", color="#ea580c", penwidth="2.0") >> ecr
        security_scan >> Edge(style="dashed", label="gates") >> build
        actions >> Edge(label="publish SBOM/signatures", color="#7c3aed") >> attest

        repo >> Edge(label="desired state", color="#059669") >> gitops
        gitops >> Edge(label="sync apps", color="#059669", penwidth="2.0") >> runtime
        ecr >> Edge(label="pull image", color="#ea580c") >> runtime
        tf >> Edge(style="dashed", label="provision infra") >> runtime
        runtime >> Edge(style="dashed", label="service + platform signals") >> metrics


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    generate_system_context()
    generate_event_flow()
    generate_cicd_supply_chain()
    print("Generated diagrams in", OUT_DIR)


if __name__ == "__main__":
    main()
