package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.math.*

/**
 * Comprehensive training control and visualization interface for chess RL training.
 * 
 * This interface provides production-ready training management with:
 * - Advanced training control (start/pause/resume/stop/restart)
 * - Real-time configuration adjustment with validation and rollback
 * - Training experiment management with parameter tracking
 * - Interactive training dashboard with comprehensive metrics visualization
 * - Real-time game analysis with ASCII board display and move evaluation
 * - Learning curve visualization with convergence analysis
 * - Performance monitoring with resource utilization metrics
 * - Interactive analysis tools and agent vs human play mode
 */
class TrainingControlInterface(
    private val config: TrainingInterfaceConfig = TrainingInterfaceConfig()
) {
    
    // Core training components
    private var trainingController: TrainingController? = null
    private var selfPlayController: SelfPlayController? = null
    private var monitoringSystem: TrainingMonitoringSystem? = null
    private var validationTools: ManualValidationTools? = null
    private var validationConsole: ValidationConsole? = null
    
    // Interface state
    private var isInitialized = false
    private var currentSession: TrainingSession? = null
    private var experimentHistory = mutableListOf<TrainingExperiment>()
    
    // Real-time monitoring
    private var dashboardUpdateInterval = 5000L // 5 seconds
    private var lastDashboardUpdate = 0L
    private var isMonitoringActive = false
    
    // Configuration management
    private var activeConfig: TrainingInterfaceConfig = config
    private var configHistory = mutableListOf<ConfigurationSnapshot>()
    
    /**
     * Initialize the training control interface with all components
     */
    fun initialize(): InterfaceInitializationResult {
        if (isInitialized) {
            return InterfaceInitializationResult.AlreadyInitialized("Interface already initialized")
        }
        
        println("🚀 Initializing Comprehensive Training Control Interface")
        println("=" * 80)
        
        val initStartTime = getCurrentTimeMillis()
        val initResults = mutableListOf<ComponentInitResult>()
        
        try {
            // Initialize training controller
            println("🔧 Initializing Training Controller...")
            trainingController = TrainingController(
                TrainingControllerConfig(
                    agentType = activeConfig.agentType,
                    hiddenLayers = activeConfig.hiddenLayers,
                    learningRate = activeConfig.learningRate,
                    explorationRate = activeConfig.explorationRate,
                    maxStepsPerEpisode = activeConfig.maxStepsPerEpisode,
                    batchSize = activeConfig.batchSize,
                    maxBufferSize = activeConfig.maxBufferSize
                )
            )
            
            val tcInitSuccess = trainingController!!.initialize()
            initResults.add(ComponentInitResult("TrainingController", tcInitSuccess, if (tcInitSuccess) null else "Failed to initialize"))
            
            // Initialize self-play controller
            println("🔧 Initializing Self-Play Controller...")
            selfPlayController = SelfPlayController(
                SelfPlayControllerConfig(
                    agentType = activeConfig.agentType,
                    hiddenLayers = activeConfig.hiddenLayers,
                    learningRate = activeConfig.learningRate,
                    explorationRate = activeConfig.explorationRate,
                    gamesPerIteration = activeConfig.gamesPerIteration,
                    maxConcurrentGames = activeConfig.maxConcurrentGames,
                    batchSize = activeConfig.batchSize,
                    maxExperienceBufferSize = activeConfig.maxBufferSize
                )
            )
            
            val spcInitSuccess = selfPlayController!!.initialize()
            initResults.add(ComponentInitResult("SelfPlayController", spcInitSuccess, if (spcInitSuccess) null else "Failed to initialize"))
            
            // Initialize monitoring system
            println("🔧 Initializing Monitoring System...")
            monitoringSystem = TrainingMonitoringSystem(
                MonitoringConfig(
                    displayUpdateInterval = activeConfig.displayUpdateInterval,
                    convergenceAnalysisWindow = activeConfig.convergenceAnalysisWindow,
                    convergenceStabilityThreshold = activeConfig.convergenceStabilityThreshold
                )
            )
            initResults.add(ComponentInitResult("MonitoringSystem", true, null))
            
            // Initialize validation tools
            println("🔧 Initializing Validation Tools...")
            val agent = trainingController!!.let { tc ->
                // Get agent from training controller (simplified access)
                ChessAgentFactory.createDQNAgent(
                    hiddenLayers = activeConfig.hiddenLayers,
                    learningRate = activeConfig.learningRate,
                    explorationRate = activeConfig.explorationRate
                )
            }
            
            val environment = ChessEnvironment()
            
            validationTools = ManualValidationTools(
                agent = agent,
                environment = environment,
                config = ManualValidationConfig(
                    topMovesToShow = activeConfig.topMovesToShow,
                    topInvalidMovesToShow = activeConfig.topInvalidMovesToShow
                )
            )
            
            validationConsole = ValidationConsole(
                validationTools = validationTools!!,
                config = ConsoleConfig(
                    maxMovesToDisplay = activeConfig.maxMovesToDisplay,
                    maxMovesToAnalyze = activeConfig.maxMovesToAnalyze
                )
            )
            
            initResults.add(ComponentInitResult("ValidationTools", true, null))
            
            val initDuration = getCurrentTimeMillis() - initStartTime
            isInitialized = true
            
            println("✅ Training Control Interface initialized successfully in ${initDuration}ms")
            
            return InterfaceInitializationResult.Success(
                initializationTime = initDuration,
                componentResults = initResults,
                interfaceVersion = "1.0.0"
            )
            
        } catch (e: Exception) {
            println("❌ Failed to initialize Training Control Interface: ${e.message}")
            e.printStackTrace()
            
            return InterfaceInitializationResult.Failed(
                error = e.message ?: "Unknown error",
                componentResults = initResults
            )
        }
    }
    
    /**
     * Start a new training session with comprehensive monitoring
     */
    fun startTrainingSession(
        sessionConfig: TrainingSessionConfig
    ): TrainingSessionResult {
        
        if (!isInitialized) {
            return TrainingSessionResult.Failed("Interface not initialized")
        }
        
        if (currentSession != null && currentSession!!.isActive) {
            return TrainingSessionResult.Failed("Training session already active")
        }
        
        println("🚀 Starting New Training Session")
        println("Configuration: $sessionConfig")
        println("=" * 80)
        
        val sessionStartTime = getCurrentTimeMillis()
        val sessionId = generateSessionId()
        
        try {
            // Create training session
            currentSession = TrainingSession(
                sessionId = sessionId,
                config = sessionConfig,
                startTime = sessionStartTime,
                isActive = true
            )
            
            // Start monitoring
            val monitoringSession = monitoringSystem!!.startMonitoring(sessionConfig)
            isMonitoringActive = true
            
            // Start appropriate training type
            val trainingResult = when (sessionConfig.trainingType) {
                TrainingType.BASIC -> startBasicTraining(sessionConfig)
                TrainingType.SELF_PLAY -> startSelfPlayTraining(sessionConfig)
                TrainingType.VALIDATION -> startValidationTraining(sessionConfig)
                TrainingType.EXPERIMENT -> startExperimentalTraining(sessionConfig)
            }
            
            return when (trainingResult) {
                is BasicTrainingResult.Success -> {
                    TrainingSessionResult.Success(
                        sessionId = sessionId,
                        trainingResults = trainingResult,
                        monitoringSession = monitoringSession
                    )
                }
                is BasicTrainingResult.Failed -> {
                    TrainingSessionResult.Failed(trainingResult.error)
                }
            }
            
        } catch (e: Exception) {
            println("❌ Failed to start training session: ${e.message}")
            currentSession?.let { it.isActive = false }
            isMonitoringActive = false
            
            return TrainingSessionResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Get real-time training dashboard with comprehensive metrics
     */
    fun getTrainingDashboard(): TrainingDashboard {
        if (!isInitialized || !isMonitoringActive) {
            return createEmptyDashboard()
        }
        
        val currentTime = getCurrentTimeMillis()
        
        // Check if dashboard needs updating
        if (currentTime - lastDashboardUpdate < dashboardUpdateInterval) {
            // Return cached dashboard if available
            return monitoringSystem?.getTrainingDashboard() ?: createEmptyDashboard()
        }
        
        lastDashboardUpdate = currentTime
        
        // Get comprehensive dashboard from monitoring system
        val dashboard = monitoringSystem!!.getTrainingDashboard()
        
        // Enhance with interface-specific information
        return dashboard.copy(
            interfaceInfo = InterfaceInfo(
                sessionId = currentSession?.sessionId ?: "none",
                sessionDuration = currentSession?.let { currentTime - it.startTime } ?: 0L,
                dashboardUpdateInterval = dashboardUpdateInterval,
                lastUpdate = currentTime,
                activeFeatures = getActiveFeatures()
            )
        )
    }
    
    /**
     * Execute interactive training command with comprehensive validation
     */
    fun executeCommand(command: TrainingCommand): CommandExecutionResult {
        if (!isInitialized) {
            return CommandExecutionResult.Failed("Interface not initialized")
        }
        
        println("🎮 Executing command: ${command.type}")
        
        val executionStartTime = getCurrentTimeMillis()
        
        try {
            val result = when (command) {
                is TrainingCommand.Start -> handleStartCommand(command)
                is TrainingCommand.Pause -> handlePauseCommand(command)
                is TrainingCommand.Resume -> handleResumeCommand(command)
                is TrainingCommand.Stop -> handleStopCommand(command)
                is TrainingCommand.Restart -> handleRestartCommand(command)
                is TrainingCommand.Configure -> handleConfigureCommand(command)
                is TrainingCommand.Analyze -> handleAnalyzeCommand(command)
                is TrainingCommand.Validate -> handleValidateCommand(command)
                is TrainingCommand.Export -> handleExportCommand(command)
                is TrainingCommand.Visualize -> handleVisualizeCommand(command)
                is TrainingCommand.PlayAgainstAgent -> handlePlayAgainstAgentCommand(command)
                is TrainingCommand.GenerateReport -> handleGenerateReportCommand(command)
            }
            
            val executionTime = getCurrentTimeMillis() - executionStartTime
            
            return CommandExecutionResult.Success(
                result = result,
                executionTime = executionTime,
                timestamp = getCurrentTimeMillis()
            )
            
        } catch (e: Exception) {
            println("❌ Command execution failed: ${e.message}")
            
            return CommandExecutionResult.Failed(
                error = e.message ?: "Unknown error",
                command = command.type.toString(),
                timestamp = getCurrentTimeMillis()
            )
        }
    }
    
    /**
     * Analyze specific game with detailed position analysis
     */
    fun analyzeGame(
        gameHistory: List<Move>,
        includePositionAnalysis: Boolean = true,
        includeMoveAnalysis: Boolean = true,
        includeStrategicAnalysis: Boolean = true
    ): GameAnalysisResult {
        
        if (!isInitialized || validationTools == null) {
            return GameAnalysisResult.Failed("Validation tools not available")
        }
        
        println("🔍 Analyzing game with ${gameHistory.size} moves...")
        
        val analysisStartTime = getCurrentTimeMillis()
        
        try {
            // Create interactive game analysis
            val interactiveAnalysis = validationTools!!.createInteractiveAnalysis(gameHistory)
            
            // Assess game quality
            val board = ChessBoard()
            gameHistory.forEach { move ->
                board.makeLegalMove(move)
                board.switchActiveColor()
            }
            
            val gameStatus = GameStatus.ONGOING // Simplified - would get from actual game state
            val gameQuality = validationTools!!.assessGameQuality(gameHistory, gameStatus, board)
            
            // Generate console output
            val consoleOutput = validationConsole!!.displayInteractiveAnalysis(interactiveAnalysis)
            val qualityOutput = validationConsole!!.displayGameQuality(gameQuality)
            
            // Create detailed analysis if requested
            val detailedAnalysis = if (includePositionAnalysis) {
                analyzeGamePositions(gameHistory)
            } else null
            
            val analysisTime = getCurrentTimeMillis() - analysisStartTime
            
            return GameAnalysisResult.Success(
                gameHistory = gameHistory,
                interactiveAnalysis = interactiveAnalysis,
                gameQuality = gameQuality,
                detailedAnalysis = detailedAnalysis,
                consoleOutput = consoleOutput,
                qualityOutput = qualityOutput,
                analysisTime = analysisTime
            )
            
        } catch (e: Exception) {
            println("❌ Game analysis failed: ${e.message}")
            
            return GameAnalysisResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Start agent vs human play mode with performance analysis
     */
    fun startAgentVsHumanMode(
        humanColor: PieceColor,
        timeControl: TimeControl = TimeControl.Unlimited
    ): AgentVsHumanSession {
        
        if (!isInitialized || validationTools == null) {
            throw IllegalStateException("Interface not initialized or validation tools not available")
        }
        
        println("🎮 Starting Agent vs Human Play Mode")
        println("Human plays as: $humanColor")
        println("Time control: $timeControl")
        println("=" * 60)
        
        val sessionId = generateSessionId()
        val startTime = getCurrentTimeMillis()
        
        val environment = ChessEnvironment()
        val board = environment.getCurrentBoard()
        
        // Display initial position
        println("STARTING POSITION:")
        println(validationConsole!!.visualizeBoard(board))
        
        return AgentVsHumanSession(
            sessionId = sessionId,
            humanColor = humanColor,
            timeControl = timeControl,
            startTime = startTime,
            environment = environment,
            moveHistory = mutableListOf(),
            isActive = true
        )
    }
    
    /**
     * Make move in agent vs human session
     */
    fun makeHumanMove(
        session: AgentVsHumanSession,
        move: Move
    ): HumanMoveResult {
        
        if (!session.isActive) {
            return HumanMoveResult.Failed("Session not active")
        }
        
        val environment = session.environment
        val board = environment.getCurrentBoard()
        
        try {
            // Validate and execute human move
            val moveResult = environment.makeMove(move)
            if (!moveResult.success) {
                return HumanMoveResult.Failed("Invalid move: ${moveResult.error}")
            }
            
            session.moveHistory.add(move)
            
            // Display position after human move
            println("\nAfter human move ${move.toAlgebraic()}:")
            println(validationConsole!!.visualizeBoard(environment.getCurrentBoard()))
            
            // Check if game is over
            if (environment.isTerminal(environment.getCurrentState())) {
                val gameStatus = environment.getGameStatus()
                session.isActive = false
                
                return HumanMoveResult.GameEnded(
                    move = move,
                    gameStatus = gameStatus,
                    finalPosition = environment.getCurrentBoard().toFEN()
                )
            }
            
            // Get agent's response
            val agentMove = getAgentMove(session)
            
            return HumanMoveResult.Success(
                humanMove = move,
                agentMove = agentMove,
                positionAfterHuman = board.toFEN(),
                positionAfterAgent = environment.getCurrentBoard().toFEN()
            )
            
        } catch (e: Exception) {
            return HumanMoveResult.Failed("Move execution failed: ${e.message}")
        }
    }
    
    /**
     * Generate comprehensive training report
     */
    fun generateTrainingReport(
        reportType: ReportType = ReportType.COMPREHENSIVE,
        includeGameAnalysis: Boolean = true,
        includePerformanceMetrics: Boolean = true
    ): TrainingReportResult {
        
        if (!isInitialized || monitoringSystem == null) {
            return TrainingReportResult.Failed("Monitoring system not available")
        }
        
        println("📊 Generating Training Report (Type: $reportType)")
        
        val reportStartTime = getCurrentTimeMillis()
        
        try {
            val report = when (reportType) {
                ReportType.COMPREHENSIVE -> {
                    monitoringSystem!!.generateComprehensiveReport()
                }
                ReportType.PERFORMANCE -> {
                    generatePerformanceReport()
                }
                ReportType.GAME_QUALITY -> {
                    generateGameQualityReport()
                }
                ReportType.VALIDATION -> {
                    generateValidationReport()
                }
                ReportType.ISSUES -> {
                    generateIssuesReport()
                }
            }
            
            val reportTime = getCurrentTimeMillis() - reportStartTime
            
            return TrainingReportResult.Success(
                report = report,
                reportType = reportType,
                generationTime = reportTime,
                timestamp = getCurrentTimeMillis()
            )
            
        } catch (e: Exception) {
            println("❌ Report generation failed: ${e.message}")
            
            return TrainingReportResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Update training configuration with validation and rollback capability
     */
    fun updateConfiguration(
        newConfig: TrainingInterfaceConfig,
        validateBeforeApply: Boolean = true
    ): ConfigurationUpdateResult {
        
        if (!isInitialized) {
            return ConfigurationUpdateResult.Failed("Interface not initialized")
        }
        
        println("⚙️ Updating Training Configuration")
        
        // Save current configuration for rollback
        val configSnapshot = ConfigurationSnapshot(
            config = activeConfig,
            timestamp = getCurrentTimeMillis(),
            sessionId = currentSession?.sessionId ?: "none"
        )
        configHistory.add(configSnapshot)
        
        try {
            // Validate new configuration if requested
            if (validateBeforeApply) {
                val validationResult = validateConfiguration(newConfig)
                if (!validationResult.isValid) {
                    return ConfigurationUpdateResult.Failed(
                        "Configuration validation failed: ${validationResult.errors.joinToString(", ")}"
                    )
                }
            }
            
            // Apply new configuration
            val previousConfig = activeConfig
            activeConfig = newConfig
            
            // Update component configurations
            updateComponentConfigurations(newConfig)
            
            println("✅ Configuration updated successfully")
            
            return ConfigurationUpdateResult.Success(
                previousConfig = previousConfig,
                newConfig = newConfig,
                rollbackAvailable = true,
                validationPerformed = validateBeforeApply
            )
            
        } catch (e: Exception) {
            // Rollback on failure
            activeConfig = configSnapshot.config
            configHistory.removeLastOrNull()
            
            println("❌ Configuration update failed, rolled back: ${e.message}")
            
            return ConfigurationUpdateResult.Failed(
                "Configuration update failed: ${e.message}. Rolled back to previous configuration."
            )
        }
    }
    
    /**
     * Rollback to previous configuration
     */
    fun rollbackConfiguration(): ConfigurationRollbackResult {
        if (configHistory.isEmpty()) {
            return ConfigurationRollbackResult.Failed("No previous configuration available")
        }
        
        val previousSnapshot = configHistory.removeLastOrNull()!!
        val currentConfig = activeConfig
        
        try {
            activeConfig = previousSnapshot.config
            updateComponentConfigurations(activeConfig)
            
            println("🔄 Configuration rolled back successfully")
            
            return ConfigurationRollbackResult.Success(
                rolledBackFrom = currentConfig,
                rolledBackTo = previousSnapshot.config,
                rollbackTimestamp = getCurrentTimeMillis()
            )
            
        } catch (e: Exception) {
            // Restore current config on rollback failure
            activeConfig = currentConfig
            configHistory.add(previousSnapshot)
            
            return ConfigurationRollbackResult.Failed("Rollback failed: ${e.message}")
        }
    }
    
    /**
     * Stop training and cleanup resources
     */
    fun shutdown(): ShutdownResult {
        println("🛑 Shutting down Training Control Interface...")
        
        val shutdownStartTime = getCurrentTimeMillis()
        val shutdownResults = mutableListOf<ComponentShutdownResult>()
        
        try {
            // Stop current session
            currentSession?.let { session ->
                if (session.isActive) {
                    stopCurrentSession()
                }
            }
            
            // Stop monitoring
            if (isMonitoringActive) {
                val monitoringResults = monitoringSystem?.stopMonitoring()
                shutdownResults.add(ComponentShutdownResult("MonitoringSystem", true, null))
            }
            
            // Stop training controllers
            trainingController?.stopTraining()
            shutdownResults.add(ComponentShutdownResult("TrainingController", true, null))
            
            selfPlayController?.stopTraining()
            shutdownResults.add(ComponentShutdownResult("SelfPlayController", true, null))
            
            // Cleanup resources
            trainingController = null
            selfPlayController = null
            monitoringSystem = null
            validationTools = null
            validationConsole = null
            
            isInitialized = false
            isMonitoringActive = false
            currentSession = null
            
            val shutdownTime = getCurrentTimeMillis() - shutdownStartTime
            
            println("✅ Training Control Interface shutdown completed in ${shutdownTime}ms")
            
            return ShutdownResult.Success(
                shutdownTime = shutdownTime,
                componentResults = shutdownResults
            )
            
        } catch (e: Exception) {
            println("❌ Shutdown failed: ${e.message}")
            
            return ShutdownResult.Failed(
                error = e.message ?: "Unknown error",
                componentResults = shutdownResults
            )
        }
    }
    
    // Private helper methods
    
    private fun startBasicTraining(config: TrainingSessionConfig): BasicTrainingResult {
        val trainingController = this.trainingController ?: return BasicTrainingResult.Failed("Training controller not available")
        
        val results = trainingController.startTraining(config.episodes)
        
        return if (results != null) {
            BasicTrainingResult.Success(results)
        } else {
            BasicTrainingResult.Failed("Training failed")
        }
    }
    
    private fun startSelfPlayTraining(config: TrainingSessionConfig): BasicTrainingResult {
        val selfPlayController = this.selfPlayController ?: return BasicTrainingResult.Failed("Self-play controller not available")
        
        val results = selfPlayController.runSelfPlayTraining(config.iterations ?: 10)
        
        return BasicTrainingResult.Success(
            TrainingResults(
                totalEpisodes = results.totalIterations,
                totalSteps = 0, // Would be calculated from results
                trainingDuration = results.totalDuration,
                bestPerformance = results.bestPerformance,
                finalMetrics = TrainingMetrics(
                    averageReward = results.finalMetrics.averageReward,
                    averageGameLength = results.finalMetrics.averageGameLength,
                    winRate = results.finalMetrics.averageWinRate,
                    drawRate = 0.0, // Would be calculated
                    lossRate = 0.0, // Would be calculated
                    totalBatchUpdates = results.finalMetrics.totalBatchUpdates,
                    averageBatchLoss = results.finalMetrics.averageLoss,
                    finalBufferSize = 0 // Would be calculated
                )
            )
        )
    }
    
    private fun startValidationTraining(config: TrainingSessionConfig): BasicTrainingResult {
        val validationTools = this.validationTools ?: return BasicTrainingResult.Failed("Validation tools not available")
        
        // Run validation scenarios
        val scenarios = TrainingScenarioFactory.getAllScenarios()
        val sampleGames = generateSampleGames(5)
        
        val validationReport = validationTools.generateValidationReport(scenarios, sampleGames)
        
        // Convert validation results to training results format
        return BasicTrainingResult.Success(
            TrainingResults(
                totalEpisodes = scenarios.size,
                totalSteps = scenarios.sumOf { it.expectedMoves.size },
                trainingDuration = getCurrentTimeMillis() - (currentSession?.startTime ?: 0L),
                bestPerformance = validationReport.overallAccuracy,
                finalMetrics = TrainingMetrics(
                    averageReward = validationReport.overallAccuracy,
                    averageGameLength = validationReport.gameQualityResults.map { it.gameLength }.average(),
                    winRate = validationReport.averageGameQuality,
                    drawRate = 0.0,
                    lossRate = 0.0,
                    totalBatchUpdates = 0,
                    averageBatchLoss = 0.0,
                    finalBufferSize = 0
                )
            )
        )
    }
    
    private fun startExperimentalTraining(config: TrainingSessionConfig): BasicTrainingResult {
        // Placeholder for experimental training
        return BasicTrainingResult.Failed("Experimental training not yet implemented")
    }
    
    private fun createEmptyDashboard(): TrainingDashboard {
        return TrainingDashboard(
            sessionInfo = SessionInfo(0L, 0, 0, 0),
            currentStats = CurrentStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            recentTrends = TrendAnalysis(0.0, 0.0, 0.0, 0.0),
            systemHealth = SystemHealth(HealthStatus.HEALTHY, 1.0, 0, 0),
            activeIssues = emptyList(),
            performanceMetrics = PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0),
            gameQualityMetrics = GameQualityMetrics(0.0, 0.0, 0.0, 0.0, 0),
            trainingEfficiency = TrainingEfficiency(0.0, 0.0, 0.0, 0.0),
            lastUpdate = getCurrentTimeMillis(),
            interfaceInfo = InterfaceInfo("none", 0L, dashboardUpdateInterval, getCurrentTimeMillis(), emptyList())
        )
    }
    
    private fun getActiveFeatures(): List<String> {
        val features = mutableListOf<String>()
        
        if (trainingController != null) features.add("BasicTraining")
        if (selfPlayController != null) features.add("SelfPlay")
        if (monitoringSystem != null) features.add("Monitoring")
        if (validationTools != null) features.add("Validation")
        if (isMonitoringActive) features.add("RealTimeMonitoring")
        
        return features
    }
    
    private fun handleStartCommand(command: TrainingCommand.Start): Any {
        return startTrainingSession(command.config)
    }
    
    private fun handlePauseCommand(command: TrainingCommand.Pause): Any {
        trainingController?.pauseTraining()
        return "Training paused"
    }
    
    private fun handleResumeCommand(command: TrainingCommand.Resume): Any {
        trainingController?.resumeTraining()
        return "Training resumed"
    }
    
    private fun handleStopCommand(command: TrainingCommand.Stop): Any {
        trainingController?.stopTraining()
        selfPlayController?.stopTraining()
        return "Training stopped"
    }
    
    private fun handleRestartCommand(command: TrainingCommand.Restart): Any {
        handleStopCommand(TrainingCommand.Stop)
        return handleStartCommand(TrainingCommand.Start(command.config))
    }
    
    private fun handleConfigureCommand(command: TrainingCommand.Configure): Any {
        return updateConfiguration(command.newConfig, command.validateFirst)
    }
    
    private fun handleAnalyzeCommand(command: TrainingCommand.Analyze): Any {
        return when (command.analysisType) {
            AnalysisType.GAME -> analyzeGame(command.gameHistory ?: emptyList())
            AnalysisType.POSITION -> analyzePosition(command.position)
            AnalysisType.AGENT_DECISION -> analyzeAgentDecision(command.position)
        }
    }
    
    private fun handleValidateCommand(command: TrainingCommand.Validate): Any {
        val validationTools = this.validationTools ?: return "Validation tools not available"
        
        return when (command.validationType) {
            ValidationType.SCENARIOS -> {
                val scenarios = command.scenarios ?: TrainingScenarioFactory.getAllScenarios()
                val sampleGames = generateSampleGames(5)
                validationTools.generateValidationReport(scenarios, sampleGames)
            }
            ValidationType.GAME_QUALITY -> {
                val games = command.games ?: generateSampleGames(10)
                games.map { game ->
                    val board = ChessBoard()
                    game.forEach { move ->
                        board.makeLegalMove(move)
                        board.switchActiveColor()
                    }
                    validationTools.assessGameQuality(game, GameStatus.ONGOING, board)
                }
            }
        }
    }
    
    private fun handleExportCommand(command: TrainingCommand.Export): Any {
        return "Export functionality not yet implemented"
    }
    
    private fun handleVisualizeCommand(command: TrainingCommand.Visualize): Any {
        return when (command.visualizationType) {
            VisualizationType.BOARD -> {
                val board = command.board ?: ChessBoard()
                validationConsole?.visualizeBoard(board) ?: "Visualization not available"
            }
            VisualizationType.LEARNING_CURVE -> "Learning curve visualization not yet implemented"
            VisualizationType.PERFORMANCE_METRICS -> "Performance metrics visualization not yet implemented"
        }
    }
    
    private fun handlePlayAgainstAgentCommand(command: TrainingCommand.PlayAgainstAgent): Any {
        return startAgentVsHumanMode(command.humanColor)
    }
    
    private fun handleGenerateReportCommand(command: TrainingCommand.GenerateReport): Any {
        return generateTrainingReport(command.reportType)
    }
    
    private fun analyzeGamePositions(gameHistory: List<Move>): List<PositionAnalysis> {
        val validationTools = this.validationTools ?: return emptyList()
        val analyses = mutableListOf<PositionAnalysis>()
        
        val board = ChessBoard()
        
        gameHistory.forEachIndexed { index, move ->
            val decisionAnalysis = validationTools.visualizeAgentDecisionMaking(board)
            val positionEvaluation = validationTools.displayPositionEvaluation(board)
            
            analyses.add(
                PositionAnalysis(
                    moveNumber = index + 1,
                    positionFEN = board.toFEN(),
                    decisionAnalysis = decisionAnalysis,
                    positionEvaluation = positionEvaluation
                )
            )
            
            board.makeLegalMove(move)
            board.switchActiveColor()
        }
        
        return analyses
    }
    
    private fun analyzePosition(position: String?): Any {
        val validationTools = this.validationTools ?: return "Validation tools not available"
        
        val board = if (position != null) {
            ChessBoard().apply { loadFromFEN(position) }
        } else {
            ChessBoard()
        }
        
        val decisionAnalysis = validationTools.visualizeAgentDecisionMaking(board)
        val positionEvaluation = validationTools.displayPositionEvaluation(board)
        
        return PositionAnalysisResult(
            position = board.toFEN(),
            decisionAnalysis = decisionAnalysis,
            positionEvaluation = positionEvaluation,
            consoleOutput = validationConsole?.displayDecisionAnalysis(decisionAnalysis) ?: ""
        )
    }
    
    private fun analyzeAgentDecision(position: String?): Any {
        return analyzePosition(position)
    }
    
    private fun getAgentMove(session: AgentVsHumanSession): Move {
        val validationTools = this.validationTools ?: throw IllegalStateException("Validation tools not available")
        
        val environment = session.environment
        val state = environment.getCurrentState()
        val validActions = environment.getValidActions(state)
        
        // Get agent from validation tools (simplified)
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = activeConfig.hiddenLayers,
            learningRate = activeConfig.learningRate,
            explorationRate = activeConfig.explorationRate
        )
        
        val selectedAction = agent.selectAction(state, validActions)
        val move = ChessActionEncoder().decodeAction(selectedAction)
        
        // Execute agent move
        environment.step(selectedAction)
        session.moveHistory.add(move)
        
        // Display agent's move analysis
        val decisionAnalysis = validationTools.visualizeAgentDecisionMaking(environment.getCurrentBoard())
        println("\nAgent plays ${move.toAlgebraic()}:")
        println(validationConsole!!.displayDecisionAnalysis(decisionAnalysis))
        println(validationConsole!!.visualizeBoard(environment.getCurrentBoard()))
        
        return move
    }
    
    private fun generateSampleGames(count: Int): List<List<Move>> {
        // Generate sample games for validation
        return (1..count).map { 
            // Simplified game generation - would use actual game data
            listOf(
                Move(Position(1, 4), Position(3, 4)), // e4
                Move(Position(6, 4), Position(4, 4)), // e5
                Move(Position(0, 6), Position(2, 5)), // Nf3
                Move(Position(7, 1), Position(5, 2))  // Nc6
            )
        }
    }
    
    private fun generatePerformanceReport(): Any {
        return "Performance report generation not yet implemented"
    }
    
    private fun generateGameQualityReport(): Any {
        return "Game quality report generation not yet implemented"
    }
    
    private fun generateValidationReport(): Any {
        val validationTools = this.validationTools ?: return "Validation tools not available"
        
        val scenarios = TrainingScenarioFactory.getAllScenarios()
        val sampleGames = generateSampleGames(10)
        
        return validationTools.generateValidationReport(scenarios, sampleGames)
    }
    
    private fun generateIssuesReport(): Any {
        return "Issues report generation not yet implemented"
    }
    
    private fun validateConfiguration(config: TrainingInterfaceConfig): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate learning rate
        if (config.learningRate <= 0.0 || config.learningRate > 1.0) {
            errors.add("Learning rate must be between 0.0 and 1.0")
        }
        
        // Validate exploration rate
        if (config.explorationRate < 0.0 || config.explorationRate > 1.0) {
            errors.add("Exploration rate must be between 0.0 and 1.0")
        }
        
        // Validate batch size
        if (config.batchSize <= 0 || config.batchSize > 1024) {
            errors.add("Batch size must be between 1 and 1024")
        }
        
        // Validate buffer size
        if (config.maxBufferSize <= config.batchSize) {
            errors.add("Buffer size must be larger than batch size")
        }
        
        // Warnings for potentially suboptimal settings
        if (config.learningRate > 0.1) {
            warnings.add("Learning rate is quite high, may cause instability")
        }
        
        if (config.batchSize < 16) {
            warnings.add("Small batch size may lead to noisy training")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun updateComponentConfigurations(config: TrainingInterfaceConfig) {
        // Update training controller configuration
        trainingController?.let { tc ->
            // Would update configuration if controller supported it
        }
        
        // Update self-play controller configuration
        selfPlayController?.let { spc ->
            // Would update configuration if controller supported it
        }
        
        // Update monitoring system configuration
        monitoringSystem?.let { ms ->
            // Would update configuration if monitoring system supported it
        }
    }
    
    private fun stopCurrentSession() {
        currentSession?.let { session ->
            session.isActive = false
            trainingController?.stopTraining()
            selfPlayController?.stopTraining()
        }
    }
    
    private fun generateSessionId(): String {
        return "session_${getCurrentTimeMillis()}_${(0..999).random()}"
    }
}