package com.chessrl.integration

/**
 * Interactive Visualization CLI with real-time dashboard
 * 
 * Features:
 * - Real-time training dashboard with live updates
 * - Interactive game analysis with ASCII board display
 * - Learning curve visualization with trend analysis
 * - Performance monitoring with resource utilization
 * - Keyboard controls for navigation and interaction
 */
object InteractiveVisualizationCLI {
    
    private var isRunning = false
    private var currentMode = DisplayMode.DASHBOARD
    private var visualizationSystem: TrainingVisualizationSystem? = null
    private var trainingController: AdvancedTrainingController? = null
    private var refreshRate = 1000L // 1 second
    
    /**
     * Main entry point for interactive visualization
     */
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            startInteractiveVisualization()
            return
        }
        
        when (args[0].lowercase()) {
            "dashboard" -> startDashboard(args.drop(1))
            "monitor" -> startMonitoring(args.drop(1))
            "analyze" -> startAnalysis(args.drop(1))
            "demo" -> startDemo()
            "help" -> printUsage()
            else -> {
                println("❌ Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }
    
    /**
     * Start interactive visualization with full dashboard
     */
    private fun startInteractiveVisualization() {
        println("🎨 Starting Interactive Training Visualization")
        println("=" * 60)
        
        try {
            // Initialize components
            trainingController = AdvancedTrainingController()
            visualizationSystem = TrainingVisualizationSystem()
            
            // Start visualization system
            val result = visualizationSystem!!.startVisualization(trainingController!!)
            when (result) {
                is VisualizationResult.Success -> {
                    println("✅ ${result.message}")
                    runInteractiveLoop()
                }
                is VisualizationResult.Error -> {
                    println("❌ ${result.message}")
                    return
                }
            }
            
        } catch (e: Exception) {
            println("❌ Failed to start visualization: ${e.message}")
        } finally {
            cleanup()
        }
    }
    
    /**
     * Main interactive loop with keyboard controls
     */
    private fun runInteractiveLoop() {
        isRunning = true
        
        println("\n🎮 Interactive Visualization Started")
        println("Controls: [D]ashboard | [M]onitor | [A]nalyze | [R]efresh | [Q]uit | [H]elp")
        println("Press Enter after each command...")
        
        // Start background refresh thread (simulated)
        startBackgroundRefresh()
        
        while (isRunning) {
            // Display current view
            displayCurrentView()
            
            // Get user input
            print("\nvisualization> ")
            val input = readLine()?.trim()?.lowercase() ?: continue
            
            when (input) {
                "d", "dashboard" -> {
                    currentMode = DisplayMode.DASHBOARD
                    println("📊 Switched to Dashboard view")
                }
                "m", "monitor" -> {
                    currentMode = DisplayMode.PERFORMANCE_MONITOR
                    println("⚡ Switched to Performance Monitor")
                }
                "a", "analyze" -> {
                    currentMode = DisplayMode.LEARNING_ANALYSIS
                    println("📈 Switched to Learning Analysis")
                }
                "g", "game" -> {
                    currentMode = DisplayMode.GAME_ANALYSIS
                    println("♟️ Switched to Game Analysis")
                }
                "r", "refresh" -> {
                    refreshDisplay()
                    println("🔄 Display refreshed")
                }
                "f", "faster" -> {
                    refreshRate = maxOf(500L, refreshRate - 500L)
                    println("⚡ Refresh rate increased to ${refreshRate}ms")
                }
                "s", "slower" -> {
                    refreshRate = minOf(5000L, refreshRate + 500L)
                    println("🐌 Refresh rate decreased to ${refreshRate}ms")
                }
                "c", "clear" -> {
                    clearScreen()
                    println("🧹 Screen cleared")
                }
                "h", "help" -> {
                    printInteractiveHelp()
                }
                "q", "quit", "exit" -> {
                    isRunning = false
                    println("👋 Exiting visualization...")
                }
                "" -> {
                    // Just refresh on empty input
                    refreshDisplay()
                }
                else -> {
                    println("❌ Unknown command: $input. Type 'h' for help.")
                }
            }
        }
    }
    
    /**
     * Display current view based on mode
     */
    private fun displayCurrentView() {
        clearScreen()
        
        when (currentMode) {
            DisplayMode.DASHBOARD -> displayDashboard()
            DisplayMode.PERFORMANCE_MONITOR -> displayPerformanceMonitor()
            DisplayMode.LEARNING_ANALYSIS -> displayLearningAnalysis()
            DisplayMode.GAME_ANALYSIS -> displayGameAnalysis()
        }
    }
    
    /**
     * Display main training dashboard
     */
    private fun displayDashboard() {
        val dashboard = visualizationSystem?.renderDashboard() ?: "Visualization not available"
        println(dashboard)
        
        // Additional status information
        println("\n📊 Dashboard Status:")
        println("Mode: Dashboard | Refresh: ${refreshRate}ms | Time: ${formatCurrentTime()}")
    }
    
    /**
     * Display performance monitoring view
     */
    private fun displayPerformanceMonitor() {
        val monitor = visualizationSystem?.renderPerformanceMonitor() ?: "Performance monitor not available"
        println(monitor)
        
        println("\n⚡ Performance Monitor Status:")
        println("Mode: Performance | Refresh: ${refreshRate}ms | Time: ${formatCurrentTime()}")
    }
    
    /**
     * Display learning curve analysis
     */
    private fun displayLearningAnalysis() {
        val analysis = visualizationSystem?.renderLearningCurve() ?: "Learning analysis not available"
        println(analysis)
        
        println("\n📈 Learning Analysis Status:")
        println("Mode: Learning Analysis | Refresh: ${refreshRate}ms | Time: ${formatCurrentTime()}")
    }
    
    /**
     * Display game analysis with board visualization
     */
    private fun displayGameAnalysis() {
        println("♟️ GAME ANALYSIS")
        println("=" * 50)
        
        // Create sample game analysis
        val boardVisualizer = ChessBoardVisualizer()
        val samplePosition = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        
        println("Current Position:")
        println(boardVisualizer.renderBoard(samplePosition))
        
        println("\n📊 Position Analysis:")
        println("Best Move: e7-e5")
        println("Evaluation: +0.25")
        println("Confidence: 87.3%")
        println("Depth: 12 plies")
        
        println("\n🎯 Move Candidates:")
        println("1. e7-e5    (+0.25)  87.3%")
        println("2. d7-d6    (+0.15)  12.1%")
        println("3. Nf6      (+0.10)   0.6%")
        
        println("\n📈 Evaluation History:")
        val evalHistory = listOf(0.0, 0.1, 0.15, 0.2, 0.25)
        val chartRenderer = ChartRenderer()
        val evalChart = chartRenderer.renderMiniChart(evalHistory, 30, 5, "Position Eval")
        println(evalChart)
        
        println("\n♟️ Game Analysis Status:")
        println("Mode: Game Analysis | Refresh: ${refreshRate}ms | Time: ${formatCurrentTime()}")
    }
    
    /**
     * Start dashboard mode
     */
    private fun startDashboard(args: List<String>) {
        println("📊 Starting Dashboard Mode")
        
        try {
            trainingController = AdvancedTrainingController()
            visualizationSystem = TrainingVisualizationSystem()
            
            visualizationSystem!!.startVisualization(trainingController!!)
            
            // Display dashboard once
            val dashboard = visualizationSystem!!.renderDashboard()
            println(dashboard)
            
        } catch (e: Exception) {
            println("❌ Dashboard failed: ${e.message}")
        }
    }
    
    /**
     * Start monitoring mode
     */
    private fun startMonitoring(args: List<String>) {
        println("⚡ Starting Performance Monitoring")
        
        try {
            trainingController = AdvancedTrainingController()
            visualizationSystem = TrainingVisualizationSystem()
            
            visualizationSystem!!.startVisualization(trainingController!!)
            
            // Display performance monitor
            val monitor = visualizationSystem!!.renderPerformanceMonitor()
            println(monitor)
            
        } catch (e: Exception) {
            println("❌ Monitoring failed: ${e.message}")
        }
    }
    
    /**
     * Start analysis mode
     */
    private fun startAnalysis(args: List<String>) {
        println("📈 Starting Learning Curve Analysis")
        
        try {
            trainingController = AdvancedTrainingController()
            visualizationSystem = TrainingVisualizationSystem()
            
            visualizationSystem!!.startVisualization(trainingController!!)
            
            // Display learning analysis
            val analysis = visualizationSystem!!.renderLearningCurve()
            println(analysis)
            
        } catch (e: Exception) {
            println("❌ Analysis failed: ${e.message}")
        }
    }
    
    /**
     * Start demonstration mode
     */
    private fun startDemo() {
        println("🎮 Starting Visualization Demo")
        println("=" * 40)
        
        try {
            // Create demo training session
            val controller = AdvancedTrainingController()
            val sessionConfig = TrainingSessionConfig(
                name = "visualization_demo",
                controllerType = ControllerType.INTEGRATED,
                iterations = 10,
                integratedConfig = IntegratedSelfPlayConfig(
                    gamesPerIteration = 2,
                    maxStepsPerGame = 5,
                    hiddenLayers = listOf(32, 16)
                )
            )
            
            // Start training
            controller.startTraining(sessionConfig)
            
            // Initialize visualization
            val visualizationSystem = TrainingVisualizationSystem()
            visualizationSystem.startVisualization(controller)
            
            // Demo sequence
            println("\n1️⃣ Dashboard Demo:")
            println(visualizationSystem.renderDashboard())
            
            Thread.sleep(2000)
            
            println("\n2️⃣ Performance Monitor Demo:")
            println(visualizationSystem.renderPerformanceMonitor())
            
            Thread.sleep(2000)
            
            println("\n3️⃣ Learning Analysis Demo:")
            println(visualizationSystem.renderLearningCurve())
            
            Thread.sleep(2000)
            
            println("\n4️⃣ Game Analysis Demo:")
            val boardVisualizer = ChessBoardVisualizer()
            println("Current Game Position:")
            println(boardVisualizer.renderBoard("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR"))
            
            controller.stopTraining()
            visualizationSystem.stopVisualization()
            
            println("\n🎉 Demo completed successfully!")
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
        }
    }
    
    /**
     * Start background refresh (simulated)
     */
    private fun startBackgroundRefresh() {
        // In a real implementation, this would be a separate thread
        // For now, we simulate background updates
        println("🔄 Background refresh started (${refreshRate}ms interval)")
    }
    
    /**
     * Refresh display with latest data
     */
    private fun refreshDisplay() {
        trainingController?.let { controller ->
            visualizationSystem?.updateVisualization(controller)
        }
    }
    
    /**
     * Clear screen (simplified)
     */
    private fun clearScreen() {
        // Simple screen clear - in real implementation would use ANSI codes
        repeat(50) { println() }
    }
    
    /**
     * Cleanup resources
     */
    private fun cleanup() {
        visualizationSystem?.stopVisualization()
        trainingController?.stopTraining()
        println("🧹 Cleanup completed")
    }
    
    /**
     * Format current time
     */
    private fun formatCurrentTime(): String {
        return "Time: ${getCurrentTimeMillis()}"
    }
    
    /**
     * Print usage information
     */
    private fun printUsage() {
        println("🎨 Interactive Training Visualization CLI")
        println("=" * 50)
        println("Usage: InteractiveVisualizationCLI <command> [options]")
        println()
        println("Commands:")
        println("  dashboard          - Show training dashboard")
        println("  monitor           - Show performance monitor")
        println("  analyze           - Show learning curve analysis")
        println("  demo              - Run visualization demo")
        println("  help              - Show this help")
        println()
        println("Interactive Mode:")
        println("  InteractiveVisualizationCLI    - Start interactive mode")
        println()
        println("Interactive Controls:")
        println("  [D] Dashboard     - Switch to main dashboard")
        println("  [M] Monitor       - Switch to performance monitor")
        println("  [A] Analyze       - Switch to learning analysis")
        println("  [G] Game          - Switch to game analysis")
        println("  [R] Refresh       - Refresh current display")
        println("  [F] Faster        - Increase refresh rate")
        println("  [S] Slower        - Decrease refresh rate")
        println("  [C] Clear         - Clear screen")
        println("  [H] Help          - Show help")
        println("  [Q] Quit          - Exit visualization")
        println()
        println("Features:")
        println("  • Real-time training dashboard with live metrics")
        println("  • Interactive game analysis with ASCII board display")
        println("  • Learning curve visualization with trend analysis")
        println("  • Performance monitoring with resource utilization")
        println("  • Convergence analysis and bottleneck detection")
        println()
        println("Examples:")
        println("  InteractiveVisualizationCLI dashboard")
        println("  InteractiveVisualizationCLI monitor")
        println("  InteractiveVisualizationCLI demo")
        println("  InteractiveVisualizationCLI    # Interactive mode")
    }
    
    /**
     * Print interactive help
     */
    private fun printInteractiveHelp() {
        println("\n🎮 Interactive Visualization Help")
        println("-" * 40)
        println("Navigation:")
        println("  D - Dashboard view (main training metrics)")
        println("  M - Performance Monitor (resource usage)")
        println("  A - Learning Analysis (curves and trends)")
        println("  G - Game Analysis (board and moves)")
        println()
        println("Controls:")
        println("  R - Refresh display manually")
        println("  F - Increase refresh rate (faster updates)")
        println("  S - Decrease refresh rate (slower updates)")
        println("  C - Clear screen")
        println("  H - Show this help")
        println("  Q - Quit visualization")
        println()
        println("Current Settings:")
        println("  Mode: $currentMode")
        println("  Refresh Rate: ${refreshRate}ms")
        println("  Status: ${if (isRunning) "Running" else "Stopped"}")
        println()
        println("Tips:")
        println("  • Press Enter after each command")
        println("  • Use 'F' and 'S' to adjust update speed")
        println("  • Switch between views to see different metrics")
        println("  • Use 'R' to force refresh if display seems stuck")
    }
}

/**
 * Display mode enumeration
 */
enum class DisplayMode {
    DASHBOARD,
    PERFORMANCE_MONITOR,
    LEARNING_ANALYSIS,
    GAME_ANALYSIS
}

/**
 * Real-time visualization dashboard with live updates
 */
class RealTimeVisualizationDashboard(
    private val controller: AdvancedTrainingController,
    private val visualizationSystem: TrainingVisualizationSystem
) {
    
    private var isActive = false
    private var updateInterval = 1000L
    
    /**
     * Start real-time dashboard
     */
    fun start() {
        isActive = true
        println("🎨 Real-time dashboard started")
        
        // Simulate real-time updates
        while (isActive) {
            updateDisplay()
            Thread.sleep(updateInterval)
        }
    }
    
    /**
     * Stop real-time dashboard
     */
    fun stop() {
        isActive = false
        println("🎨 Real-time dashboard stopped")
    }
    
    /**
     * Update display with latest data
     */
    private fun updateDisplay() {
        // Clear screen and show updated dashboard
        repeat(50) { println() }
        
        val dashboard = visualizationSystem.renderDashboard()
        println(dashboard)
        
        // Show update timestamp
        println("Last updated: ${formatCurrentTime()}")
    }
    
    /**
     * Set update interval
     */
    fun setUpdateInterval(intervalMs: Long) {
        updateInterval = intervalMs.coerceIn(500L, 10000L)
    }
    
    private fun formatCurrentTime(): String {
        return "Time: ${getCurrentTimeMillis()}"
    }
}