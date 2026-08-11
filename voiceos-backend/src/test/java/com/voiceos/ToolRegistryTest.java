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
        calculatorTool = new CalculatorTool();
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
    void testUnknownToolHandling() {
        ToolResult result = toolRegistry.executeTool("non_existent_tool", Map.of());
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Unknown tool"));
    }
}
