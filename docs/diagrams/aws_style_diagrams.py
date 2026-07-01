"""Generate simplified AWS-style architecture diagrams.

Outputs (PNG):
- docs/diagrams/aws-infrastructure-overview.png
- docs/diagrams/aws-application-overview.png
"""

from pathlib import Path

from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EKS
from diagrams.aws.database import Dynamodb, RDS
from diagrams.aws.general import Users
from diagrams.aws.integration import SQS
from diagrams.aws.network import ALB, NATGateway
from diagrams.aws.security import IAM, KMS
from diagrams.aws.storage import ECR, S3
from diagrams.onprem.ci import GithubActions
from diagrams.onprem.gitops import Argocd
from diagrams.onprem.iac import Terraform
from diagrams.onprem.vcs import Github

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
        filename=str(OUT_DIR / "aws-infrastructure-overview"),
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

        with Cluster("Delivery Control Plane"):
            git = Github("GitHub")
            ci = GithubActions("GitHub Actions")
            tf = Terraform("Terraform")
            gitops = Argocd("ArgoCD")
            ecr = ECR("ECR")
            iam = IAM("OIDC / IRSA")

        users >> Edge(label="HTTPS") >> alb >> eks >> apps
        apps >> Edge(label="DB") >> rds
        apps >> Edge(label="state") >> data_store
        apps >> Edge(label="files") >> object_store
        object_store >> Edge(label="events") >> queue >> apps

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
        filename=str(OUT_DIR / "aws-application-overview"),
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
