package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ResultVerifier {
    private static final Logger log = LoggerFactory.getLogger(ResultVerifier.class);

    public void verify(Action action) {
        log.info("ResultVerifier: Verifying outcome of action {}", action.getId());
        
        // In reality, this might poll external APIs or check DB state
        boolean success = action.getResult() != null && !action.getResult().isEmpty();
        
        if (success) {
            action.setStatus(ActionStatus.COMPLETED);
            action.setCompletedAt(Instant.now());
            log.info("ResultVerifier: Action {} verified as COMPLETED.", action.getId());
        } else {
            action.setStatus(ActionStatus.FAILED);
            action.setCompletedAt(Instant.now());
            log.error("ResultVerifier: Action {} verified as FAILED.", action.getId());
        }
    }
}
