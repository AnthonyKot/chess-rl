package com.chessrl.integration.backend

import com.chessrl.integration.AlphaZeroSample
import com.chessrl.nn.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random

/**
 * Dual-head neural network for AlphaZero-style training.
 *
 * Architecture:
 *   Input (839) → Shared trunk [ReLU layers] → split:
 *     Policy head → [policySize] logits  (softmax applied externally)
 *     Value head  → [1] scalar ∈ [-1, 1] (tanh activation)
 *
 * Gradient flow per training step:
 *   1. Forward through trunk → trunkOutput
 *   2. Forward through each head independently
 *   3. Compute policy cross-entropy gradient and value MSE gradient
 *   4. Backprop through each head → gradients at trunk output
 *   5. Sum trunk gradients, backprop through trunk
 *   6. Adam update on all layers
 */
class DualHeadNetwork(
    val inputSize: Int = 839,
    val trunkSizes: List<Int> = listOf(512, 256),
    val policySize: Int = 4096,
    val learningRate: Double = 0.001,
    private val random: Random = Random.Default
) {
    private val trunkLayers: List<DenseLayer>
    private val policyLayer: DenseLayer
    private val valueLayer: DenseLayer
    private val optimizer: AdamOptimizer
    private val allLayers: List<DenseLayer>

    init {
        val sizes = listOf(inputSize) + trunkSizes
        trunkLayers = (0 until trunkSizes.size).map { i ->
            DenseLayer(sizes[i], sizes[i + 1], ReLUActivation(), random)
        }
        val trunkOut = trunkSizes.last()
        policyLayer = DenseLayer(trunkOut, policySize, LinearActivation(), random)
        valueLayer = DenseLayer(trunkOut, 1, TanhActivation(), random)
        allLayers = trunkLayers + listOf(policyLayer, valueLayer)
        optimizer = AdamOptimizer(learningRate)
    }

    /**
     * Forward pass returning (policy_logits, value_scalar).
     * Policy logits are raw (pre-softmax). Caller applies softmax + legal-move masking.
     */
    fun forwardDual(input: DoubleArray): Pair<DoubleArray, Double> {
        var x = input
        for (layer in trunkLayers) x = layer.forward(x)
        val policyLogits = policyLayer.forward(x)
        val valueOut = valueLayer.forward(x)
        return Pair(policyLogits, valueOut[0])
    }

    /**
     * Train on a batch of AlphaZero samples.
     * Returns (avgPolicyLoss, avgValueLoss).
     */
    fun trainBatch(samples: List<AlphaZeroSample>): Pair<Double, Double> {
        if (samples.isEmpty()) return Pair(0.0, 0.0)

        var totalPolicyLoss = 0.0
        var totalValueLoss = 0.0

        for (sample in samples) {
            // --- Forward ---
            var trunkOut = sample.state
            for (layer in trunkLayers) trunkOut = layer.forward(trunkOut)

            val policyLogits = policyLayer.forward(trunkOut)
            val valueOut = valueLayer.forward(trunkOut)[0]

            // --- Policy loss: cross-entropy(softmax(logits), π) ---
            val probs = softmax(policyLogits)
            val policyLoss = crossEntropy(probs, sample.policyTarget)
            totalPolicyLoss += policyLoss
            // Combined softmax + cross-entropy gradient: dL/d(logit[i]) = prob[i] - target[i]
            val policyGrad = DoubleArray(policySize) { i -> probs[i] - sample.policyTarget[i] }

            // --- Value loss: MSE(tanh_output, z) ---
            val diff = valueOut - sample.valueTarget
            totalValueLoss += diff * diff
            // dL/d(value_output) = 2 * (pred - z)
            val valueGrad = doubleArrayOf(2.0 * diff)

            // --- Backward through heads → get gradients at trunk output ---
            val trunkGradFromPolicy = policyLayer.backward(policyGrad)
            val trunkGradFromValue  = valueLayer.backward(valueGrad)

            // Sum contributions from both heads
            val combinedTrunkGrad = DoubleArray(trunkOut.size) { j ->
                trunkGradFromPolicy[j] + trunkGradFromValue[j]
            }

            // --- Backprop through trunk ---
            var grad = combinedTrunkGrad
            for (layer in trunkLayers.reversed()) grad = layer.backward(grad)
        }

        // --- Weight update (Adam averages by gradientCount internally) ---
        optimizer.updateWeights(allLayers)

        val n = samples.size.toDouble()
        return Pair(totalPolicyLoss / n, totalValueLoss / n)
    }

    /**
     * Copy weights from this network into target. Used to update the opponent network.
     */
    fun copyWeightsTo(target: DualHeadNetwork) {
        require(target.trunkSizes == trunkSizes && target.policySize == policySize) {
            "Cannot copy weights between networks with different architectures"
        }
        fun copyLayer(src: DenseLayer, dst: DenseLayer) {
            dst.setWeights(src.getWeights())
            dst.setBiases(src.getBiases())
        }

        trunkLayers.zip(target.trunkLayers).forEach { (src, dst) -> copyLayer(src, dst) }
        copyLayer(policyLayer, target.policyLayer)
        copyLayer(valueLayer, target.valueLayer)
    }

    fun save(path: String) {
        val sb = StringBuilder()
        sb.appendLine("DUAL_HEAD_V1")
        sb.appendLine("inputSize=$inputSize")
        sb.appendLine("trunkSizes=${trunkSizes.joinToString(",")}")
        sb.appendLine("policySize=$policySize")
        sb.appendLine("learningRate=$learningRate")

        fun serializeLayer(name: String, layer: DenseLayer) {
            sb.appendLine("LAYER $name")
            val w = layer.getWeights()
            val b = layer.getBiases()
            sb.appendLine("rows=${w.size} cols=${if (w.isNotEmpty()) w[0].size else 0}")
            for (row in w) sb.appendLine(row.joinToString(" "))
            sb.appendLine(b.joinToString(" "))
        }

        trunkLayers.forEachIndexed { i, layer -> serializeLayer("trunk_$i", layer) }
        serializeLayer("policy", policyLayer)
        serializeLayer("value", valueLayer)

        File(path).writeText(sb.toString())
    }

    fun load(path: String) {
        val lines = File(path).readLines().iterator()
        check(lines.next() == "DUAL_HEAD_V1") { "Unknown format" }
        // Skip header fields — architecture must match constructor args
        repeat(4) { lines.next() }

        fun deserializeLayer(layer: DenseLayer) {
            val header = lines.next() // "LAYER name"
            check(header.startsWith("LAYER")) { "Expected LAYER header, got: $header" }
            val dim = lines.next() // "rows=R cols=C"
            val rows = dim.substringAfter("rows=").substringBefore(" ").toInt()
            val weights = Array(rows) { lines.next().split(" ").map { it.toDouble() }.toDoubleArray() }
            val biases = lines.next().split(" ").map { it.toDouble() }.toDoubleArray()
            layer.setWeights(weights)
            layer.setBiases(biases)
        }

        trunkLayers.forEach { deserializeLayer(it) }
        deserializeLayer(policyLayer)
        deserializeLayer(valueLayer)
    }

    private fun softmax(logits: DoubleArray): DoubleArray {
        val max = logits.maxOrNull() ?: 0.0
        val exp = DoubleArray(logits.size) { exp(logits[it] - max) }
        val sum = exp.sum().coerceAtLeast(1e-10)
        return DoubleArray(logits.size) { exp[it] / sum }
    }

    private fun crossEntropy(probs: DoubleArray, targets: DoubleArray): Double {
        var loss = 0.0
        for (i in probs.indices) {
            if (targets[i] > 0.0) loss -= targets[i] * ln(probs[i].coerceAtLeast(1e-10))
        }
        return loss
    }
}
