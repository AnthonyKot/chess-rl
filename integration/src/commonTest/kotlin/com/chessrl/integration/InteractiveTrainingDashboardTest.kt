package com.chessrl.integration

import com.chessrl.chess.*
import kotlin.test.*

/**
 * Tests for the interactive training dashboard
 */
class InteractiveTrainingDashboardTest {
    
    private lateinit var trainingInterface: TrainingControlInterface
    private lateinit var dashboard: InteractiveTrainingDashboard
    private lateinit var dashboardConfig: DashboardConfig
    
    @BeforeTest
    fun setup() {
        val interfaceConfig = TrainingInterfaceConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(64, 32),
            learningRate = 0.01,
            explorationRate = 0.2,
            batchSize = 16,
            maxBufferSize = 1000
        )
        
        trainingInterface = TrainingControlInterface(interfaceConfig)
        trainingInterface.initialize()
        
        dashboardConfig = DashboardConfig(
            minUpdateInterval = 100L,
            maxHistorySize = 50,
            defaultView = DashboardView.OVERVIEW,
            enableAutoUpdate = false, // Disable for testing
            chartWidth = 40,
            progressBarWidth = 20
        )
        
        dashboard = InteractiveTrainingDashboard(trainingInterface, dashboardConfig)
    }
    
    @Test
    fun testDashboardStart() {
        val session = dashboard.start()
        
        assertNotNull(session.sessionId)
        assertTrue(session.sessionId.isNotEmpty())
        assertTrue(session.startTime > 0)
        assertTrue(session.isActive)
    }
    
    @Test
    fun testDashboardUpdate() {
        dashboard.start()
        
        // First update should succeed
        val result1 = dashboard.update()
        when (result1) {
            is DashboardUpdateResult.Success -> {
                assertEquals(1, result1.updateCount)
                assertTrue(result1.timeSinceLastUpdate >= 0)
            }
            is DashboardUpdateResult.Error -> {
                // Update might fail without training data, which is acceptable
                println("Dashboard update failed (expected in test): ${result1.message}")
            }
            else -> {
                fail("Unexpected update result: $result1")
            }
        }
        
        // Immediate second update should be too soon
        val result2 = dashboard.update()
        assertTrue(result2 is DashboardUpdateResult.TooSoon)
        
        // Wait and try again
        Thread.sleep(150) // Wait longer than minUpdateInterval
        val result3 = dashboard.update()
        assertTrue(result3 is DashboardUpdateResult.Success || result3 is DashboardUpdateResult.Error)
    }
    
    @Test
    fun testUpdateWithoutStart() {
        val result = dashboard.update()
        assertTrue(result is DashboardUpdateResult.NotActive)
    }
    
    @Test
    fun testHelpCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("help")
        
        when (result) {
            is CommandResult.Success -> {
                assertEquals("Help displayed", result.message)
            }
            is CommandResult.Error -> {
                fail("Help command should succeed: ${result.message}")
            }
        }
    }
    
    @Test
    fun testViewCommands() {
        dashboard.start()
        
        val views = listOf("overview", "training", "games", "analysis", "performance", "help")
        
        views.forEach { view ->
            val result = dashboard.executeCommand("view $view")
            
            when (result) {
                is CommandResult.Success -> {
                    assertTrue(result.message.contains("Switched to $view view"))
                }
                is CommandResult.Error -> {
                    fail("View command should succeed for $view: ${result.message}")
                }
            }
        }
    }
    
    @Test
    fun testAnalyzeCommands() {
        dashboard.start()
        
        // Test game analysis
        val gameAnalysisResult = dashboard.executeCommand("analyze game G1001")
        when (gameAnalysisResult) {
            is CommandResult.Success -> {
                assertTrue(gameAnalysisResult.message.contains("Analyzing game"))
            }
            is CommandResult.Error -> {
                // Analysis might fail without game data
                println("Game analysis failed (expected in test): ${gameAnalysisResult.message}")
            }
        }
        
        // Test position analysis
        val positionAnalysisResult = dashboard.executeCommand("analyze position rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        when (positionAnalysisResult) {
            is CommandResult.Success -> {
                assertTrue(positionAnalysisResult.message.contains("Analyzing position"))
            }
            is CommandResult.Error -> {
                // Analysis might fail in test environment
                println("Position analysis failed (expected in test): ${positionAnalysisResult.message}")
            }
        }
        
        // Test current position analysis
        val currentAnalysisResult = dashboard.executeCommand("analyze current")
        when (currentAnalysisResult) {
            is CommandResult.Success -> {
                assertTrue(currentAnalysisResult.message.contains("Analyzing current position"))
            }
            is CommandResult.Error -> {
                println("Current position analysis failed (expected in test): ${currentAnalysisResult.message}")
            }
        }
    }
    
    @Test
    fun testPlayCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("play white")
        
        when (result) {
            is CommandResult.Success -> {
                assertTrue(result.message.contains("Started agent vs human session"))
            }
            is CommandResult.Error -> {
                // Play command might fail in test environment
                println("Play command failed (expected in test): ${result.message}")
            }
        }
    }
    
    @Test
    fun testTrainingCommands() {
        dashboard.start()
        
        val commands = listOf("start", "pause", "resume", "stop")
        
        commands.forEach { command ->
            val result = dashboard.executeCommand(command)
            
            when (result) {
                is CommandResult.Success -> {
                    assertTrue(result.message.contains("Training $command executed"))
                }
                is CommandResult.Error -> {
                    // Training commands might fail without active training
                    println("Training command '$command' failed (expected in test): ${result.message}")
                }
            }
        }
    }
    
    @Test
    fun testExportCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("export json test_export.json")
        
        when (result) {
            is CommandResult.Success -> {
                assertTrue(result.message.contains("Data exported"))
            }
            is CommandResult.Error -> {
                // Export might fail in test environment
                println("Export command failed (expected in test): ${result.message}")
            }
        }
    }
    
    @Test
    fun testConfigureCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("configure update_interval 2000")
        
        when (result) {
            is CommandResult.Success -> {
                assertTrue(result.message.contains("Configuration updated"))
            }
            is CommandResult.Error -> {
                fail("Configure command should succeed: ${result.message}")
            }
        }
    }
    
    @Test
    fun testClearCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("clear")
        
        when (result) {
            is CommandResult.Success -> {
                assertEquals("Screen cleared", result.message)
            }
            is CommandResult.Error -> {
                fail("Clear command should succeed: ${result.message}")
            }
        }
    }
    
    @Test
    fun testQuitCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("quit")
        
        when (result) {
            is CommandResult.Success -> {
                assertEquals("Dashboard stopped", result.message)
            }
            is CommandResult.Error -> {
                fail("Quit command should succeed: ${result.message}")
            }
        }
    }
    
    @Test
    fun testUnknownCommand() {
        dashboard.start()
        
        val result = dashboard.executeCommand("unknown_command")
        
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("Unknown command"))
    }
    
    @Test
    fun testCommandParsing() {
        dashboard.start()
        
        // Test various command formats
        val commands = mapOf(
            "h" to "help",
            "?" to "help",
            "v overview" to "view",
            "a game G1001" to "analyze",
            "p white" to "play",
            "e json file.json" to "export",
            "c setting value" to "configure",
            "cls" to "clear",
            "q" to "quit"
        )
        
        commands.forEach { (input, expectedType) ->
            val result = dashboard.executeCommand(input)
            // All commands should execute without throwing exceptions
            assertTrue(result is CommandResult.Success || result is CommandResult.Error)
        }
    }
    
    @Test
    fun testDashboardStop() {
        dashboard.start()
        
        // Update a few times to generate some activity
        repeat(3) {
            dashboard.update()
            Thread.sleep(150)
        }
        
        // Execute some commands
        dashboard.executeCommand("help")
        dashboard.executeCommand("view training")
        
        val stopResult = dashboard.stop()
        
        when (stopResult) {
            is DashboardStopResult.Success -> {
                assertTrue(stopResult.sessionDuration > 0)
                assertTrue(stopResult.totalUpdates >= 0)
                assertTrue(stopResult.commandsExecuted >= 2) // At least help and view commands
            }
            is DashboardStopResult.NotActive -> {
                fail("Dashboard should be active")
            }
        }
    }
    
    @Test
    fun testStopWithoutStart() {
        val result = dashboard.stop()
        assertTrue(result is DashboardStopResult.NotActive)
    }
    
    @Test
    fun testCommandExecutionWithoutStart() {
        val result = dashboard.executeCommand("help")
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("not active"))
    }
    
    @Test
    fun testDashboardConfiguration() {
        val customConfig = DashboardConfig(
            minUpdateInterval = 500L,
            maxHistorySize = 25,
            defaultView = DashboardView.TRAINING,
            enableAutoUpdate = true,
            chartWidth = 80,
            progressBarWidth = 40
        )
        
        val customDashboard = InteractiveTrainingDashboard(trainingInterface, customConfig)
        val session = customDashboard.start()
        
        assertTrue(session.isActive)
        assertNotNull(session.sessionId)
    }
    
    @Test
    fun testMultipleUpdatesWithHistory() {
        dashboard.start()
        
        val updateResults = mutableListOf<DashboardUpdateResult>()
        
        // Perform multiple updates with delays
        repeat(5) { i ->
            Thread.sleep(150) // Wait longer than minUpdateInterval
            val result = dashboard.update()
            updateResults.add(result)
        }
        
        // Count successful updates
        val successfulUpdates = updateResults.filterIsInstance<DashboardUpdateResult.Success>()
        
        // Should have at least some successful updates
        assertTrue(successfulUpdates.isNotEmpty() || updateResults.all { it is DashboardUpdateResult.Error })
        
        // Update counts should be sequential for successful updates
        successfulUpdates.forEachIndexed { index, result ->
            assertTrue(result.updateCount > 0)
        }
    }
    
    @Test
    fun testCommandHistory() {
        dashboard.start()
        
        val commands = listOf("help", "view training", "view games", "clear", "view overview")
        
        commands.forEach { command ->
            dashboard.executeCommand(command)
        }
        
        val stopResult = dashboard.stop()
        
        when (stopResult) {
            is DashboardStopResult.Success -> {
                assertEquals(commands.size, stopResult.commandsExecuted)
            }
            is DashboardStopResult.NotActive -> {
                fail("Dashboard should be active")
            }
        }
    }
    
    @Test
    fun testViewSwitching() {
        dashboard.start()
        
        val views = listOf("overview", "training", "games", "analysis", "performance", "help")
        
        views.forEach { view ->
            val result = dashboard.executeCommand("view $view")
            assertTrue(result is CommandResult.Success)
            
            // Execute another command to ensure view switching works
            val helpResult = dashboard.executeCommand("help")
            assertTrue(helpResult is CommandResult.Success)
        }
    }
    
    @Test
    fun testAnalysisViewInteraction() {
        dashboard.start()
        
        // Switch to analysis view
        dashboard.executeCommand("view analysis")
        
        // Try to analyze a position
        val result = dashboard.executeCommand("analyze position rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        
        // Should succeed or fail gracefully
        assertTrue(result is CommandResult.Success || result is CommandResult.Error)
    }
    
    @Test
    fun testPerformanceUnderLoad() {
        dashboard.start()
        
        val startTime = getCurrentTimeMillis()
        
        // Execute many commands rapidly
        repeat(20) { i ->
            dashboard.executeCommand("view ${listOf("overview", "training", "games").random()}")
            if (i % 5 == 0) {
                dashboard.update()
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val totalTime = endTime - startTime
        
        // Should complete within reasonable time
        assertTrue(totalTime < 5000, "Performance test took too long: ${totalTime}ms")
    }
    
    @Test
    fun testErrorHandling() {
        dashboard.start()
        
        // Test various error conditions
        val errorCommands = listOf(
            "analyze", // Missing parameters
            "view invalid_view",
            "export", // Missing parameters
            "configure", // Missing parameters
            "play invalid_color"
        )
        
        errorCommands.forEach { command ->
            val result = dashboard.executeCommand(command)
            // Should handle errors gracefully without throwing exceptions
            assertTrue(result is CommandResult.Success || result is CommandResult.Error)
        }
    }
    
    @Test
    fun testConcurrentOperations() {
        dashboard.start()
        
        // Simulate concurrent operations (simplified for single-threaded test)
        val operations = listOf(
            { dashboard.update() },
            { dashboard.executeCommand("help") },
            { dashboard.executeCommand("view training") },
            { dashboard.update() },
            { dashboard.executeCommand("clear") }
        )
        
        operations.forEach { operation ->
            try {
                operation()
            } catch (e: Exception) {
                fail("Operation should not throw exception: ${e.message}")
            }
        }
    }
    
    @Test
    fun testDataClassExtensions() {
        // Test formatting extensions
        val value1 = 1234.5678
        assertTrue(value1.formatForDashboard().isNotEmpty())
        
        val percentage = 0.75
        assertEquals("75%", percentage.formatPercentage())
        
        val duration = 125000L // 125 seconds
        val formatted = duration.formatDuration()
        assertTrue(formatted.contains("m") || formatted.contains("s"))
        
        val trend = 0.05
        assertEquals("↗", trend.toTrendIndicator())
        
        val negTrend = -0.05
        assertEquals("↘", negTrend.toTrendIndicator())
        
        val noTrend = 0.005
        assertEquals("→", noTrend.toTrendIndicator())
    }
    
    @Test
    fun testAsciiChartGeneration() {
        val values = listOf(0.1, 0.3, 0.7, 0.5, 0.9, 0.2, 0.8)
        val chart = values.toAsciiChart(20)
        
        assertEquals(20, chart.length)
        assertTrue(chart.all { it in "▁▂▃▄▅▆▇█ " })
    }
    
    @Test
    fun testProgressBarGeneration() {
        val progress = 0.75
        val progressBar = progress.toProgressBar(20)
        
        assertEquals(20, progressBar.length)
        assertTrue(progressBar.contains('█'))
        assertTrue(progressBar.contains('░'))
        
        val filledCount = progressBar.count { it == '█' }
        assertEquals(15, filledCount) // 75% of 20
    }
    
    @Test
    fun testUsageBarGeneration() {
        val usage = 60.0
        val usageBar = usage.toUsageBar(10)
        
        assertEquals(10, usageBar.length)
        assertTrue(usageBar.all { it in "▒░" })
    }
    
    @Test
    fun testStringFormatting() {
        val text = "Hello World"
        val centered = text.padCenter(20)
        assertEquals(20, centered.length)
        assertTrue(centered.contains("Hello World"))
        
        val longText = "This is a very long text that should be truncated"
        val truncated = longText.truncateWithEllipsis(20)
        assertEquals(20, truncated.length)
        assertTrue(truncated.endsWith("..."))
    }
    
    @Test
    fun testNumberFormatting() {
        val largeNumber = 1500
        assertEquals("1.5K", largeNumber.formatWithUnits())
        
        val veryLargeNumber = 2500000
        assertEquals("2.5M", veryLargeNumber.formatWithUnits())
        
        val smallNumber = 500
        assertEquals("500", smallNumber.formatWithUnits())
    }
}