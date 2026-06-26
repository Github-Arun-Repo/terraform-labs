# Observability Architecture

At 2am, when a supplier upload fails, this platform is designed to identify the fault domain in under 60 seconds: metrics quantify blast radius, traces reconstruct causal flow across services, and structured logs pin the exact failing payload and code path.

## 🔭 Overview
Why this matters: operational excellence is determined by correlation speed, not by the number of dashboards.

```mermaid
flowchart LR
    subgraph SRC[Application Sources]
      UMS[user-management-service]
      DAS[document-api-service]
      DPS[document-processing-service]
      DRS[document-review-service]
    end

    subgraph MET[Metrics Path]
      MIC[Micrometer]
      PRO[Prometheus]
      GRA[Grafana]
    end

    subgraph TRC[Tracing Path]
      OSDK[OpenTelemetry SDK]
      OEXP[OTLP Exporter]
      TBACK[Tempo or Jaeger]
    end

    subgraph LOG[Logging Path]
      LGB[Logback JSON]
      OUT[stdout]
      FLB[Fluent Bit]
      LBACK[Loki or Graylog]
    end

    UMS -->|metrics samples| MIC
    DAS -->|metrics samples| MIC
    DPS -->|metrics samples| MIC
    DRS -->|metrics samples| MIC
    MIC -->|scrapes /actuator/prometheus every 15s| PRO
    PRO -->|PromQL query results| GRA

    UMS -->|spans| OSDK
    DAS -->|spans| OSDK
    DPS -->|spans| OSDK
    DRS -->|spans| OSDK
    OSDK -->|OTLP gRPC :4317| OEXP
    OEXP -->|trace batches| TBACK

    UMS -->|JSON log lines| LGB
    DAS -->|JSON log lines| LGB
    DPS -->|JSON log lines| LGB
    DRS -->|JSON log lines| LGB
    LGB -->|container output| OUT
    OUT -->|tail + enrich + ship| FLB
    FLB -->|indexed streams| LBACK

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class UMS,DAS,DPS,DRS services;
    class MIC,PRO,GRA,OSDK,OEXP,LGB,FLB backends;
    class TBACK,OUT,LBACK storage;
```

## 📊 Metrics
Why this matters: metrics are the fastest way to detect regression, estimate customer impact, and confirm recovery.

```mermaid
flowchart LR
    SM[ServiceMonitor] -->|target discovery| PO[Prometheus Operator]
    PO -->|render scrape config| PR[Prometheus]
    PR -->|datasource queries| GD[Grafana]

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class SM services;
    class PO,PR,GD backends;
```

Each Spring Boot service exposes `/actuator/prometheus`. In Kubernetes, `ServiceMonitor` resources scrape those endpoints every 15 seconds and are selected via `prometheus: kube-prometheus`.

### Custom business metrics

| Service | Metric Name | Type | What It Measures |
|---|---|---|---|
| document-api-service | documents.upload.requested | Counter | Upload requests accepted by the API boundary |
| document-api-service | documents.upload.failed | Counter | Upload request failures caused by validation or runtime exceptions |
| document-api-service | documents.upload.duration | Timer | End-to-end upload request handling latency |
| document-processing-service | documents.processing.started | Counter | Processing executions that begin work on a document |
| document-processing-service | documents.processing.failed | Counter | Processing executions ending in failure paths |
| document-processing-service | documents.processing.duration (planned) | Timer | End-to-end processing latency per document |
| document-processing-service | documents.duplicate.detected (planned) | Counter | Documents classified as duplicates |
| document-review-service | documents.review.approved | Counter | Approved review decisions |
| document-review-service | documents.review.rejected | Counter | Rejected review decisions |
| document-review-service | documents.manual.review.triggered (planned) | Counter | Manual review escalations triggered |
| user-management-service | auth.login.success | Counter | Successful user login operations |
| user-management-service | auth.login.failed | Counter | Failed user login operations |
| user-management-service | auth.token.refresh (planned) | Counter | Refresh token exchanges producing new access tokens |

### Prometheus exposition example

```text
# HELP documents_upload_requested_total Upload requests accepted by the API boundary
# TYPE documents_upload_requested_total counter
documents_upload_requested_total{application="document-api-service",environment="local"} 249

# HELP auth_login_success_total Successful user login operations
# TYPE auth_login_success_total counter
auth_login_success_total{application="user-management-service",environment="local"} 911

# HELP documents_processing_failed_total Processing executions ending in failure paths
# TYPE documents_processing_failed_total counter
documents_processing_failed_total{application="document-processing-service",environment="local"} 7
```

## 🔍 Tracing
Why this matters: distributed traces transform a multi-service outage into a deterministic hop-by-hop timeline.

```mermaid
sequenceDiagram
    participant Supplier
    participant API as document-api-service
    participant S3 as S3 presigned URL generation
    participant SQS as SQS
    participant PROC as document-processing-service
    participant REV as document-review-service

    Supplier->>API: Upload request (traceId: abc123, spanId: 001)
    API->>S3: Presigned URL generation (traceId: abc123, spanId: 002)
    S3-->>SQS: Object event (traceId: abc123)
    SQS->>PROC: Process event (traceId: abc123, spanId: 003)
    PROC->>REV: Review update call/event (traceId: abc123, spanId: 004)
```

```mermaid
flowchart LR
    W3C[W3C Trace Context] --> MBRIDGE[micrometer-tracing-bridge-otel]
    MBRIDGE --> MDC[MDC traceId/spanId]
    MDC --> JLOG[JSON logs]
    MBRIDGE --> OTLPX[OTLP exporter]
    OTLPX --> TRACEBK[Tempo or Jaeger]

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class W3C,MBRIDGE services;
    class OTLPX backends;
    class MDC,JLOG,TRACEBK storage;
```

Operational tracing notes:
1. `micrometer-tracing-bridge-otel` injects `traceId` and `spanId` into MDC automatically; every log line becomes trace-correlated.
2. W3C Trace Context headers propagate trace identity across HTTP boundaries.
3. OTLP export sends spans to any OTLP-compatible collector.
4. In Grafana Tempo, search by `traceId` copied from logs to retrieve the complete cross-service trace.

## 📋 Logging
Why this matters: logs preserve payload-level context that metrics and traces intentionally aggregate away.

```mermaid
flowchart LR
    APP[Spring Boot service] -->|MDC-enriched event| LB[Logback JSON pattern]
    LB -->|stdout| CRT[Container runtime]
    CRT -->|forwarder| FB[Fluent Bit]
    FB -->|index and search| LOUT[Loki or Graylog]

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class APP services;
    class LB,FB backends;
    class CRT,LOUT storage;
```

### Scenario 1: successful document upload (INFO)

```json
{"timestamp":"2026-06-26 02:10:14","level":"INFO","service":"document-api-service","traceId":"abc123","spanId":"001","thread":"http-nio-8082-exec-5","logger":"com.documentplatform.documentapi.controller.DocumentController","message":"Upload request accepted","documentId":"doc-2001","customerId":"cust-88","contentType":"application/pdf"}
```

### Scenario 2: SQS processing failure (ERROR)

```json
{"timestamp":"2026-06-26 02:10:29","level":"ERROR","service":"document-processing-service","traceId":"abc123","spanId":"003","thread":"sqs-poller-1","logger":"com.documentplatform.documentprocessing.service.DocumentProcessingService","message":"Processing failed for document","documentId":"doc-2001","exceptionClass":"software.amazon.awssdk.services.s3.model.NoSuchKeyException","exceptionMessage":"The specified key does not exist"}
```

### Scenario 3: review decision approved (INFO)

```json
{"timestamp":"2026-06-26 02:11:02","level":"INFO","service":"document-review-service","traceId":"abc123","spanId":"004","thread":"http-nio-8084-exec-2","logger":"com.documentplatform.documentreview.service.DecisionService","message":"Document approved","documentId":"doc-2001","reviewerId":"fin-approver-17"}
```

### Logback pattern configuration

```xml
<pattern>{"timestamp":"%d{yyyy-MM-dd HH:mm:ss}","level":"%p","service":"document-api-service","traceId":"%X{traceId}","spanId":"%X{spanId}","message":"%msg"}%n</pattern>
```

Token semantics:

| Token | Meaning |
|---|---|
| `%d{yyyy-MM-dd HH:mm:ss}` | Emission timestamp |
| `%p` | Log severity |
| `%X{traceId}` | Trace ID from MDC context |
| `%X{spanId}` | Span ID from MDC context |
| `%msg` | Application message |
| `%n` | Newline delimiter |

## 📈 Grafana
Why this matters: dashboards should answer operational questions directly, not force ad-hoc query archaeology during incidents.

Grafana deployment references:
1. Script: [k8s/scripts/deploy-grafana.sh](../k8s/scripts/deploy-grafana.sh)
2. ArgoCD app: [cicd/argocd/grafana-application.yaml](../cicd/argocd/grafana-application.yaml)

```mermaid
flowchart TB
    subgraph ROW1[Row 1]
      P11[Request Rate per service]
      P12[Error Rate per service]
      P13[Active Documents in PROCESSING state]
    end

    subgraph ROW2[Row 2]
      P21[Upload Duration p50 p95 p99]
      P22[Processing Duration p50 p95 p99]
      P23[SQS Queue Depth]
    end

    subgraph ROW3[Row 3]
      P31[Document State Transition Heatmap]
      P32[Review Decision Ratio]
      P33[Auth Login Success vs Failed]
    end

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class P11,P12,P13,P21,P22,P23,P31,P32,P33 backends;
```

### Panel PromQL

Note: queries that depend on planned metrics require the corresponding instrumentation to be added before panels will return data.

Request rate per service:
```promql
rate(http_server_requests_seconds_count{application=~"user-management-service|document-api-service|document-processing-service|document-review-service"}[5m])
```

Error rate per service:
```promql
rate(http_server_requests_seconds_count{application=~"user-management-service|document-api-service|document-processing-service|document-review-service",status=~"5.."}[5m])
```

Active documents in PROCESSING state:
```promql
sum(documents_state_total{state="PROCESSING"})
```

Upload duration p50:
```promql
histogram_quantile(0.50, rate(documents_upload_duration_seconds_bucket[5m]))
```

Upload duration p95:
```promql
histogram_quantile(0.95, rate(documents_upload_duration_seconds_bucket[5m]))
```

Upload duration p99:
```promql
histogram_quantile(0.99, rate(documents_upload_duration_seconds_bucket[5m]))
```

Processing duration p50:
```promql
histogram_quantile(0.50, rate(documents_processing_duration_seconds_bucket[5m]))
```

Processing duration p95:
```promql
histogram_quantile(0.95, rate(documents_processing_duration_seconds_bucket[5m]))
```

Processing duration p99:
```promql
histogram_quantile(0.99, rate(documents_processing_duration_seconds_bucket[5m]))
```

SQS queue depth:
```promql
aws_sqs_approximate_number_of_messages_visible_average{queue_name="document-ingestion-queue"}
```

Document state transition heatmap:
```promql
sum by (from_state, to_state) (increase(documents_state_transitions_total[15m]))
```

Review decision ratio (approved vs rejected):
```promql
sum(rate(documents_review_approved_total[5m])) / clamp_min(sum(rate(documents_review_rejected_total[5m])), 1)
```

Auth login success vs failed:
```promql
sum(rate(auth_login_success_total[5m]))
```

```promql
sum(rate(auth_login_failed_total[5m]))
```

## 🧪 Local Verification
Why this matters: an explicit runbook makes observability verification reproducible during onboarding and release validation.

```mermaid
flowchart LR
    S1[Start services] --> S2[Probe health/metrics/prometheus]
    S2 --> S3[Generate request and capture traceId]
    S3 --> S4[Validate Prometheus /targets]
    S4 --> S5[Search trace in Grafana]

    classDef services fill:#d9f7e8,stroke:#1f8b4c,color:#0b3d25,stroke-width:1.5px;
    classDef backends fill:#dbeafe,stroke:#2563eb,color:#1e3a8a,stroke-width:1.5px;
    classDef storage fill:#ffedd5,stroke:#ea580c,color:#7c2d12,stroke-width:1.5px;

    class S1 services;
    class S2,S3,S4 backends;
    class S5 storage;
```

1. Start services locally.
```bash
cd applications/user-management-service && docker compose up --build
cd applications/document-api-service && docker compose up --build
cd applications/document-processing-service && docker compose up --build
cd applications/document-review-service && docker compose up --build
```

2. Verify `/actuator/prometheus` on each service.
```bash
curl -s http://localhost:8081/actuator/prometheus | head -n 20
curl -s http://localhost:8082/actuator/prometheus | head -n 20
curl -s http://localhost:8083/actuator/prometheus | head -n 20
curl -s http://localhost:8084/actuator/prometheus | head -n 20
```
Expected output:
1. `# HELP` and `# TYPE` headers.
2. Metric sample lines with labels such as `application` and `environment`.

3. Verify `/actuator/health` and `/actuator/metrics` on each service.
```bash
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health
curl -s http://localhost:8083/actuator/health
curl -s http://localhost:8084/actuator/health

curl -s http://localhost:8081/actuator/metrics
curl -s http://localhost:8082/actuator/metrics
curl -s http://localhost:8083/actuator/metrics
curl -s http://localhost:8084/actuator/metrics
```
Expected output:
1. Health payload with status `UP`.
2. Metrics catalog containing custom business metric names.

4. Trigger an API call and verify trace correlation fields in logs.
```bash
curl -X POST http://localhost:8082/api/v1/documents/upload-request \
  -H "Authorization: Bearer REPLACE_WITH_VALID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"cust-1","documentType":"INVOICE","fileName":"invoice.pdf","contentType":"application/pdf","fileSize":1024}'
```
Expected output:
1. JSON log line containing `traceId` and `spanId`.

5. Confirm Prometheus scrape status.
1. Open Prometheus UI path: `/targets`.
2. Ensure each service target is `UP`.

6. Find the trace in Grafana.
1. Copy `traceId` from logs.
2. Open Grafana Explore and select Tempo datasource.
3. Search by the copied trace ID and inspect end-to-end spans.

## 🚨 Alerting Rules
Why this matters: objective SLO-aligned alerting catches failure patterns before user tickets and escalation noise.

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: document-platform-observability-alerts
  namespace: monitoring
spec:
  groups:
    - name: document-platform.observability
      rules:
        - alert: HighDocumentProcessingFailureRate
          expr: rate(documents_processing_failed_total[1m]) * 60 > 5
          for: 2m
          labels:
            severity: warning
          annotations:
            summary: "Document processing failure rate exceeded threshold"
            description: "documents.processing.failed is above 5 per minute for 2 minutes."

        - alert: DocumentUploadLatencyHigh
          expr: histogram_quantile(0.99, rate(documents_upload_duration_seconds_bucket[5m])) > 3
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "Upload p99 latency is high"
            description: "p99 upload duration is above 3 seconds for 5 minutes."

        - alert: AuthLoginFailureSurge
          expr: rate(auth_login_failed_total[1m]) * 60 > 20
          for: 1m
          labels:
            severity: warning
          annotations:
            summary: "Login failure surge detected"
            description: "auth.login.failed is above 20 per minute for 1 minute."
```

## Golden Signals at a Glance

| Golden Signal | Metric | Grafana Panel |
|---|---|---|
| Latency | documents.upload.duration, documents.processing.duration | Upload Duration p50/p95/p99, Processing Duration p50/p95/p99 |
| Traffic | http_server_requests_seconds_count, documents.upload.requested | Request Rate per service |
| Errors | documents.upload.failed, documents.processing.failed, auth.login.failed | Error Rate per service, Auth Login Success vs Failed |
| Saturation | aws_sqs_approximate_number_of_messages_visible_average, documents_state_total{state="PROCESSING"} | SQS Queue Depth, Active Documents in PROCESSING state |
