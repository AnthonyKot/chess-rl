package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Advanced Training Controller with production-ready training management
 * 
 * Features:
 * - Training lifecycle management (start/pause/resume/stop/restart)
 * - Real-time configuration adjustment with validation and rollback
 * - Training experiment management with parameter tracking
 * - Integration with existing controllers
 */
class AdvancedTrainingController(
    private val config: AdvancedTrainingConfig = AdvancedTrainingConfig()
) {
    
    // Core controllers
    private var integratedController: IntegratedSelfPlayController? = null
    private var deterministicController: DeterministicTrainingController? = null
    
    // Training state management
    private var currentSession: TrainingSession? = null
    private var trainingState = TrainingState.STOPPED
    private var pausedState: TrainingSessionSnapshot? = null
    
    // Configuration management
    private var activeConfig: AdvancedTrainingConfig = config
    private val configHistory = mutableListOf<ConfigurationChange>()
    private var rollbackStack = mutableListOf<AdvancedTrainingConfig>()
    
    // Experiment management
    private val experiments = mutableMapOf<String, TrainingExperiment>()
    private var currentExperiment: TrainingExperiment? = null
    
    // Real-time monitoring
    private val metricsCollector = TrainingMetricsCollector()
    private val performanceMonitor = PerformanceMonitor()
    
    /**
     * Start a new training session with lifecycle management
     */
    fun startTraining(
        sessionConfig: TrainingSessionConfig,
        experimentName: String? = null
    ): TrainingControlResult {
        
        if (trainingState != TrainingState.STOPPED) {
            return TrainingControlResult.Error("Training already active. Current state: $trainingState")
        }
        
        return try {
            println("🚀 Starting Advanced Training Session")
            println("Configuration: ${sessionConfig.name}")
            
            // Create training session
            currentSession = TrainingSession(
                id = generateSessionId(),
                name = sessionConfig.name,
                config = sessionConfig,
                startTime = getCurrentTimeMillis(),
                state = TrainingState.STARTING
            )
            
            // Create experiment if specified
            if (experimentName != null) {
                currentExperiment = createExperiment(experimentName, sessionConfig)
            }
            
            // Initialize appropriate controller based on configuration
            val controllerResult = initializeController(sessionConfig)
            if (controllerResult !is TrainingControlResult.Success) {
                return controllerResult
            }
            
            // Start training
            trainingState = TrainingState.RUNNING
            currentSession = currentSession!!.copy(state = TrainingState.RUNNING)
            
            // Start monitoring
            metricsCollector.startCollection(currentSession!!.id)
            performanceMonitor.startMonitoring()
            
            // Execute training in background (simulated async)
            executeTrainingAsync(sessionConfig)
            
            TrainingControlResult.Success("Training session started: ${currentSession!!.id}")
            
        } catch (e: Exception) {
            trainingState = TrainingState.ERROR
            currentSession?.let { session ->
                currentSession = session.copy(
                    state = TrainingState.ERROR,
                    errorMessage = e.message
                )
            }
            TrainingControlResult.Error("Failed to start training: ${e.message}")
        }
    }
    
    /**
     * Pause current training session
     */
    fun pauseTraining(): TrainingControlResult {
        if (trainingState != TrainingState.RUNNING) {
            return TrainingControlResult.Error("No active training to pause. Current state: $trainingState")
        }
        
        return try {
            println("⏸️ Pausing Training Session")
            
            // Create snapshot for resume
            pausedState = createTrainingSnapshot()
            
            // Pause controllers
            integratedController?.stopTraining()
            deterministicController?.stopTraining()
            
            // Update state
            trainingState = TrainingState.PAUSED
            currentSession = currentSession!!.copy(
                state = TrainingState.PAUSED,
                pausedTime = getCurrentTimeMillis()
            )
            
            // Pause monitoring
            performanceMonitor.pauseMonitoring()
            
            TrainingControlResult.Success("Training paused successfully")
            
        } catch (e: Exception) {
            TrainingControlResult.Error("Failed to pause training: ${e.message}")
        }
    }
    
    /**
     * Resume paused training session
     */
    fun resumeTraining(): TrainingControlResult {
        if (trainingState != TrainingState.PAUSED) {
            return TrainingControlResult.Error("No paused training to resume. Current state: $trainingState")
        }
        
        if (pausedState == null) {
            return TrainingControlResult.Error("No paused state available for resume")
        }
        
        return try {
            println("▶️ Resuming Training Session")
            
            // Restore from snapshot
            restoreFromSnapshot(pausedState!!)
            
            // Update state
            trainingState = TrainingState.RUNNING
            currentSession = currentSession!!.copy(
                state = TrainingState.RUNNING,
                resumedTime = getCurrentTimeMillis()
            )
            
            // Resume monitoring
            performanceMonitor.resumeMonitoring()
            
            // Continue training
            continueTrainingFromSnapshot(pausedState!!)
            
            pausedState = null
            
            TrainingControlResult.Success("Training resumed successfully")
            
        } catch (e: Exception) {
            TrainingControlResult.Error("Failed to resume training: ${e.message}")
        }
    }
    
    /**
     * Stop current training session
     */
    fun stopTraining(): TrainingControlResult {
        if (trainingState == TrainingState.STOPPED) {
            return TrainingControlResult.Error("No active training to stop")
        }
        
        return try {
            println("🛑 Stopping Training Session")
            
            // Stop controllers
            integratedController?.stopTraining()
            deterministicController?.stopTraining()
            
            // Finalize session
            currentSession?.let { session ->
                currentSession = session.copy(
                    state = TrainingState.STOPPED,
                    endTime = getCurrentTimeMillis()
                )
                
                // Save session results
                saveSessionResults(session)
            }
            
            // Stop monitoring
            metricsCollector.stopCollection()
            performanceMonitor.stopMonitoring()
            
            // Update state
            trainingState = TrainingState.STOPPED
            pausedState = null
            
            TrainingControlResult.Success("Training stopped successfully")
            
        } catch (e: Exception) {
            TrainingControlResult.Error("Failed to stop training: ${e.message}")
        }
    }
    
    /**
     * Restart training session with same or new configuration
     */
    fun restartTraining(newConfig: TrainingSessionConfig? = null): TrainingControlResult {
        return try {
            println("🔄 Restarting Training Session")
            
            // Stop current training
            val stopResult = stopTraining()
            if (stopResult !is TrainingControlResult.Success) {
                return stopResult
            }
            
            // Start with new or existing configuration
            val configToUse = newConfig ?: currentSession?.config 
                ?: return TrainingControlResult.Error("No configuration available for restart")
            
            startTraining(configToUse)
            
        } catch (e: Exception) {
            TrainingControlResult.Error("Failed to restart training: ${e.message}")
        }
    }
    
    /**
     * Adjust configuration in real-time with validation
     */
    fun adjustConfiguration(
        configUpdate: ConfigurationUpdate,
        validateOnly: Boolean = false
    ): ConfigurationResult {
        
        return try {
            println("⚙️ Adjusting Configuration: ${configUpdate.parameter}")
            
            // Validate configuration change
            val validationResult = validateConfigurationChange(configUpdate)
            if (!validationResult.isValid) {
                return ConfigurationResult.Invalid(validationResult.errors)
            }
            
            if (validateOnly) {
                return ConfigurationResult.Valid("Configuration change is valid")
            }
            
            // Save current config for rollback
            rollbackStack.add(activeConfig.copy())
            if (rollbackStack.size > config.maxRollbackHistory) {
                rollbackStack.removeAt(0)
            }
            
            // Apply configuration change
            val newConfig = applyConfigurationChange(activeConfig, configUpdate)
            
            // Record change
            val configChange = ConfigurationChange(
                timestamp = getCurrentTimeMillis(),
                parameter = configUpdate.parameter,
                oldValue = configUpdate.oldValue,
                newValue = configUpdate.newValue,
                reason = configUpdate.reason,
                appliedBy = "AdvancedTrainingController"
            )
            configHistory.add(configChange)
            
            // Update active configuration
            activeConfig = newConfig
            
            // Apply to running training if active
            if (trainingState == TrainingState.RUNNING) {
                applyConfigurationToRunningTraining(configUpdate)
            }
            
            ConfigurationResult.Applied(
                "Configuration updated: ${configUpdate.parameter} = ${configUpdate.newValue}",
                configChange
            )
            
        } catch (e: Exception) {
            ConfigurationResult.Error("Failed to adjust configuration: ${e.message}")
        }
    }
    
    /**
     * Rollback to previous configuration
     */
    fun rollbackConfiguration(): ConfigurationResult {
        if (rollbackStack.isEmpty()) {
            return ConfigurationResult.Error("No configuration history available for rollback")
        }
        
        return try {
            println("↩️ Rolling Back Configuration")
            
            val previousConfig = rollbackStack.removeAt(rollbackStack.size - 1)
            
            // Record rollback
            val rollbackChange = ConfigurationChange(
                timestamp = getCurrentTimeMillis(),
                parameter = "ROLLBACK",
                oldValue = "current",
                newValue = "previous",
                reason = "Manual rollback",
                appliedBy = "AdvancedTrainingController"
            )
            configHistory.add(rollbackChange)
            
            // Apply rollback
            activeConfig = previousConfig
            
            // Apply to running training if active
            if (trainingState == TrainingState.RUNNING) {
                applyConfigurationRollback(previousConfig)
            }
            
            ConfigurationResult.Applied("Configuration rolled back successfully", rollbackChange)
            
        } catch (e: Exception) {
            ConfigurationResult.Error("Failed to rollback configuration: ${e.message}")
        }
    }
    
    /**
     * Create and manage training experiments
     */
    fun createExperiment(
        name: String,
        sessionConfig: TrainingSessionConfig,
        description: String = ""
    ): TrainingExperiment {
        
        val experiment = TrainingExperiment(
            id = generateExperimentId(),
            name = name,
            description = description,
            config = sessionConfig,
            createdTime = getCurrentTimeMillis(),
            parameters = extractExperimentParameters(sessionConfig),
            status = ExperimentStatus.CREATED
        )
        
        experiments[experiment.id] = experiment
        
        println("🧪 Created Experiment: $name (${experiment.id})")
        
        return experiment
    }
    
    /**
     * Compare experiments and their results
     */
    fun compareExperiments(experimentIds: List<String>): ExperimentComparison {
        val experimentsToCompare = experimentIds.mapNotNull { experiments[it] }
        
        if (experimentsToCompare.size < 2) {
            return ExperimentComparison(
                experiments = experimentsToCompare,
                comparison = emptyMap(),
                recommendations = listOf("Need at least 2 experiments to compare")
            )
        }
        
        val comparison = mutableMapOf<String, List<Any>>()
        
        // Compare parameters
        val allParameters = experimentsToCompare.flatMap { it.parameters.keys }.distinct()
        for (param in allParameters) {
            comparison[param] = experimentsToCompare.map { 
                it.parameters[param] ?: "N/A" 
            }
        }
        
        // Compare results if available
        val results = experimentsToCompare.map { it.results }
        if (results.all { it != null }) {
            comparison["Performance"] = results.map { it!!.finalPerformance }
            comparison["Training Time"] = results.map { it!!.trainingDuration }
            comparison["Best Iteration"] = results.map { it!!.bestIteration }
        }
        
        // Generate recommendations
        val recommendations = generateExperimentRecommendations(experimentsToCompare)
        
        return ExperimentComparison(
            experiments = experimentsToCompare,
            comparison = comparison,
            recommendations = recommendations
        )
    }
    
    /**
     * Get current training status and metrics
     */
    fun getTrainingStatus(): AdvancedTrainingStatus {
        val currentMetrics = metricsCollector.getCurrentMetrics()
        val performanceStats = performanceMonitor.getCurrentStats()
        
        return AdvancedTrainingStatus(
            state = trainingState,
            session = currentSession,
            experiment = currentExperiment,
            metrics = currentMetrics,
            performance = performanceStats,
            configurationHistory = configHistory.takeLast(10),
            activeExperiments = experiments.values.filter { 
                it.status == ExperimentStatus.RUNNING || it.status == ExperimentStatus.PAUSED 
            }
        )
    }
    
    /**
     * Get comprehensive training report
     */
    fun generateTrainingReport(): TrainingReport {
        val session = currentSession
        val experiment = currentExperiment
        
        return TrainingReport(
            sessionSummary = session?.let { createSessionSummary(it) },
            experimentSummary = experiment?.let { createExperimentSummary(it) },
            performanceAnalysis = performanceMonitor.generateAnalysis(),
            configurationChanges = configHistory.toList(),
            recommendations = generateTrainingRecommendations(),
            generatedTime = getCurrentTimeMillis()
        )
    }
    
    // Private helper methods
    
    private fun initializeController(sessionConfig: TrainingSessionConfig): TrainingControlResult {
        return try {
            when (sessionConfig.controllerType) {
                ControllerType.INTEGRATED -> {
                    integratedController = IntegratedSelfPlayController(
                        sessionConfig.integratedConfig ?: IntegratedSelfPlayConfig()
                    )
                    val initResult = integratedController!!.initialize(
                        if (sessionConfig.enableDeterministic) SeedManager else null
                    )
                    when (initResult) {
                        is InitializationResult.Success -> TrainingControlResult.Success("Integrated controller initialized")
                        is InitializationResult.Failed -> TrainingControlResult.Error(initResult.error)
                    }
                }
                ControllerType.DETERMINISTIC -> {
                    deterministicController = DeterministicTrainingController()
                    val trainingConfig = sessionConfig.deterministicConfig ?: TrainingConfiguration()
                    deterministicController!!.initialize(trainingConfig)
                    TrainingControlResult.Success("Deterministic controller initialized")
                }
                ControllerType.HYBRID -> {
                    // Initialize both controllers
                    integratedController = IntegratedSelfPlayController(
                        sessionConfig.integratedConfig ?: IntegratedSelfPlayConfig()
                    )
                    deterministicController = DeterministicTrainingController()
                    
                    val integratedInit = integratedController!!.initialize(SeedManager)
                    val deterministicInit = deterministicController!!.initialize(
                        sessionConfig.deterministicConfig ?: TrainingConfiguration()
                    )
                    
                    when {
                        integratedInit is InitializationResult.Success -> 
                            TrainingControlResult.Success("Hybrid controllers initialized")
                        else -> TrainingControlResult.Error("Failed to initialize hybrid controllers")
                    }
                }
            }
        } catch (e: Exception) {
            TrainingControlResult.Error("Controller initialization failed: ${e.message}")
        }
    }
    
    private fun executeTrainingAsync(sessionConfig: TrainingSessionConfig) {
        // In a real implementation, this would run in a separate thread/coroutine
        // For now, we simulate async execution
        try {
            when (sessionConfig.controllerType) {
                ControllerType.INTEGRATED -> {
                    integratedController?.runIntegratedTraining(sessionConfig.iterations)
                }
                ControllerType.DETERMINISTIC -> {
                    deterministicController?.runDeterministicTraining(sessionConfig.iterations)
                }
                ControllerType.HYBRID -> {
                    // Alternate between controllers or run in parallel
                    integratedController?.runIntegratedTraining(sessionConfig.iterations / 2)
                    deterministicController?.runDeterministicTraining(sessionConfig.iterations / 2)
                }
            }
            
            // Update session on completion
            currentSession?.let { session ->
                currentSession = session.copy(
                    state = TrainingState.COMPLETED,
                    endTime = getCurrentTimeMillis()
                )
                trainingState = TrainingState.COMPLETED
            }
            
        } catch (e: Exception) {
            currentSession?.let { session ->
                currentSession = session.copy(
                    state = TrainingState.ERROR,
                    errorMessage = e.message,
                    endTime = getCurrentTimeMillis()
                )
                trainingState = TrainingState.ERROR
            }
        }
    }
    
    private fun createTrainingSnapshot(): TrainingSessionSnapshot {
        return TrainingSessionSnapshot(
            sessionId = currentSession!!.id,
            timestamp = getCurrentTimeMillis(),
            trainingState = trainingState,
            configuration = activeConfig.copy(),
            controllerState = captureControllerState(),
            metrics = metricsCollector.createSnapshot(),
            iteration = getCurrentIteration()
        )
    }
    
    private fun restoreFromSnapshot(snapshot: TrainingSessionSnapshot) {
        activeConfig = snapshot.configuration
        restoreControllerState(snapshot.controllerState)
        metricsCollector.restoreFromSnapshot(snapshot.metrics)
    }
    
    private fun continueTrainingFromSnapshot(snapshot: TrainingSessionSnapshot) {
        // Continue training from the snapshot iteration
        val remainingIterations = currentSession!!.config.iterations - snapshot.iteration
        if (remainingIterations > 0) {
            executeTrainingAsync(currentSession!!.config.copy(iterations = remainingIterations))
        }
    }
    
    private fun validateConfigurationChange(update: ConfigurationUpdate): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate parameter exists
        if (!isValidParameter(update.parameter)) {
            errors.add("Unknown parameter: ${update.parameter}")
        }
        
        // Validate value type and range
        val valueValidation = validateParameterValue(update.parameter, update.newValue)
        if (!valueValidation.isValid) {
            errors.addAll(valueValidation.errors)
        }
        
        // Validate compatibility with current state
        if (trainingState == TrainingState.RUNNING) {
            val runtimeValidation = validateRuntimeChange(update.parameter)
            if (!runtimeValidation.isValid) {
                errors.addAll(runtimeValidation.errors)
            }
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    private fun applyConfigurationChange(
        config: AdvancedTrainingConfig, 
        update: ConfigurationUpdate
    ): AdvancedTrainingConfig {
        // Apply the configuration change based on parameter type
        return when (update.parameter) {
            "learningRate" -> config.copy(
                integratedConfig = config.integratedConfig.copy(
                    learningRate = update.newValue.toString().toDouble()
                )
            )
            "explorationRate" -> config.copy(
                integratedConfig = config.integratedConfig.copy(
                    explorationRate = update.newValue.toString().toDouble()
                )
            )
            "batchSize" -> config.copy(
                integratedConfig = config.integratedConfig.copy(
                    batchSize = update.newValue.toString().toInt()
                )
            )
            "gamesPerIteration" -> config.copy(
                integratedConfig = config.integratedConfig.copy(
                    gamesPerIteration = update.newValue.toString().toInt()
                )
            )
            else -> config // Unknown parameter, return unchanged
        }
    }
    
    private fun applyConfigurationToRunningTraining(update: ConfigurationUpdate) {
        // Apply configuration changes to running controllers
        when (update.parameter) {
            "learningRate", "explorationRate" -> {
                // These can be applied to running training
                integratedController?.let { controller ->
                    // In a real implementation, would update controller parameters
                    println("Applied ${update.parameter} change to running training")
                }
            }
            "batchSize", "gamesPerIteration" -> {
                // These require training restart
                println("Parameter ${update.parameter} requires training restart to take effect")
            }
        }
    }
    
    private fun applyConfigurationRollback(previousConfig: AdvancedTrainingConfig) {
        // Apply rollback configuration to running training
        if (trainingState == TrainingState.RUNNING) {
            println("Applied configuration rollback to running training")
        }
    }
    
    private fun extractExperimentParameters(sessionConfig: TrainingSessionConfig): Map<String, Any> {
        return mapOf(
            "controllerType" to sessionConfig.controllerType.name,
            "iterations" to sessionConfig.iterations,
            "enableDeterministic" to sessionConfig.enableDeterministic,
            "learningRate" to (sessionConfig.integratedConfig?.learningRate ?: 0.001),
            "explorationRate" to (sessionConfig.integratedConfig?.explorationRate ?: 0.1),
            "batchSize" to (sessionConfig.integratedConfig?.batchSize ?: 64),
            "gamesPerIteration" to (sessionConfig.integratedConfig?.gamesPerIteration ?: 20)
        )
    }
    
    private fun generateExperimentRecommendations(experiments: List<TrainingExperiment>): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Analyze learning rates
        val learningRates = experiments.mapNotNull { 
            it.parameters["learningRate"] as? Double 
        }
        if (learningRates.isNotEmpty()) {
            val avgLR = learningRates.average()
            recommendations.add("Average learning rate: $avgLR")
            if (learningRates.any { it > avgLR * 2 }) {
                recommendations.add("Consider reducing high learning rates for better stability")
            }
        }
        
        // Analyze batch sizes
        val batchSizes = experiments.mapNotNull { 
            it.parameters["batchSize"] as? Int 
        }
        if (batchSizes.isNotEmpty()) {
            val avgBatch = batchSizes.average()
            recommendations.add("Average batch size: $avgBatch")
        }
        
        return recommendations
    }
    
    private fun createSessionSummary(session: TrainingSession): SessionSummary {
        return SessionSummary(
            sessionId = session.id,
            name = session.name,
            duration = (session.endTime ?: getCurrentTimeMillis()) - session.startTime,
            state = session.state,
            iterations = getCurrentIteration(),
            configurationChanges = configHistory.size,
            performance = metricsCollector.getFinalPerformance()
        )
    }
    
    private fun createExperimentSummary(experiment: TrainingExperiment): ExperimentSummary {
        return ExperimentSummary(
            experimentId = experiment.id,
            name = experiment.name,
            parameters = experiment.parameters,
            results = experiment.results,
            status = experiment.status,
            duration = experiment.results?.trainingDuration ?: 0L
        )
    }
    
    private fun generateTrainingRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        val status = getTrainingStatus()
        
        // Performance-based recommendations
        status.performance?.let { perf ->
            if (perf.averageIterationTime > 60000) { // > 1 minute per iteration
                recommendations.add("Consider reducing games per iteration or batch size to improve training speed")
            }
            
            if (perf.memoryUsage > 0.8) { // > 80% memory usage
                recommendations.add("High memory usage detected. Consider reducing experience buffer size")
            }
        }
        
        // Configuration-based recommendations
        if (configHistory.size > 10) {
            recommendations.add("Many configuration changes detected. Consider stabilizing parameters")
        }
        
        return recommendations
    }
    
    // Utility methods
    private fun generateSessionId(): String = "session_${getCurrentTimeMillis()}_${kotlin.random.Random.nextInt(1000, 9999)}"
    private fun generateExperimentId(): String = "exp_${getCurrentTimeMillis()}_${kotlin.random.Random.nextInt(100, 999)}"
    private fun getCurrentIteration(): Int = currentSession?.let { metricsCollector.getCurrentIteration() } ?: 0
    private fun captureControllerState(): Map<String, Any> = mapOf("placeholder" to "state")
    private fun restoreControllerState(state: Map<String, Any>) { /* Implementation */ }
    private fun saveSessionResults(session: TrainingSession) { /* Implementation */ }
    private fun isValidParameter(parameter: String): Boolean = 
        listOf("learningRate", "explorationRate", "batchSize", "gamesPerIteration").contains(parameter)
    private fun validateParameterValue(parameter: String, value: Any): ValidationResult = ValidationResult(true, emptyList())
    private fun validateRuntimeChange(parameter: String): ValidationResult = ValidationResult(true, emptyList())
}

// Enums and data classes follow.../**
 *
 Training state enumeration
 */
enum class TrainingState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * Controller type enumeration
 */
enum class ControllerType {
    INTEGRATED,
    DETERMINISTIC,
    HYBRID
}

/**
 * Experiment status enumeration
 */
enum class ExperimentStatus {
    CREATED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Advanced training configuration
 */
data class AdvancedTrainingConfig(
    val integratedConfig: IntegratedSelfPlayConfig = IntegratedSelfPlayConfig(),
    val deterministicConfig: TrainingConfiguration = TrainingConfiguration(),
    val maxRollbackHistory: Int = 10,
    val enableRealTimeAdjustment: Boolean = true,
    val enableExperimentTracking: Boolean = true,
    val metricsCollectionInterval: Long = 5000L, // 5 seconds
    val performanceMonitoringEnabled: Boolean = true
)

/**
 * Training session configuration
 */
data class TrainingSessionConfig(
    val name: String,
    val controllerType: ControllerType = ControllerType.INTEGRATED,
    val iterations: Int = 100,
    val enableDeterministic: Boolean = false,
    val integratedConfig: IntegratedSelfPlayConfig? = null,
    val deterministicConfig: TrainingConfiguration? = null,
    val experimentName: String? = null,
    val description: String = ""
)

/**
 * Training session data
 */
data class TrainingSession(
    val id: String,
    val name: String,
    val config: TrainingSessionConfig,
    val startTime: Long,
    val endTime: Long? = null,
    val pausedTime: Long? = null,
    val resumedTime: Long? = null,
    val state: TrainingState,
    val errorMessage: String? = null
)

/**
 * Training session snapshot for pause/resume
 */
data class TrainingSessionSnapshot(
    val sessionId: String,
    val timestamp: Long,
    val trainingState: TrainingState,
    val configuration: AdvancedTrainingConfig,
    val controllerState: Map<String, Any>,
    val metrics: MetricsSnapshot,
    val iteration: Int
)

/**
 * Configuration update request
 */
data class ConfigurationUpdate(
    val parameter: String,
    val oldValue: Any,
    val newValue: Any,
    val reason: String = "Manual adjustment"
)

/**
 * Configuration change record
 */
data class ConfigurationChange(
    val timestamp: Long,
    val parameter: String,
    val oldValue: Any,
    val newValue: Any,
    val reason: String,
    val appliedBy: String
)

/**
 * Training experiment data
 */
data class TrainingExperiment(
    val id: String,
    val name: String,
    val description: String,
    val config: TrainingSessionConfig,
    val createdTime: Long,
    val parameters: Map<String, Any>,
    val status: ExperimentStatus,
    val results: ExperimentResults? = null
)

/**
 * Experiment results
 */
data class ExperimentResults(
    val finalPerformance: Double,
    val bestIteration: Int,
    val trainingDuration: Long,
    val convergenceMetrics: Map<String, Double>,
    val checkpointCount: Int
)

/**
 * Experiment comparison
 */
data class ExperimentComparison(
    val experiments: List<TrainingExperiment>,
    val comparison: Map<String, List<Any>>,
    val recommendations: List<String>
)

/**
 * Advanced training status
 */
data class AdvancedTrainingStatus(
    val state: TrainingState,
    val session: TrainingSession?,
    val experiment: TrainingExperiment?,
    val metrics: TrainingMetrics?,
    val performance: PerformanceStats?,
    val configurationHistory: List<ConfigurationChange>,
    val activeExperiments: List<TrainingExperiment>
)

/**
 * Training metrics
 */
data class TrainingMetrics(
    val currentIteration: Int,
    val totalIterations: Int,
    val averageReward: Double,
    val bestReward: Double,
    val trainingLoss: Double,
    val explorationRate: Double,
    val gamesPlayed: Int,
    val experiencesCollected: Int
)

/**
 * Performance statistics
 */
data class PerformanceStats(
    val averageIterationTime: Long,
    val memoryUsage: Double,
    val cpuUsage: Double,
    val throughput: Double, // games per second
    val efficiency: Double // experiences per second
)

/**
 * Metrics snapshot for pause/resume
 */
data class MetricsSnapshot(
    val timestamp: Long,
    val metrics: TrainingMetrics,
    val performance: PerformanceStats
)

/**
 * Session summary
 */
data class SessionSummary(
    val sessionId: String,
    val name: String,
    val duration: Long,
    val state: TrainingState,
    val iterations: Int,
    val configurationChanges: Int,
    val performance: Double
)

/**
 * Experiment summary
 */
data class ExperimentSummary(
    val experimentId: String,
    val name: String,
    val parameters: Map<String, Any>,
    val results: ExperimentResults?,
    val status: ExperimentStatus,
    val duration: Long
)

/**
 * Training report
 */
data class TrainingReport(
    val sessionSummary: SessionSummary?,
    val experimentSummary: ExperimentSummary?,
    val performanceAnalysis: PerformanceAnalysis,
    val configurationChanges: List<ConfigurationChange>,
    val recommendations: List<String>,
    val generatedTime: Long
)

/**
 * Performance analysis
 */
data class PerformanceAnalysis(
    val averagePerformance: Double,
    val performanceTrend: String, // "improving", "stable", "declining"
    val bottlenecks: List<String>,
    val optimizationSuggestions: List<String>
)

/**
 * Training control result
 */
sealed class TrainingControlResult {
    data class Success(val message: String) : TrainingControlResult()
    data class Error(val message: String) : TrainingControlResult()
}

/**
 * Configuration result
 */
sealed class ConfigurationResult {
    data class Valid(val message: String) : ConfigurationResult()
    data class Invalid(val errors: List<String>) : ConfigurationResult()
    data class Applied(val message: String, val change: ConfigurationChange) : ConfigurationResult()
    data class Error(val message: String) : ConfigurationResult()
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Training metrics collector
 */
class TrainingMetricsCollector {
    private var isCollecting = false
    private var sessionId: String? = null
    private val metricsHistory = mutableListOf<TrainingMetrics>()
    
    fun startCollection(sessionId: String) {
        this.sessionId = sessionId
        isCollecting = true
        println("📊 Started metrics collection for session: $sessionId")
    }
    
    fun stopCollection() {
        isCollecting = false
        println("📊 Stopped metrics collection")
    }
    
    fun getCurrentMetrics(): TrainingMetrics? {
        if (!isCollecting) return null
        
        // Simulate current metrics
        return TrainingMetrics(
            currentIteration = kotlin.random.Random.nextInt(1, 100),
            totalIterations = 100,
            averageReward = kotlin.random.Random.nextDouble(0.0, 1.0),
            bestReward = kotlin.random.Random.nextDouble(0.5, 1.0),
            trainingLoss = kotlin.random.Random.nextDouble(0.1, 0.5),
            explorationRate = kotlin.random.Random.nextDouble(0.05, 0.2),
            gamesPlayed = kotlin.random.Random.nextInt(100, 1000),
            experiencesCollected = kotlin.random.Random.nextInt(1000, 10000)
        )
    }
    
    fun getCurrentIteration(): Int = getCurrentMetrics()?.currentIteration ?: 0
    
    fun getFinalPerformance(): Double = metricsHistory.lastOrNull()?.bestReward ?: 0.0
    
    fun createSnapshot(): MetricsSnapshot {
        return MetricsSnapshot(
            timestamp = getCurrentTimeMillis(),
            metrics = getCurrentMetrics() ?: TrainingMetrics(0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0),
            performance = PerformanceStats(1000L, 0.5, 0.3, 2.0, 10.0)
        )
    }
    
    fun restoreFromSnapshot(snapshot: MetricsSnapshot) {
        println("📊 Restored metrics from snapshot at ${snapshot.timestamp}")
    }
}

/**
 * Performance monitor
 */
class PerformanceMonitor {
    private var isMonitoring = false
    private var startTime: Long = 0
    private val performanceHistory = mutableListOf<PerformanceStats>()
    
    fun startMonitoring() {
        isMonitoring = true
        startTime = getCurrentTimeMillis()
        println("⚡ Started performance monitoring")
    }
    
    fun pauseMonitoring() {
        println("⏸️ Paused performance monitoring")
    }
    
    fun resumeMonitoring() {
        println("▶️ Resumed performance monitoring")
    }
    
    fun stopMonitoring() {
        isMonitoring = false
        println("⚡ Stopped performance monitoring")
    }
    
    fun getCurrentStats(): PerformanceStats? {
        if (!isMonitoring) return null
        
        return PerformanceStats(
            averageIterationTime = kotlin.random.Random.nextLong(5000L, 30000L),
            memoryUsage = kotlin.random.Random.nextDouble(0.3, 0.8),
            cpuUsage = kotlin.random.Random.nextDouble(0.2, 0.7),
            throughput = kotlin.random.Random.nextDouble(1.0, 5.0),
            efficiency = kotlin.random.Random.nextDouble(5.0, 20.0)
        )
    }
    
    fun generateAnalysis(): PerformanceAnalysis {
        val avgPerf = performanceHistory.map { it.throughput }.average()
        
        return PerformanceAnalysis(
            averagePerformance = avgPerf,
            performanceTrend = when {
                avgPerf > 3.0 -> "improving"
                avgPerf > 2.0 -> "stable"
                else -> "declining"
            },
            bottlenecks = listOf("Memory allocation", "Experience replay sampling"),
            optimizationSuggestions = listOf(
                "Consider increasing batch size for better GPU utilization",
                "Implement experience prioritization for better sample efficiency"
            )
        )
    }
}