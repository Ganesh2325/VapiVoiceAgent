package com.voiceos;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import com.voiceos.tool.impl.CalculatorTool;
import com.voiceos.tool.impl.SearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    private ToolRegistry toolRegistry;
    private CalculatorTool calculatorTool;
    private SearchTool searchTool;

    @BeforeEach
    void setUp() {
        calculatorTool = new CalculatorTool(new com.voiceos.provider.calculator.RealLocalCalculatorProvider());
        searchTool = new SearchTool();
        toolRegistry = new ToolRegistry(List.of(calculatorTool, searchTool));
    }

    @Test
    void testToolDiscovery() {
        assertEquals(2, toolRegistry.getAllTools().size());
        assertTrue(toolRegistry.getTool("calculator").isPresent());
        assertTrue(toolRegistry.getTool("search_web").isPresent());
    }

    @Test
    void testCalculatorExecution() {
        ToolResult result = toolRegistry.executeTool("calculator", Map.of(
                "expression", "100 + 50 + 25",
                "operation", "sum"
        ));

        assertTrue(result.success());
        assertEquals("175.00", result.rawOutput());
    }

    @Test
    void testCalculatorMultiplication() {
        ToolResult result = toolRegistry.executeTool("calculator", Map.of(
                "expression", "25 * 4"
        ));
        assertTrue(result.success());
        assertEquals("100.00", result.rawOutput());
    }

    @Test
    void testCalculatorMissingExpressionFailsValidation() {
        ToolResult result = toolRegistry.executeTool("calculator", Map.of());
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Invalid tool input"));
    }

    @Test
    void testCalculatorNonNumericExpressionFailsHonestly() {
        ToolResult result = toolRegistry.executeTool("calculator", Map.of(
                "expression", "not-a-number"
        ));
        assertFalse(result.success());
        assertTrue(result.errorMessage() != null && result.errorMessage().contains("INVALID_REQUEST"));
        assertNotEquals("0.00", result.rawOutput());
    }

    @Test
    void testSearchExecution() {
        ToolResult result = toolRegistry.executeTool("search_web", Map.of(
                "query", "Bangalore flights and hotels"
        ));

        assertTrue(result.success());
        assertNotNull(result.data().get("results"));
        assertTrue(result.rawOutput().contains("Found"));
    }

    @Test
    void testRiskClassification() {
        assertEquals(Tool.ToolRiskLevel.LOW, calculatorTool.getRiskLevel());
        assertFalse(calculatorTool.getRiskLevel().requiresHumanApproval());
    }

    @Test
    void timeoutReportedDurationIsFailure() {
        Tool slow = new Tool() {
            @Override public String getName() { return "slow_probe"; }
            @Override public String getDescription() { return "timeout probe"; }
            @Override public ToolRiskLevel getRiskLevel() { return ToolRiskLevel.LOW; }
            @Override public long getTimeoutMs() { return 5; }
            @Override public int getMaxRetries() { return 1; }
            @Override public ToolResult execute(Map<String, Object> params) {
                return ToolResult.success("should-not-count", 50);
            }
        };
        ToolRegistry registry = new ToolRegistry(List.of(slow));
        ToolResult result = registry.executeTool("slow_probe", Map.of());
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("TIMEOUT"));
    }

    @Test
    void testUnknownToolHandling() {
        ToolResult result = toolRegistry.executeTool("non_existent_tool", Map.of());
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Unknown tool"));
    }
}
