package com.chessrl.integration

import com.chessrl.rl.Experience
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class CheckpointResumeJvmTest {

    @Test
    fun `checkpoint create-load restores agent predictions`() {
        // Deterministic seeds for reproducibility
        SeedManager.initializeWithSeed(12345L)

        // Create a seeded DQN agent
        val agent = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = listOf(64, 32),
            learningRate = 0.001,
            explorationRate = 0.05,
            config = ChessAgentConfig(batchSize = 8, maxBufferSize = 512)
        )

        // Prepare a dummy chess-like state and a small action subset
        val stateSize = 776
        val state = DoubleArray(stateSize) { i -> if (i % 7 == 0) 1.0 else 0.0 }
        val actions = (0 until 16).toList()

        // Baseline Q-values snapshot
        val qBefore = agent.getQValues(state, actions)

        // Checkpoint manager writing into a temp directory
        val tmpDir: Path = Files.createTempDirectory("advanced_cp_jvm_")
        val manager = CheckpointManager(
            config = CheckpointConfig(
                baseDirectory = tmpDir.toString(),
                maxVersions = 5,
                compressionEnabled = false,
                validationEnabled = false
            )
        )

        // Create checkpoint version 1
        val cp = manager.createCheckpoint(
            agent = agent,
            version = 1,
            metadata = CheckpointMetadata(
                cycle = 0,
                performance = 0.0,
                description = "roundtrip"
            )
        )

        // Train/perturb the agent so predictions change
        val experiences = buildList {
            repeat(32) {
                val action = actions[it % actions.size]
                add(
                    Experience(
                        state = state.copyOf(),
                        action = action,
                        reward = if (it % 3 == 0) 1.0 else -0.5,
                        nextState = state.copyOf(),
                        done = (it % 8 == 0)
                    )
                )
            }
        }
        // Use batch path to ensure weight updates
        agent.trainBatch(experiences)

        val qAfter = agent.getQValues(state, actions)
        var changed = false
        for (a in actions) {
            if (kotlin.math.abs((qBefore[a] ?: 0.0) - (qAfter[a] ?: 0.0)) > 1e-9) {
                changed = true
                break
            }
        }
        assertTrue(changed, "Predictions should change after training before reload")

        // Load the checkpoint and ensure predictions match original snapshot
        val result = manager.loadCheckpoint(cp, agent)
        assertTrue(result.success, "Checkpoint load should succeed on JVM")

        val qRestored = agent.getQValues(state, actions)
        var restoredMatches = true
        for (a in actions) {
            val before = qBefore[a] ?: 0.0
            val after = qRestored[a] ?: 0.0
            if (kotlin.math.abs(before - after) > 1e-9) {
                restoredMatches = false
                break
            }
        }
        assertTrue(restoredMatches, "Predictions after load should match pre-checkpoint values")
    }
}
