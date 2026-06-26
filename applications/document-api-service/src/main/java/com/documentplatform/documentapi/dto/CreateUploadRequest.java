package com.documentplatform.documentapi.dto;

import com.documentplatform.documentapi.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUploadRequest {

    @NotBlank
    private String customerId;

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    @NotNull
    @Positive
    private Long fileSize;
}
