# Architecture PNG Diagrams (AWS Icons)

This folder contains a Python diagrams (mingrammer) source that renders portfolio-grade architecture visuals as PNG files:

- system-context.png
- event-flow.png
- cicd-supply-chain.png
- aws-infrastructure-overview.png
- aws-application-overview.png

## Prerequisites

- Python 3.10+
- pip
- Graphviz (`dot` command available)

## Install

```bash
python3 -m pip install -r docs/diagrams/requirements.txt
```

## Generate PNGs

```bash
python3 docs/diagrams/architecture.py
python3 docs/diagrams/aws_style_diagrams.py
```

## Generate via GitHub Actions (no local setup)

If your local environment cannot install pip or Graphviz, run the repository workflow:

- Workflow: `Render Architecture Diagrams`
- Trigger: `workflow_dispatch`

The workflow renders `docs/diagrams/*.png` and commits updated files automatically.

Generated files are written to `docs/diagrams/` and are embedded by the top-level README.

## Notes

- If `dot` is missing, install Graphviz first (package name is usually `graphviz`).
- These diagrams intentionally focus on enterprise communication clarity: trust boundaries, eventing path, and CI/CD-to-runtime chain of custody.
