package com.voiceos.provider;

import com.voiceos.action.model.Action;

/**
 * Base abstraction for all external integrations.
 */
public interface ServiceProvider {
    String getName();
    String getDescription();
    boolean supportsIntent(String intent);
    
    /**
     * Executes the requested action after all policies and approvals are met.
     */
    void execute(Action action);
}
