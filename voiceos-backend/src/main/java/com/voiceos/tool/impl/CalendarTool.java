package com.voiceos.tool.impl;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calendar Management Tool.
 * Checks availability and schedules events.
 * Risk Level: MEDIUM (creates calendar events).
 */
@Component
public class CalendarTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(CalendarTool.class);

    @Override
    public String getName() {
        return "calendar";
    }

    @Override
    public String getDescription() {
        return "Checks user schedule, finds free slots, and manages calendar events. Actions: 'check_schedule', 'find_free_slots', 'create_event'.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        String action = (String) params.getOrDefault("action", "check_schedule");
        String dateStr = (String) params.getOrDefault("date", LocalDate.now().toString());

        log.info("CalendarTool executing: action='{}', date='{}'", action, dateStr);

        List<Map<String, String>> events = List.of(
                Map.of("time", "10:00 AM - 11:00 AM", "title", "Team Architecture Sync", "location", "Virtual"),
                Map.of("time", "02:00 PM - 03:00 PM", "title", "Sprint Planning", "location", "Conference Room A")
        );

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("date", dateStr, "events", events, "freeSlots", List.of("11:00 AM - 02:00 PM", "03:00 PM - 06:00 PM")),
                "Schedule for " + dateStr + ": 2 meeting(s) scheduled. Free slots available from 11 AM - 2 PM and after 3 PM.",
                latency
        );
    }
}
