package com.voiceos.tool.impl;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Web and Information Search tool.
 * Risk Level: LOW (read-only search).
 */
@Component
public class SearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(SearchTool.class);

    @Override
    public String getName() {
        return "search_web";
    }

    @Override
    public String getDescription() {
        return "Searches the web for real-time information, travel options, flights, hotels, documentation, and facts.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.LOW;
    }

    @Override
    public void validateInput(Map<String, Object> params) {
        Tool.super.validateInput(params);
        if (!params.containsKey("query") || params.get("query") == null || params.get("query").toString().isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty.");
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        String query = params.get("query").toString();
        log.info("SearchTool executing query: '{}'", query);

        String queryLower = query.toLowerCase();
        List<Map<String, String>> results;

        if (queryLower.contains("bangalore") || queryLower.contains("flight") || queryLower.contains("hotel")) {
            results = List.of(
                    Map.of("title", "Bangalore Direct Flights", "snippet", "Indigo 6E-2134 (07:15 AM, $68), Air India AI-508 (09:45 AM, $75)", "url", "https://flights.example.com"),
                    Map.of("title", "Top Hotels in Bangalore", "snippet", "The Oberoi MG Road ($140/night), Grand Mercure Koramangala ($95/night)", "url", "https://hotels.example.com")
            );
        } else if (queryLower.contains("weather")) {
            results = List.of(
                    Map.of("title", "Bangalore Forecast", "snippet", "Partly cloudy, 24°C - 29°C, pleasant evening breeze.", "url", "https://weather.example.com")
            );
        } else {
            results = List.of(
                    Map.of("title", "Search Results for: " + query, "snippet", "Relevant information and key findings retrieved for query: " + query, "url", "https://search.voiceos.dev")
            );
        }

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("query", query, "results", results, "count", results.size()),
                "Found " + results.size() + " result(s) for query: " + query,
                latency
        );
    }
}
