package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);
    private final ResultVerifier resultVerifier;
    private final Map<String, ServiceProvider> providerRegistry = new ConcurrentHashMap<>();

    public ActionExecutor(ResultVerifier resultVerifier, List<ServiceProvider> providers) {
        this.resultVerifier = resultVerifier;
        for (ServiceProvider provider : providers) {
            providerRegistry.put(provider.getName(), provider);
        }
    }

    public void execute(Action action) {
        log.info("ActionExecutor: Executing action {} through provider {}", action.getId(), action.getTargetProvider());
        
        ServiceProvider provider = providerRegistry.get(action.getTargetProvider());
        if (provider != null) {
            provider.execute(action);
        } else {
            action.setResult("Fallback execution via generic mock executor.");
        }
        
        action.setStatus(ActionStatus.VERIFYING);
        
        resultVerifier.verify(action);
    }
}
