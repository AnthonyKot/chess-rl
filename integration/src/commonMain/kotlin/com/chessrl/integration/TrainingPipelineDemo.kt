package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*

/**
 * Comprehensive demonstration of the chess RL training pipeline
 * Shows end-to-end training with efficient batching, monitoring, and analysis
 */
object TrainingPipelineDemo {
    
    /**
     * Run complete training pipeline demonstration
     */
    fun runCompleteDemo() {
        println("🚀 Chess RL Training Pipeline Demo")
        println("=" * 60)
        
        try {
            // 1. Basic pipeline demonstration
            demonstrateBasicPipeline()
            
            // 2. Advanced pipeline features
            demonstrateAdvancedFeatures()
            
            // 3. Training controller demonstration
            demonstrateTrainingController()
            
            // 4. Performance analysis
            demonstratePerformanceAnalysis()
            
            // 5. Batch training optimization
            demonstrateBatchOptimization()
            
            println("\n🎉 Complete training pipeline demonstration finished!")
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Demonstrate basic training pipeline functionality
     */
    private fun demonstrateBasicPipeline() {
        println("\n📚 1. Basic Training Pipeline Demonstration")
        println("-" * 50)
        
        // Create basic components
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(64, 32),
            learningRate = 0.001,
            explorationRate = 0.15
        )
        
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0
            )
        )
        
        // Create pipeline with basic configuration
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 30,
                batchSize = 16,
                batchTrainingFrequency = 2,
                maxBufferSize = 200,
                progressReportInterval = 5
            )
        )
        
        println("🏋️ Running basic training (10 episodes)...")
        val results = pipeline.train(totalEpisodes = 10)
        
        // Display results
        displayTrainingResults(results, "Basic Pipeline")
    }
    
    /**
     * Demonstrate advanced pipeline features
     */
    private fun demonstrateAdvancedFeatures() {
        println("\n🔬 2. Advanced Pipeline Features Demonstration")
        println("-" * 50)
        
        // Create agent with larger network
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(128, 64, 32),
            learningRate = 0.0005,
            explorationRate = 0.2
        )
        
        // Environment with position rewards
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 2.0,
                lossReward = -2.0,
                drawReward = 0.1,
                enablePositionRewards = true,
                materialWeight = 0.01,
                pieceActivityWeight = 0.005
            )
        )
        
        // Advanced pipeline configuration
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 50,
                batchSize = 32,
                batchTrainingFrequency = 1,
                updatesPerBatch = 2,
                maxBufferSize = 1000,
                samplingStrategy = SamplingStrategy.MIXED,
                progressReportInterval = 3,
                gradientClipThreshold = 5.0,
                minPolicyEntropy = 0.2
            )
        )
        
        println("🧠 Running advanced training (15 episodes)...")
        val results = pipeline.train(totalEpisodes = 15)
        
        // Display detailed results
        displayAdvancedResults(results)
    }
    
    /**
     * Demonstrate training controller
     */
    private fun demonstrateTrainingController() {
        println("\n🎮 3. Training Controller Demonstration")
        println("-" * 50)
        
        // Create controller with comprehensive configuration
        val controller = TrainingController(
            config = TrainingControllerConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(96, 48, 24),
                learningRate = 0.001,
                explorationRate = 0.1,
                maxStepsPerEpisode = 40,
                batchSize = 24,
                batchTrainingFrequency = 1,
                maxBufferSize = 500,
                enablePositionRewards = true,
                progressReportInterval = 4
            )
        )
        
        // Initialize controller
        println("🔧 Initializing training controller...")
        if (!controller.initialize()) {
            println("❌ Failed to initialize controller")
            return
        }
        
        // Run game demonstration
        println("🎮 Running game demonstration...")
        val demonstration = controller.demonstrateGame()
        if (demonstration != null) {
            displayGameDemonstration(demonstration)
        }
        
        // Run performance analysis
        println("📊 Running performance analysis...")
        val analysis = controller.analyzePerformance()
        if (analysis != null) {
            displayPerformanceAnalysis(analysis)
        }
        
        // Run training
        println("🏋️ Running controller training (12 episodes)...")
        val results = controller.startTraining(episodes = 12)
        if (results != null) {
            displayTrainingResults(results, "Controller Training")
        }
    }
    
    /**
     * Demonstrate performance analysis features
     */
    private fun demonstratePerformanceAnalysis() {
        println("\n📈 4. Performance Analysis Demonstration")
        println("-" * 50)
        
        // Compare different agent configurations
        val configurations = listOf(
            Pair("Small Network", listOf(32, 16)),
            Pair("Medium Network", listOf(64, 32, 16)),
            Pair("Large Network", listOf(128, 64, 32))
        )
        
        val results = mutableListOf<Pair<String, TrainingResults>>()
        
        for ((name, hiddenLayers) in configurations) {
            println("🧪 Testing $name configuration...")
            
            val agent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = hiddenLayers,
                learningRate = 0.001,
                explorationRate = 0.15
            )
            
            val environment = ChessEnvironment()
            
            val pipeline = ChessTrainingPipeline(
                agent = agent,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = 25,
                    batchSize = 16,
                    maxBufferSize = 200,
                    progressReportInterval = 10
                )
            )
            
            val result = pipeline.train(totalEpisodes = 8)
            results.add(Pair(name, result))
        }
        
        // Compare results
        compareConfigurations(results)
    }
    
    /**
     * Demonstrate batch training optimization
     */
    private fun demonstrateBatchOptimization() {
        println("\n⚡ 5. Batch Training Optimization Demonstration")
        println("-" * 50)
        
        // Test different batch sizes
        val batchSizes = listOf(8, 16, 32, 64)
        val batchResults = mutableListOf<Pair<Int, TrainingResults>>()
        
        for (batchSize in batchSizes) {
            println("🔄 Testing batch size: $batchSize")
            
            val agent = ChessAgentFactory.createDQNAgent(
                hiddenLayers = listOf(64, 32),
                learningRate = 0.001,
                explorationRate = 0.15
            )
            
            val environment = ChessEnvironment()
            
            val pipeline = ChessTrainingPipeline(
                agent = agent,
                environment = environment,
                config = TrainingPipelineConfig(
                    maxStepsPerEpisode = 30,
                    batchSize = batchSize,
                    batchTrainingFrequency = 1,
                    updatesPerBatch = 1,
                    maxBufferSize = 300,
                    progressReportInterval = 20
                )
            )
            
            val result = pipeline.train(totalEpisodes = 10)
            batchResults.add(Pair(batchSize, result))
        }
        
        // Analyze batch optimization
        analyzeBatchOptimization(batchResults)
    }
    
    /**
     * Display training results
     */
    private fun displayTrainingResults(results: TrainingResults, title: String) {
        println("\n📊 $title Results:")
        println("   Episodes: ${results.totalEpisodes}")
        println("   Total Steps: ${results.totalSteps}")
        println("   Training Duration: ${results.trainingDuration}ms")
        println("   Best Performance: ${results.bestPerformance}")
        
        val finalMetrics = results.finalMetrics
        println("   Average Reward: ${finalMetrics.averageReward}")
        println("   Average Game Length: ${finalMetrics.averageGameLength} steps")
        println("   Win Rate: ${(finalMetrics.winRate * 100)}%")
        println("   Draw Rate: ${(finalMetrics.drawRate * 100)}%")
        println("   Total Batch Updates: ${finalMetrics.totalBatchUpdates}")
        println("   Final Buffer Size: ${finalMetrics.finalBufferSize}")
    }
    
    /**
     * Display advanced training results
     */
    private fun displayAdvancedResults(results: TrainingResults) {
        displayTrainingResults(results, "Advanced Pipeline")
        
        println("\n🔍 Advanced Metrics:")
        if (results.batchHistory.isNotEmpty()) {
            val avgBatchLoss = results.batchHistory.map { it.averageLoss }.average()
            val avgPolicyEntropy = results.batchHistory.map { it.averagePolicyEntropy }.average()
            val avgGradientNorm = results.batchHistory.map { it.averageGradientNorm }.average()
            
            println("   Average Batch Loss: ${avgBatchLoss}")
            println("   Average Policy Entropy: ${avgPolicyEntropy}")
            println("   Average Gradient Norm: ${avgGradientNorm}")
            println("   Batch Updates: ${results.batchHistory.size}")
        }
        
        // Show episode progression
        println("\n📈 Episode Progression (last 5 episodes):")
        val lastEpisodes = results.episodeHistory.takeLast(5)
        for (episode in lastEpisodes) {
            println("   Episode ${episode.episode}: " +
                   "Reward=${episode.reward}, " +
                   "Steps=${episode.steps}, " +
                   "Result=${episode.gameResult}")
        }
    }
    
    /**
     * Display game demonstration
     */
    private fun displayGameDemonstration(demonstration: GameDemonstration) {
        println("\n🎮 Game Demonstration Results:")
        println("   Total Moves: ${demonstration.totalMoves}")
        println("   Final Status: ${demonstration.finalStatus}")
        
        val chessMetrics = demonstration.chessMetrics
        println("   Game Length: ${chessMetrics.gameLength}")
        println("   Captures: ${chessMetrics.captureCount}")
        println("   Checks: ${chessMetrics.checkCount}")
        
        if (demonstration.moves.isNotEmpty()) {
            println("   Sample Moves:")
            val sampleMoves = demonstration.moves.take(5)
            for (move in sampleMoves) {
                println("     Step ${move.step}: ${move.move} (reward: ${move.reward})")
            }
        }
    }
    
    /**
     * Display performance analysis
     */
    private fun displayPerformanceAnalysis(analysis: PerformanceAnalysis) {
        println("\n📊 Performance Analysis Results:")
        println("   Tests Performed: ${analysis.testsPerformed}")
        println("   Average Max Q-Value: ${analysis.averageMaxQValue}")
        println("   Average Min Q-Value: ${analysis.averageMinQValue}")
        println("   Average Entropy: ${analysis.averageEntropy}")
        println("   Average Valid Actions: ${analysis.averageValidActions}")
        
        if (analysis.testResults.isNotEmpty()) {
            println("   Sample Test Results:")
            val sampleTests = analysis.testResults.take(3)
            for (test in sampleTests) {
                println("     Test ${test.testIndex}: " +
                       "Actions=${test.validActions}, " +
                       "MaxQ=${test.maxQValue}, " +
                       "Entropy=${test.entropyScore}")
            }
        }
    }
    
    /**
     * Compare different configurations
     */
    private fun compareConfigurations(results: List<Pair<String, TrainingResults>>) {
        println("\n🔬 Configuration Comparison:")
        println("   Configuration        | Avg Reward | Best Reward | Avg Length | Win Rate")
        println("   " + "-" * 70)
        
        for ((name, result) in results) {
            val finalMetrics = result.finalMetrics
            println("   $name | ${finalMetrics.averageReward} | ${result.bestPerformance} | ${finalMetrics.averageGameLength} | ${finalMetrics.winRate * 100}%")
        }
        
        // Find best configuration
        val bestConfig = results.maxByOrNull { it.second.bestPerformance }
        if (bestConfig != null) {
            println("\n🏆 Best Configuration: ${bestConfig.first}")
            println("   Best Performance: ${bestConfig.second.bestPerformance}")
        }
    }
    
    /**
     * Analyze batch optimization results
     */
    private fun analyzeBatchOptimization(results: List<Pair<Int, TrainingResults>>) {
        println("\n⚡ Batch Size Optimization Analysis:")
        println("   Batch Size | Avg Reward | Total Steps | Batch Updates | Avg Batch Loss")
        println("   " + "-" * 70)
        
        for ((batchSize, result) in results) {
            val finalMetrics = result.finalMetrics
            val avgBatchLoss = if (result.batchHistory.isNotEmpty()) {
                result.batchHistory.map { it.averageLoss }.average()
            } else {
                0.0
            }
            
            println("   $batchSize | ${finalMetrics.averageReward} | ${result.totalSteps} | ${finalMetrics.totalBatchUpdates} | $avgBatchLoss")
        }
        
        // Find optimal batch size
        val optimalBatch = results.maxByOrNull { it.second.finalMetrics.averageReward }
        if (optimalBatch != null) {
            println("\n🎯 Optimal Batch Size: ${optimalBatch.first}")
            println("   Average Reward: ${optimalBatch.second.finalMetrics.averageReward}")
        }
    }
    
    /**
     * Run quick validation test
     */
    fun runQuickValidation() {
        println("🧪 Quick Training Pipeline Validation")
        println("=" * 40)
        
        try {
            val controller = TrainingController(
                config = TrainingControllerConfig(
                    hiddenLayers = listOf(16, 8),
                    maxStepsPerEpisode = 10,
                    batchSize = 4,
                    progressReportInterval = 2
                )
            )
            
            val success = controller.runTrainingTest(episodes = 3)
            
            if (success) {
                println("✅ Training pipeline validation PASSED")
            } else {
                println("❌ Training pipeline validation FAILED")
            }
            
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
        }
    }
}

