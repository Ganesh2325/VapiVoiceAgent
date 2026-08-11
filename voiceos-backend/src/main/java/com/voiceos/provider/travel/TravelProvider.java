package com.voiceos.provider.travel;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.provider.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TravelProvider implements ServiceProvider {
    private static final Logger log = LoggerFactory.getLogger(TravelProvider.class);

    @Override
    public String getName() {
        return "TravelProvider";
    }

    @Override
    public String getDescription() {
        return "Official Travel Provider Mock for flight/hotel bookings.";
    }

    @Override
    public boolean supportsIntent(String intent) {
        return intent.toLowerCase().contains("travel") || intent.toLowerCase().contains("flight") || intent.toLowerCase().contains("hotel");
    }

    @Override
    public void execute(Action action) {
        log.info("TravelProvider: Processing action {}", action.getId());
        
        // Mock flight search
        if (action.getIntent().toLowerCase().contains("search")) {
            action.setResult("Found 3 flights matching your criteria.");
            return;
        }

        // Mock flight booking
        if (action.getIntent().toLowerCase().contains("book")) {
            // Verify payment was completed
            if (action.getPayload().containsKey("paymentCompleted") && (Boolean) action.getPayload().get("paymentCompleted")) {
                action.setResult("Booking confirmed. Ticket XYZ-123 generated.");
                log.info("TravelProvider: Booking confirmed successfully.");
            } else {
                action.setStatus(ActionStatus.REQUIRES_PAYMENT);
                log.warn("TravelProvider: Missing payment. Cannot book.");
            }
        }
    }
}
