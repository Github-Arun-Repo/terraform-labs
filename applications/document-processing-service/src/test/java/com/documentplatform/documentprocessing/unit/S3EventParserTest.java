package com.documentplatform.documentprocessing.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.documentplatform.documentprocessing.service.S3EventParser;
import org.junit.jupiter.api.Test;

class S3EventParserTest {

    @Test
    void shouldParseSimplifiedEvent() {
        S3EventParser parser = new S3EventParser();
        var event = parser.parse("{\"bucket\":\"b\",\"key\":\"invoice/raw/customer-1/doc-1/file.pdf\"}");
        assertEquals("b", event.bucket());
        assertEquals("invoice/raw/customer-1/doc-1/file.pdf", event.key());
    }
}
