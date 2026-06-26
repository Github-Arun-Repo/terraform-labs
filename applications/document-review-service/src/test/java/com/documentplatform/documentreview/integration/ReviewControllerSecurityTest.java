package com.documentplatform.documentreview.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.documentplatform.documentreview.DocumentReviewApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = DocumentReviewApplication.class)
@AutoConfigureMockMvc
class ReviewControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectWhenNoToken() throws Exception {
        mockMvc.perform(get("/api/review/queue"))
                .andExpect(status().isForbidden());
    }
}
