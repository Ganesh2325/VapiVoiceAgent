package com.voiceos.provider.whatsapp;

import com.voiceos.action.model.Action;
import com.voiceos.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppProvider implements ServiceProvider {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppProvider.class);

    @Override
    public String getName() {
        return "WhatsAppProvider";
    }

    @Override
    public String getDescription() {
        return "Official WhatsApp Business API integration.";
    }

    @Override
    public boolean supportsIntent(String intent) {
        return intent.toLowerCase().contains("whatsapp") || intent.toLowerCase().contains("message");
    }

    @Override
    public void execute(Action action) {
        log.info("WhatsAppProvider: Executing action {}", action.getId());
        
        // Mocking the official WhatsApp Business API
        String message = (String) action.getPayload().getOrDefault("message", "Default WhatsApp Notification.");
        log.info("WhatsAppProvider: Sending message: {}", message);
        
        action.setResult("Message successfully delivered to WhatsApp.");
    }
}
