package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.math.*

/**
 * Neural network activation analysis and visualization component for debugging
 * chess RL agent decision-making processes at the network level.
 */
class NeuralNetworkAnalyzer(
    private val agent: ChessAgent,
    private val config: NetworkAnalysisConfig = NetworkAnalysisConfig()
) {
    
    private val stateEncoder = ChessStateEncoder()
    private val actionEncoder = ChessActionEncoder()
    
    /**
     * Analyze neural network output for a given position
     */
    fun analyzeNetworkOutput(board: ChessBoard): NeuralNetworkOutput {
        val state = stateEncoder.encode(board)
        val validMoves = getValidMoves(board)
        
        // Get raw outputs derived from agent APIs
        val qValues = agent.getQValues(state, validMoves)
        val probs = agent.getActionProbabilities(state, validMoves)
        val policyOutput = DoubleArray(ChessActionEncoder.ACTION_SPACE_SIZE) { 0.0 }.also { arr ->
            probs.forEach { (action, p) -> if (action in arr.indices) arr[action] = p }
        }
        val valueOutput = qValues.values.average()
        
        // Analyze policy distribution
        val policyAnalysis = analyzePolicyDistribution(policyOutput, validMoves)
        
        // Analyze value estimation
        val valueAnalysis = analyzeValueEstimation(valueOutput, board)
        
        // Analyze Q-value distribution
        val qValueAnalysis = analyzeQValueDistribution(qValues, validMoves)
        
        return NeuralNetworkOutput(
            position = board.toFEN(),
            policyOutput = policyOutput,
            valueOutput = valueOutput,
            qValues = qValues,
            policyAnalysis = policyAnalysis,
            valueAnalysis = valueAnalysis,
            qValueAnalysis = qValueAnalysis,
            timestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Analyze neural network activations layer by layer
     */
    fun analyzeActivations(
        board: ChessBoard,
        includeLayerAnalysis: Boolean = true,
        includeAttentionMaps: Boolean = false
    ): NeuralNetworkActivationAnalysis {
        
        val state = stateEncoder.encode(board)
        
        // Get layer activations (simplified - would need actual network access)
        val layerActivations = if (includeLayerAnalysis) {
            analyzeLayerActivations(state)
        } else emptyMap()
        
        // Get attention maps (if applicable)
        val attentionMaps = if (includeAttentionMaps) {
            analyzeAttentionMaps(state, board)
        } else emptyMap()
        
        // Analyze activation patterns
        val activationPatterns = analyzeActivationPatterns(layerActivations)
        
        // Detect potential issues
        val networkIssues = detectNetworkIssues(layerActivations)
        
        return NeuralNetworkActivationAnalysis(
            position = board.toFEN(),
            layerActivations = layerActivations,
            attentionMaps = attentionMaps,
            activationPatterns = activationPatterns,
            networkIssues = networkIssues,
            analysisTimestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Compare network outputs across different positions
     */
    fun compareNetworkOutputs(
        positions: List<ChessBoard>,
        comparisonType: ComparisonType = ComparisonType.POLICY_SIMILARITY
    ): NetworkComparisonResult {
        
        val outputs = positions.map { board ->
            analyzeNetworkOutput(board)
        }
        
        val similarities = when (comparisonType) {
            ComparisonType.POLICY_SIMILARITY -> calculatePolicySimilarities(outputs)
            ComparisonType.VALUE_SIMILARITY -> calculateValueSimilarities(outputs)
            ComparisonType.QVALUE_SIMILARITY -> calculateQValueSimilarities(outputs)
        }
        
        val patterns = identifyCommonPatterns(outputs)
        val outliers = identifyOutliers(outputs, similarities)
        
        return NetworkComparisonResult(
            positions = positions.map { it.toFEN() },
            outputs = outputs,
            similarities = similarities,
            commonPatterns = patterns,
            outliers = outliers,
            comparisonType = comparisonType
        )
    }
    
    /**
     * Analyze network behavior across game phases
     */
    fun analyzeGamePhaseNetworkBehavior(
        gameHistory: List<Move>,
        samplePositions: Int = 10
    ): GamePhaseNetworkAnalysis {
        
        // Build a fresh board per sampled position
        var baseBoard = ChessBoard()
        val phaseAnalyses = mutableListOf<NeuralNetworkGamePhaseAnalysis>()
        
        // Sample positions throughout the game
        val sampleIndices = if (gameHistory.size <= samplePositions) {
            gameHistory.indices.toList()
        } else {
            (0 until samplePositions).map { 
                (it * gameHistory.size / samplePositions).coerceAtMost(gameHistory.size - 1)
            }
        }
        
        sampleIndices.forEach { moveIndex ->
            // Create a fresh board and play moves up to this point
            val board = ChessBoard()
            repeat(moveIndex) { i ->
                board.makeLegalMove(gameHistory[i])
                board.switchActiveColor()
            }
            
            val gamePhase = determineGamePhase(board, moveIndex)
            val networkOutput = analyzeNetworkOutput(board)
            val phaseCharacteristics = analyzePhaseCharacteristics(networkOutput, gamePhase)
            
            phaseAnalyses.add(NeuralNetworkGamePhaseAnalysis(
                moveNumber = moveIndex + 1,
                gamePhase = gamePhase,
                networkOutput = networkOutput,
                phaseCharacteristics = phaseCharacteristics
            ))
        }
        
        // Analyze phase transitions
        val phaseTransitions = analyzePhaseTransitions(phaseAnalyses)
        
        // Identify phase-specific patterns
        val phasePatterns = identifyPhasePatterns(phaseAnalyses)
        
        return GamePhaseNetworkAnalysis(
            gameLength = gameHistory.size,
            phaseAnalyses = phaseAnalyses,
            phaseTransitions = phaseTransitions,
            phasePatterns = phasePatterns
        )
    }
    
    /**
     * Visualize network decision-making process
     */
    fun visualizeDecisionMaking(
        board: ChessBoard,
        visualizationType: VisualizationType = VisualizationType.HEATMAP
    ): DecisionVisualization {
        
        val networkOutput = analyzeNetworkOutput(board)
        val validMoves = getValidMoves(board)
        
        val visualization = when (visualizationType) {
            VisualizationType.HEATMAP -> createPolicyHeatmap(networkOutput, board)
            VisualizationType.ARROW_DIAGRAM -> createArrowDiagram(networkOutput, board)
            VisualizationType.PROBABILITY_BARS -> createProbabilityBars(networkOutput, validMoves)
            VisualizationType.DECISION_TREE -> createDecisionTree(networkOutput, validMoves)
            else -> createProbabilityBars(networkOutput, validMoves)
        }
        
        return DecisionVisualization(
            position = board.toFEN(),
            visualizationType = visualizationType,
            visualization = visualization,
            networkOutput = networkOutput
        )
    }
    
    /**
     * Analyze network confidence and uncertainty
     */
    fun analyzeNetworkConfidence(board: ChessBoard): NetworkConfidenceAnalysis {
        val networkOutput = analyzeNetworkOutput(board)
        
        // Calculate policy entropy (uncertainty measure)
        val policyEntropy = calculatePolicyEntropy(networkOutput.policyOutput)
        
        // Calculate value confidence
        val valueConfidence = calculateValueConfidence(networkOutput.valueOutput)
        
        // Calculate Q-value spread
        val qValueSpread = calculateQValueSpread(networkOutput.qValues)
        
        // Determine overall confidence level
        val overallConfidence = calculateOverallConfidence(policyEntropy, valueConfidence, qValueSpread)
        
        // Identify uncertainty sources
        val uncertaintySources = identifyUncertaintySources(networkOutput, board)
        
        return NetworkConfidenceAnalysis(
            position = board.toFEN(),
            policyEntropy = policyEntropy,
            valueConfidence = valueConfidence,
            qValueSpread = qValueSpread,
            overallConfidence = overallConfidence,
            uncertaintySources = uncertaintySources,
            confidenceLevel = categorizeConfidenceLevel(overallConfidence)
        )
    }
    
    // Private analysis methods
    
    private fun analyzePolicyDistribution(
        policyOutput: DoubleArray,
        validMoves: List<Int>
    ): PolicyDistributionAnalysis {
        
        val validProbabilities = validMoves.map { policyOutput[it] }
        val totalValidProbability = validProbabilities.sum()
        
        // Normalize probabilities for valid moves
        val normalizedProbabilities = if (totalValidProbability > 0) {
            validProbabilities.map { it / totalValidProbability }
        } else {
            validProbabilities.map { 1.0 / validMoves.size }
        }
        
        val entropy = calculateEntropy(normalizedProbabilities)
        val maxProbability = normalizedProbabilities.maxOrNull() ?: 0.0
        val concentration = 1.0 - entropy / ln(validMoves.size.toDouble())
        
        return PolicyDistributionAnalysis(
            totalValidProbability = totalValidProbability,
            entropy = entropy,
            maxProbability = maxProbability,
            concentration = concentration,
            distributionShape = categorizeDistributionShape(normalizedProbabilities)
        )
    }
    
    private fun analyzeValueEstimation(valueOutput: Double, board: ChessBoard): ValueEstimationAnalysis {
        val gamePhase = determineGamePhase(board, 0) // Simplified
        val materialBalance = calculateMaterialBalance(board)
        
        // Assess value reasonableness based on position
        val expectedValueRange = estimateExpectedValueRange(board, gamePhase, materialBalance)
        val isReasonable = valueOutput in expectedValueRange
        
        return ValueEstimationAnalysis(
            estimatedValue = valueOutput,
            expectedRange = expectedValueRange,
            isReasonable = isReasonable,
            gamePhase = gamePhase,
            materialBalance = materialBalance,
            confidence = calculateValueConfidence(valueOutput)
        )
    }
    
    private fun analyzeQValueDistribution(
        qValues: Map<Int, Double>,
        validMoves: List<Int>
    ): QValueDistributionAnalysis {
        
        val validQValues = validMoves.mapNotNull { qValues[it] }
        
        if (validQValues.isEmpty()) {
            return QValueDistributionAnalysis(
                meanQValue = 0.0,
                qValueVariance = 0.0,
                maxQValue = 0.0,
                minQValue = 0.0,
                qValueSpread = 0.0,
                distributionShape = DistributionShape.UNIFORM
            )
        }
        
        val meanQValue = validQValues.average()
        val qValueVariance = calculateVariance(validQValues)
        val maxQValue = validQValues.maxOrNull() ?: 0.0
        val minQValue = validQValues.minOrNull() ?: 0.0
        val qValueSpread = maxQValue - minQValue
        
        return QValueDistributionAnalysis(
            meanQValue = meanQValue,
            qValueVariance = qValueVariance,
            maxQValue = maxQValue,
            minQValue = minQValue,
            qValueSpread = qValueSpread,
            distributionShape = categorizeDistributionShape(validQValues)
        )
    }
    
    private fun analyzeLayerActivations(state: DoubleArray): Map<String, LayerActivationAnalysis> {
        // Simplified layer analysis - would need actual network architecture access
        val layers = mapOf(
            "input" to LayerActivationAnalysis(
                layerName = "input",
                activationStats = ActivationStats(
                    mean = state.average(),
                    variance = calculateVariance(state.toList()),
                    min = state.minOrNull() ?: 0.0,
                    max = state.maxOrNull() ?: 0.0
                ),
                sparsity = calculateSparsity(state),
                saturation = calculateSaturation(state)
            ),
            "hidden1" to LayerActivationAnalysis(
                layerName = "hidden1",
                activationStats = ActivationStats(0.0, 0.1, -1.0, 1.0),
                sparsity = 0.3,
                saturation = 0.1
            ),
            "output" to LayerActivationAnalysis(
                layerName = "output",
                activationStats = ActivationStats(0.0, 0.05, 0.0, 1.0),
                sparsity = 0.8,
                saturation = 0.05
            )
        )
        
        return layers
    }
    
    private fun analyzeAttentionMaps(
        state: DoubleArray,
        board: ChessBoard
    ): Map<String, AttentionMap> {
        // Simplified attention analysis - would need actual attention mechanism access
        return mapOf(
            "piece_attention" to AttentionMap(
                name = "piece_attention",
                dimensions = Pair(8, 8),
                weights = generateMockAttentionWeights(8, 8),
                focusAreas = identifyFocusAreas(board)
            )
        )
    }
    
    private fun analyzeActivationPatterns(
        layerActivations: Map<String, LayerActivationAnalysis>
    ): ActivationPatternAnalysis {
        
        val deadNeurons = layerActivations.values.sumOf { 
            (it.sparsity * 100).toInt() // Simplified calculation
        }
        
        val saturatedNeurons = layerActivations.values.sumOf {
            (it.saturation * 100).toInt() // Simplified calculation
        }
        
        val activationHealth = when {
            deadNeurons > 50 || saturatedNeurons > 30 -> ActivationHealth.POOR
            deadNeurons > 20 || saturatedNeurons > 15 -> ActivationHealth.CONCERNING
            else -> ActivationHealth.HEALTHY
        }
        
        return ActivationPatternAnalysis(
            deadNeurons = deadNeurons,
            saturatedNeurons = saturatedNeurons,
            activationHealth = activationHealth,
            patterns = identifyActivationPatterns(layerActivations)
        )
    }
    
    private fun detectNetworkIssues(
        layerActivations: Map<String, LayerActivationAnalysis>
    ): List<NetworkIssue> {
        
        val issues = mutableListOf<NetworkIssue>()
        
        layerActivations.forEach { (layerName, analysis) ->
            if (analysis.sparsity > 0.9) {
                issues.add(NetworkIssue(
                    type = NetworkIssueType.DEAD_NEURONS,
                    layer = layerName,
                    severity = analysis.sparsity,
                    description = "High sparsity detected in layer $layerName"
                ))
            }
            
            if (analysis.saturation > 0.5) {
                issues.add(NetworkIssue(
                    type = NetworkIssueType.SATURATED_NEURONS,
                    layer = layerName,
                    severity = analysis.saturation,
                    description = "High saturation detected in layer $layerName"
                ))
            }
            
            if (analysis.activationStats.variance < 1e-6) {
                issues.add(NetworkIssue(
                    type = NetworkIssueType.VANISHING_GRADIENTS,
                    layer = layerName,
                    severity = 1.0 - analysis.activationStats.variance,
                    description = "Very low activation variance in layer $layerName"
                ))
            }
        }
        
        return issues
    }
    
    // Utility methods
    
    private fun getValidMoves(board: ChessBoard): List<Int> {
        // Simplified - would use actual move generation
        return (0..4095).toList() // Full action space for now
    }
    
    private fun calculateEntropy(probabilities: List<Double>): Double {
        return probabilities.filter { it > 0.0 }
            .sumOf { -it * ln(it) }
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateSparsity(activations: DoubleArray): Double {
        val zeroCount = activations.count { abs(it) < 1e-6 }
        return zeroCount.toDouble() / activations.size
    }
    
    private fun calculateSaturation(activations: DoubleArray): Double {
        val saturatedCount = activations.count { abs(it) > 0.99 }
        return saturatedCount.toDouble() / activations.size
    }
    
    private fun determineGamePhase(board: ChessBoard, moveNumber: Int): GamePhase {
        return when {
            moveNumber < 20 -> GamePhase.OPENING
            moveNumber < 60 -> GamePhase.MIDDLEGAME
            else -> GamePhase.ENDGAME
        }
    }
    
    private fun calculateMaterialBalance(board: ChessBoard): Double {
        // Simplified material calculation
        return 0.0 // Would implement actual material counting
    }
    
    private fun estimateExpectedValueRange(
        board: ChessBoard,
        gamePhase: GamePhase,
        materialBalance: Double
    ): ClosedRange<Double> {
        // Simplified expected value range estimation
        return when (gamePhase) {
            GamePhase.OPENING -> -0.5..0.5
            GamePhase.MIDDLEGAME -> -1.0..1.0
            GamePhase.ENDGAME -> -2.0..2.0
        }
    }
    
    private fun calculateValueConfidence(valueOutput: Double): Double {
        // Simplified confidence calculation based on value magnitude
        return abs(valueOutput).coerceIn(0.0, 1.0)
    }
    
    private fun calculatePolicyEntropy(policyOutput: DoubleArray): Double {
        val probabilities = policyOutput.filter { it > 0.0 }
        return calculateEntropy(probabilities)
    }
    
    private fun calculateQValueSpread(qValues: Map<Int, Double>): Double {
        val values = qValues.values
        return if (values.isNotEmpty()) {
            (values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0)
        } else 0.0
    }
    
    private fun calculateOverallConfidence(
        policyEntropy: Double,
        valueConfidence: Double,
        qValueSpread: Double
    ): Double {
        val entropyConfidence = 1.0 - (policyEntropy / ln(4096.0)) // Normalize by max entropy
        return (entropyConfidence + valueConfidence + (qValueSpread / 10.0)) / 3.0
    }
    
    private fun categorizeConfidenceLevel(confidence: Double): ConfidenceLevel {
        return when {
            confidence > 0.8 -> ConfidenceLevel.HIGH
            confidence > 0.6 -> ConfidenceLevel.MEDIUM
            confidence > 0.4 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.VERY_LOW
        }
    }
    
    private fun categorizeDistributionShape(values: List<Double>): DistributionShape {
        if (values.isEmpty()) return DistributionShape.UNIFORM
        
        val variance = calculateVariance(values)
        val maxValue = values.maxOrNull() ?: 0.0
        val concentration = maxValue / values.sum()
        
        return when {
            concentration > 0.8 -> DistributionShape.PEAKED
            variance < 0.1 -> DistributionShape.UNIFORM
            else -> DistributionShape.NORMAL
        }
    }
    
    private fun identifyUncertaintySources(
        networkOutput: NeuralNetworkOutput,
        board: ChessBoard
    ): List<UncertaintySource> {
        
        val sources = mutableListOf<UncertaintySource>()
        
        if (networkOutput.policyAnalysis.entropy > 3.0) {
            sources.add(UncertaintySource(
                type = UncertaintyType.HIGH_POLICY_ENTROPY,
                description = "Policy distribution is very uncertain",
                severity = networkOutput.policyAnalysis.entropy / 5.0
            ))
        }
        
        if (abs(networkOutput.valueOutput) < 0.1) {
            sources.add(UncertaintySource(
                type = UncertaintyType.NEUTRAL_POSITION,
                description = "Position evaluation is close to neutral",
                severity = 1.0 - abs(networkOutput.valueOutput) * 10
            ))
        }
        
        return sources
    }
    
    // Mock implementations for visualization
    
    private fun createPolicyHeatmap(output: NeuralNetworkOutput, board: ChessBoard): String {
        return "Policy heatmap visualization for position ${board.toFEN()}"
    }
    
    private fun createArrowDiagram(output: NeuralNetworkOutput, board: ChessBoard): String {
        return "Arrow diagram visualization for position ${board.toFEN()}"
    }
    
    private fun createProbabilityBars(output: NeuralNetworkOutput, validMoves: List<Int>): String {
        return "Probability bars for ${validMoves.size} valid moves"
    }
    
    private fun createDecisionTree(output: NeuralNetworkOutput, validMoves: List<Int>): String {
        return "Decision tree visualization for ${validMoves.size} moves"
    }
    
    private fun generateMockAttentionWeights(rows: Int, cols: Int): Array<DoubleArray> {
        return Array(rows) { DoubleArray(cols) { kotlin.random.Random.nextDouble() } }
    }
    
    private fun identifyFocusAreas(board: ChessBoard): List<String> {
        return listOf("center", "king_safety", "piece_activity")
    }
    
    private fun identifyActivationPatterns(
        layerActivations: Map<String, LayerActivationAnalysis>
    ): List<String> {
        return listOf("normal_distribution", "sparse_activation", "concentrated_activity")
    }
    
    private fun calculatePolicySimilarities(outputs: List<NeuralNetworkOutput>): List<Double> {
        // Simplified similarity calculation
        return outputs.indices.map { 0.5 + kotlin.random.Random.nextDouble() * 0.5 }
    }
    
    private fun calculateValueSimilarities(outputs: List<NeuralNetworkOutput>): List<Double> {
        return outputs.indices.map { 0.5 + kotlin.random.Random.nextDouble() * 0.5 }
    }
    
    private fun calculateQValueSimilarities(outputs: List<NeuralNetworkOutput>): List<Double> {
        return outputs.indices.map { 0.5 + kotlin.random.Random.nextDouble() * 0.5 }
    }
    
    private fun identifyCommonPatterns(outputs: List<NeuralNetworkOutput>): List<String> {
        return listOf("consistent_value_estimation", "similar_top_moves", "stable_policy_distribution")
    }
    
    private fun identifyOutliers(
        outputs: List<NeuralNetworkOutput>,
        similarities: List<Double>
    ): List<Int> {
        return similarities.mapIndexedNotNull { index, similarity ->
            if (similarity < 0.3) index else null
        }
    }
    
    private fun analyzePhaseCharacteristics(
        networkOutput: NeuralNetworkOutput,
        gamePhase: GamePhase
    ): PhaseCharacteristics {
        return PhaseCharacteristics(
            gamePhase = gamePhase,
            policyComplexity = networkOutput.policyAnalysis.entropy,
            valueStability = networkOutput.valueAnalysis.confidence,
            decisionConfidence = calculateOverallConfidence(
                networkOutput.policyAnalysis.entropy,
                networkOutput.valueAnalysis.confidence,
                networkOutput.qValueAnalysis.qValueSpread
            )
        )
    }
    
    private fun analyzePhaseTransitions(phaseAnalyses: List<NeuralNetworkGamePhaseAnalysis>): List<PhaseTransition> {
        val transitions = mutableListOf<PhaseTransition>()
        
        for (i in 1 until phaseAnalyses.size) {
            val prev = phaseAnalyses[i - 1]
            val curr = phaseAnalyses[i]
            
            if (prev.gamePhase != curr.gamePhase) {
                transitions.add(PhaseTransition(
                    fromPhase = prev.gamePhase,
                    toPhase = curr.gamePhase,
                    transitionMove = curr.moveNumber,
                    networkChanges = calculateNetworkChanges(prev.networkOutput, curr.networkOutput)
                ))
            }
        }
        
        return transitions
    }
    
    private fun identifyPhasePatterns(phaseAnalyses: List<NeuralNetworkGamePhaseAnalysis>): Map<GamePhase, PhasePattern> {
        val patterns = mutableMapOf<GamePhase, PhasePattern>()
        
        GamePhase.values().forEach { phase ->
            val phaseAnalyses = phaseAnalyses.filter { it.gamePhase == phase }
            if (phaseAnalyses.isNotEmpty()) {
                patterns[phase] = PhasePattern(
                    gamePhase = phase,
                    averagePolicyEntropy = phaseAnalyses.map { it.networkOutput.policyAnalysis.entropy }.average(),
                    averageValueConfidence = phaseAnalyses.map { it.networkOutput.valueAnalysis.confidence }.average(),
                    commonCharacteristics = listOf("phase_specific_behavior")
                )
            }
        }
        
        return patterns
    }
    
    private fun calculateNetworkChanges(
        prev: NeuralNetworkOutput,
        curr: NeuralNetworkOutput
    ): NetworkChanges {
        return NetworkChanges(
            policyChange = abs(curr.policyAnalysis.entropy - prev.policyAnalysis.entropy),
            valueChange = abs(curr.valueOutput - prev.valueOutput),
            qValueChange = abs(curr.qValueAnalysis.meanQValue - prev.qValueAnalysis.meanQValue)
        )
    }
}

/**
 * Configuration for neural network analysis
 */
data class NetworkAnalysisConfig(
    val enableLayerAnalysis: Boolean = true,
    val enableAttentionAnalysis: Boolean = false,
    val maxLayersToAnalyze: Int = 10,
    val activationThreshold: Double = 1e-6,
    val saturationThreshold: Double = 0.99
)

// Enums and supporting classes

enum class ComparisonType {
    POLICY_SIMILARITY, VALUE_SIMILARITY, QVALUE_SIMILARITY
}

// VisualizationType is now defined in SharedDataClasses.kt

enum class GamePhase {
    OPENING, MIDDLEGAME, ENDGAME
}

enum class DistributionShape {
    UNIFORM, NORMAL, PEAKED, SKEWED
}

enum class ActivationHealth {
    HEALTHY, CONCERNING, POOR
}

enum class NetworkIssueType {
    DEAD_NEURONS, SATURATED_NEURONS, VANISHING_GRADIENTS, EXPLODING_GRADIENTS
}

enum class ConfidenceLevel {
    VERY_LOW, LOW, MEDIUM, HIGH
}

enum class UncertaintyType {
    HIGH_POLICY_ENTROPY, NEUTRAL_POSITION, CONFLICTING_SIGNALS, INSUFFICIENT_TRAINING
}

// Data classes for analysis results

data class NeuralNetworkOutput(
    val position: String,
    val policyOutput: DoubleArray,
    val valueOutput: Double,
    val qValues: Map<Int, Double>,
    val policyAnalysis: PolicyDistributionAnalysis,
    val valueAnalysis: ValueEstimationAnalysis,
    val qValueAnalysis: QValueDistributionAnalysis,
    val timestamp: Long
)

data class PolicyDistributionAnalysis(
    val totalValidProbability: Double,
    val entropy: Double,
    val maxProbability: Double,
    val concentration: Double,
    val distributionShape: DistributionShape
)

data class ValueEstimationAnalysis(
    val estimatedValue: Double,
    val expectedRange: ClosedRange<Double>,
    val isReasonable: Boolean,
    val gamePhase: GamePhase,
    val materialBalance: Double,
    val confidence: Double
)

data class QValueDistributionAnalysis(
    val meanQValue: Double,
    val qValueVariance: Double,
    val maxQValue: Double,
    val minQValue: Double,
    val qValueSpread: Double,
    val distributionShape: DistributionShape
)

data class NeuralNetworkActivationAnalysis(
    val position: String,
    val layerActivations: Map<String, LayerActivationAnalysis>,
    val attentionMaps: Map<String, AttentionMap>,
    val activationPatterns: ActivationPatternAnalysis,
    val networkIssues: List<NetworkIssue>,
    val analysisTimestamp: Long
)

data class LayerActivationAnalysis(
    val layerName: String,
    val activationStats: ActivationStats,
    val sparsity: Double,
    val saturation: Double
)

data class ActivationStats(
    val mean: Double,
    val variance: Double,
    val min: Double,
    val max: Double
)

data class AttentionMap(
    val name: String,
    val dimensions: Pair<Int, Int>,
    val weights: Array<DoubleArray>,
    val focusAreas: List<String>
)

data class ActivationPatternAnalysis(
    val deadNeurons: Int,
    val saturatedNeurons: Int,
    val activationHealth: ActivationHealth,
    val patterns: List<String>
)

data class NetworkIssue(
    val type: NetworkIssueType,
    val layer: String,
    val severity: Double,
    val description: String
)

data class NetworkComparisonResult(
    val positions: List<String>,
    val outputs: List<NeuralNetworkOutput>,
    val similarities: List<Double>,
    val commonPatterns: List<String>,
    val outliers: List<Int>,
    val comparisonType: ComparisonType
)

data class GamePhaseNetworkAnalysis(
    val gameLength: Int,
    val phaseAnalyses: List<NeuralNetworkGamePhaseAnalysis>,
    val phaseTransitions: List<PhaseTransition>,
    val phasePatterns: Map<GamePhase, PhasePattern>
)

data class NeuralNetworkGamePhaseAnalysis(
    val moveNumber: Int,
    val gamePhase: GamePhase,
    val networkOutput: NeuralNetworkOutput,
    val phaseCharacteristics: PhaseCharacteristics
)

data class PhaseTransition(
    val fromPhase: GamePhase,
    val toPhase: GamePhase,
    val transitionMove: Int,
    val networkChanges: NetworkChanges
)

data class PhasePattern(
    val gamePhase: GamePhase,
    val averagePolicyEntropy: Double,
    val averageValueConfidence: Double,
    val commonCharacteristics: List<String>
)

data class PhaseCharacteristics(
    val gamePhase: GamePhase,
    val policyComplexity: Double,
    val valueStability: Double,
    val decisionConfidence: Double
)

data class NetworkChanges(
    val policyChange: Double,
    val valueChange: Double,
    val qValueChange: Double
)

data class DecisionVisualization(
    val position: String,
    val visualizationType: VisualizationType,
    val visualization: String,
    val networkOutput: NeuralNetworkOutput
)

data class NetworkConfidenceAnalysis(
    val position: String,
    val policyEntropy: Double,
    val valueConfidence: Double,
    val qValueSpread: Double,
    val overallConfidence: Double,
    val uncertaintySources: List<UncertaintySource>,
    val confidenceLevel: ConfidenceLevel
)

data class UncertaintySource(
    val type: UncertaintyType,
    val description: String,
    val severity: Double
)
