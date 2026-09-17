package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.provider.ProviderRegistry;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResult;
import com.voiceos.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Legacy pause/resume executor. Live HTTP/Vapi execution goes through ActionEngine → Tool → Provider.
 * This adapter now uses {@link ProviderRegistry} and honest {@link ProviderResult} values.
 */
@Service
public class ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);
    private final ResultVerifier resultVerifier;
    private final ProviderRegistry providerRegistry;

    public ActionExecutor(ResultVerifier resultVerifier, ProviderRegistry providerRegistry) {
        this.resultVerifier = resultVerifier;
        this.providerRegistry = providerRegistry;
    }

    public void execute(Action action) {
        log.info("ActionExecutor: Executing action {} through provider {}", action.getId(), action.getTargetProvider());

        ServiceProvider provider = providerRegistry.findByName(action.getTargetProvider()).orElse(null);
        if (provider == null) {
            action.setErrorMessage("No provider registered for " + action.getTargetProvider());
            action.getPayload().put("toolSuccess", false);
        } else {
            action.setProviderMode(provider.getMode().name());
            action.setTargetProvider(provider.getName());
            ProviderRequest request = new ProviderRequest(
                    action.getActionType() != null ? action.getActionType() : action.getIntent(),
                    action.getPayload(),
                    null
            );
            ProviderResult result = provider.execute(request);
            action.setProviderMode(result.mode().name());
            if (result.success()) {
                action.setResult(result.message());
            } else {
                action.setResult(null);
                action.setErrorMessage(result.message());
                action.getPayload().put("toolSuccess", false);
                if (result.errorCode() != null) {
                    action.getPayload().put("providerErrorCode", result.errorCode().name());
                }
            }
        }

        action.setStatus(ActionStatus.VERIFYING);
        resultVerifier.verify(action);
    }
}
