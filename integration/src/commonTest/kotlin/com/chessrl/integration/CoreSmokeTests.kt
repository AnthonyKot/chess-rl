package com.chessrl.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoreSmokeTests {
    @Test
    fun trainingPipelineRuns() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2,
            config = ChessAgentConfig(batchSize = 8, maxBufferSize = 100)
        )
        val env = ChessEnvironment()
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = env,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 10,
                batchSize = 8,
                batchTrainingFrequency = 1,
                maxBufferSize = 100,
                progressReportInterval = 2
            )
        )
        val results = pipeline.runTraining(numEpisodes = 2)
        assertEquals(2, results.totalEpisodes)
        assertEquals(2, results.episodeResults.size)
        assertNotNull(results.finalMetrics)
    }
}

