package com.terraformlabs.documentprocessor.controller;

import com.terraformlabs.documentprocessor.dto.UploadConstraintsResponse;
import com.terraformlabs.documentprocessor.dto.UploadInvoiceResponse;
import com.terraformlabs.documentprocessor.service.InvoiceUploadService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
@Validated
public class InvoiceController {

    private final InvoiceUploadService invoiceUploadService;

    public InvoiceController(InvoiceUploadService invoiceUploadService) {
        this.invoiceUploadService = invoiceUploadService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadInvoiceResponse uploadInvoice(
            @RequestParam("customerId")
            @Pattern(
                    regexp = "^[a-zA-Z0-9_-]{3,64}$",
                    message = "customerId must be 3-64 chars with letters, numbers, '_' or '-'"
            ) String customerId,
            @RequestParam("file") MultipartFile file
    ) {
        return invoiceUploadService.uploadInvoice(customerId, file);
    }

    @GetMapping("/constraints")
    public UploadConstraintsResponse getUploadConstraints() {
        return invoiceUploadService.getConstraints();
    }
}
