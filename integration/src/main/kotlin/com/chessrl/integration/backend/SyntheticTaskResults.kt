package com.chessrl.integration.backend

/**
 * Result of a single synthetic task for one backend
 */
data class SyntheticTaskResult(
    val backend: String,
    val taskName: String,
    val converged: Boolean,
    val finalAccuracy: Double,
    val convergenceEpoch: Int,
    val trainingTimeMs: Long,
    val inferenceTimeMs: Long,
    val finalLoss: Double,
    val lossHistory: List<Double>,
    val accuracyHistory: List<Double>,
    val errorMessage: String? = null
)

/**
 * Comparison result across multiple backends for a single task
 */
data class ComparisonResult(
    val taskName: String,
    val results: Map<BackendType, SyntheticTaskResult>,
    val summary: String
)

/**
 * Performance metrics for a single backend
 */
data class PerformanceMetrics(
    val backend: String,
    val avgInferenceTimeMs: Double,
    val avgTrainingTimeMs: Double,
    val memoryUsageMB: Long,
    val parameterCount: Long,
    val errorMessage: String? = null
)

/**
 * Performance benchmark result across multiple backends
 */
data class PerformanceBenchmarkResult(
    val taskName: String,
    val inputSize: Int,
    val outputSize: Int,
    val batchSize: Int,
    val numIterations: Int,
    val results: Map<BackendType, PerformanceMetrics>
)

/**
 * Summary of all metrics for a single backend
 */
data class BackendSummary(
    val backend: String,
    val xorAccuracy: Double,
    val xorConverged: Boolean,
    val sineAccuracy: Double,
    val sineConverged: Boolean,
    val chessAccuracy: Double,
    val chessConverged: Boolean,
    val avgInferenceTimeMs: Double,
    val avgTrainingTimeMs: Double,
    val memoryUsageMB: Long,
    val parameterCount: Long,
    val overallScore: Double
)

/**
 * Comprehensive comparison report across all tasks and backends
 */
data class ComprehensiveComparisonReport(
    val xorResult: ComparisonResult,
    val sineResult: ComparisonResult,
    val chessResult: ComparisonResult,
    val inferenceResult: PerformanceBenchmarkResult,
    val trainingResult: PerformanceBenchmarkResult,
    val backendSummaries: Map<BackendType, BackendSummary>,
    val recommendations: List<String>
) {
    /**
     * Generate a formatted text report
     */
    fun generateTextReport(): String {
        val report = StringBuilder()
        
        report.appendLine("=".repeat(80))
        report.appendLine("NEURAL NETWORK BACKEND COMPARISON REPORT")
        report.appendLine("=".repeat(80))
        report.appendLine()
        
        // Executive Summary
        report.appendLine("EXECUTIVE SUMMARY")
        report.appendLine("-".repeat(40))
        recommendations.forEach { recommendation ->
            report.appendLine("• $recommendation")
        }
        report.appendLine()
        
        // Backend Summaries
        report.appendLine("BACKEND PERFORMANCE SUMMARY")
        report.appendLine("-".repeat(40))
        val sortedBackends = backendSummaries.values.sortedByDescending { it.overallScore }
        
        for (summary in sortedBackends) {
            report.appendLine("${summary.backend.uppercase()}:")
            report.appendLine("  Overall Score: ${String.format("%.2f", summary.overallScore)}/100")
            report.appendLine("  Accuracy:")
            report.appendLine("    XOR: ${String.format("%.3f", summary.xorAccuracy)} ${if (summary.xorConverged) "✓" else "✗"}")
            report.appendLine("    Sine: ${String.format("%.3f", summary.sineAccuracy)} ${if (summary.sineConverged) "✓" else "✗"}")
            report.appendLine("    Chess: ${String.format("%.3f", summary.chessAccuracy)} ${if (summary.chessConverged) "✓" else "✗"}")
            report.appendLine("  Performance:")
            report.appendLine("    Inference: ${String.format("%.2f", summary.avgInferenceTimeMs)}ms")
            report.appendLine("    Training: ${String.format("%.2f", summary.avgTrainingTimeMs)}ms")
            report.appendLine("    Memory: ${summary.memoryUsageMB}MB")
            report.appendLine("    Parameters: ${summary.parameterCount}")
            report.appendLine()
        }
        
        // Detailed Task Results
        report.appendLine("DETAILED TASK RESULTS")
        report.appendLine("-".repeat(40))
        
        // XOR Results
        report.appendLine("XOR Learning Task:")
        report.append(xorResult.summary)
        report.appendLine()
        
        // Sine Results
        report.appendLine("Sine Regression Task:")
        report.append(sineResult.summary)
        report.appendLine()
        
        // Chess Results
        report.appendLine("Chess Pattern Recognition Task:")
        report.append(chessResult.summary)
        report.appendLine()
        
        // Performance Benchmarks
        report.appendLine("PERFORMANCE BENCHMARKS")
        report.appendLine("-".repeat(40))
        
        report.appendLine("Inference Speed (${inferenceResult.inputSize}→${inferenceResult.outputSize}, batch=${inferenceResult.batchSize}):")
        for ((backend, metrics) in inferenceResult.results) {
            val status = if (metrics.errorMessage != null) "ERROR: ${metrics.errorMessage}" else "${String.format("%.2f", metrics.avgInferenceTimeMs)}ms"
            report.appendLine("  ${backend.name}: $status")
        }
        report.appendLine()
        
        report.appendLine("Training Speed (${trainingResult.inputSize}→${trainingResult.outputSize}, batch=${trainingResult.batchSize}):")
        for ((backend, metrics) in trainingResult.results) {
            val status = if (metrics.errorMessage != null) "ERROR: ${metrics.errorMessage}" else "${String.format("%.2f", metrics.avgTrainingTimeMs)}ms"
            report.appendLine("  ${backend.name}: $status")
        }
        report.appendLine()
        
        report.appendLine("=".repeat(80))
        
        return report.toString()
    }
    
    /**
     * Generate a CSV report for further analysis
     */
    fun generateCSVReport(): String {
        val csv = StringBuilder()
        
        // Header
        csv.appendLine("Backend,XOR_Accuracy,XOR_Converged,Sine_Accuracy,Sine_Converged,Chess_Accuracy,Chess_Converged,Inference_Time_ms,Training_Time_ms,Memory_MB,Parameters,Overall_Score")
        
        // Data rows
        for ((_, summary) in backendSummaries) {
            csv.appendLine("${summary.backend},${summary.xorAccuracy},${summary.xorConverged},${summary.sineAccuracy},${summary.sineConverged},${summary.chessAccuracy},${summary.chessConverged},${summary.avgInferenceTimeMs},${summary.avgTrainingTimeMs},${summary.memoryUsageMB},${summary.parameterCount},${summary.overallScore}")
        }
        
        return csv.toString()
    }
}
