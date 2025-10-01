package com.chessrl.teacher

import com.chessrl.chess.*
import com.chessrl.nn.*
import kotlin.random.Random
import java.io.File

data class ImitationConfig(
    val dataPath: String = "data/teacher.ndjson",
    val epochs: Int = 2,
    val batchSize: Int = 16,
    val learningRate: Double = 1e-3,
    val seed: Long = 42L,
    val modelOut: String = "data/imitation_qnet.json",
    val smoothingEpsilon: Double = 0.05,
    val valSplit: Double = 0.1
)

private data class Sample(
    val fen: String,
    val input: DoubleArray,
    val teacherPolicy: Map<Int, Double>,
    val validActions: IntArray
)

fun runImitationTraining(cfg: ImitationConfig) {
    val samples = loadDataset(cfg.dataPath)
    println("Loaded ${samples.size} samples from ${cfg.dataPath}")

    val inputSize = FeatureEncoding.FEATURE_SIZE
    val outputSize = 64 * 64

    val net = FeedforwardNetwork(
        _layers = listOf(
            DenseLayer(inputSize, 512, ReLUActivation(), Random(cfg.seed)),
            DenseLayer(512, 256, ReLUActivation(), Random(cfg.seed + 1)),
            DenseLayer(256, 128, ReLUActivation(), Random(cfg.seed + 2)),
            DenseLayer(128, outputSize, LinearActivation(), Random(cfg.seed + 3))
        ),
        lossFunction = SoftmaxCrossEntropyLoss(),
        optimizer = AdamOptimizer(cfg.learningRate)
    )

    val rng = Random(cfg.seed)
    // Build train/val split by FEN buckets to reduce leakage
    val fenToIdx = samples.indices.groupBy { samples[it].fen }
    val trainIdx = mutableListOf<Int>()
    val valIdx = mutableListOf<Int>()
    for ((_, idxs) in fenToIdx) {
        if (rng.nextDouble() < cfg.valSplit) valIdx += idxs else trainIdx += idxs
    }
    println("Split: train=${trainIdx.size}, val=${valIdx.size}")
    repeat(cfg.epochs) { epoch ->
        trainIdx.shuffle(rng)
        var totalLoss = 0.0
        var count = 0
        for (start in trainIdx.indices step cfg.batchSize) {
            val end = minOf(start + cfg.batchSize, trainIdx.size)
            val batchIdx = trainIdx.subList(start, end)
            val bsz = batchIdx.size
            val inputs = Array(bsz) { DoubleArray(inputSize) }
            val targets = Array(bsz) { DoubleArray(outputSize) }

            // Build masked targets (probability distribution over valid actions)
            for ((bi, si) in batchIdx.withIndex()) {
                val s = samples[si]
                inputs[bi] = s.input
                val t = DoubleArray(outputSize) { 0.0 }
                // Collect teacher scores over valid actions
                var sum = 0.0
                for (a in s.validActions) {
                    val v = s.teacherPolicy[a] ?: 0.0
                    if (v > 0.0) sum += v
                }
                if (sum <= 0.0) {
                    // Fallback: uniform over valid actions
                    val inv = if (s.validActions.isNotEmpty()) 1.0 / s.validActions.size else 0.0
                    for (a in s.validActions) t[a] = inv
                } else {
                    // Normalize to probabilities
                    val invK = if (s.validActions.isNotEmpty()) 1.0 / s.validActions.size else 0.0
                    val eps = cfg.smoothingEpsilon.coerceIn(0.0, 0.2)
                    for (a in s.validActions) {
                        val v = (s.teacherPolicy[a] ?: 0.0) / sum
                        // Label smoothing within valid actions
                        t[a] = (1.0 - eps) * v + eps * invK
                    }
                }
                targets[bi] = t
            }

            val metrics = net.trainBatch(TrainingBatch(inputs, targets), epoch)
            totalLoss += metrics.averageLoss
            count++
            if (count % 50 == 0) {
                println("epoch=${epoch+1} step=$count loss=${"%.5f".format(totalLoss / count)}")
            }
        }
        println("Epoch ${epoch+1}/${cfg.epochs} avg_loss=${"%.5f".format(totalLoss / maxOf(1, count))}")

        // Validation metrics: Match@1 and KL over valid actions
        if (valIdx.isNotEmpty()) {
            var match1 = 0
            var klSum = 0.0
            for (si in valIdx) {
                val s = samples[si]
                val q = net.predict(s.input)
                // Argmax over valid actions
                var bestA = -1
                var bestV = Double.NEGATIVE_INFINITY
                for (a in s.validActions) {
                    if (q[a] > bestV) { bestV = q[a]; bestA = a }
                }
                val teacherBest = s.teacherPolicy.maxByOrNull { it.value }?.key ?: -1
                if (bestA == teacherBest) match1++
                // KL(pT || pHat) over valid actions
                // Build pHat softmax over valid actions
                if (s.validActions.isNotEmpty()) {
                    var maxV = Double.NEGATIVE_INFINITY
                    for (a in s.validActions) if (q[a] > maxV) maxV = q[a]
                    var denom = 0.0
                    val exps = DoubleArray(s.validActions.size) { i ->
                        val ev = kotlin.math.exp(q[s.validActions[i]] - maxV)
                        denom += ev
                        ev
                    }
                    // Normalize teacher over valid actions
                    var sumT = 0.0
                    for (a in s.validActions) sumT += (s.teacherPolicy[a] ?: 0.0)
                    val kl = if (denom == 0.0) 0.0 else run {
                        var acc = 0.0
                        for ((i, a) in s.validActions.withIndex()) {
                            val pT = if (sumT > 0.0) (s.teacherPolicy[a] ?: 0.0) / sumT else 0.0
                            if (pT > 0.0) {
                                val pHat = (exps[i] / denom).coerceIn(1e-12, 1.0)
                                acc += pT * kotlin.math.ln(pT / pHat)
                            }
                        }
                        acc
                    }
                    klSum += kl
                }
            }
            val matchAt1 = match1.toDouble() / valIdx.size
            val klAvg = klSum / valIdx.size
            println("Validation: match@1=${"%.3f".format(matchAt1)} KL=${"%.4f".format(klAvg)}")
        }
    }

    // Save model
    val out = File(cfg.modelOut)
    out.parentFile?.mkdirs()
    net.save(out.absolutePath)
    println("Saved imitation model to ${out.absolutePath}")

    // Quick eval: Match@1 and KL over a subset
    val evalCount = minOf(1000, samples.size)
    val evalIdx = (0 until samples.size).shuffled(Random(cfg.seed)).take(evalCount)
    var match1 = 0
    var klSum = 0.0
    for (si in evalIdx) {
        val s = samples[si]
        val q = net.predict(s.input)
        var bestA = -1
        var bestV = Double.NEGATIVE_INFINITY
        var denom = 0.0
        // Softmax over valid actions
        val vals = DoubleArray(s.validActions.size)
        for ((i, a) in s.validActions.withIndex()) {
            vals[i] = q[a]
            if (q[a] > bestV) { bestV = q[a]; bestA = a }
        }
        val maxV = vals.maxOrNull() ?: 0.0
        val exps = DoubleArray(vals.size) { i -> kotlin.math.exp(vals[i] - maxV) }
        for (v in exps) denom += v
        val pHat = DoubleArray(vals.size) { i -> exps[i] / denom }
        // Teacher best
        val teacherBest = s.teacherPolicy.maxByOrNull { it.value }?.key ?: -1
        if (bestA == teacherBest) match1++
        // KL(pT || pHat) over valid actions (fill 0 if teacher has no prob)
        var kl = 0.0
        for ((i, a) in s.validActions.withIndex()) {
            val pT = s.teacherPolicy[a] ?: 0.0
            if (pT > 0) {
                val ph = pHat[i].coerceIn(1e-12, 1.0)
                kl += pT * kotlin.math.ln(pT / ph)
            }
        }
        klSum += kl
    }
    val matchAt1 = match1.toDouble() / evalCount
    val klAvg = klSum / evalCount
    println("Eval subset=${evalCount} match@1=${"%.3f".format(matchAt1)} KL=${"%.4f".format(klAvg)}")
}

private fun loadDataset(path: String): List<Sample> {
    val f = File(path)
    require(f.exists()) { "Dataset not found: $path" }
    val result = ArrayList<Sample>(1024)
    f.forEachLine { line ->
        if (line.isBlank()) return@forEachLine
        val obj = parseNdjson(line)
        val fen = obj["fen"] as String
        val pol = (obj["teacher_policy"] as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = k as? Int
                val value = v as? Double
                if (key != null && value != null) key to value else null
            }
            ?.toMap() ?: emptyMap()
        val valid = (obj["valid_actions"] as? IntArray) ?: IntArray(0)
        val board = ChessBoard()
        if (!board.fromFEN(fen)) return@forEachLine
        val input = FeatureEncoding.boardToFeatures(board)
        // Sample expects (fen, input, teacherPolicy, validActions)
        result.add(Sample(fen, input, pol, valid))
    }
    return result
}

// Very small JSON parser tailored to NDJSON we emit
private fun parseNdjson(line: String): Map<String, Any> {
    // We only need three fields robustly: fen (string), teacher_policy (object), valid_actions (array)
    fun unquote(s: String) = s.trim().removePrefix("\"").removeSuffix("\"")

    val map = mutableMapOf<String, Any>()
    // Extract fen
    val fenKey = "\"fen\":\""
    val fenStart = line.indexOf(fenKey)
    if (fenStart >= 0) {
        val s = fenStart + fenKey.length
        val e = line.indexOf("\"", s)
        if (e > s) map["fen"] = line.substring(s, e)
    }
    // Extract teacher_policy
    val tpKey = "\"teacher_policy\":"
    val tpStart = line.indexOf(tpKey)
    if (tpStart >= 0) {
        val s = tpStart + tpKey.length
        val end = line.indexOf('}', s)
        if (end > s) {
            val body = line.substring(s + 1, end) // inside {...}
            val entries = body.split(',').filter { it.isNotBlank() }
            val mapPol = mutableMapOf<Int, Double>()
            for (e in entries) {
                val parts = e.split(':')
                if (parts.size == 2) {
                    val k = unquote(parts[0]).toIntOrNull()
                    val v = parts[1].trim().replace(',', '.').toDoubleOrNull()
                    if (k != null && v != null) mapPol[k] = v
                }
            }
            map["teacher_policy"] = mapPol
        } else map["teacher_policy"] = emptyMap<Int, Double>()
    }
    // Extract valid_actions
    val vaKey = "\"valid_actions\":"
    val vaStart = line.indexOf(vaKey)
    if (vaStart >= 0) {
        val s = vaStart + vaKey.length
        val end = line.indexOf(']', s)
        if (end > s) {
            val body = line.substring(s + 1, end) // inside [...]
            val arr = body.split(',').filter { it.isNotBlank() }.mapNotNull { it.trim().toIntOrNull() }.toIntArray()
            map["valid_actions"] = arr
        } else map["valid_actions"] = IntArray(0)
    }
    return map
}

/**
 * CLI: run imitation training on a NDJSON teacher dataset
 */
fun main(args: Array<String>) {
    var cfg = ImitationConfig()
    var train = false
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--train-imitation" -> { train = true; i++ }
            "--data" -> { cfg = cfg.copy(dataPath = args.getOrNull(++i) ?: cfg.dataPath); i++ }
            "--epochs" -> { cfg = cfg.copy(epochs = args.getOrNull(++i)?.toInt() ?: cfg.epochs); i++ }
            "--batch" -> { cfg = cfg.copy(batchSize = args.getOrNull(++i)?.toInt() ?: cfg.batchSize); i++ }
            "--lr" -> { cfg = cfg.copy(learningRate = args.getOrNull(++i)?.toDouble() ?: cfg.learningRate); i++ }
            "--smooth" -> { cfg = cfg.copy(smoothingEpsilon = args.getOrNull(++i)?.toDouble() ?: cfg.smoothingEpsilon); i++ }
            "--val-split" -> { cfg = cfg.copy(valSplit = args.getOrNull(++i)?.toDouble() ?: cfg.valSplit); i++ }
            "--seed" -> { cfg = cfg.copy(seed = args.getOrNull(++i)?.toLong() ?: cfg.seed); i++ }
            "--out" -> { cfg = cfg.copy(modelOut = args.getOrNull(++i) ?: cfg.modelOut); i++ }
            else -> { println("Unknown arg: ${args[i]}"); i++ }
        }
    }
    if (!train) {
        println("No action. Use --train-imitation --data <path> to train.")
        return
    }
    println("Imitation training starting: $cfg")
    runImitationTraining(cfg)
}
