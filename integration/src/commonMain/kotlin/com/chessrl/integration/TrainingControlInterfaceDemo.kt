package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*

/**
 * Comprehensive demonstration of the training control and visualization interface.
 * 
 * This demo showcases all major features:
 * - Advanced training control (start/pause/resume/stop/restart)
 * - Real-time configuration adjustment with validation and rollback
 * - Training experiment management with parameter tracking
 * - Interactive training dashboard with comprehensive metrics visualization
 * - Real-time game analysis with ASCII board display and move evaluation
 * - Learning curve visualization with convergence analysis
 * - Performance monitoring with resource utilization metrics
 * - Interactive analysis tools and agent vs human play mode
 */
class TrainingControlInterfaceDemo {
    
    private lateinit var trainingInterface: TrainingControlInterface
    private lateinit var dashboard: InteractiveTrainingDashboard
    
    /**
     * Run the complete demonstration
     */
    fun runDemo() {
        println("🚀 Chess RL Training Control Interface - Comprehensive Demo")
        println("=" * 80)
        
        try {
            // Phase 1: Interface Initialization
            demonstrateInitialization()
            
            // Phase 2: Configuration Management
            demonstrateConfigurationManagement()
            
            // Phase 3: Training Sessions
            demonstrateTrainingSessions()
            
            // Phase 4: Interactive Dashboard
            demonstrateInteractiveDashboard()
            
            // Phase 5: Game Analysis
            demonstrateGameAnalysis()
            
            // Phase 6: Agent vs Human Play
            demonstrateAgentVsHumanPlay()
            
            // Phase 7: Training Validation
            demonstrateTrainingValidation()
            
            // Phase 8: Report Generation
            demonstrateReportGeneration()
            
            // Phase 9: Advanced Features
            demonstrateAdvancedFeatures()
            
            // Phase 10: Cleanup
            demonstrateCleanup()
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Demonstrate interface initialization with all components
     */
    private fun demonstrateInitialization() {
        println("\n📋 Phase 1: Interface Initialization")
        println("-" * 50)
        
        // Create configuration
        val config = TrainingInterfaceConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(512, 256, 128),
            learningRate = 0.001,
            explorationRate = 0.1,
            maxStepsPerEpisode = 200,
            batchSize = 64,
            maxBufferSize = 50000,
            gamesPerIteration = 20,
            maxConcurrentGames = 4,
            displayUpdateInterval = 5,
            topMovesToShow = 5,
            maxMovesToDisplay = 10
        )
        
        println("🔧 Creating Training Control Interface...")
        trainingInterface = TrainingControlInterface(config)
        
        println("🔧 Initializing all components...")
        val initResult = trainingInterface.initialize()
        
        when (initResult) {
            is InterfaceInitializationResult.Success -> {
                println("✅ Interface initialized successfully!")
                println("   Initialization time: ${initResult.initializationTime}ms")
                println("   Interface version: ${initResult.interfaceVersion}")
                println("   Components initialized:")
                initResult.componentResults.forEach { component ->
                    val status = if (component.success) "✅" else "❌"
                    println("     $status ${component.componentName}")
                    if (component.error != null) {
                        println("       Error: ${component.error}")
                    }
                }
            }
            is InterfaceInitializationResult.Failed -> {
                println("❌ Interface initialization failed: ${initResult.error}")
                return
            }
            is InterfaceInitializationResult.AlreadyInitialized -> {
                println("⚠️ Interface already initialized: ${initResult.message}")
            }
        }
        
        // Create interactive dashboard
        println("\n🎮 Creating Interactive Dashboard...")
        val dashboardConfig = DashboardConfig(
            minUpdateInterval = 1000L,
            maxHistorySize = 100,
            defaultView = DashboardView.OVERVIEW,
            enableAutoUpdate = false,
            chartWidth = 60,
            progressBarWidth = 50
        )
        
        dashboard = InteractiveTrainingDashboard(trainingInterface, dashboardConfig)
        println("✅ Dashboard created successfully!")
    }
    
    /**
     * Demonstrate configuration management with validation and rollback
     */
    private fun demonstrateConfigurationManagement() {
        println("\n⚙️ Phase 2: Configuration Management")
        println("-" * 50)
        
        // Test configuration validation
        println("🔍 Testing configuration validation...")
        val invalidConfig = TrainingInterfaceConfig(
            learningRate = -0.1, // Invalid
            explorationRate = 1.5, // Invalid
            batchSize = 0 // Invalid
        )
        
        val invalidResult = trainingInterface.updateConfiguration(invalidConfig, validateBeforeApply = true)
        when (invalidResult) {
            is ConfigurationUpdateResult.Success -> {
                println("⚠️ Invalid configuration was accepted (unexpected)")
            }
            is ConfigurationUpdateResult.Failed -> {
                println("✅ Invalid configuration rejected: ${invalidResult.error}")
            }
        }
        
        // Test valid configuration update
        println("\n🔧 Updating configuration with valid settings...")
        val newConfig = TrainingInterfaceConfig(
            agentType = AgentType.POLICY_GRADIENT,
            hiddenLayers = listOf(256, 128, 64),
            learningRate = 0.005,
            explorationRate = 0.15,
            temperature = 1.2,
            batchSize = 32,
            maxBufferSize = 25000
        )
        
        val updateResult = trainingInterface.updateConfiguration(newConfig, validateBeforeApply = true)
        when (updateResult) {
            is ConfigurationUpdateResult.Success -> {
                println("✅ Configuration updated successfully!")
                println("   Validation performed: ${updateResult.validationPerformed}")
                println("   Rollback available: ${updateResult.rollbackAvailable}")
                println("   New learning rate: ${updateResult.newConfig.learningRate}")
                println("   New agent type: ${updateResult.newConfig.agentType}")
            }
            is ConfigurationUpdateResult.Failed -> {
                println("❌ Configuration update failed: ${updateResult.error}")
            }
        }
        
        // Test configuration rollback
        println("\n🔄 Testing configuration rollback...")
        val rollbackResult = trainingInterface.rollbackConfiguration()
        when (rollbackResult) {
            is ConfigurationRollbackResult.Success -> {
                println("✅ Configuration rolled back successfully!")
                println("   Rolled back from learning rate: ${rollbackResult.rolledBackFrom.learningRate}")
                println("   Rolled back to learning rate: ${rollbackResult.rolledBackTo.learningRate}")
            }
            is ConfigurationRollbackResult.Failed -> {
                println("❌ Configuration rollback failed: ${rollbackResult.error}")
            }
        }
    }
    
    /**
     * Demonstrate different types of training sessions
     */
    private fun demonstrateTrainingSessions() {
        println("\n🎯 Phase 3: Training Sessions")
        println("-" * 50)
        
        // Basic training session
        println("🔥 Starting basic training session...")
        val basicConfig = TrainingSessionConfig(
            trainingType = TrainingType.BASIC,
            episodes = 10,
            enableMonitoring = true,
            enableValidation = false,
            experimentName = "Basic Training Demo",
            description = "Demonstration of basic RL training"
        )
        
        val basicResult = trainingInterface.startTrainingSession(basicConfig)
        when (basicResult) {
            is TrainingSessionResult.Success -> {
                println("✅ Basic training session started!")
                println("   Session ID: ${basicResult.sessionId}")
                
                // Demonstrate training control commands
                demonstrateTrainingControl()
            }
            is TrainingSessionResult.Failed -> {
                println("❌ Basic training session failed: ${basicResult.error}")
            }
        }
        
        // Self-play training session
        println("\n🎮 Starting self-play training session...")
        val selfPlayConfig = TrainingSessionConfig(
            trainingType = TrainingType.SELF_PLAY,
            iterations = 3,
            enableMonitoring = true,
            experimentName = "Self-Play Demo",
            description = "Demonstration of self-play training"
        )
        
        val selfPlayResult = trainingInterface.startTrainingSession(selfPlayConfig)
        when (selfPlayResult) {
            is TrainingSessionResult.Success -> {
                println("✅ Self-play training session started!")
                println("   Session ID: ${selfPlayResult.sessionId}")
            }
            is TrainingSessionResult.Failed -> {
                println("❌ Self-play training session failed: ${selfPlayResult.error}")
            }
        }
        
        // Validation session
        println("\n🔍 Starting validation session...")
        val validationConfig = TrainingSessionConfig(
            trainingType = TrainingType.VALIDATION,
            episodes = 5,
            enableMonitoring = false,
            experimentName = "Validation Demo",
            description = "Demonstration of training validation"
        )
        
        val validationResult = trainingInterface.startTrainingSession(validationConfig)
        when (validationResult) {
            is TrainingSessionResult.Success -> {
                println("✅ Validation session started!")
                println("   Session ID: ${validationResult.sessionId}")
            }
            is TrainingSessionResult.Failed -> {
                println("❌ Validation session failed: ${validationResult.error}")
            }
        }
    }
    
    /**
     * Demonstrate training control commands
     */
    private fun demonstrateTrainingControl() {
        println("\n🎮 Demonstrating training control commands...")
        
        // Pause training
        val pauseResult = trainingInterface.executeCommand(TrainingCommand.Pause())
        when (pauseResult) {
            is CommandExecutionResult.Success -> {
                println("⏸️ Training paused successfully")
            }
            is CommandExecutionResult.Failed -> {
                println("⚠️ Pause command failed: ${pauseResult.error}")
            }
        }
        
        // Resume training
        val resumeResult = trainingInterface.executeCommand(TrainingCommand.Resume())
        when (resumeResult) {
            is CommandExecutionResult.Success -> {
                println("▶️ Training resumed successfully")
            }
            is CommandExecutionResult.Failed -> {
                println("⚠️ Resume command failed: ${resumeResult.error}")
            }
        }
        
        // Stop training
        val stopResult = trainingInterface.executeCommand(TrainingCommand.Stop())
        when (stopResult) {
            is CommandExecutionResult.Success -> {
                println("🛑 Training stopped successfully")
            }
            is CommandExecutionResult.Failed -> {
                println("⚠️ Stop command failed: ${stopResult.error}")
            }
        }
    }
    
    /**
     * Demonstrate interactive dashboard features
     */
    private fun demonstrateInteractiveDashboard() {
        println("\n📊 Phase 4: Interactive Dashboard")
        println("-" * 50)
        
        // Start dashboard
        println("🎮 Starting interactive dashboard...")
        val dashboardSession = dashboard.start()
        println("✅ Dashboard started!")
        println("   Session ID: ${dashboardSession.sessionId}")
        
        // Demonstrate dashboard updates
        println("\n📈 Demonstrating dashboard updates...")
        repeat(3) { i ->
            Thread.sleep(1100) // Wait for minimum update interval
            val updateResult = dashboard.update()
            when (updateResult) {
                is DashboardUpdateResult.Success -> {
                    println("✅ Dashboard update ${updateResult.updateCount} completed (${updateResult.timeSinceLastUpdate}ms since last)")
                }
                is DashboardUpdateResult.Error -> {
                    println("⚠️ Dashboard update failed: ${updateResult.message}")
                }
                is DashboardUpdateResult.TooSoon -> {
                    println("⏰ Update too soon (${updateResult.timeSinceLastUpdate}ms)")
                }
                is DashboardUpdateResult.NotActive -> {
                    println("❌ Dashboard not active")
                }
            }
        }
        
        // Demonstrate dashboard commands
        println("\n🎮 Demonstrating dashboard commands...")
        val commands = listOf(
            "help" to "Show help information",
            "view training" to "Switch to training view",
            "view games" to "Switch to games view",
            "view analysis" to "Switch to analysis view",
            "view performance" to "Switch to performance view",
            "clear" to "Clear screen",
            "view overview" to "Return to overview"
        )
        
        commands.forEach { (command, description) ->
            println("   Executing: $command ($description)")
            val result = dashboard.executeCommand(command)
            when (result) {
                is CommandResult.Success -> {
                    println("   ✅ ${result.message}")
                }
                is CommandResult.Error -> {
                    println("   ❌ ${result.message}")
                }
            }
        }
    }
    
    /**
     * Demonstrate game analysis capabilities
     */
    private fun demonstrateGameAnalysis() {
        println("\n🔍 Phase 5: Game Analysis")
        println("-" * 50)
        
        // Create sample game
        val sampleGame = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2)), // Nb8-c6
            Move(Position(0, 5), Position(3, 2)), // Bf1-c4
            Move(Position(7, 5), Position(4, 2)), // Bf8-c5
            Move(Position(0, 3), Position(4, 7)), // Qd1-h5
            Move(Position(7, 6), Position(5, 5))  // Ng8-f6
        )
        
        println("🎯 Analyzing sample game with ${sampleGame.size} moves...")
        val analysisResult = trainingInterface.analyzeGame(
            gameHistory = sampleGame,
            includePositionAnalysis = true,
            includeMoveAnalysis = true,
            includeStrategicAnalysis = true
        )
        
        when (analysisResult) {
            is GameAnalysisResult.Success -> {
                println("✅ Game analysis completed!")
                println("   Analysis time: ${analysisResult.analysisTime}ms")
                println("   Game quality score: ${String.format("%.3f", analysisResult.gameQuality.qualityScore)}")
                println("   Game length: ${analysisResult.gameQuality.gameLength} moves")
                
                // Display some analysis output
                println("\n📋 Game Quality Assessment:")
                println(analysisResult.qualityOutput.take(500) + "...")
                
                if (analysisResult.detailedAnalysis != null) {
                    println("\n📊 Position Analysis Summary:")
                    analysisResult.detailedAnalysis.take(3).forEach { analysis ->
                        println("   Move ${analysis.moveNumber}: Confidence ${String.format("%.1f", analysis.decisionAnalysis.decisionConfidence * 100)}%")
                    }
                }
            }
            is GameAnalysisResult.Failed -> {
                println("❌ Game analysis failed: ${analysisResult.error}")
            }
        }
        
        // Demonstrate position analysis
        println("\n🏁 Analyzing specific position...")
        val startingPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val positionCommand = TrainingCommand.Analyze(
            analysisType = AnalysisType.POSITION,
            position = startingPosition
        )
        
        val positionResult = trainingInterface.executeCommand(positionCommand)
        when (positionResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Position analysis completed!")
                println("   Execution time: ${positionResult.executionTime}ms")
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Position analysis failed: ${positionResult.error}")
            }
        }
    }
    
    /**
     * Demonstrate agent vs human play mode
     */
    private fun demonstrateAgentVsHumanPlay() {
        println("\n🎮 Phase 6: Agent vs Human Play")
        println("-" * 50)
        
        println("🤖 Starting agent vs human play session...")
        val session = trainingInterface.startAgentVsHumanMode(
            humanColor = PieceColor.WHITE,
            timeControl = TimeControl.Unlimited
        )
        
        println("✅ Agent vs human session started!")
        println("   Session ID: ${session.sessionId}")
        println("   Human plays as: ${session.humanColor}")
        println("   Time control: ${session.timeControl}")
        
        // Simulate a few moves
        println("\n♟️ Simulating human moves...")
        val humanMoves = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(0, 5), Position(3, 2))  // Bf1-c4
        )
        
        humanMoves.forEach { move ->
            println("   Human plays: ${move.toAlgebraic()}")
            val moveResult = trainingInterface.makeHumanMove(session, move)
            
            when (moveResult) {
                is HumanMoveResult.Success -> {
                    println("   Agent responds: ${moveResult.agentMove.toAlgebraic()}")
                }
                is HumanMoveResult.GameEnded -> {
                    println("   Game ended: ${moveResult.gameStatus}")
                    break
                }
                is HumanMoveResult.Failed -> {
                    println("   Move failed: ${moveResult.error}")
                    break
                }
            }
        }
    }
    
    /**
     * Demonstrate training validation capabilities
     */
    private fun demonstrateTrainingValidation() {
        println("\n✅ Phase 7: Training Validation")
        println("-" * 50)
        
        // Test with predefined scenarios
        println("🎯 Running tactical scenario validation...")
        val tacticalCommand = TrainingCommand.Validate(
            validationType = ValidationType.SCENARIOS,
            scenarios = TrainingScenarioFactory.createTacticalScenarios()
        )
        
        val tacticalResult = trainingInterface.executeCommand(tacticalCommand)
        when (tacticalResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Tactical validation completed!")
                println("   Execution time: ${tacticalResult.executionTime}ms")
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Tactical validation failed: ${tacticalResult.error}")
            }
        }
        
        // Test with endgame scenarios
        println("\n🏁 Running endgame scenario validation...")
        val endgameCommand = TrainingCommand.Validate(
            validationType = ValidationType.SCENARIOS,
            scenarios = TrainingScenarioFactory.createEndgameScenarios()
        )
        
        val endgameResult = trainingInterface.executeCommand(endgameCommand)
        when (endgameResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Endgame validation completed!")
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Endgame validation failed: ${endgameResult.error}")
            }
        }
        
        // Test game quality validation
        println("\n🎲 Running game quality validation...")
        val qualityCommand = TrainingCommand.Validate(
            validationType = ValidationType.GAME_QUALITY,
            games = listOf(
                listOf(
                    Move(Position(1, 4), Position(3, 4)), // e4
                    Move(Position(6, 4), Position(4, 4)), // e5
                    Move(Position(0, 6), Position(2, 5)), // Nf3
                    Move(Position(7, 1), Position(5, 2))  // Nc6
                )
            )
        )
        
        val qualityResult = trainingInterface.executeCommand(qualityCommand)
        when (qualityResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Game quality validation completed!")
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Game quality validation failed: ${qualityResult.error}")
            }
        }
    }
    
    /**
     * Demonstrate report generation capabilities
     */
    private fun demonstrateReportGeneration() {
        println("\n📊 Phase 8: Report Generation")
        println("-" * 50)
        
        val reportTypes = listOf(
            ReportType.COMPREHENSIVE to "Comprehensive training report",
            ReportType.PERFORMANCE to "Performance analysis report",
            ReportType.GAME_QUALITY to "Game quality analysis report",
            ReportType.VALIDATION to "Validation results report"
        )
        
        reportTypes.forEach { (reportType, description) ->
            println("📋 Generating $description...")
            val reportResult = trainingInterface.generateTrainingReport(
                reportType = reportType,
                includeGameAnalysis = true,
                includePerformanceMetrics = true
            )
            
            when (reportResult) {
                is TrainingReportResult.Success -> {
                    println("✅ $description generated!")
                    println("   Generation time: ${reportResult.generationTime}ms")
                    println("   Report type: ${reportResult.reportType}")
                }
                is TrainingReportResult.Failed -> {
                    println("❌ $description failed: ${reportResult.error}")
                }
            }
        }
    }
    
    /**
     * Demonstrate advanced features
     */
    private fun demonstrateAdvancedFeatures() {
        println("\n🚀 Phase 9: Advanced Features")
        println("-" * 50)
        
        // Demonstrate visualization commands
        println("🎨 Testing visualization features...")
        val boardVisualization = TrainingCommand.Visualize(
            visualizationType = VisualizationType.BOARD,
            board = ChessBoard()
        )
        
        val vizResult = trainingInterface.executeCommand(boardVisualization)
        when (vizResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Board visualization generated!")
                val output = vizResult.result as String
                println("   Output length: ${output.length} characters")
                
                // Display first few lines of the board
                val lines = output.split("\n").take(5)
                lines.forEach { line ->
                    println("   $line")
                }
                if (output.split("\n").size > 5) {
                    println("   ... (truncated)")
                }
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Board visualization failed: ${vizResult.error}")
            }
        }
        
        // Demonstrate export functionality
        println("\n💾 Testing export functionality...")
        val exportCommand = TrainingCommand.Export(
            format = "json",
            path = "demo_export.json",
            includeRawData = false
        )
        
        val exportResult = trainingInterface.executeCommand(exportCommand)
        when (exportResult) {
            is CommandExecutionResult.Success -> {
                println("✅ Export completed!")
            }
            is CommandExecutionResult.Failed -> {
                println("❌ Export failed: ${exportResult.error}")
            }
        }
        
        // Demonstrate dashboard advanced commands
        println("\n🎮 Testing advanced dashboard commands...")
        val advancedCommands = listOf(
            "analyze current",
            "export json dashboard_data.json",
            "configure auto_update true"
        )
        
        advancedCommands.forEach { command ->
            println("   Executing: $command")
            val result = dashboard.executeCommand(command)
            when (result) {
                is CommandResult.Success -> {
                    println("   ✅ ${result.message}")
                }
                is CommandResult.Error -> {
                    println("   ❌ ${result.message}")
                }
            }
        }
    }
    
    /**
     * Demonstrate cleanup and shutdown
     */
    private fun demonstrateCleanup() {
        println("\n🧹 Phase 10: Cleanup and Shutdown")
        println("-" * 50)
        
        // Stop dashboard
        println("🛑 Stopping dashboard...")
        val dashboardStopResult = dashboard.stop()
        when (dashboardStopResult) {
            is DashboardStopResult.Success -> {
                println("✅ Dashboard stopped successfully!")
                println("   Session duration: ${dashboardStopResult.sessionDuration}ms")
                println("   Total updates: ${dashboardStopResult.totalUpdates}")
                println("   Commands executed: ${dashboardStopResult.commandsExecuted}")
            }
            is DashboardStopResult.NotActive -> {
                println("⚠️ Dashboard was not active")
            }
        }
        
        // Shutdown training interface
        println("\n🛑 Shutting down training interface...")
        val shutdownResult = trainingInterface.shutdown()
        when (shutdownResult) {
            is ShutdownResult.Success -> {
                println("✅ Training interface shutdown completed!")
                println("   Shutdown time: ${shutdownResult.shutdownTime}ms")
                println("   Components shut down:")
                shutdownResult.componentResults.forEach { component ->
                    val status = if (component.success) "✅" else "❌"
                    println("     $status ${component.componentName}")
                    if (component.error != null) {
                        println("       Error: ${component.error}")
                    }
                }
            }
            is ShutdownResult.Failed -> {
                println("❌ Shutdown failed: ${shutdownResult.error}")
            }
        }
        
        println("\n🎉 Demo completed successfully!")
        println("=" * 80)
    }
    
    /**
     * Display training dashboard information
     */
    private fun displayDashboardInfo() {
        val dashboard = trainingInterface.getTrainingDashboard()
        
        println("\n📊 Current Training Dashboard:")
        println("   Session Duration: ${dashboard.sessionInfo.elapsedTime}ms")
        println("   Total Episodes: ${dashboard.sessionInfo.totalCycles}")
        println("   Total Games: ${dashboard.sessionInfo.totalGames}")
        println("   Average Reward: ${String.format("%.3f", dashboard.currentStats.averageReward)}")
        println("   Win Rate: ${String.format("%.1f", dashboard.currentStats.winRate * 100)}%")
        println("   System Health: ${dashboard.systemHealth.status} (${String.format("%.1f", dashboard.systemHealth.score * 100)}%)")
        
        if (dashboard.activeIssues.isNotEmpty()) {
            println("   Active Issues: ${dashboard.activeIssues.size}")
            dashboard.activeIssues.take(3).forEach { issue ->
                println("     - ${issue.type}: ${issue.description}")
            }
        }
    }
    
    /**
     * Demonstrate error handling
     */
    private fun demonstrateErrorHandling() {
        println("\n⚠️ Demonstrating error handling...")
        
        // Try to start session without initialization
        val uninitializedInterface = TrainingControlInterface()
        val sessionConfig = TrainingSessionConfig(TrainingType.BASIC, episodes = 5)
        val result = uninitializedInterface.startTrainingSession(sessionConfig)
        
        when (result) {
            is TrainingSessionResult.Failed -> {
                println("✅ Properly handled uninitialized interface: ${result.error}")
            }
            is TrainingSessionResult.Success -> {
                println("⚠️ Unexpected success with uninitialized interface")
            }
        }
        
        // Try invalid commands
        val invalidCommands = listOf(
            TrainingCommand.Analyze(AnalysisType.GAME, gameHistory = emptyList()),
            TrainingCommand.Export("invalid_format", ""),
            TrainingCommand.Validate(ValidationType.SCENARIOS, scenarios = emptyList())
        )
        
        invalidCommands.forEach { command ->
            val commandResult = trainingInterface.executeCommand(command)
            when (commandResult) {
                is CommandExecutionResult.Failed -> {
                    println("✅ Properly handled invalid command: ${commandResult.error}")
                }
                is CommandExecutionResult.Success -> {
                    println("⚠️ Invalid command succeeded unexpectedly")
                }
            }
        }
    }
}

/**
 * Main function to run the demo
 */
fun main() {
    val demo = TrainingControlInterfaceDemo()
    demo.runDemo()
}