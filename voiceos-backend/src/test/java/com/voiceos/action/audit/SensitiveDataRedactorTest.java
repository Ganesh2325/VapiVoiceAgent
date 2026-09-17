package com.voiceos.action.audit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveDataRedactorTest {

    @Test
    void redactsCredentialKeysAndJwtShapedValues() {
        Map<String, Object> redacted = SensitiveDataRedactor.redact(Map.of(
                "expression", "125 * 24",
                "password", "hunter2",
                "jwt", "aaa.bbb.ccc",
                "authorization", "Bearer abc.def.ghi"
        ));
        assertEquals("125 * 24", redacted.get("expression"));
        assertEquals("[REDACTED]", redacted.get("password"));
        assertEquals("[REDACTED]", redacted.get("jwt"));
        assertEquals("[REDACTED]", redacted.get("authorization"));
        assertFalse(redacted.values().stream().map(String::valueOf).anyMatch(v -> v.contains("hunter2")));
    }
}
