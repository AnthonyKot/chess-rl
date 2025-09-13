package com.chessrl.integration

/**
 * Advanced Training CLI with comprehensive lifecycle management
 * 
 * Features:
 * - Training lifecycle management (start/pause/resume/stop/restart)
 * - Real-time configuration adjustment with validation
 * - Training experiment management and comparison
 * - Integration with all existing controllers
 */
object AdvancedTrainingCLI {
    
    private val controller = AdvancedTrainingController()
    private var isInteractiveMode = false
    
    /**
     * Main CLI entry point
     */
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            startInteractiveMode()
            return
        }
        
        when (args[0].lowercase()) {
            "start" -> handleStartCommand(args.drop(1))
            "pause" -> handlePauseCommand()
            "resume" -> handleResumeCommand()
            "stop" -> handleStopCommand()
            "restart" -> handleRestartCommand(args.drop(1))
            "status" -> handleStatusCommand()
            "config" -> handleConfigCommand(args.drop(1))
            "experiment" -> handleExperimentCommand(args.drop(1))
            "report" -> handleReportCommand()
            "interactive" -> startInteractiveMode()
            "help" -> printUsage()
            else -> {
                println("❌ Unknown command: ${args[0]}")
                printUsage()
            }
        }
    }
    
    /**
     * Start interactive mode for real-time training management
     */
    private fun startInteractiveMode() {
        isInteractiveMode = true
        println("🎮 Advanced Training Control - Interactive Mode")
        println("=" * 60)
        println("Type 'help' for commands or 'exit' to quit")
        
        while (isInteractiveMode) {
            print("\ntraining> ")
            val input = readLine()?.trim() ?: continue
            
            if (input.isEmpty()) continue
            
            val args = input.split(" ")
            when (args[0].lowercase()) {
                "exit", "quit" -> {
                    isInteractiveMode = false
                    println("👋 Exiting interactive mode")
                }
                "help" -> printInteractiveHelp()
                "start" -> handleStartCommand(args.drop(1))
                "pause" -> handlePauseCommand()
                "resume" -> handleResumeCommand()
                "stop" -> handleStopCommand()
                "restart" -> handleRestartCommand(args.drop(1))
                "status" -> handleStatusCommand()
                "config" -> handleConfigCommand(args.drop(1))
                "experiment" -> handleExperimentCommand(args.drop(1))
                "report" -> handleReportCommand()
                "monitor" -> startRealTimeMonitoring()
                "clear" -> clearScreen()
                else -> println("❌ Unknown command: ${args[0]}. Type 'help' for available commands.")
            }
        }
    }
    
    /**
     * Handle start training command
     */
    private fun handleStartCommand(args: List<String>) {
        println("🚀 Starting Training Session")
        
        try {
            val sessionConfig = parseSessionConfig(args)
            val experimentName = args.find { it.startsWith("--experiment=") }
                ?.substringAfter("=")
            
            println("Session Configuration:")
            println("  Name: ${sessionConfig.name}")
            println("  Controller: ${sessionConfig.controllerType}")
            println("  Iterations: ${sessionConfig.iterations}")
            println("  Deterministic: ${sessionConfig.enableDeterministic}")
            if (experimentName != null) {
                println("  Experiment: $experimentName")
            }
            
            val result = controller.startTraining(sessionConfig, experimentName)
            
            when (result) {
                is TrainingControlResult.Success -> {
                    println("✅ ${result.message}")
                    if (isInteractiveMode) {
                        println("💡 Use 'status' to monitor progress, 'pause' to pause, or 'stop' to stop")
                    }
                }
                is TrainingControlResult.Error -> {
                    println("❌ ${result.message}")
                }
            }
            
        } catch (e: Exception) {
            println("❌ Failed to start training: ${e.message}")
        }
    }
    
    /**
     * Handle pause training command
     */
    private fun handlePauseCommand() {
        println("⏸️ Pausing Training")
        
        val result = controller.pauseTraining()
        when (result) {
            is TrainingControlResult.Success -> {
                println("✅ ${result.message}")
                if (isInteractiveMode) {
                    println("💡 Use 'resume' to continue training or 'stop' to end session")
                }
            }
            is TrainingControlResult.Error -> {
                println("❌ ${result.message}")
            }
        }
    }
    
    /**
     * Handle resume training command
     */
    private fun handleResumeCommand() {
        println("▶️ Resuming Training")
        
        val result = controller.resumeTraining()
        when (result) {
            is TrainingControlResult.Success -> {
                println("✅ ${result.message}")
                if (isInteractiveMode) {
                    println("💡 Training resumed. Use 'status' to monitor progress")
                }
            }
            is TrainingControlResult.Error -> {
                println("❌ ${result.message}")
            }
        }
    }
    
    /**
     * Handle stop training command
     */
    private fun handleStopCommand() {
        println("🛑 Stopping Training")
        
        val result = controller.stopTraining()
        when (result) {
            is TrainingControlResult.Success -> {
                println("✅ ${result.message}")
                if (isInteractiveMode) {
                    println("💡 Use 'report' to view session results or 'start' to begin new session")
                }
            }
            is TrainingControlResult.Error -> {
                println("❌ ${result.message}")
            }
        }
    }
    
    /**
     * Handle restart training command
     */
    private fun handleRestartCommand(args: List<String>) {
        println("🔄 Restarting Training")
        
        val newConfig = if (args.isNotEmpty()) {
            parseSessionConfig(args)
        } else null
        
        val result = controller.restartTraining(newConfig)
        when (result) {
            is TrainingControlResult.Success -> {
                println("✅ ${result.message}")
            }
            is TrainingControlResult.Error -> {
                println("❌ ${result.message}")
            }
        }
    }
    
    /**
     * Handle status command
     */
    private fun handleStatusCommand() {
        println("📊 Training Status")
        println("-" * 30)
        
        val status = controller.getTrainingStatus()
        
        println("State: ${getStateIcon(status.state)} ${status.state}")
        
        status.session?.let { session ->
            println("Session: ${session.name} (${session.id})")
            println("Started: ${formatTimestamp(session.startTime)}")
            if (session.endTime != null) {
                println("Ended: ${formatTimestamp(session.endTime)}")
                println("Duration: ${formatDuration(session.endTime - session.startTime)}")
            } else {
                val currentDuration = getCurrentTimeMillis() - session.startTime
                println("Running for: ${formatDuration(currentDuration)}")
            }
        }
        
        status.experiment?.let { experiment ->
            println("Experiment: ${experiment.name} (${experiment.status})")
        }
        
        status.metrics?.let { metrics ->
            println("\nTraining Metrics:")
            println("  Iteration: ${metrics.currentIteration}/${metrics.totalIterations}")
            println("  Average Reward: ${String.format("%.4f", metrics.averageReward)}")
            println("  Best Reward: ${String.format("%.4f", metrics.bestReward)}")
            println("  Training Loss: ${String.format("%.4f", metrics.trainingLoss)}")
            println("  Exploration Rate: ${String.format("%.4f", metrics.explorationRate)}")
            println("  Games Played: ${metrics.gamesPlayed}")
            println("  Experiences: ${metrics.experiencesCollected}")
        }
        
        status.performance?.let { perf ->
            println("\nPerformance:")
            println("  Avg Iteration Time: ${formatDuration(perf.averageIterationTime)}")
            println("  Memory Usage: ${String.format("%.1f%%", perf.memoryUsage * 100)}")
            println("  CPU Usage: ${String.format("%.1f%%", perf.cpuUsage * 100)}")
            println("  Throughput: ${String.format("%.2f", perf.throughput)} games/sec")
            println("  Efficiency: ${String.format("%.2f", perf.efficiency)} exp/sec")
        }
        
        if (status.configurationHistory.isNotEmpty()) {
            println("\nRecent Configuration Changes:")
            status.configurationHistory.takeLast(3).forEach { change ->
                println("  ${formatTimestamp(change.timestamp)}: ${change.parameter} = ${change.newValue}")
            }
        }
        
        if (status.activeExperiments.isNotEmpty()) {
            println("\nActive Experiments:")
            status.activeExperiments.forEach { exp ->
                println("  ${exp.name} (${exp.status})")
            }
        }
    }
    
    /**
     * Handle configuration commands
     */
    private fun handleConfigCommand(args: List<String>) {
        if (args.isEmpty()) {
            println("❌ Configuration command requires action: set, get, rollback, validate")
            return
        }
        
        when (args[0].lowercase()) {
            "set" -> handleConfigSet(args.drop(1))
            "get" -> handleConfigGet(args.drop(1))
            "rollback" -> handleConfigRollback()
            "validate" -> handleConfigValidate(args.drop(1))
            "history" -> handleConfigHistory()
            else -> {
                println("❌ Unknown config action: ${args[0]}")
                println("Available actions: set, get, rollback, validate, history")
            }
        }
    }
    
    /**
     * Handle config set command
     */
    private fun handleConfigSet(args: List<String>) {
        if (args.size < 2) {
            println("❌ Usage: config set <parameter> <value> [--reason=\"reason\"]")
            return
        }
        
        val parameter = args[0]
        val value = args[1]
        val reason = args.find { it.startsWith("--reason=") }
            ?.substringAfter("=")?.removeSurrounding("\"") 
            ?: "CLI adjustment"
        
        println("⚙️ Setting Configuration: $parameter = $value")
        
        val update = ConfigurationUpdate(
            parameter = parameter,
            oldValue = "current", // Would get actual current value in real implementation
            newValue = value,
            reason = reason
        )
        
        val result = controller.adjustConfiguration(update)
        
        when (result) {
            is ConfigurationResult.Applied -> {
                println("✅ ${result.message}")
                println("Change ID: ${result.change.timestamp}")
            }
            is ConfigurationResult.Invalid -> {
                println("❌ Invalid configuration:")
                result.errors.forEach { error ->
                    println("  - $error")
                }
            }
            is ConfigurationResult.Error -> {
                println("❌ ${result.message}")
            }
            is ConfigurationResult.Valid -> {
                println("✅ ${result.message}")
            }
        }
    }
    
    /**
     * Handle config validate command
     */
    private fun handleConfigValidate(args: List<String>) {
        if (args.size < 2) {
            println("❌ Usage: config validate <parameter> <value>")
            return
        }
        
        val parameter = args[0]
        val value = args[1]
        
        println("🔍 Validating Configuration: $parameter = $value")
        
        val update = ConfigurationUpdate(
            parameter = parameter,
            oldValue = "current",
            newValue = value,
            reason = "Validation check"
        )
        
        val result = controller.adjustConfiguration(update, validateOnly = true)
        
        when (result) {
            is ConfigurationResult.Valid -> {
                println("✅ Configuration is valid")
            }
            is ConfigurationResult.Invalid -> {
                println("❌ Configuration is invalid:")
                result.errors.forEach { error ->
                    println("  - $error")
                }
            }
            else -> {
                println("❌ Validation failed")
            }
        }
    }
    
    /**
     * Handle config rollback command
     */
    private fun handleConfigRollback() {
        println("↩️ Rolling Back Configuration")
        
        val result = controller.rollbackConfiguration()
        
        when (result) {
            is ConfigurationResult.Applied -> {
                println("✅ ${result.message}")
            }
            is ConfigurationResult.Error -> {
                println("❌ ${result.message}")
            }
            else -> {
                println("❌ Rollback failed")
            }
        }
    }
    
    /**
     * Handle config history command
     */
    private fun handleConfigHistory() {
        println("📜 Configuration History")
        println("-" * 40)
        
        val status = controller.getTrainingStatus()
        
        if (status.configurationHistory.isEmpty()) {
            println("No configuration changes recorded")
            return
        }
        
        status.configurationHistory.forEach { change ->
            println("${formatTimestamp(change.timestamp)}")
            println("  Parameter: ${change.parameter}")
            println("  Change: ${change.oldValue} → ${change.newValue}")
            println("  Reason: ${change.reason}")
            println("  Applied by: ${change.appliedBy}")
            println()
        }
    }
    
    /**
     * Handle experiment commands
     */
    private fun handleExperimentCommand(args: List<String>) {
        if (args.isEmpty()) {
            println("❌ Experiment command requires action: create, list, compare, results")
            return
        }
        
        when (args[0].lowercase()) {
            "create" -> handleExperimentCreate(args.drop(1))
            "list" -> handleExperimentList()
            "compare" -> handleExperimentCompare(args.drop(1))
            "results" -> handleExperimentResults(args.drop(1))
            else -> {
                println("❌ Unknown experiment action: ${args[0]}")
                println("Available actions: create, list, compare, results")
            }
        }
    }
    
    /**
     * Handle experiment create command
     */
    private fun handleExperimentCreate(args: List<String>) {
        if (args.isEmpty()) {
            println("❌ Usage: experiment create <name> [--description=\"desc\"] [config options]")
            return
        }
        
        val name = args[0]
        val description = args.find { it.startsWith("--description=") }
            ?.substringAfter("=")?.removeSurrounding("\"") ?: ""
        
        val sessionConfig = parseSessionConfig(args)
        
        println("🧪 Creating Experiment: $name")
        
        val experiment = controller.createExperiment(name, sessionConfig, description)
        
        println("✅ Experiment created:")
        println("  ID: ${experiment.id}")
        println("  Name: ${experiment.name}")
        println("  Description: ${experiment.description}")
        println("  Parameters: ${experiment.parameters.size} configured")
        
        if (isInteractiveMode) {
            println("💡 Use 'start --experiment=${experiment.name}' to run this experiment")
        }
    }
    
    /**
     * Handle experiment list command
     */
    private fun handleExperimentList() {
        println("🧪 Experiment List")
        println("-" * 30)
        
        val status = controller.getTrainingStatus()
        val allExperiments = status.activeExperiments // In real implementation, would get all experiments
        
        if (allExperiments.isEmpty()) {
            println("No experiments found")
            return
        }
        
        allExperiments.forEach { experiment ->
            println("${experiment.name} (${experiment.id})")
            println("  Status: ${experiment.status}")
            println("  Created: ${formatTimestamp(experiment.createdTime)}")
            println("  Parameters: ${experiment.parameters.size}")
            if (experiment.results != null) {
                println("  Performance: ${String.format("%.4f", experiment.results.finalPerformance)}")
            }
            println()
        }
    }
    
    /**
     * Handle experiment compare command
     */
    private fun handleExperimentCompare(args: List<String>) {
        if (args.size < 2) {
            println("❌ Usage: experiment compare <exp1_id> <exp2_id> [exp3_id...]")
            return
        }
        
        println("📊 Comparing Experiments")
        println("-" * 35)
        
        val comparison = controller.compareExperiments(args)
        
        if (comparison.experiments.size < 2) {
            println("❌ Need at least 2 valid experiments to compare")
            return
        }
        
        println("Experiments:")
        comparison.experiments.forEach { exp ->
            println("  ${exp.name} (${exp.id})")
        }
        
        println("\nComparison:")
        comparison.comparison.forEach { (parameter, values) ->
            println("  $parameter:")
            values.forEachIndexed { index, value ->
                val expName = comparison.experiments[index].name
                println("    $expName: $value")
            }
        }
        
        if (comparison.recommendations.isNotEmpty()) {
            println("\nRecommendations:")
            comparison.recommendations.forEach { rec ->
                println("  • $rec")
            }
        }
    }
    
    /**
     * Handle report command
     */
    private fun handleReportCommand() {
        println("📋 Generating Training Report")
        println("-" * 40)
        
        val report = controller.generateTrainingReport()
        
        report.sessionSummary?.let { session ->
            println("Session Summary:")
            println("  Name: ${session.name}")
            println("  Duration: ${formatDuration(session.duration)}")
            println("  State: ${session.state}")
            println("  Iterations: ${session.iterations}")
            println("  Config Changes: ${session.configurationChanges}")
            println("  Performance: ${String.format("%.4f", session.performance)}")
            println()
        }
        
        report.experimentSummary?.let { experiment ->
            println("Experiment Summary:")
            println("  Name: ${experiment.name}")
            println("  Status: ${experiment.status}")
            println("  Duration: ${formatDuration(experiment.duration)}")
            experiment.results?.let { results ->
                println("  Final Performance: ${String.format("%.4f", results.finalPerformance)}")
                println("  Best Iteration: ${results.bestIteration}")
                println("  Checkpoints: ${results.checkpointCount}")
            }
            println()
        }
        
        println("Performance Analysis:")
        val analysis = report.performanceAnalysis
        println("  Average Performance: ${String.format("%.4f", analysis.averagePerformance)}")
        println("  Trend: ${analysis.performanceTrend}")
        
        if (analysis.bottlenecks.isNotEmpty()) {
            println("  Bottlenecks:")
            analysis.bottlenecks.forEach { bottleneck ->
                println("    • $bottleneck")
            }
        }
        
        if (analysis.optimizationSuggestions.isNotEmpty()) {
            println("  Optimization Suggestions:")
            analysis.optimizationSuggestions.forEach { suggestion ->
                println("    • $suggestion")
            }
        }
        
        if (report.recommendations.isNotEmpty()) {
            println("\nRecommendations:")
            report.recommendations.forEach { rec ->
                println("  • $rec")
            }
        }
        
        println("\nReport generated at: ${formatTimestamp(report.generatedTime)}")
    }
    
    /**
     * Start real-time monitoring
     */
    private fun startRealTimeMonitoring() {
        println("📡 Starting Real-Time Monitoring")
        println("Press 'q' to quit monitoring")
        println("-" * 40)
        
        var monitoring = true
        
        // Simulate real-time monitoring
        while (monitoring) {
            val status = controller.getTrainingStatus()
            
            // Clear previous output (simplified)
            println("\n" + "=" * 50)
            println("Real-Time Training Monitor")
            println("State: ${getStateIcon(status.state)} ${status.state}")
            
            status.metrics?.let { metrics ->
                println("Progress: ${metrics.currentIteration}/${metrics.totalIterations} " +
                       "(${(metrics.currentIteration.toDouble() / metrics.totalIterations * 100).toInt()}%)")
                println("Reward: ${String.format("%.4f", metrics.averageReward)} " +
                       "(Best: ${String.format("%.4f", metrics.bestReward)})")
                println("Loss: ${String.format("%.4f", metrics.trainingLoss)}")
            }
            
            status.performance?.let { perf ->
                println("Performance: ${String.format("%.2f", perf.throughput)} games/sec, " +
                       "Memory: ${String.format("%.1f%%", perf.memoryUsage * 100)}")
            }
            
            println("Press 'q' + Enter to quit")
            
            // Simulate monitoring interval
            Thread.sleep(2000)
            
            // Check for quit (simplified - in real implementation would handle input properly)
            if (kotlin.random.Random.nextDouble() < 0.1) { // Random quit for demo
                monitoring = false
            }
        }
        
        println("📡 Monitoring stopped")
    }
    
    /**
     * Parse session configuration from command line arguments
     */
    private fun parseSessionConfig(args: List<String>): TrainingSessionConfig {
        var name = "training_${getCurrentTimeMillis()}"
        var controllerType = ControllerType.INTEGRATED
        var iterations = 100
        var enableDeterministic = false
        var integratedConfig: IntegratedSelfPlayConfig? = null
        var deterministicConfig: TrainingConfiguration? = null
        
        for (arg in args) {
            when {
                arg.startsWith("--name=") -> name = arg.substringAfter("=")
                arg.startsWith("--controller=") -> {
                    controllerType = when (arg.substringAfter("=").uppercase()) {
                        "INTEGRATED" -> ControllerType.INTEGRATED
                        "DETERMINISTIC" -> ControllerType.DETERMINISTIC
                        "HYBRID" -> ControllerType.HYBRID
                        else -> ControllerType.INTEGRATED
                    }
                }
                arg.startsWith("--iterations=") -> iterations = arg.substringAfter("=").toIntOrNull() ?: 100
                arg == "--deterministic" -> enableDeterministic = true
                arg.startsWith("--learning-rate=") -> {
                    val lr = arg.substringAfter("=").toDoubleOrNull() ?: 0.001
                    integratedConfig = (integratedConfig ?: IntegratedSelfPlayConfig()).copy(learningRate = lr)
                }
                arg.startsWith("--exploration=") -> {
                    val exploration = arg.substringAfter("=").toDoubleOrNull() ?: 0.1
                    integratedConfig = (integratedConfig ?: IntegratedSelfPlayConfig()).copy(explorationRate = exploration)
                }
                arg.startsWith("--batch-size=") -> {
                    val batchSize = arg.substringAfter("=").toIntOrNull() ?: 64
                    integratedConfig = (integratedConfig ?: IntegratedSelfPlayConfig()).copy(batchSize = batchSize)
                }
                arg.startsWith("--games=") -> {
                    val games = arg.substringAfter("=").toIntOrNull() ?: 20
                    integratedConfig = (integratedConfig ?: IntegratedSelfPlayConfig()).copy(gamesPerIteration = games)
                }
            }
        }
        
        return TrainingSessionConfig(
            name = name,
            controllerType = controllerType,
            iterations = iterations,
            enableDeterministic = enableDeterministic,
            integratedConfig = integratedConfig,
            deterministicConfig = deterministicConfig
        )
    }
    
    // Utility methods
    
    private fun getStateIcon(state: TrainingState): String {
        return when (state) {
            TrainingState.STOPPED -> "⏹️"
            TrainingState.STARTING -> "🔄"
            TrainingState.RUNNING -> "▶️"
            TrainingState.PAUSED -> "⏸️"
            TrainingState.COMPLETED -> "✅"
            TrainingState.ERROR -> "❌"
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simplified timestamp formatting
        return "Time: $timestamp"
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    private fun clearScreen() {
        // Simplified screen clear
        repeat(50) { println() }
    }
    
    private fun printUsage() {
        println("🎮 Advanced Training Control CLI")
        println("=" * 50)
        println("Usage: AdvancedTrainingCLI <command> [options]")
        println()
        println("Lifecycle Commands:")
        println("  start [options]     - Start new training session")
        println("  pause              - Pause current training")
        println("  resume             - Resume paused training")
        println("  stop               - Stop current training")
        println("  restart [options]  - Restart training with same/new config")
        println()
        println("Monitoring Commands:")
        println("  status             - Show current training status")
        println("  report             - Generate comprehensive training report")
        println("  interactive        - Start interactive mode")
        println()
        println("Configuration Commands:")
        println("  config set <param> <value>    - Adjust configuration in real-time")
        println("  config validate <param> <val> - Validate configuration change")
        println("  config rollback               - Rollback to previous configuration")
        println("  config history                - Show configuration change history")
        println()
        println("Experiment Commands:")
        println("  experiment create <name>      - Create new experiment")
        println("  experiment list               - List all experiments")
        println("  experiment compare <ids...>   - Compare experiment results")
        println("  experiment results <id>       - Show experiment results")
        println()
        println("Start Options:")
        println("  --name=<name>          - Session name")
        println("  --controller=<type>    - Controller type (INTEGRATED|DETERMINISTIC|HYBRID)")
        println("  --iterations=<n>       - Number of training iterations")
        println("  --deterministic        - Enable deterministic training")
        println("  --learning-rate=<lr>   - Learning rate")
        println("  --exploration=<rate>   - Exploration rate")
        println("  --batch-size=<size>    - Batch size")
        println("  --games=<n>           - Games per iteration")
        println("  --experiment=<name>    - Associate with experiment")
        println()
        println("Configuration Parameters:")
        println("  learningRate       - Neural network learning rate")
        println("  explorationRate    - Agent exploration rate")
        println("  batchSize         - Training batch size")
        println("  gamesPerIteration - Games per training iteration")
        println()
        println("Examples:")
        println("  AdvancedTrainingCLI start --name=\"experiment1\" --iterations=50 --deterministic")
        println("  AdvancedTrainingCLI config set learningRate 0.001")
        println("  AdvancedTrainingCLI experiment create \"lr_test\" --learning-rate=0.01")
        println("  AdvancedTrainingCLI interactive")
    }
    
    private fun printInteractiveHelp() {
        println("🎮 Interactive Mode Commands:")
        println("-" * 30)
        println("Lifecycle:")
        println("  start [options]    - Start training")
        println("  pause             - Pause training")
        println("  resume            - Resume training")
        println("  stop              - Stop training")
        println("  restart [options] - Restart training")
        println()
        println("Monitoring:")
        println("  status            - Show status")
        println("  monitor           - Real-time monitoring")
        println("  report            - Generate report")
        println()
        println("Configuration:")
        println("  config set <param> <value> - Adjust config")
        println("  config rollback            - Rollback config")
        println("  config history             - Show history")
        println()
        println("Experiments:")
        println("  experiment create <name>   - Create experiment")
        println("  experiment list            - List experiments")
        println("  experiment compare <ids>   - Compare experiments")
        println()
        println("Utility:")
        println("  clear             - Clear screen")
        println("  help              - Show this help")
        println("  exit              - Exit interactive mode")
    }
}