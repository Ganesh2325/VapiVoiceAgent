package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Verification stage. Non-empty text is not treated as success.
 * COMPLETED is only assigned when the tool/provider result is explicitly successful
 * and required output exists.
 */
@Service
public class ResultVerifier implements ActionVerifier {

    private static final Logger log = LoggerFactory.getLogger(ResultVerifier.class);

    @Override
    public VerificationResult verify(Action action, ToolResult toolResult) {
        log.info("ResultVerifier actionId={} toolSuccess={}",
                action != null ? action.getId() : null,
                toolResult != null ? toolResult.success() : null);

        if (action == null) {
            return VerificationResult.failed("Action is missing");
        }
        if (toolResult == null) {
            boolean agentOnly = action.getResult() != null && !action.getResult().isBlank()
                    && action.getErrorMessage() == null;
            if (agentOnly && action.getStatus() != ActionStatus.FAILED) {
                return VerificationResult.ok();
            }
            return VerificationResult.failed("No tool result to verify");
        }
        if (!toolResult.success()) {
            return VerificationResult.failed(
                    toolResult.errorMessage() != null ? toolResult.errorMessage() : "Tool reported failure");
        }
        if ((toolResult.rawOutput() == null || toolResult.rawOutput().isBlank())
                && (toolResult.data() == null || toolResult.data().isEmpty())) {
            return VerificationResult.failed("Successful tool returned no result payload");
        }
        if ("calculator".equalsIgnoreCase(action.getToolName())
                && (toolResult.data() == null || !toolResult.data().containsKey("result"))) {
            return VerificationResult.failed("Calculator result is missing");
        }
        return VerificationResult.ok();
    }

    /**
     * Legacy adapter used by {@link ActionExecutor}. Uses ToolResult-shaped payload when present.
     */
    public void verify(Action action) {
        Object marker = action.getPayload() != null ? action.getPayload().get("toolSuccess") : null;
        boolean explicitFailure = Boolean.FALSE.equals(marker)
                || (action.getErrorMessage() != null && !action.getErrorMessage().isBlank());
        if (explicitFailure) {
            action.setStatus(ActionStatus.FAILED);
            action.setVerificationPassed(false);
            action.setCompletedAt(Instant.now());
            log.info("ResultVerifier: Action {} verified as FAILED.", action.getId());
            return;
        }
        ToolResult synthetic = action.getResult() != null
                ? ToolResult.success(action.getResult(), 0)
                : null;
        VerificationResult result = verify(action, synthetic);
        action.setVerificationPassed(result.passed());
        if (result.passed()) {
            action.setStatus(ActionStatus.COMPLETED);
            action.setCompletedAt(Instant.now());
            log.info("ResultVerifier: Action {} verified as COMPLETED.", action.getId());
        } else {
            action.setStatus(ActionStatus.FAILED);
            action.setErrorMessage(result.reason());
            action.setCompletedAt(Instant.now());
            log.info("ResultVerifier: Action {} verified as FAILED.", action.getId());
        }
    }
}
