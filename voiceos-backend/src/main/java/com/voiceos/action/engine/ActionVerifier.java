package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.tool.core.ToolResult;

/**
 * Verifies that a provider/tool outcome is actually successful before COMPLETED.
 */
public interface ActionVerifier {

    VerificationResult verify(Action action, ToolResult toolResult);

    record VerificationResult(boolean passed, String reason) {
        public static VerificationResult ok() {
            return new VerificationResult(true, "verified");
        }

        public static VerificationResult failed(String reason) {
            return new VerificationResult(false, reason);
        }
    }
}
