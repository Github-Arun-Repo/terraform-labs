package com.documentplatform.documentapi.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class DocumentMetricsService {

    private final Counter uploadRequestsTotal;
    private final Counter uploadRequestsFailedTotal;
    private final Counter viewUrlGeneratedTotal;
    private final Timer uploadDuration;

    public DocumentMetricsService(MeterRegistry meterRegistry) {
        this.uploadRequestsTotal = meterRegistry.counter("documents_upload_requests_total");
        this.uploadRequestsFailedTotal = meterRegistry.counter("documents_upload_requests_failed_total");
        this.viewUrlGeneratedTotal = meterRegistry.counter("documents_view_url_generated_total");
        this.uploadDuration = meterRegistry.timer("documents_upload_request_duration_seconds");
    }

    public Counter uploadRequestsTotal() {
        return uploadRequestsTotal;
    }

    public Counter uploadRequestsFailedTotal() {
        return uploadRequestsFailedTotal;
    }

    public Counter viewUrlGeneratedTotal() {
        return viewUrlGeneratedTotal;
    }

    public Timer uploadDuration() {
        return uploadDuration;
    }
}
