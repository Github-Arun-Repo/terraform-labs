"""Generate simplified AWS-style architecture diagrams.

Outputs (PNG):
- docs/diagrams/system-context.png
- docs/diagrams/event-flow.png
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EKS
from diagrams.aws.database import Dynamodb, RDS
from diagrams.aws.devtools import Codepipeline
from diagrams.aws.general import Users
from diagrams.aws.integration import SQS
from diagrams.aws.network import ALB, NATGateway
from diagrams.aws.security import KMS, SecretsManager
from diagrams.aws.storage import ECR, S3

OUT_DIR = Path(__file__).resolve().parent


def _graph_attrs() -> dict[str, str]:
    return {
        "fontsize": "18",
        "pad": "0.35",
        "splines": "ortho",
        "nodesep": "0.7",
        "ranksep": "1.0",
        "bgcolor": "white",
        "labelloc": "t",
        "fontname": "Helvetica",
    }


def generate_infrastructure_overview() -> None:
    with Diagram(
        "Terraform Labs - AWS Infrastructure Overview",
        filename=str(OUT_DIR / "system-context"),
        show=False,
        outformat="png",
        direction="LR",
        graph_attr=_graph_attrs(),
    ):
        users = Users("Users")
        alb = ALB("Public ALB")

        with Cluster("AWS VPC (3-AZ)"):
            with Cluster("Private App Subnets"):
                eks = EKS("EKS cluster")
                apps = EKS("Service workloads")

            with Cluster("Private DB Subnets"):
                rds = RDS("RDS PostgreSQL")

            data_store = Dynamodb("DynamoDB")
            object_store = S3("S3 documents")
            queue = SQS("SQS + DLQ")
            kms = KMS("KMS CMKs")
            nat = NATGateway("NAT Gateway")
            secrets = SecretsManager("Secrets Manager")

        with Cluster("Delivery and Control Plane"):
            git = Users("Git Repository")
            ci = Codepipeline("GitHub Actions CI")
            tf = Codepipeline("Terraform Provisioning")
            gitops = Codepipeline("ArgoCD GitOps")
            ecr = ECR("ECR")
            iam = Users("OIDC / IRSA Access")

        users >> Edge(label="HTTPS") >> alb >> eks >> apps
        apps >> Edge(label="DB") >> rds
        apps >> Edge(label="state") >> data_store
        apps >> Edge(label="files") >> object_store
        object_store >> Edge(label="events") >> queue >> apps
        apps >> Edge(label="credentials") >> secrets

        kms >> object_store
        kms >> data_store
        kms >> queue

        apps >> Edge(style="dashed", label="egress") >> nat

        git >> ci >> ecr >> apps
        git >> tf
        tf >> Edge(label="provision") >> eks
        tf >> rds
        tf >> data_store
        tf >> object_store
        tf >> queue
        tf >> iam
        git >> gitops >> Edge(label="sync") >> apps


def generate_application_overview() -> None:
    with Diagram(
        "Terraform Labs - Application Workflow Overview",
        filename=str(OUT_DIR / "event-flow"),
        show=False,
        outformat="png",
        direction="LR",
        graph_attr=_graph_attrs(),
    ):
        supplier = Users("Supplier/Admin")
        finance = Users("Finance Reviewer")

        ums = EKS("user-management-service")
        api = EKS("document-api-service")
        processing = EKS("document-processing-service")
        review = EKS("document-review-service")

        postgres = RDS("Identity DB")
        bucket = S3("Document Bucket")
        queue = SQS("Ingestion Queue")
        inventory = Dynamodb("DocumentInventory")

        supplier >> Edge(label="login") >> ums >> postgres
        supplier >> Edge(label="upload request") >> api
        api >> inventory
        supplier >> Edge(label="upload file") >> bucket
        bucket >> Edge(label="ObjectCreated") >> queue >> processing
        processing >> inventory
        processing >> bucket
        finance >> Edge(label="review / decision") >> review >> inventory


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    generate_infrastructure_overview()
    generate_application_overview()
    print("Generated simplified diagrams in", OUT_DIR)


if __name__ == "__main__":
    main()
