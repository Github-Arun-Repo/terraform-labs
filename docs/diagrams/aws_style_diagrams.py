"""Generate simplified AWS-style architecture diagrams.

Outputs (PNG):
- docs/diagrams/system-context.png
- docs/diagrams/event-flow.png
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EKS
from diagrams.aws.database import Dynamodb, RDS
from diagrams.aws.devtools import Codebuild
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
            ci = Codebuild("GitHub Actions CI")
            tf = Codebuild("Terraform Provisioning")
            gitops = Codebuild("ArgoCD GitOps")
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
        "Terraform Labs - Application Architecture Overview",
        filename=str(OUT_DIR / "event-flow"),
        show=False,
        outformat="png",
        direction="LR",
        graph_attr=_graph_attrs(),
    ):
        supplier = Users("Supplier/Admin")
        finance = Users("Finance Reviewer")
        alb = ALB("Public ALB")

        with Cluster("EKS Application Tier"):
            ums = EKS("user-management-service")
            api = EKS("document-api-service")
            processing = EKS("document-processing-service")
            review = EKS("document-review-service")

        with Cluster("Core Data Services"):
            postgres = RDS("RDS PostgreSQL\nIdentity + Refresh Tokens")
            inventory = Dynamodb("DynamoDB\nDocumentInventory")
            bucket = S3("S3\nRaw + Processed Documents")
            queue = SQS("SQS\nIngestion Queue")
            dlq = SQS("SQS\nDead-Letter Queue")

        with Cluster("Security Controls"):
            secrets = SecretsManager("Secrets Manager")
            kms = KMS("KMS CMKs")

        supplier >> Edge(label="authenticate") >> alb >> ums
        ums >> Edge(label="user auth state") >> postgres

        supplier >> Edge(label="JWT + upload request") >> alb >> api
        api >> Edge(label="metadata") >> inventory
        api >> Edge(label="presigned URL") >> bucket

        supplier >> Edge(label="upload document") >> bucket
        bucket >> Edge(label="ObjectCreated event") >> queue
        queue >> Edge(label="consume + process") >> processing
        queue >> Edge(label="failed messages", style="dashed") >> dlq

        processing >> Edge(label="status + extraction") >> inventory
        processing >> Edge(label="processed artifacts") >> bucket

        finance >> Edge(label="review / approve / reject") >> alb >> review
        review >> Edge(label="read/write decisions") >> inventory
        review >> Edge(label="fetch documents") >> bucket

        secrets >> ums
        secrets >> api
        secrets >> processing
        secrets >> review

        kms >> bucket
        kms >> queue
        kms >> inventory


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    generate_infrastructure_overview()
    generate_application_overview()
    print("Generated simplified diagrams in", OUT_DIR)


if __name__ == "__main__":
    main()
