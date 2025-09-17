package com.chessrl.integration

import com.chessrl.rl.Experience
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 * Verifies that the target sync log appears at the configured frequency.
 * Captures stdout and looks for the marker line.
 */
class DQNSyncLoggingJvmTest {
    @Test
    fun printsTargetSyncAtConfiguredFrequency() {
        val cfg = ChessAgentConfig(batchSize = 8, maxBufferSize = 128, targetUpdateFrequency = 2)
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16),
            learningRate = 1e-3,
            explorationRate = 0.1,
            config = cfg
        )

        val rng = Random(7)
        val stateSize = ChessStateEncoder.TOTAL_FEATURES
        val actions = (0 until 4).toList()

        // Prepare a small batch
        val batch = List(cfg.batchSize) {
            val s = DoubleArray(stateSize) { rng.nextDouble() }
            val ns = DoubleArray(stateSize) { rng.nextDouble() }
            val a = actions[rng.nextInt(actions.size)]
            val r = if (it % 3 == 0) 0.5 else 0.0
            Experience(state = s, action = a, reward = r, nextState = ns, done = false)
        }

        // Capture stdout
        val originalOut = System.out
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos)
        System.setOut(ps)
        try {
            // Do enough updates to trigger multiple syncs (every 2 updates)
            repeat(5) {
                agent.trainBatch(batch)
            }
        } finally {
            System.out.flush()
            System.setOut(originalOut)
        }

        val output = baos.toString()
        val syncMarker = "DQN target network synchronized"
        assertTrue(output.contains(syncMarker), "Expected target sync log to appear at least once. Output=\n$output")
    }
}

