# Configuration System Migration Guide

Important: The legacy bridge to `AdvancedSelfPlayConfig` is deprecated and not available in the current codebase. Prefer using `ChessRLConfig` directly with the consolidated `TrainingPipeline`. Sections explicitly tagged as [Archived] are kept for historical context.

## Overview

The legacy `TrainingConfiguration`/`AdvancedSelfPlayConfig` stack has been retired. This guide explains how to migrate any remaining code to the unified `ChessRLConfig` plus `TrainingPipeline` + backend approach.

## Migration Steps

### 1. Replace TrainingConfiguration with ChessRLConfig

**Before:**
```kotlin
val config = TrainingConfiguration(
    episodes = 1000,
    batchSize = 64,
    learningRate = 0.001,
    explorationRate = 0.1,
    hiddenLayers = listOf(512, 256, 128),
    gamesPerIteration = 20,
    parallelGames = 4,
    maxStepsPerGame = 200,
    stepLimitPenalty = -0.05
)
```

**After:**
```kotlin
val config = ChessRLConfig(
    maxCycles = 1000,        // episodes -> maxCycles
    batchSize = 64,
    learningRate = 0.001,
    explorationRate = 0.1,
    hiddenLayers = listOf(512, 256, 128),
    gamesPerCycle = 20,      // gamesPerIteration -> gamesPerCycle
    maxConcurrentGames = 4,  // parallelGames -> maxConcurrentGames
    maxStepsPerGame = 200,
    stepLimitPenalty = -0.05
)
```

### 2. [Archived] Legacy adapter to AdvancedSelfPlayConfig

> **Archived:** The adapter layer is no longer part of the codebase. The sample is kept for historical context only.

This bridge existed to map `ChessRLConfig` to a much larger historical configuration. It is not required for the consolidated pipeline and can be ignored for new code.

**Before (legacy):**
```kotlin
val advancedConfig = AdvancedSelfPlayConfig(
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 0.001,
    batchSize = 64,
    explorationRate = 0.1,
    initialGamesPerCycle = 20,
    maxConcurrentGames = 4,
    // ... 50+ other parameters
)
```

// No longer required: use ChessRLConfig directly with TrainingPipeline

### 3. Replace Profile Loading

**Before:**
```kotlin
val profiles = ProfilesLoader.loadProfiles(listOf("profiles.yaml"))
val profileData = profiles["fast-debug"] ?: error("Profile not found")
// Manual parsing of string values...
```

**After:**
```kotlin
// For JVM code with file access:
val config = JvmConfigParser.loadProfile("fast-debug")

// For multiplatform code:
val config = ConfigParser.loadProfile("fast-debug")
```

### 4. Replace CLI Argument Parsing

**Before:**
```kotlin
val config = ConfigurationParser.parseCliArguments(args)
// Complex parsing with many experimental flags
```

**After:**
```kotlin
val config = ConfigParser.parseArgs(args)
// Simple parsing with only essential parameters
```

## Parameter Mapping Reference

### Core Parameters
| Old Parameter | New Parameter | Notes |
|---------------|---------------|-------|
| `episodes` | `maxCycles` | Training duration |
| `gamesPerIteration` | `gamesPerCycle` | Self-play games per cycle |
| `parallelGames` | `maxConcurrentGames` | Concurrent game limit |
| `maxStepsPerEpisode` | `maxStepsPerGame` | Game length limit |
| `maxBufferSize` | `maxExperienceBuffer` | Experience buffer size |

### Removed Parameters
These experimental parameters have been removed. The new pipeline/backends expose a
leaner surface and apply sensible defaults internally:

| Removed Parameter | Replacement Behaviour |
|-------------------|-----------------------|
| `enablePositionRewards` | Currently disabled; expose via backend if reintroduced |
| `gameLengthNormalization` | Managed by backend (off in default DQN backend) |
| `enableDoubleDQN` | Backend-specific toggle; default backend keeps it off |
| `explorationWarmupCycles`, `opponentWarmupCycles` | Exploration handled directly by backend logic |
| `repetitionPenalty`, `enableLocalThreefoldDraw` | Chess engine handles repetition—no extra shaping |
| `l2Regularization`, `gradientClipping` | Responsibility of the underlying NN/RL framework |

## Migration Examples

### Example 1: Training Pipeline

**Before:**
```kotlin
class TrainingPipeline(
    private val config: TrainingConfiguration
) {
    fun train() {
        val advancedConfig = AdvancedSelfPlayConfig(
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
            // ... manual mapping of 50+ parameters
        )
        // ...
    }
}
```

**After (consolidated):**
```kotlin
class TrainingPipeline(
    private val config: ChessRLConfig
) {
    fun train() {
        // Use config directly; no adapter required
        // run training cycles using config.* values
    }
}
```

### Example 2: CLI Application

**Before:**
```kotlin
fun main(args: Array<String>) {
    val config = ConfigurationParser.parseCliArguments(args)
    val profiles = ProfilesLoader.loadProfiles(listOf("profiles.yaml"))
    val profileName = extractProfile(args)
    val profileData = profiles[profileName]
    // Complex merging logic...
}
```

**After:**
```kotlin
fun main(args: Array<String>) {
    var config = ConfigParser.parseArgs(args)
    
    // Profile loading is integrated
    if (args.contains("--profile")) {
        val profileName = args[args.indexOf("--profile") + 1]
        config = JvmConfigParser.loadProfile(profileName)
        // Re-apply CLI overrides
        config = ConfigParser.parseArgs(args) // TODO: Add base config support
    }
}
```

### Example 3: Testing

**Before:**
```kotlin
@Test
fun testTraining() {
    val config = TrainingConfiguration(
        episodes = 10,
        batchSize = 16,
        learningRate = 0.01,
        explorationRate = 0.05,
        hiddenLayers = listOf(64, 32),
        gamesPerIteration = 5,
        parallelGames = 2,
        maxStepsPerGame = 50,
        enableDebugMode = true,
        // ... many more parameters
    )
}
```

**After:**
```kotlin
@Test
fun testTraining() {
    val config = ChessRLConfig()
        .forFastDebug()
        .withSeed(12345L)
        .copy(
            batchSize = 16,
            learningRate = 0.01
        )
    // Much simpler and clearer!
}
```

## Validation and Testing

### Validate Profiles and CLI

```kotlin
@Test
fun testProfileMigration() {
    // Test that old profiles still work
    val config = JvmConfigParser.loadProfile("fast-debug")
    val validation = config.validate()
    assertTrue(validation.isValid)
}
```

## Common Migration Issues

### Issue 1: Missing Parameters

**Problem:** Code expects parameters that were removed.

**Solution:** Remove reliance on the obsolete fields. If a feature is still
required, push the configuration into the learning backend (e.g., expose a
`enablePositionRewards` flag on a custom backend implementation). The default
backend keeps these features disabled.

### Issue 2: Parameter Name Changes

**Problem:** Code uses old parameter names.

**Solution:** Update to new names or use legacy mapping in JvmConfigParser:

```kotlin
// Legacy profile with old names still works
val profileData = mapOf(
    "episodes" to "100",        // Maps to maxCycles
    "parallelGames" to "4"      // Maps to maxConcurrentGames
)
val config = JvmConfigParser.parseProfileData(profileData)
```

### Issue 3: Complex Profile Logic

**Problem:** Existing code has complex profile merging logic.

**Solution:** Use the new profile system:

```kotlin
// Old complex logic
val baseConfig = loadBaseConfig()
val profileOverrides = loadProfile(profileName)
val cliOverrides = parseCliArgs(args)
val finalConfig = mergeConfigs(baseConfig, profileOverrides, cliOverrides)

// New simple logic
var config = ConfigParser.parseArgs(args)
if (profileName != null) {
    config = JvmConfigParser.loadProfile(profileName)
    // TODO: Add support for CLI overrides on top of profile
}
```

## Rollback Plan (Archived)

If migration issues arise, you can temporarily use both systems:

```kotlin
// Gradual migration approach
class TrainingPipeline(
    private val legacyConfig: TrainingConfiguration? = null,
    private val newConfig: ChessRLConfig? = null
) {
    private val effectiveConfig by lazy {
        when {
            newConfig != null -> newConfig
            legacyConfig != null -> {
                // Minimal shim: map critical fields manually or keep legacy codepaths
                ChessRLConfig(gamesPerCycle = legacyConfig.gamesPerIteration,
                              maxConcurrentGames = legacyConfig.parallelGames,
                              maxStepsPerGame = legacyConfig.maxStepsPerGame)
            }
            else -> ChessRLConfig()
        }
    }
}
```

## Timeline

### Phase 1: Parallel Systems (Current)
- ✅ New configuration system implemented
- ✅ Consolidated training pipeline with pluggable backend
- ✅ Legacy configuration still readable via `JvmConfigParser`

### Phase 2: Migration (Next 2-4 weeks)
- Update CLI entry points to use the new pipeline/backends
- Update tests to use `TrainingPipeline` + backend abstraction
- Add CLI override support on top of profiles

### Phase 3: Cleanup (After migration complete)
- Remove TrainingConfiguration class
- Remove old profile parsing logic
- Remove legacy parameter mapping
- Simplify profiles.yaml to use new parameter names

## Support

For migration questions or issues:
1. Check this guide for common patterns
2. Review `TrainingPipeline` and `DqnLearningBackend` for reference usage
3. Refer to CONFIGURATION_INTEGRATION_NOTES.md for architecture details
