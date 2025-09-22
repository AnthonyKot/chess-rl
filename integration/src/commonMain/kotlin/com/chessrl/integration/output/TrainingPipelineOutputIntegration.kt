package com.chessrl.integration.output

import com.chessrl.integration.*
import com.chessrl.integration.config.ChessRLConfig

/**
 * Demonstration of how to integrate OutputManager into TrainingPipeline.
 * This shows the key integration points and modifications needed.
 */
class TrainingPipelineOutputIntegration {
    
    /**
     * Example of how to modify TrainingPipeline constructor to include OutputManager
     */
    fun createTrainingPipelineWithOutput(
        @Suppress("UNUSED_PARAMETER") config: ChessRLConfig,
        outputConfig: OutputConfig = OutputConfig()
    ): OutputManager {
        // Create OutputManager instance
        val outputManager = OutputManager(outputConfig)
        
        // This would be added to TrainingPipeline as a private field:
        // private val outputManager = OutputManager(outputConfig)
        
        return outputManager
    }
    
    /**
     * Example of how to modify the initialize() method to use OutputManager
     */
    fun demonstrateInitializeIntegration(
        config: ChessRLConfig,
        outputManager: OutputManager,
        profiles: List<String> = emptyList(),
        overrides: Map<String, Any> = emptyMap()
    ) {
        // Replace the existing ChessRLLogger.logConfiguration(config) call with:
        val (profileList, overrideMap, keyParams) = TrainingPipelineIntegration.createConfigSummaryData(
            config, profiles, overrides
        )
        outputManager.reportConfigSummary(profileList, overrideMap, keyParams)
        
        // The rest of the initialization remains the same
    }
    
    /**
     * Example of how to modify runTrainingCycle() to use OutputManager
     */
    fun demonstrateCycleReporting(
        result: TrainingCycleResult,
        totalCycles: Int,
        bufferSize: Int,
        maxBufferSize: Int,
        outputManager: OutputManager,
        checkpointEvent: CheckpointEvent? = null
    ) {
        // Replace the existing ChessRLLogger.logTrainingCycle() call with:
        val cycleData = result.toCycleProgressData(
            totalCycles = totalCycles,
            bufferSize = bufferSize,
            maxBufferSize = maxBufferSize,
            checkpointEvent = checkpointEvent
        )
        
        outputManager.reportCycleProgress(cycleData)
    }
    
    /**
     * Example of how to modify the final summary reporting
     */
    fun demonstrateFinalSummaryReporting(
        results: TrainingResults,
        outputManager: OutputManager
    ) {
        // Replace the existing final summary logging with:
        val finalSummary = results.toFinalSummaryData()
        outputManager.reportFinalSummary(finalSummary)
    }
    
    /**
     * Example of how to handle checkpoint events
     */
    fun demonstrateCheckpointReporting(
        outputManager: OutputManager,
        checkpointPath: String,
        currentPerformance: Double,
        previousBest: Double,
        isBest: Boolean = false
    ) {
        // Create checkpoint event
        val checkpointEvent = TrainingPipelineIntegration.createCheckpointEvent(
            type = if (isBest) CheckpointType.BEST else CheckpointType.REGULAR,
            path = checkpointPath,
            currentPerformance = currentPerformance,
            previousBest = previousBest,
            isBest = isBest
        )
        
        // Report the checkpoint event
        outputManager.reportCheckpointEvent(checkpointEvent)
    }
    
    /**
     * Example of how to handle validation results
     */
    fun demonstrateValidationReporting(
        outputManager: OutputManager,
        validationResults: List<AggregatedValidationMessage>
    ) {
        // Report validation results through OutputManager
        outputManager.reportValidationResults(validationResults)
    }
}

/**
 * Integration instructions for modifying TrainingPipeline:
 * 
 * 1. Add OutputManager as a constructor parameter or create it from config:
 *    ```kotlin
 *    class TrainingPipeline(
 *        private val config: ChessRLConfig,
 *        private val backend: LearningBackend = DqnLearningBackend(),
 *        private val outputConfig: OutputConfig = OutputConfig()
 *    ) {
 *        private val outputManager = OutputManager(outputConfig)
 *        // ... rest of the class
 *    }
 *    ```
 * 
 * 2. In initialize() method, replace:
 *    ```kotlin
 *    ChessRLLogger.logConfiguration(config)
 *    ```
 *    with:
 *    ```kotlin
 *    val (profiles, overrides, keyParams) = TrainingPipelineIntegration.createConfigSummaryData(config)
 *    outputManager.reportConfigSummary(profiles, overrides, keyParams)
 *    ```
 * 
 * 3. In runTrainingCycle(), replace:
 *    ```kotlin
 *    ChessRLLogger.logTrainingCycle(...)
 *    ```
 *    with:
 *    ```kotlin
 *    val cycleData = result.toCycleProgressData(config.maxCycles, experienceManager.getBufferSize(), config.maxExperienceBuffer)
 *    outputManager.reportCycleProgress(cycleData)
 *    ```
 * 
 * 4. For checkpoint events, replace checkpoint logging with:
 *    ```kotlin
 *    val checkpointEvent = TrainingPipelineIntegration.createCheckpointEvent(...)
 *    outputManager.reportCheckpointEvent(checkpointEvent)
 *    ```
 * 
 * 5. In the final summary, replace:
 *    ```kotlin
 *    logger.info("🏁 Training Completed!")
 *    // ... other final logging
 *    ```
 *    with:
 *    ```kotlin
 *    val finalSummary = results.toFinalSummaryData()
 *    outputManager.reportFinalSummary(finalSummary)
 *    ```
 * 
 * 6. Remove or comment out the old logging calls:
 *    - logger.info("Training Cycle $cycle/${config.maxCycles}")
 *    - logger.info("-".repeat(40))
 *    - All the individual metric logging calls
 * 
 * This integration maintains backward compatibility while providing the new structured output system.
 */
