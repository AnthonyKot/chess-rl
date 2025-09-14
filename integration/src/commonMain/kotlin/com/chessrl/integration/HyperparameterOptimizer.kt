package com.chessrl.integration

import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced Hyperparameter Optimizer for chess-specific tuning
 */
class HyperparameterOptimizer {
    private val searchSpace = HyperparameterSearchSpace()
    private val evaluator = HyperparameterEvaluator()
    private val abTester = ABTester()
    
    data class HyperparameterConfig(
        val learningRate: Double = 0.001,
        val batchSize: Int = 64,
        val explorationRate: Double = 0.1,
        val explorationDecay: Double = 0.995,
        val rewardScaling: Double = 1.0,
        val trainingFrequency: Int = 4,
        val networkArchitecture: NetworkArchitecture = NetworkArchitecture.DEFAULT,
        val experienceSamplingStrategy: SamplingStrategy = SamplingStrategy.UNIFORM,
        val selfPlayFrequency: Int = 10
    )
    
    enum class NetworkArchitecture(val hiddenLayers: List<Int>) {
        SMALL(listOf(256, 128)),
        DEFAULT(listOf(512, 256)),
        LARGE(listOf(1024, 512, 256)),
        DEEP(listOf(512, 512, 256, 128))
    }
    
    enum class SamplingStrategy {
        UNIFORM, RECENT, MIXED, PRIORITIZED
    }
    
    fun optimizeHyperparameters(
        baseConfig: HyperparameterConfig,
        optimizationBudget: Int = 50
    ): HyperparameterOptimizationResult {
        val startTime = getCurrentTimeMillis()
        
        // Generate search candidates
        val candidates = searchSpace.generateCandidates(baseConfig, optimizationBudget)
        
        // Evaluate candidates
        val evaluationResults = mutableListOf<HyperparameterEvaluation>()
        for ((index, candidate) in candidates.withIndex()) {
            println("Evaluating candidate ${index + 1}/$optimizationBudget")
            val evaluation = evaluator.evaluate(candidate)
            evaluationResults.add(HyperparameterEvaluation(candidate, evaluation))
        }
        
        // Find optimal configuration
        val optimalConfig = evaluationResults.maxByOrNull { it.performance.overallScore }!!
        
        // Perform A/B testing on top candidates
        val topCandidates = evaluationResults.sortedByDescending { it.performance.overallScore }.take(3)
        val abTestResult = abTester.performABTest(topCandidates.map { it.config })
        
        val totalTime = getCurrentTimeMillis() - startTime
        
        return HyperparameterOptimizationResult(
            optimalConfig = optimalConfig.config,
            optimizationHistory = evaluationResults,
            abTestResult = abTestResult,
            totalOptimizationTime = totalTime,
            improvementFactor = optimalConfig.performance.overallScore / evaluateBaseline(baseConfig)
        )
    }
    
    private fun evaluateBaseline(config: HyperparameterConfig): Double {
        return evaluator.evaluate(config).overallScore
    }
}

/**
 * Hyperparameter Search Space Generator
 */
class HyperparameterSearchSpace {
    fun generateCandidates(
        baseConfig: HyperparameterOptimizer.HyperparameterConfig,
        count: Int
    ): List<HyperparameterOptimizer.HyperparameterConfig> {
        val candidates = mutableListOf<HyperparameterOptimizer.HyperparameterConfig>()
        
        // Add base configuration
        candidates.add(baseConfig)
        
        // Generate random search candidates
        repeat(count - 1) {
            candidates.add(generateRandomCandidate(baseConfig))
        }
        
        // Add some systematic variations
        candidates.addAll(generateSystematicVariations(baseConfig))
        
        return candidates.take(count)
    }
    
    private fun generateRandomCandidate(
        base: HyperparameterOptimizer.HyperparameterConfig
    ): HyperparameterOptimizer.HyperparameterConfig {
        return HyperparameterOptimizer.HyperparameterConfig(
            learningRate = sampleLearningRate(),
            batchSize = sampleBatchSize(),
            explorationRate = sampleExplorationRate(),
            explorationDecay = sampleExplorationDecay(),
            rewardScaling = sampleRewardScaling(),
            trainingFrequency = sampleTrainingFrequency(),
            networkArchitecture = sampleNetworkArchitecture(),
            experienceSamplingStrategy = sampleSamplingStrategy(),
            selfPlayFrequency = sampleSelfPlayFrequency()
        )
    }
    
    private fun generateSystematicVariations(
        base: HyperparameterOptimizer.HyperparameterConfig
    ): List<HyperparameterOptimizer.HyperparameterConfig> {
        val variations = mutableListOf<HyperparameterOptimizer.HyperparameterConfig>()
        
        // Learning rate variations
        for (lr in listOf(0.0001, 0.001, 0.01)) {
            variations.add(base.copy(learningRate = lr))
        }
        
        // Batch size variations
        for (bs in listOf(32, 64, 128)) {
            variations.add(base.copy(batchSize = bs))
        }
        
        // Architecture variations
        for (arch in HyperparameterOptimizer.NetworkArchitecture.values()) {
            variations.add(base.copy(networkArchitecture = arch))
        }
        
        return variations
    }
    
    private fun sampleLearningRate(): Double {
        val logMin = log10(0.0001)
        val logMax = log10(0.1)
        val logValue = Random.nextDouble(logMin, logMax)
        return 10.0.pow(logValue)
    }
    
    private fun sampleBatchSize(): Int {
        return listOf(16, 32, 64, 128, 256).random()
    }
    
    private fun sampleExplorationRate(): Double {
        return Random.nextDouble(0.01, 0.3)
    }
    
    private fun sampleExplorationDecay(): Double {
        return Random.nextDouble(0.99, 0.9999)
    }
    
    private fun sampleRewardScaling(): Double {
        return Random.nextDouble(0.1, 10.0)
    }
    
    private fun sampleTrainingFrequency(): Int {
        return listOf(1, 2, 4, 8, 16).random()
    }
    
    private fun sampleNetworkArchitecture(): HyperparameterOptimizer.NetworkArchitecture {
        return HyperparameterOptimizer.NetworkArchitecture.values().random()
    }
    
    private fun sampleSamplingStrategy(): HyperparameterOptimizer.SamplingStrategy {
        return HyperparameterOptimizer.SamplingStrategy.values().random()
    }
    
    private fun sampleSelfPlayFrequency(): Int {
        return Random.nextInt(5, 50)
    }
}

/**
 * Hyperparameter Evaluator
 */
class HyperparameterEvaluator {
    fun evaluate(config: HyperparameterOptimizer.HyperparameterConfig): HyperparameterPerformance {
        // Simulate training with the given configuration
        val trainingResult = simulateTraining(config)
        
        return HyperparameterPerformance(
            convergenceSpeed = evaluateConvergenceSpeed(trainingResult),
            finalPerformance = evaluateFinalPerformance(trainingResult),
            trainingStability = evaluateTrainingStability(trainingResult),
            resourceEfficiency = evaluateResourceEfficiency(config),
            chessSpecificMetrics = evaluateChessMetrics(trainingResult),
            overallScore = calculateOverallScore(trainingResult, config)
        )
    }
    
    private fun simulateTraining(config: HyperparameterOptimizer.HyperparameterConfig): TrainingSimulationResult {
        val episodes = 100
        val winRates = mutableListOf<Double>()
        val losses = mutableListOf<Double>()
        val gameQualities = mutableListOf<Double>()
        
        var currentWinRate = 0.3 // Start with random play
        var currentLoss = 1.0
        var currentGameQuality = 0.2
        
        for (episode in 1..episodes) {
            // Simulate learning progress based on hyperparameters
            val learningProgress = simulateLearningStep(config, episode)
            
            currentWinRate = min(0.9, currentWinRate + learningProgress.winRateImprovement)
            currentLoss = max(0.01, currentLoss * learningProgress.lossReduction)
            currentGameQuality = min(1.0, currentGameQuality + learningProgress.qualityImprovement)
            
            winRates.add(currentWinRate)
            losses.add(currentLoss)
            gameQualities.add(currentGameQuality)
        }
        
        return TrainingSimulationResult(
            winRates = winRates,
            losses = losses,
            gameQualities = gameQualities,
            convergenceEpisode = findConvergenceEpisode(winRates),
            finalWinRate = winRates.last(),
            trainingTime = estimateTrainingTime(config)
        )
    }
    
    private fun simulateLearningStep(
        config: HyperparameterOptimizer.HyperparameterConfig,
        episode: Int
    ): LearningProgress {
        // Simulate learning based on hyperparameters
        val baseLearningRate = config.learningRate
        val explorationEffect = config.explorationRate * exp(-episode * 0.01)
        val batchEffect = min(1.0, config.batchSize / 64.0)
        val architectureEffect = when (config.networkArchitecture) {
            HyperparameterOptimizer.NetworkArchitecture.SMALL -> 0.8
            HyperparameterOptimizer.NetworkArchitecture.DEFAULT -> 1.0
            HyperparameterOptimizer.NetworkArchitecture.LARGE -> 1.2
            HyperparameterOptimizer.NetworkArchitecture.DEEP -> 1.1
        }
        
        val learningFactor = baseLearningRate * batchEffect * architectureEffect
        val stabilityFactor = 1.0 - explorationEffect * 0.1
        
        return LearningProgress(
            winRateImprovement = learningFactor * 0.01 * stabilityFactor,
            lossReduction = 0.99 + learningFactor * 0.001,
            qualityImprovement = learningFactor * 0.005 * stabilityFactor
        )
    }
    
    private fun findConvergenceEpisode(winRates: List<Double>): Int {
        val windowSize = 10
        val threshold = 0.01
        
        for (i in windowSize until winRates.size) {
            val recentWindow = winRates.subList(i - windowSize, i)
            val variance = calculateVariance(recentWindow)
            if (variance < threshold) {
                return i
            }
        }
        
        return winRates.size
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun evaluateConvergenceSpeed(result: TrainingSimulationResult): Double {
        return max(0.0, 1.0 - result.convergenceEpisode / 100.0)
    }
    
    private fun evaluateFinalPerformance(result: TrainingSimulationResult): Double {
        return result.finalWinRate
    }
    
    private fun evaluateTrainingStability(result: TrainingSimulationResult): Double {
        val winRateVariance = calculateVariance(result.winRates.takeLast(20))
        return max(0.0, 1.0 - winRateVariance * 10)
    }
    
    private fun evaluateResourceEfficiency(config: HyperparameterOptimizer.HyperparameterConfig): Double {
        val batchEfficiency = min(1.0, 64.0 / config.batchSize)
        val architectureEfficiency = when (config.networkArchitecture) {
            HyperparameterOptimizer.NetworkArchitecture.SMALL -> 1.0
            HyperparameterOptimizer.NetworkArchitecture.DEFAULT -> 0.8
            HyperparameterOptimizer.NetworkArchitecture.LARGE -> 0.6
            HyperparameterOptimizer.NetworkArchitecture.DEEP -> 0.7
        }
        val frequencyEfficiency = min(1.0, 4.0 / config.trainingFrequency)
        
        return (batchEfficiency + architectureEfficiency + frequencyEfficiency) / 3.0
    }
    
    private fun evaluateChessMetrics(result: TrainingSimulationResult): ChessSpecificMetrics {
        return ChessSpecificMetrics(
            gameQuality = result.gameQualities.average(),
            moveAccuracy = result.finalWinRate * 0.8 + 0.2,
            strategicUnderstanding = result.gameQualities.last(),
            tacticalStrength = result.finalWinRate
        )
    }
    
    private fun calculateOverallScore(
        result: TrainingSimulationResult,
        config: HyperparameterOptimizer.HyperparameterConfig
    ): Double {
        val convergenceScore = evaluateConvergenceSpeed(result)
        val performanceScore = evaluateFinalPerformance(result)
        val stabilityScore = evaluateTrainingStability(result)
        val efficiencyScore = evaluateResourceEfficiency(config)
        
        return (convergenceScore * 0.2 + 
                performanceScore * 0.4 + 
                stabilityScore * 0.2 + 
                efficiencyScore * 0.2)
    }
    
    private fun estimateTrainingTime(config: HyperparameterOptimizer.HyperparameterConfig): Long {
        val baseTime = 1000L // Base time in milliseconds
        val batchFactor = config.batchSize / 64.0
        val architectureFactor = when (config.networkArchitecture) {
            HyperparameterOptimizer.NetworkArchitecture.SMALL -> 0.5
            HyperparameterOptimizer.NetworkArchitecture.DEFAULT -> 1.0
            HyperparameterOptimizer.NetworkArchitecture.LARGE -> 2.0
            HyperparameterOptimizer.NetworkArchitecture.DEEP -> 1.5
        }
        
        return (baseTime * batchFactor * architectureFactor).toLong()
    }
    
    data class TrainingSimulationResult(
        val winRates: List<Double>,
        val losses: List<Double>,
        val gameQualities: List<Double>,
        val convergenceEpisode: Int,
        val finalWinRate: Double,
        val trainingTime: Long
    )
    
    data class LearningProgress(
        val winRateImprovement: Double,
        val lossReduction: Double,
        val qualityImprovement: Double
    )
}

/**
 * A/B Tester for hyperparameter validation
 */
class ABTester {
    fun performABTest(
        configs: List<HyperparameterOptimizer.HyperparameterConfig>
    ): ABTestResult {
        val testResults = mutableMapOf<HyperparameterOptimizer.HyperparameterConfig, ABTestMetrics>()
        
        for (config in configs) {
            val metrics = runABTestTrial(config)
            testResults[config] = metrics
        }
        
        val bestConfig = testResults.maxByOrNull { it.value.statisticalSignificance }!!
        
        return ABTestResult(
            testResults = testResults,
            winnerConfig = bestConfig.key,
            confidenceLevel = bestConfig.value.statisticalSignificance,
            effectSize = calculateEffectSize(testResults)
        )
    }
    
    private fun runABTestTrial(config: HyperparameterOptimizer.HyperparameterConfig): ABTestMetrics {
        val trials = 10
        val results = mutableListOf<Double>()
        
        repeat(trials) {
            val result = simulateTrainingTrial(config)
            results.add(result)
        }
        
        val mean = results.average()
        val variance = results.map { (it - mean).pow(2) }.average()
        val standardError = sqrt(variance / trials)
        val tStatistic = mean / standardError
        val pValue = calculatePValue(tStatistic, trials - 1)
        
        return ABTestMetrics(
            meanPerformance = mean,
            standardError = standardError,
            statisticalSignificance = 1.0 - pValue,
            sampleSize = trials
        )
    }
    
    private fun simulateTrainingTrial(config: HyperparameterOptimizer.HyperparameterConfig): Double {
        // Simulate a single training trial
        val basePerformance = 0.5
        val learningRateEffect = config.learningRate * 100
        val batchSizeEffect = min(1.0, config.batchSize / 64.0) * 0.1
        val explorationEffect = (1.0 - config.explorationRate) * 0.1
        val noise = Random.Default.nextGaussian() * 0.05
        
        return basePerformance + learningRateEffect + batchSizeEffect + explorationEffect + noise
    }
    
    private fun calculatePValue(tStatistic: Double, degreesOfFreedom: Int): Double {
        // Simplified p-value calculation (in real implementation, use proper statistical library)
        val absT = abs(tStatistic)
        return max(0.001, 1.0 / (1.0 + absT))
    }
    
    private fun calculateEffectSize(
        testResults: Map<HyperparameterOptimizer.HyperparameterConfig, ABTestMetrics>
    ): Double {
        val performances = testResults.values.map { it.meanPerformance }
        val maxPerformance = performances.maxOrNull() ?: 0.0
        val minPerformance = performances.minOrNull() ?: 0.0
        return maxPerformance - minPerformance
    }
    
    data class ABTestMetrics(
        val meanPerformance: Double,
        val standardError: Double,
        val statisticalSignificance: Double,
        val sampleSize: Int
    )
}

// Data classes for results
data class HyperparameterOptimizationResult(
    val optimalConfig: HyperparameterOptimizer.HyperparameterConfig,
    val optimizationHistory: List<HyperparameterEvaluation>,
    val abTestResult: ABTestResult,
    val totalOptimizationTime: Long,
    val improvementFactor: Double
)

data class HyperparameterEvaluation(
    val config: HyperparameterOptimizer.HyperparameterConfig,
    val performance: HyperparameterPerformance
)

data class HyperparameterPerformance(
    val convergenceSpeed: Double,
    val finalPerformance: Double,
    val trainingStability: Double,
    val resourceEfficiency: Double,
    val chessSpecificMetrics: ChessSpecificMetrics,
    val overallScore: Double
)

data class ChessSpecificMetrics(
    val gameQuality: Double,
    val moveAccuracy: Double,
    val strategicUnderstanding: Double,
    val tacticalStrength: Double
)

data class ABTestResult(
    val testResults: Map<HyperparameterOptimizer.HyperparameterConfig, ABTester.ABTestMetrics>,
    val winnerConfig: HyperparameterOptimizer.HyperparameterConfig,
    val confidenceLevel: Double,
    val effectSize: Double
)
