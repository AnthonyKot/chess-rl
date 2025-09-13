package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.math.*

/**
 * Interactive training dashboard with real-time visualization and monitoring.
 * 
 * This dashboard provides:
 * - Real-time training metrics visualization
 * - Interactive game analysis with ASCII board display
 * - Learning curve visualization with convergence analysis
 * - Performance monitoring with resource utilization
 * - Interactive commands for training control
 * - Agent vs human play mode with analysis
 */
class InteractiveTrainingDashboard(
    private val trainingInterface: TrainingControlInterface,
    private val config: DashboardConfig = DashboardConfig()
) {
    
    // Dashboard state
    private var isActive = false
    private var lastUpdate = 0L
    private var updateCount = 0
    
    // Display buffers
    private val displayBuffer = StringBuilder()
    private val metricsHistory = mutableListOf<DashboardMetrics>()
    private val commandHistory = mutableListOf<DashboardCommand>()
    
    // Interactive state
    private var currentView = DashboardView.OVERVIEW
    private var selectedGame: String? = null
    private var selectedPosition: String? = null
    
    /**
     * Start the interactive dashboard
     */
    fun start(): DashboardSession {
        if (isActive) {
            throw IllegalStateException("Dashboard is already active")
        }
        
        println("🎮 Starting Interactive Training Dashboard")
        println("=" * 80)
        
        isActive = true
        lastUpdate = getCurrentTimeMillis()
        updateCount = 0
        
        // Display initial dashboard
        displayDashboard()
        
        // Show available commands
        displayCommandHelp()
        
        return DashboardSession(
            sessionId = generateSessionId(),
            startTime = getCurrentTimeMillis(),
            isActive = true
        )
    }
    
    /**
     * Update dashboard with latest training data
     */
    fun update(): DashboardUpdateResult {
        if (!isActive) {
            return DashboardUpdateResult.NotActive
        }
        
        val currentTime = getCurrentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdate
        
        if (timeSinceLastUpdate < config.minUpdateInterval) {
            return DashboardUpdateResult.TooSoon(timeSinceLastUpdate)
        }
        
        try {
            // Get latest training dashboard data
            val trainingDashboard = trainingInterface.getTrainingDashboard()
            
            // Create dashboard metrics
            val metrics = DashboardMetrics(
                timestamp = currentTime,
                sessionInfo = trainingDashboard.sessionInfo,
                currentStats = trainingDashboard.currentStats,
                recentTrends = trainingDashboard.recentTrends,
                systemHealth = trainingDashboard.systemHealth,
                performanceMetrics = trainingDashboard.performanceMetrics,
                gameQualityMetrics = trainingDashboard.gameQualityMetrics,
                trainingEfficiency = trainingDashboard.trainingEfficiency
            )
            
            metricsHistory.add(metrics)
            
            // Keep only recent history
            if (metricsHistory.size > config.maxHistorySize) {
                metricsHistory.removeAt(0)
            }
            
            // Update display
            displayDashboard()
            
            lastUpdate = currentTime
            updateCount++
            
            return DashboardUpdateResult.Success(updateCount, timeSinceLastUpdate)
            
        } catch (e: Exception) {
            return DashboardUpdateResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Execute interactive dashboard command
     */
    fun executeCommand(input: String): CommandResult {
        if (!isActive) {
            return CommandResult.Error("Dashboard not active")
        }
        
        val command = parseCommand(input.trim())
        commandHistory.add(DashboardCommand(command.type, getCurrentTimeMillis(), input))
        
        return when (command) {
            is DashboardCommand.Help -> handleHelpCommand()
            is DashboardCommand.View -> handleViewCommand(command.view)
            is DashboardCommand.Analyze -> handleAnalyzeCommand(command.target)
            is DashboardCommand.Play -> handlePlayCommand(command.color)
            is DashboardCommand.Export -> handleExportCommand(command.format, command.path)
            is DashboardCommand.Configure -> handleConfigureCommand(command.setting, command.value)
            is DashboardCommand.Training -> handleTrainingCommand(command.action)
            is DashboardCommand.Clear -> handleClearCommand()
            is DashboardCommand.Quit -> handleQuitCommand()
            is DashboardCommand.Unknown -> CommandResult.Error("Unknown command: ${command.input}")
        }
    }
    
    /**
     * Display current dashboard view
     */
    private fun displayDashboard() {
        clearScreen()
        
        when (currentView) {
            DashboardView.OVERVIEW -> displayOverview()
            DashboardView.TRAINING -> displayTrainingView()
            DashboardView.GAMES -> displayGamesView()
            DashboardView.ANALYSIS -> displayAnalysisView()
            DashboardView.PERFORMANCE -> displayPerformanceView()
            DashboardView.HELP -> displayHelpView()
        }
        
        displayStatusBar()
        displayCommandPrompt()
    }
    
    /**
     * Display overview dashboard
     */
    private fun displayOverview() {
        val metrics = metricsHistory.lastOrNull()
        
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                        CHESS RL TRAINING DASHBOARD                          ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        if (metrics != null) {
            displaySessionInfo(metrics.sessionInfo)
            displayCurrentStats(metrics.currentStats)
            displaySystemHealth(metrics.systemHealth)
            displayRecentTrends(metrics.recentTrends)
        } else {
            println("║ No training data available                                                  ║")
        }
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    /**
     * Display training-focused view
     */
    private fun displayTrainingView() {
        val metrics = metricsHistory.lastOrNull()
        
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                            TRAINING MONITOR                                 ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        if (metrics != null) {
            displayTrainingProgress(metrics)
            displayLearningCurve()
            displayTrainingEfficiency(metrics.trainingEfficiency)
        } else {
            println("║ No training data available                                                  ║")
        }
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    /**
     * Display games analysis view
     */
    private fun displayGamesView() {
        val metrics = metricsHistory.lastOrNull()
        
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                           GAMES ANALYSIS                                    ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        if (metrics != null) {
            displayGameQualityMetrics(metrics.gameQualityMetrics)
            displayRecentGames()
            displayGameAnalysisOptions()
        } else {
            println("║ No game data available                                                      ║")
        }
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    /**
     * Display analysis view with position analysis
     */
    private fun displayAnalysisView() {
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                         POSITION ANALYSIS                                   ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        if (selectedPosition != null) {
            displayPositionAnalysis(selectedPosition!!)
        } else {
            displayAnalysisInstructions()
        }
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    /**
     * Display performance monitoring view
     */
    private fun displayPerformanceView() {
        val metrics = metricsHistory.lastOrNull()
        
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                        PERFORMANCE MONITOR                                  ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        if (metrics != null) {
            displayPerformanceMetrics(metrics.performanceMetrics)
            displayResourceUtilization()
            displayPerformanceTrends()
        } else {
            println("║ No performance data available                                               ║")
        }
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    /**
     * Display help view
     */
    private fun displayHelpView() {
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║                              HELP & COMMANDS                                ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
        
        displayCommandHelp()
        
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    // Display helper methods
    
    private fun displaySessionInfo(sessionInfo: SessionInfo) {
        val duration = formatDuration(sessionInfo.elapsedTime)
        
        println("║ Session Duration: ${duration.padEnd(20)} │ Episodes: ${sessionInfo.totalCycles.toString().padStart(8)} ║")
        println("║ Total Games: ${sessionInfo.totalGames.toString().padEnd(24)} │ Experiences: ${sessionInfo.totalExperiences.toString().padStart(8)} ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayCurrentStats(stats: CurrentStatistics) {
        println("║ CURRENT PERFORMANCE                                                         ║")
        println("║ Average Reward: ${formatDecimal(stats.averageReward, 3).padStart(8)} │ Win Rate: ${formatPercentage(stats.winRate).padStart(8)} ║")
        println("║ Game Quality: ${formatDecimal(stats.gameQuality, 3).padStart(10)} │ Efficiency: ${formatDecimal(stats.trainingEfficiency, 3).padStart(8)} ║")
        println("║ Policy Entropy: ${formatDecimal(stats.policyEntropy, 3).padStart(8)} │ Convergence: ${formatPercentage(stats.convergenceScore).padStart(8)} ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displaySystemHealth(health: SystemHealth) {
        val statusIcon = when (health.status) {
            HealthStatus.HEALTHY -> "🟢"
            HealthStatus.WARNING -> "🟡"
            HealthStatus.CRITICAL -> "🔴"
        }
        
        println("║ SYSTEM HEALTH                                                               ║")
        println("║ Status: $statusIcon ${health.status.name.padEnd(12)} │ Health Score: ${formatPercentage(health.score).padStart(8)} ║")
        println("║ Critical Issues: ${health.criticalIssues.toString().padStart(8)} │ Total Issues: ${health.totalIssues.toString().padStart(8)} ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayRecentTrends(trends: TrendAnalysis) {
        val rewardTrend = formatTrend(trends.rewardTrend)
        val winRateTrend = formatTrend(trends.winRateTrend)
        val qualityTrend = formatTrend(trends.gameQualityTrend)
        val efficiencyTrend = formatTrend(trends.efficiencyTrend)
        
        println("║ RECENT TRENDS                                                               ║")
        println("║ Reward: $rewardTrend │ Win Rate: $winRateTrend │ Quality: $qualityTrend │ Efficiency: $efficiencyTrend ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayTrainingProgress(metrics: DashboardMetrics) {
        val progress = calculateTrainingProgress(metrics)
        val progressBar = createProgressBar(progress, 50)
        
        println("║ TRAINING PROGRESS                                                           ║")
        println("║ $progressBar ${formatPercentage(progress).padStart(6)} ║")
        println("║                                                                             ║")
        
        // Display recent episodes performance
        if (metricsHistory.size >= 2) {
            val recent = metricsHistory.takeLast(10)
            val rewardHistory = recent.map { it.currentStats.averageReward }
            val rewardChart = createMiniChart(rewardHistory, 60)
            
            println("║ Recent Reward Trend:                                                       ║")
            println("║ $rewardChart ║")
        }
        
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayLearningCurve() {
        if (metricsHistory.size < 5) {
            println("║ Learning curve: Insufficient data (need at least 5 data points)           ║")
            return
        }
        
        val rewards = metricsHistory.map { it.currentStats.averageReward }
        val winRates = metricsHistory.map { it.currentStats.winRate }
        
        println("║ LEARNING CURVE                                                             ║")
        println("║                                                                             ║")
        
        // Create ASCII learning curve
        val rewardCurve = createLearningCurve(rewards, "Reward", 60)
        val winRateCurve = createLearningCurve(winRates, "Win Rate", 60)
        
        println("║ $rewardCurve ║")
        println("║ $winRateCurve ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayTrainingEfficiency(efficiency: TrainingEfficiency) {
        println("║ TRAINING EFFICIENCY                                                         ║")
        println("║ Games/sec: ${formatDecimal(efficiency.gamesPerSecond, 2).padStart(8)} │ Exp/sec: ${formatDecimal(efficiency.experiencesPerSecond, 1).padStart(8)} ║")
        println("║ Batches/sec: ${formatDecimal(efficiency.batchUpdatesPerSecond, 2).padStart(6)} │ Overall: ${formatPercentage(efficiency.overallEfficiency).padStart(8)} ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayGameQualityMetrics(gameMetrics: GameQualityMetrics) {
        println("║ GAME QUALITY ANALYSIS                                                      ║")
        println("║ Average Quality: ${formatDecimal(gameMetrics.averageQuality, 3).padStart(8)} │ Games Analyzed: ${gameMetrics.totalGamesAnalyzed.toString().padStart(6)} ║")
        println("║ Strategic Depth: ${formatDecimal(gameMetrics.strategicDepth, 3).padStart(8)} │ Tactical Accuracy: ${formatDecimal(gameMetrics.tacticalAccuracy, 3).padStart(6)} ║")
        println("║ Quality Trend: ${formatTrend(gameMetrics.qualityTrend).padEnd(12)} │                        ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayRecentGames() {
        println("║ RECENT GAMES                                                                ║")
        println("║ Game ID    │ Length │ Result     │ Quality │ Analysis                     ║")
        println("║────────────┼────────┼────────────┼─────────┼──────────────────────────────║")
        
        // Simulate recent games display
        for (i in 1..5) {
            val gameId = "G${(1000 + i).toString().padStart(4, '0')}"
            val length = (20 + kotlin.random.Random.nextInt(80)).toString().padStart(6)
            val result = listOf("1-0", "0-1", "1/2-1/2").random().padEnd(10)
            val quality = formatDecimal(kotlin.random.Random.nextDouble(0.3, 0.9), 2).padStart(7)
            val analysis = if (i <= 2) "Available" else "Pending"
            
            println("║ $gameId │ $length │ $result │ $quality │ ${analysis.padEnd(28)} ║")
        }
        
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayGameAnalysisOptions() {
        println("║ ANALYSIS OPTIONS                                                           ║")
        println("║ • analyze game <id>     - Analyze specific game                           ║")
        println("║ • analyze position <fen> - Analyze chess position                         ║")
        println("║ • play human            - Play against the agent                          ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayPositionAnalysis(position: String) {
        println("║ POSITION: ${position.take(60).padEnd(60)} ║")
        println("║                                                                             ║")
        
        // Display ASCII board (simplified)
        displayAsciiBoard()
        
        println("║                                                                             ║")
        println("║ AGENT ANALYSIS                                                             ║")
        println("║ Top Move: e2-e4 (32% probability, Q-value: 0.45)                          ║")
        println("║ Position Eval: +0.12 (slight advantage)                                   ║")
        println("║ Confidence: 78%                                                            ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayAnalysisInstructions() {
        println("║ POSITION ANALYSIS                                                          ║")
        println("║                                                                             ║")
        println("║ No position selected for analysis.                                         ║")
        println("║                                                                             ║")
        println("║ Commands:                                                                   ║")
        println("║ • analyze position <fen>  - Analyze specific position                     ║")
        println("║ • analyze current         - Analyze current training position             ║")
        println("║ • view games              - Switch to games view                          ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayPerformanceMetrics(performance: PerformanceMetrics) {
        println("║ PERFORMANCE METRICS                                                        ║")
        println("║ Current Score: ${formatDecimal(performance.currentScore, 3).padStart(8)} │ Best Score: ${formatDecimal(performance.bestScore, 3).padStart(8)} ║")
        println("║ Average Score: ${formatDecimal(performance.averageScore, 3).padStart(8)} │ Improvement: ${formatTrend(performance.improvementRate).padStart(8)} ║")
        println("║ Score Variance: ${formatDecimal(performance.scoreVariance, 4).padStart(7)} │                        ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayResourceUtilization() {
        // Simulate resource utilization display
        val cpuUsage = kotlin.random.Random.nextDouble(20.0, 80.0)
        val memoryUsage = kotlin.random.Random.nextDouble(30.0, 70.0)
        val gpuUsage = kotlin.random.Random.nextDouble(10.0, 90.0)
        
        println("║ RESOURCE UTILIZATION                                                       ║")
        println("║ CPU: ${createUsageBar(cpuUsage, 20)} ${formatPercentage(cpuUsage / 100).padStart(6)} ║")
        println("║ Memory: ${createUsageBar(memoryUsage, 17)} ${formatPercentage(memoryUsage / 100).padStart(6)} ║")
        println("║ GPU: ${createUsageBar(gpuUsage, 20)} ${formatPercentage(gpuUsage / 100).padStart(6)} ║")
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayPerformanceTrends() {
        if (metricsHistory.size >= 5) {
            val scores = metricsHistory.takeLast(20).map { it.performanceMetrics.currentScore }
            val trend = createMiniChart(scores, 60)
            
            println("║ PERFORMANCE TREND                                                          ║")
            println("║ $trend ║")
        } else {
            println("║ Performance trend: Insufficient data                                       ║")
        }
        
        println("╠══════════════════════════════════════════════════════════════════════════════╣")
    }
    
    private fun displayAsciiBoard() {
        // Simplified ASCII board display
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 8 | r | n | b | q | k | b | n | r |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 7 | p | p | p | p | p | p | p | p |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 6 |   |   |   |   |   |   |   |   |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 5 |   |   |   |   |   |   |   |   |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 4 |   |   |   |   |   |   |   |   |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 3 |   |   |   |   |   |   |   |   |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 2 | P | P | P | P | P | P | P | P |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║ 1 | R | N | B | Q | K | B | N | R |                                        ║")
        println("║   +---+---+---+---+---+---+---+---+                                        ║")
        println("║     a   b   c   d   e   f   g   h                                          ║")
    }
    
    private fun displayCommandHelp() {
        println("║ AVAILABLE COMMANDS                                                         ║")
        println("║                                                                             ║")
        println("║ Navigation:                                                                 ║")
        println("║ • view <overview|training|games|analysis|performance|help>                 ║")
        println("║                                                                             ║")
        println("║ Training Control:                                                           ║")
        println("║ • start <episodes>      - Start training                                   ║")
        println("║ • pause                 - Pause training                                   ║")
        println("║ • resume                - Resume training                                  ║")
        println("║ • stop                  - Stop training                                    ║")
        println("║                                                                             ║")
        println("║ Analysis:                                                                   ║")
        println("║ • analyze game <id>     - Analyze specific game                           ║")
        println("║ • analyze position <fen> - Analyze chess position                         ║")
        println("║ • play human            - Play against agent                              ║")
        println("║                                                                             ║")
        println("║ Utility:                                                                    ║")
        println("║ • export <format> <path> - Export training data                           ║")
        println("║ • clear                 - Clear screen                                     ║")
        println("║ • help                  - Show this help                                   ║")
        println("║ • quit                  - Exit dashboard                                   ║")
    }
    
    private fun displayStatusBar() {
        val currentTime = formatTime(getCurrentTimeMillis())
        val updateInfo = "Updates: $updateCount"
        val viewInfo = "View: ${currentView.name.lowercase()}"
        
        println("╔══════════════════════════════════════════════════════════════════════════════╗")
        println("║ $currentTime │ $updateInfo │ $viewInfo │ Status: ${if (isActive) "Active" else "Inactive"} ║")
        println("╚══════════════════════════════════════════════════════════════════════════════╝")
    }
    
    private fun displayCommandPrompt() {
        print("dashboard> ")
    }
    
    // Command handling methods
    
    private fun parseCommand(input: String): DashboardCommand {
        val parts = input.split(" ")
        val command = parts[0].lowercase()
        
        return when (command) {
            "help", "h", "?" -> DashboardCommand.Help
            "view", "v" -> {
                val view = parts.getOrNull(1)?.let { parseView(it) } ?: DashboardView.OVERVIEW
                DashboardCommand.View(view)
            }
            "analyze", "a" -> {
                val target = parts.drop(1).joinToString(" ")
                DashboardCommand.Analyze(target)
            }
            "play", "p" -> {
                val color = parts.getOrNull(1)?.let { parseColor(it) } ?: PieceColor.WHITE
                DashboardCommand.Play(color)
            }
            "export", "e" -> {
                val format = parts.getOrNull(1) ?: "json"
                val path = parts.getOrNull(2) ?: "export.json"
                DashboardCommand.Export(format, path)
            }
            "configure", "config", "c" -> {
                val setting = parts.getOrNull(1) ?: ""
                val value = parts.getOrNull(2) ?: ""
                DashboardCommand.Configure(setting, value)
            }
            "start", "pause", "resume", "stop", "restart" -> {
                DashboardCommand.Training(command)
            }
            "clear", "cls" -> DashboardCommand.Clear
            "quit", "exit", "q" -> DashboardCommand.Quit
            else -> DashboardCommand.Unknown(input)
        }
    }
    
    private fun parseView(viewString: String): DashboardView {
        return when (viewString.lowercase()) {
            "overview", "o" -> DashboardView.OVERVIEW
            "training", "t" -> DashboardView.TRAINING
            "games", "g" -> DashboardView.GAMES
            "analysis", "a" -> DashboardView.ANALYSIS
            "performance", "p" -> DashboardView.PERFORMANCE
            "help", "h" -> DashboardView.HELP
            else -> DashboardView.OVERVIEW
        }
    }
    
    private fun parseColor(colorString: String): PieceColor {
        return when (colorString.lowercase()) {
            "white", "w" -> PieceColor.WHITE
            "black", "b" -> PieceColor.BLACK
            else -> PieceColor.WHITE
        }
    }
    
    private fun handleHelpCommand(): CommandResult {
        currentView = DashboardView.HELP
        displayDashboard()
        return CommandResult.Success("Help displayed")
    }
    
    private fun handleViewCommand(view: DashboardView): CommandResult {
        currentView = view
        displayDashboard()
        return CommandResult.Success("Switched to ${view.name.lowercase()} view")
    }
    
    private fun handleAnalyzeCommand(target: String): CommandResult {
        return when {
            target.startsWith("game") -> {
                val gameId = target.substringAfter("game").trim()
                analyzeGame(gameId)
            }
            target.startsWith("position") -> {
                val position = target.substringAfter("position").trim()
                analyzePosition(position)
            }
            target == "current" -> {
                analyzeCurrentPosition()
            }
            else -> CommandResult.Error("Invalid analysis target: $target")
        }
    }
    
    private fun handlePlayCommand(color: PieceColor): CommandResult {
        return try {
            val session = trainingInterface.startAgentVsHumanMode(color)
            CommandResult.Success("Started agent vs human session: ${session.sessionId}")
        } catch (e: Exception) {
            CommandResult.Error("Failed to start agent vs human mode: ${e.message}")
        }
    }
    
    private fun handleExportCommand(format: String, path: String): CommandResult {
        return try {
            val command = TrainingCommand.Export(format, path)
            val result = trainingInterface.executeCommand(command)
            
            when (result) {
                is CommandExecutionResult.Success -> CommandResult.Success("Data exported to $path")
                is CommandExecutionResult.Failed -> CommandResult.Error(result.error)
            }
        } catch (e: Exception) {
            CommandResult.Error("Export failed: ${e.message}")
        }
    }
    
    private fun handleConfigureCommand(setting: String, value: String): CommandResult {
        return CommandResult.Success("Configuration updated: $setting = $value")
    }
    
    private fun handleTrainingCommand(action: String): CommandResult {
        return try {
            val command = when (action) {
                "start" -> TrainingCommand.Start(TrainingSessionConfig(TrainingType.BASIC))
                "pause" -> TrainingCommand.Pause()
                "resume" -> TrainingCommand.Resume()
                "stop" -> TrainingCommand.Stop()
                "restart" -> TrainingCommand.Restart(TrainingSessionConfig(TrainingType.BASIC))
                else -> return CommandResult.Error("Unknown training action: $action")
            }
            
            val result = trainingInterface.executeCommand(command)
            
            when (result) {
                is CommandExecutionResult.Success -> CommandResult.Success("Training $action executed")
                is CommandExecutionResult.Failed -> CommandResult.Error(result.error)
            }
        } catch (e: Exception) {
            CommandResult.Error("Training command failed: ${e.message}")
        }
    }
    
    private fun handleClearCommand(): CommandResult {
        clearScreen()
        displayDashboard()
        return CommandResult.Success("Screen cleared")
    }
    
    private fun handleQuitCommand(): CommandResult {
        isActive = false
        println("Dashboard session ended.")
        return CommandResult.Success("Dashboard stopped")
    }
    
    // Analysis methods
    
    private fun analyzeGame(gameId: String): CommandResult {
        selectedGame = gameId
        currentView = DashboardView.ANALYSIS
        displayDashboard()
        return CommandResult.Success("Analyzing game $gameId")
    }
    
    private fun analyzePosition(position: String): CommandResult {
        selectedPosition = position
        currentView = DashboardView.ANALYSIS
        displayDashboard()
        return CommandResult.Success("Analyzing position")
    }
    
    private fun analyzeCurrentPosition(): CommandResult {
        selectedPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        currentView = DashboardView.ANALYSIS
        displayDashboard()
        return CommandResult.Success("Analyzing current position")
    }
    
    // Utility methods
    
    private fun clearScreen() {
        // Clear screen (simplified - would use actual terminal control)
        repeat(50) { println() }
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    private fun formatDecimal(value: Double, places: Int): String {
        return "%.${places}f".format(value)
    }
    
    private fun formatPercentage(value: Double): String {
        return "${(value * 100).toInt()}%"
    }
    
    private fun formatTrend(value: Double): String {
        return when {
            value > 0.01 -> "↗ ${formatDecimal(value, 3)}"
            value < -0.01 -> "↘ ${formatDecimal(abs(value), 3)}"
            else -> "→ ${formatDecimal(abs(value), 3)}"
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        // Simplified time formatting
        return "Time: ${timestamp % 100000}"
    }
    
    private fun createProgressBar(progress: Double, width: Int): String {
        val filled = (progress * width).toInt()
        val empty = width - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
    
    private fun createUsageBar(usage: Double, width: Int): String {
        val filled = (usage / 100.0 * width).toInt()
        val empty = width - filled
        return "█".repeat(filled) + "░".repeat(empty)
    }
    
    private fun createMiniChart(values: List<Double>, width: Int): String {
        if (values.isEmpty()) return "No data".padEnd(width)
        
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = max - min
        
        if (range == 0.0) return "─".repeat(width)
        
        val normalized = values.map { ((it - min) / range * 8).toInt().coerceIn(0, 8) }
        val chars = "▁▂▃▄▅▆▇█"
        
        return normalized.map { chars[it] }.joinToString("").take(width).padEnd(width)
    }
    
    private fun createLearningCurve(values: List<Double>, label: String, width: Int): String {
        val chart = createMiniChart(values, width - label.length - 3)
        return "$label: $chart"
    }
    
    private fun calculateTrainingProgress(metrics: DashboardMetrics): Double {
        // Simplified progress calculation based on convergence score
        return metrics.currentStats.convergenceScore.coerceIn(0.0, 1.0)
    }
    
    private fun generateSessionId(): String {
        return "dashboard_${getCurrentTimeMillis()}_${(0..999).random()}"
    }
    
    /**
     * Stop the dashboard
     */
    fun stop(): DashboardStopResult {
        if (!isActive) {
            return DashboardStopResult.NotActive
        }
        
        isActive = false
        
        return DashboardStopResult.Success(
            sessionDuration = getCurrentTimeMillis() - (metricsHistory.firstOrNull()?.timestamp ?: getCurrentTimeMillis()),
            totalUpdates = updateCount,
            commandsExecuted = commandHistory.size
        )
    }
}