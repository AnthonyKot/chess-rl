package com.chessrl.integration

import kotlin.test.*

/**
 * Comprehensive tests for the Advanced Training Control system
 */
class AdvancedTrainingControlTest {
    
    private lateinit var controller: AdvancedTrainingController
    
    @BeforeTest
    fun setup() {
        controller = AdvancedTrainingController()
    }
    
    @Test
    fun testTrainingLifecycleManagement() {
        println("Testing Training Lifecycle Management")
        
        // Test start training
        val sessionConfig = TrainingSessionConfig(
            name = "test_session",
            controllerType = ControllerType.INTEGRATED,
            iterations = 5,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 3,
                hiddenLayers = listOf(16, 8)
            )
        )
        
        val startResult = controller.startTraining(sessionConfig)
        assertTrue(startResult is TrainingControlResult.Success, "Training should start successfully")
        
        // Verify training is running
        val status1 = controller.getTrainingStatus()
        assertTrue(status1.state == TrainingState.RUNNING || status1.state == TrainingState.STARTING,
                  "Training should be in running or starting state")
        assertNotNull(status1.session, "Session should be created")
        assertEquals("test_session", status1.session!!.name, "Session name should match")
        
        // Test pause training
        val pauseResult = controller.pauseTraining()
        assertTrue(pauseResult is TrainingControlResult.Success, "Training should pause successfully")
        
        val status2 = controller.getTrainingStatus()
        assertEquals(TrainingState.PAUSED, status2.state, "Training should be paused")
        
        // Test resume training
        val resumeResult = controller.resumeTraining()
        assertTrue(resumeResult is TrainingControlResult.Success, "Training should resume successfully")
        
        val status3 = controller.getTrainingStatus()
        assertEquals(TrainingState.RUNNING, status3.state, "Training should be running after resume")
        
        // Test stop training
        val stopResult = controller.stopTraining()
        assertTrue(stopResult is TrainingControlResult.Success, "Training should stop successfully")
        
        val status4 = controller.getTrainingStatus()
        assertEquals(TrainingState.STOPPED, status4.state, "Training should be stopped")
    }
    
    @Test
    fun testRealTimeConfigurationAdjustment() {
        println("Testing Real-Time Configuration Adjustment")
        
        // Start training first
        val sessionConfig = TrainingSessionConfig(
            name = "config_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 10,
            integratedConfig = IntegratedSelfPlayConfig(
                learningRate = 0.001,
                explorationRate = 0.1,
                batchSize = 32
            )
        )
        
        controller.startTraining(sessionConfig)
        
        // Test valid configuration change
        val validUpdate = ConfigurationUpdate(
            parameter = "learningRate",
            oldValue = 0.001,
            newValue = 0.002,
            reason = "Performance optimization"
        )
        
        val validResult = controller.adjustConfiguration(validUpdate)
        assertTrue(validResult is ConfigurationResult.Applied, "Valid configuration should be applied")
        
        // Test configuration validation only
        val validateUpdate = ConfigurationUpdate(
            parameter = "explorationRate",
            oldValue = 0.1,
            newValue = 0.05,
            reason = "Validation test"
        )
        
        val validateResult = controller.adjustConfiguration(validateUpdate, validateOnly = true)
        assertTrue(validateResult is ConfigurationResult.Valid, "Configuration validation should pass")
        
        // Test invalid configuration
        val invalidUpdate = ConfigurationUpdate(
            parameter = "invalidParameter",
            oldValue = "old",
            newValue = "new",
            reason = "Test invalid parameter"
        )
        
        val invalidResult = controller.adjustConfiguration(invalidUpdate)
        assertTrue(invalidResult is ConfigurationResult.Invalid, "Invalid configuration should be rejected")
        
        // Test configuration rollback
        val rollbackResult = controller.rollbackConfiguration()
        assertTrue(rollbackResult is ConfigurationResult.Applied, "Configuration rollback should succeed")
        
        controller.stopTraining()
    }
    
    @Test
    fun testExperimentManagement() {
        println("Testing Experiment Management")
        
        // Create experiment
        val sessionConfig = TrainingSessionConfig(
            name = "experiment_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 5,
            integratedConfig = IntegratedSelfPlayConfig(
                learningRate = 0.001,
                explorationRate = 0.1
            )
        )
        
        val experiment = controller.createExperiment(
            name = "test_experiment",
            sessionConfig = sessionConfig,
            description = "Test experiment for unit testing"
        )
        
        assertNotNull(experiment, "Experiment should be created")
        assertEquals("test_experiment", experiment.name, "Experiment name should match")
        assertEquals(ExperimentStatus.CREATED, experiment.status, "Experiment should be in created status")
        assertTrue(experiment.parameters.isNotEmpty(), "Experiment should have parameters")
        
        // Create second experiment for comparison
        val sessionConfig2 = sessionConfig.copy(
            integratedConfig = sessionConfig.integratedConfig?.copy(learningRate = 0.002)
        )
        
        val experiment2 = controller.createExperiment(
            name = "test_experiment_2",
            sessionConfig = sessionConfig2,
            description = "Second test experiment"
        )
        
        // Test experiment comparison
        val comparison = controller.compareExperiments(listOf(experiment.id, experiment2.id))
        
        assertEquals(2, comparison.experiments.size, "Should compare 2 experiments")
        assertTrue(comparison.comparison.isNotEmpty(), "Should have comparison data")
        assertTrue(comparison.comparison.containsKey("learningRate"), "Should compare learning rates")
        
        val learningRates = comparison.comparison["learningRate"]
        assertNotNull(learningRates, "Learning rates should be compared")
        assertEquals(2, learningRates!!.size, "Should have 2 learning rate values")
    }
    
    @Test
    fun testTrainingWithExperiment() {
        println("Testing Training with Experiment")
        
        val sessionConfig = TrainingSessionConfig(
            name = "experiment_training",
            controllerType = ControllerType.INTEGRATED,
            iterations = 3,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        // Start training with experiment
        val startResult = controller.startTraining(sessionConfig, "training_experiment")
        assertTrue(startResult is TrainingControlResult.Success, "Training with experiment should start")
        
        val status = controller.getTrainingStatus()
        assertNotNull(status.experiment, "Should have associated experiment")
        assertEquals("training_experiment", status.experiment!!.name, "Experiment name should match")
        
        controller.stopTraining()
    }
    
    @Test
    fun testDeterministicTrainingController() {
        println("Testing Deterministic Training Controller Integration")
        
        val sessionConfig = TrainingSessionConfig(
            name = "deterministic_test",
            controllerType = ControllerType.DETERMINISTIC,
            iterations = 2,
            enableDeterministic = true,
            deterministicConfig = TrainingConfiguration(
                masterSeed = 12345L,
                episodes = 2,
                batchSize = 4
            )
        )
        
        val startResult = controller.startTraining(sessionConfig)
        assertTrue(startResult is TrainingControlResult.Success, "Deterministic training should start")
        
        val status = controller.getTrainingStatus()
        assertNotNull(status.session, "Should have training session")
        assertEquals(ControllerType.DETERMINISTIC, status.session!!.config.controllerType,
                    "Should use deterministic controller")
        
        controller.stopTraining()
    }
    
    @Test
    fun testHybridController() {
        println("Testing Hybrid Controller Integration")
        
        val sessionConfig = TrainingSessionConfig(
            name = "hybrid_test",
            controllerType = ControllerType.HYBRID,
            iterations = 2,
            enableDeterministic = true,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            ),
            deterministicConfig = TrainingConfiguration(
                masterSeed = 54321L,
                episodes = 2
            )
        )
        
        val startResult = controller.startTraining(sessionConfig)
        assertTrue(startResult is TrainingControlResult.Success, "Hybrid training should start")
        
        val status = controller.getTrainingStatus()
        assertNotNull(status.session, "Should have training session")
        assertEquals(ControllerType.HYBRID, status.session!!.config.controllerType,
                    "Should use hybrid controller")
        
        controller.stopTraining()
    }
    
    @Test
    fun testTrainingRestart() {
        println("Testing Training Restart")
        
        val sessionConfig = TrainingSessionConfig(
            name = "restart_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 3,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        // Start initial training
        controller.startTraining(sessionConfig)
        val initialStatus = controller.getTrainingStatus()
        val initialSessionId = initialStatus.session?.id
        
        // Restart with same configuration
        val restartResult = controller.restartTraining()
        assertTrue(restartResult is TrainingControlResult.Success, "Training should restart successfully")
        
        val restartStatus = controller.getTrainingStatus()
        assertNotNull(restartStatus.session, "Should have new session after restart")
        assertNotEquals(initialSessionId, restartStatus.session!!.id, "Should have new session ID")
        
        // Restart with new configuration
        val newConfig = sessionConfig.copy(
            name = "restart_test_new",
            iterations = 5
        )
        
        val restartNewResult = controller.restartTraining(newConfig)
        assertTrue(restartNewResult is TrainingControlResult.Success, "Training should restart with new config")
        
        val newStatus = controller.getTrainingStatus()
        assertEquals("restart_test_new", newStatus.session!!.name, "Should use new configuration")
        assertEquals(5, newStatus.session!!.config.iterations, "Should use new iteration count")
        
        controller.stopTraining()
    }
    
    @Test
    fun testTrainingReport() {
        println("Testing Training Report Generation")
        
        val sessionConfig = TrainingSessionConfig(
            name = "report_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 2,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        // Start training with experiment
        controller.startTraining(sessionConfig, "report_experiment")
        
        // Make some configuration changes
        val configUpdate = ConfigurationUpdate(
            parameter = "learningRate",
            oldValue = 0.001,
            newValue = 0.002,
            reason = "Report test"
        )
        controller.adjustConfiguration(configUpdate)
        
        // Generate report
        val report = controller.generateTrainingReport()
        
        assertNotNull(report, "Report should be generated")
        assertNotNull(report.sessionSummary, "Report should have session summary")
        assertNotNull(report.experimentSummary, "Report should have experiment summary")
        assertNotNull(report.performanceAnalysis, "Report should have performance analysis")
        assertTrue(report.configurationChanges.isNotEmpty(), "Report should include configuration changes")
        assertTrue(report.recommendations.isNotEmpty(), "Report should include recommendations")
        
        assertEquals("report_test", report.sessionSummary!!.name, "Session name should match")
        assertEquals("report_experiment", report.experimentSummary!!.name, "Experiment name should match")
        
        controller.stopTraining()
    }
    
    @Test
    fun testErrorHandling() {
        println("Testing Error Handling")
        
        // Test pause without active training
        val pauseResult = controller.pauseTraining()
        assertTrue(pauseResult is TrainingControlResult.Error, "Pause without training should fail")
        
        // Test resume without paused training
        val resumeResult = controller.resumeTraining()
        assertTrue(resumeResult is TrainingControlResult.Error, "Resume without paused training should fail")
        
        // Test stop without active training
        val stopResult = controller.stopTraining()
        assertTrue(stopResult is TrainingControlResult.Error, "Stop without training should fail")
        
        // Test rollback without configuration history
        val rollbackResult = controller.rollbackConfiguration()
        assertTrue(rollbackResult is ConfigurationResult.Error, "Rollback without history should fail")
        
        // Test experiment comparison with insufficient experiments
        val comparison = controller.compareExperiments(listOf("nonexistent"))
        assertTrue(comparison.experiments.size < 2, "Should not have enough experiments to compare")
        assertTrue(comparison.recommendations.contains("Need at least 2 experiments to compare"),
                  "Should recommend more experiments")
    }
    
    @Test
    fun testMetricsCollection() {
        println("Testing Metrics Collection")
        
        val sessionConfig = TrainingSessionConfig(
            name = "metrics_test",
            controllerType = ControllerType.INTEGRATED,
            iterations = 2,
            integratedConfig = IntegratedSelfPlayConfig(
                gamesPerIteration = 1,
                maxStepsPerGame = 2,
                hiddenLayers = listOf(8, 4)
            )
        )
        
        controller.startTraining(sessionConfig)
        
        val status = controller.getTrainingStatus()
        
        // Verify metrics are being collected
        assertNotNull(status.metrics, "Should have training metrics")
        assertNotNull(status.performance, "Should have performance metrics")
        
        status.metrics?.let { metrics ->
            assertTrue(metrics.currentIteration >= 0, "Current iteration should be non-negative")
            assertTrue(metrics.totalIterations > 0, "Total iterations should be positive")
            assertTrue(metrics.averageReward >= 0.0, "Average reward should be non-negative")
            assertTrue(metrics.explorationRate >= 0.0, "Exploration rate should be non-negative")
        }
        
        status.performance?.let { perf ->
            assertTrue(perf.averageIterationTime > 0, "Iteration time should be positive")
            assertTrue(perf.memoryUsage >= 0.0 && perf.memoryUsage <= 1.0, "Memory usage should be 0-1")
            assertTrue(perf.cpuUsage >= 0.0 && perf.cpuUsage <= 1.0, "CPU usage should be 0-1")
            assertTrue(perf.throughput >= 0.0, "Throughput should be non-negative")
        }
        
        controller.stopTraining()
    }
}