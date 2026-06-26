package com.documentplatform.documentapi.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.documentplatform.documentapi.exception.BadRequestException;
import com.documentplatform.documentapi.util.FileNameSanitizer;
import org.junit.jupiter.api.Test;

class FileNameSanitizerTest {

    private final FileNameSanitizer sanitizer = new FileNameSanitizer();

    @Test
    void shouldSanitizeValidName() {
        assertEquals("invoice-1001.pdf", sanitizer.sanitize("invoice 1001.pdf"));
    }

    @Test
    void shouldRejectPathTraversal() {
        assertThrows(BadRequestException.class, () -> sanitizer.sanitize("../secret.txt"));
    }
}
