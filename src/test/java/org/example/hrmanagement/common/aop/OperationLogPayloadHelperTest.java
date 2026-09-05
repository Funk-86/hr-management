package org.example.hrmanagement.common.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogPayloadHelperTest {

    private OperationLogPayloadHelper helper;

    @BeforeEach
    void setUp() {
        helper = new OperationLogPayloadHelper(new ObjectMapper());
    }

    @Test
    void buildRequestInfoMasksSensitiveFields() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/employees");
        request.setContentType("application/json");
        request.setParameter("token", "secret-token");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Alice");
        body.put("password", "PlainPass!");
        body.put("mfaCode", "123456");

        String json = helper.buildRequestInfo(request, new Object[] { body });
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("***"));
        assertFalse(json.contains("PlainPass!"));
        assertFalse(json.contains("123456"));
        assertFalse(json.contains("secret-token"));
        assertTrue(json.contains("contentType"));
    }

    @Test
    void multipartOnlyKeepsFileMeta() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "contract.pdf", "application/pdf", new byte[] { 1, 2, 3, 4 });
        String json = helper.buildRequestInfo(null, new Object[] { file });
        assertTrue(json.contains("contract.pdf"));
        assertTrue(json.contains("\"size\":4"));
        assertFalse(json.contains("\\u0001"));
    }

    @Test
    void responseInfoTruncatesLongPayload() {
        // include spaces so it is not treated as large base64 and omitted
        String longText = ("msg ").repeat(OperationLogPayloadHelper.BODY_MAX / 2);
        Map<String, Object> result = Map.of("data", longText);
        String json = helper.buildResponseInfo(result);
        assertTrue(json.contains("truncated"));
        assertTrue(json.length() < OperationLogPayloadHelper.BODY_MAX + 80);
    }

    @Test
    void errorResponseContainsTypeAndMessage() {
        String json = helper.buildErrorResponseInfo(new IllegalStateException("boom"));
        assertTrue(json.contains("IllegalStateException"));
        assertTrue(json.contains("boom"));
    }
}
