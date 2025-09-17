# Training Visualization System

> Note: Archived/overview document. For practical, supported flows use the integration CLI described in README.md.

## Overview

The Training Visualization System provides sophisticated real-time visualization and monitoring for Chess RL training with interactive dashboards, game analysis, learning curve visualization, and comprehensive performance monitoring.

## 🎨 Key Features

### **Real-Time Interactive Dashboard**
- **Live Training Metrics** - Real-time display of iteration progress, rewards, loss, and exploration rates
- **Multi-Panel Layout** - Organized dashboard with training progress, game analysis, and performance monitoring
- **Auto-Refresh** - Configurable refresh rates for live updates
- **Interactive Controls** - Keyboard navigation and mode switching

### **ASCII Chess Board Visualization**
- **Real-Time Game Display** - Live ASCII chess board showing current game positions
- **Move Analysis** - Best move suggestions with evaluation scores and confidence levels
- **Position Evaluation** - Real-time position analysis with numerical evaluations
- **Game History** - Track multiple games with move-by-move analysis

### **Learning Curve Analysis**
- **Reward Progression** - Visual charts showing reward improvement over time
- **Loss Tracking** - Training loss visualization with trend analysis
- **Convergence Detection** - Automatic detection of training convergence
- **Trend Analysis** - Statistical analysis of learning trends and stability

### **Performance Monitoring**
- **Resource Utilization** - Real-time memory, CPU, and GPU usage monitoring
- **Efficiency Metrics** - Games per second, experiences per second, throughput analysis
- **Bottleneck Detection** - Automatic identification of performance bottlenecks
- **Optimization Suggestions** - AI-powered recommendations for performance improvements

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                TrainingVisualizationSystem                      │
│                    (Central Coordinator)                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   Dashboard     │  │   Game Analysis │  │   Performance   │  │
│  │   Renderer      │  │   Visualizer    │  │   Monitor       │  │
│  │                 │  │                 │  │                 │  │
│  │ • Progress      │  │ • ASCII Board   │  │ • Resource      │  │
│  │ • Metrics       │  │ • Move Eval     │  │   Usage         │  │
│  │ • Charts        │  │ • Position      │  │ • Efficiency    │  │
│  │ • Status        │  │   Analysis      │  │ • Bottlenecks   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   Chart         │  │   Trend         │  │   Interactive   │  │
│  │   Renderer      │  │   Analyzer      │  │   CLI           │  │
│  │                 │  │                 │  │                 │  │
│  │ • Line Charts   │  │ • Convergence   │  │ • Mode Switch   │  │
│  │ • Mini Charts   │  │ • Stability     │  │ • Keyboard      │  │
│  │ • Progress Bars │  │ • Predictions   │  │   Controls      │  │
│  │ • Sparklines    │  │ • Trends        │  │ • Real-time     │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Usage Examples

### Basic Visualization

```kotlin
// Create visualization system
val visualizationSystem = TrainingVisualizationSystem()
val trainingController = AdvancedTrainingController()

// Start visualization
val result = visualizationSystem.startVisualization(trainingController)
when (result) {
    is VisualizationResult.Success -> println("Visualization started")
    is VisualizationResult.Error -> println("Failed: ${result.message}")
}

// Render dashboard
val dashboard = visualizationSystem.renderDashboard()
println(dashboard)
```

### Interactive CLI

```bash
# Start interactive visualization
InteractiveVisualizationCLI

# Direct commands
InteractiveVisualizationCLI dashboard
InteractiveVisualizationCLI monitor
InteractiveVisualizationCLI analyze
InteractiveVisualizationCLI demo
```

### Real-Time Dashboard

```kotlin
// Create real-time dashboard
val realTimeDashboard = RealTimeVisualizationDashboard(controller, visualizationSystem)

// Configure update rate
realTimeDashboard.setUpdateInterval(1000L) // 1 second updates

// Start real-time updates
realTimeDashboard.start()
```

## Dashboard Layout

```
┌─────────────────────────────────────────────────────────────────┐
│                🎮 CHESS RL TRAINING DASHBOARD 🎮                │
│    Last Update: Time: 1234567890 | 🟢 ACTIVE                   │
├─────────────────┬─────────────────┬─────────────────────────────┤
│ TRAINING        │ GAME ANALYSIS   │ PERFORMANCE MONITOR         │
│ PROGRESS        │                 │                             │
│                 │ Current Game:   │ ┌─ RESOURCE USAGE ─────────┐ │
│ Progress: [████] │ Move: 15        │ │ Memory:  [████████    ] │ │
│ 45%             │                 │ │ CPU:     [██████      ] │ │
│                 │ ┌─ BOARD ─────┐ │ │ GPU:     [███████     ] │ │
│ ┌─ METRICS ────┐ │ │  a b c d e f│ │ └─────────────────────────┘ │
│ │ Iteration: 45│ │ │8 r n b q k b│ │                             │
│ │ Reward: 0.75 │ │ │7 p p p p p p│ │ ┌─ EFFICIENCY ───────────┐ │
│ │ Loss:   0.23 │ │ │6 . . . . . .│ │ │ Games/sec:    2.5      │ │
│ │ Explore: 0.1 │ │ │5 . . . . . .│ │ │ Exp/sec:     12.3      │ │
│ └──────────────┘ │ │4 . . . P . .│ │ │ Iter Time:  5.2s       │ │
│                 │ │3 . . . . . .│ │ └─────────────────────────┘ │
│ ┌─ LEARNING ───┐ │ │2 P P P . P P│ │                             │
│ │     Reward   │ │ │1 R N B Q K B│ │ ┌─ TRENDS ──────────────┐ │
│ │ 1.0 ████     │ │ │  a b c d e f│ │ │ Reward:   📈 Increasing│ │
│ │ 0.8 ███      │ │ └─────────────┘ │ │ Loss:     📉 Decreasing│ │
│ │ 0.6 ██       │ │                 │ │ Converge: 🎯 Converging│ │
│ │ 0.4 █        │ │ Best: e2-e4     │ └─────────────────────────┘ │
│ │ 0.2          │ │ Eval: +0.25     │                             │
│ │ 0.0 ────────►│ │ Conf: 87%       │                             │
│ └──────────────┘ │                 │                             │
└─────────────────┴─────────────────┴─────────────────────────────┤
│ Controls: [R]efresh | [P]ause | [S]ave | [Q]uit | [H]elp        │
└─────────────────────────────────────────────────────────────────┘
```

## Interactive Controls

### Keyboard Navigation

| Key | Action | Description |
|-----|--------|-------------|
| `D` | Dashboard | Switch to main training dashboard |
| `M` | Monitor | Switch to performance monitoring view |
| `A` | Analyze | Switch to learning curve analysis |
| `G` | Game | Switch to game analysis view |
| `R` | Refresh | Manually refresh current display |
| `F` | Faster | Increase refresh rate (faster updates) |
| `S` | Slower | Decrease refresh rate (slower updates) |
| `C` | Clear | Clear screen |
| `H` | Help | Show help and controls |
| `Q` | Quit | Exit visualization |

### Display Modes

1. **Dashboard Mode** - Complete overview with all panels
2. **Performance Monitor** - Detailed resource and efficiency monitoring
3. **Learning Analysis** - Learning curves and convergence analysis
4. **Game Analysis** - Chess board and move analysis

## Learning Curve Visualization

### Reward Progression Chart
```
📈 LEARNING CURVE ANALYSIS
==================================================
Average Reward Over Time
------------------------
Reward
1.00 │                                    ████
0.90 │                               ████████
0.80 │                          ████████████
0.70 │                     ████████████████
0.60 │                ████████████████████
0.50 │           ████████████████████████
0.40 │      ████████████████████████████
0.30 │ ████████████████████████████████
0.20 │████████████████████████████████
0.10 │████████████████████████████████
0.00 ────────────────────────────────────────
     0    10   20   30   40   50   60   70

🎯 CONVERGENCE ANALYSIS
------------------------------
Status: Converging
Trend: Increasing
Stability: 87.3%
Estimated Convergence: Iteration 85
```

### Trend Detection

- **Increasing Trend** 📈 - Performance improving over time
- **Decreasing Trend** 📉 - Performance declining (for loss metrics)
- **Stable Trend** ➡️ - Performance plateaued or converged
- **Volatile Trend** 📊 - High variance, unstable training

## Performance Monitoring

### Resource Utilization Display
```
⚡ PERFORMANCE MONITORING
========================================
🖥️ RESOURCE UTILIZATION
-------------------------
Memory: [████████████████████████      ] 80.5%
CPU:    [████████████████              ] 65.2%
GPU:    [██████████████████████        ] 75.8%

📊 EFFICIENCY METRICS
--------------------
Games per second:      2.45
Experiences per second: 12.3
Average iteration time: 4.2s
Overall throughput:     2.45

📈 PERFORMANCE TRENDS
--------------------
Throughput trend:  📈 Increasing
Memory trend:      ➡️ Stable
Efficiency trend:  📈 Increasing

⚠️ DETECTED BOTTLENECKS
--------------------
• High memory usage (80.5%)
• Consider reducing batch size for better memory efficiency
```

### Bottleneck Detection

The system automatically detects:
- **High Memory Usage** (>90%) - Suggests reducing batch size or buffer size
- **High CPU Usage** (>90%) - Suggests optimizing computation or reducing parallelism
- **Slow Iterations** (>30s) - Suggests performance optimization
- **Low Throughput** (<1 game/sec) - Suggests configuration adjustments

## Game Analysis Features

### ASCII Chess Board
```
♟️ GAME ANALYSIS
==================================================
Current Position:
  a b c d e f g h
8 r n b q k b n r 8
7 p p p p p p p p 7
6 . . . . . . . . 6
5 . . . . . . . . 5
4 . . . . P . . . 4
3 . . . . . . . . 3
2 P P P P . P P P 2
1 R N B Q K B N R 1
  a b c d e f g h

📊 Position Analysis:
Best Move: e7-e5
Evaluation: +0.25
Confidence: 87.3%
Depth: 12 plies

🎯 Move Candidates:
1. e7-e5    (+0.25)  87.3%
2. d7-d6    (+0.15)  12.1%
3. Nf6      (+0.10)   0.6%
```

### Move Evaluation
- **Best Move** - AI's top choice with algebraic notation
- **Evaluation** - Position evaluation in centipawns or win probability
- **Confidence** - AI's confidence in the evaluation
- **Move Candidates** - Alternative moves with their evaluations

## Configuration Options

### Visualization Configuration
```kotlin
val config = VisualizationConfig(
    dashboardWidth = 105,        // Total dashboard width
    dashboardHeight = 30,        // Dashboard height
    panelWidth = 33,            // Individual panel width
    updateIntervalMs = 1000L,    // Refresh rate (1 second)
    maxHistorySize = 1000,      // Maximum data points to keep
    enableRealTimeUpdates = true, // Enable live updates
    enableGameAnalysis = true,   // Enable game visualization
    enablePerformanceMonitoring = true // Enable performance monitoring
)
```

### Display Customization
- **Dashboard Dimensions** - Adjustable width and height
- **Panel Layout** - Configurable panel sizes and arrangement
- **Update Frequency** - Customizable refresh rates (0.5s to 10s)
- **Data History** - Configurable history buffer size
- **Feature Toggles** - Enable/disable specific visualization components

## CLI Commands

### Basic Commands
```bash
# Show training dashboard
InteractiveVisualizationCLI dashboard

# Show performance monitor
InteractiveVisualizationCLI monitor

# Show learning analysis
InteractiveVisualizationCLI analyze

# Run demonstration
InteractiveVisualizationCLI demo

# Start interactive mode
InteractiveVisualizationCLI
```

### Interactive Mode
```bash
visualization> d          # Switch to dashboard
visualization> m          # Switch to monitor
visualization> a          # Switch to analysis
visualization> g          # Switch to game view
visualization> r          # Refresh display
visualization> f          # Faster updates
visualization> s          # Slower updates
visualization> h          # Show help
visualization> q          # Quit
```

## Integration with Training Controllers

### Advanced Training Controller Integration
```kotlin
// Create integrated system
val trainingController = AdvancedTrainingController()
val visualizationSystem = TrainingVisualizationSystem()

// Start training with visualization
val sessionConfig = TrainingSessionConfig(
    name = "visualized_training",
    controllerType = ControllerType.INTEGRATED,
    iterations = 100
)

trainingController.startTraining(sessionConfig)
visualizationSystem.startVisualization(trainingController)

// Real-time updates
while (trainingController.getTrainingStatus().state == TrainingState.RUNNING) {
    visualizationSystem.updateVisualization(trainingController)
    Thread.sleep(1000) // Update every second
}
```

### Deterministic Training Visualization
```kotlin
// Visualize deterministic training
val sessionConfig = TrainingSessionConfig(
    controllerType = ControllerType.DETERMINISTIC,
    enableDeterministic = true,
    deterministicConfig = TrainingConfiguration(masterSeed = 12345L)
)

trainingController.startTraining(sessionConfig)
visualizationSystem.startVisualization(trainingController)
```

## Advanced Features

### Convergence Analysis
- **Automatic Detection** - Identifies when training has converged
- **Stability Metrics** - Measures training stability and variance
- **Trend Prediction** - Estimates convergence iteration
- **Early Stopping** - Suggests when to stop training

### Performance Optimization
- **Bottleneck Identification** - Automatically detects performance issues
- **Resource Monitoring** - Tracks memory, CPU, and GPU usage
- **Efficiency Analysis** - Measures training efficiency and throughput
- **Optimization Suggestions** - AI-powered performance recommendations

### Real-Time Analytics
- **Live Metrics** - Real-time training progress and performance data
- **Trend Analysis** - Statistical analysis of training trends
- **Predictive Analytics** - Estimates training completion and convergence
- **Anomaly Detection** - Identifies unusual training behavior

## Testing and Validation

### Comprehensive Test Suite
```kotlin
@Test
fun testVisualizationSystemInitialization() {
    val visualizationSystem = TrainingVisualizationSystem()
    val result = visualizationSystem.startVisualization(trainingController)
    assertTrue(result is VisualizationResult.Success)
}

@Test
fun testDashboardRendering() {
    visualizationSystem.startVisualization(trainingController)
    val dashboard = visualizationSystem.renderDashboard()
    assertTrue(dashboard.contains("TRAINING DASHBOARD"))
}
```

### Demo System
```kotlin
// Run complete visualization demo
val results = VisualizationDemo.runCompleteVisualizationDemo()

// Test specific features
InteractiveVisualizationCLI.main(arrayOf("demo"))
```

## Performance Characteristics

### Rendering Performance
- **Fast ASCII Rendering** - Optimized text-based visualization
- **Efficient Data Processing** - Minimal overhead for real-time updates
- **Configurable Refresh Rates** - Balance between responsiveness and performance
- **Memory Efficient** - Bounded history buffers prevent memory leaks

### Scalability
- **Large Training Sessions** - Handles long-running training sessions
- **High-Frequency Updates** - Supports sub-second refresh rates
- **Multiple Concurrent Views** - Multiple visualization modes simultaneously
- **Resource Monitoring** - Self-monitoring to prevent resource exhaustion

## Troubleshooting

### Common Issues

1. **Visualization Not Starting**
   - Check training controller initialization
   - Verify visualization system configuration
   - Ensure sufficient terminal size

2. **Dashboard Not Updating**
   - Check refresh rate configuration
   - Verify training controller is active
   - Ensure updateVisualization() is being called

3. **Performance Issues**
   - Reduce refresh rate (increase interval)
   - Decrease history buffer size
   - Disable unnecessary visualization components

4. **Display Formatting Issues**
   - Ensure terminal supports required width (105+ characters)
   - Check terminal encoding (UTF-8 recommended)
   - Verify ASCII character support

### Debug Mode
```kotlin
val debugConfig = VisualizationConfig(
    updateIntervalMs = 500L,     // Faster updates for debugging
    maxHistorySize = 100,        // Smaller buffer for debugging
    enableRealTimeUpdates = true
)
```

## Future Enhancements

### Planned Features
1. **Web-Based Dashboard** - Browser-based visualization interface
2. **Export Capabilities** - Save charts and data to files
3. **Custom Themes** - Configurable color schemes and layouts
4. **Advanced Analytics** - Machine learning-based insights
5. **Multi-Session Comparison** - Compare multiple training sessions

### API Extensions
1. **REST API** - HTTP-based visualization data access
2. **WebSocket Streaming** - Real-time data streaming to web clients
3. **Plugin System** - Custom visualization components
4. **Export Formats** - PNG, SVG, PDF chart export

## Contributing

When contributing to the visualization system:

1. **Maintain ASCII Compatibility** - Ensure all visualizations work in text terminals
2. **Optimize Performance** - Keep rendering fast and memory-efficient
3. **Add Comprehensive Tests** - Test all visualization components
4. **Update Documentation** - Keep documentation current with new features
5. **Follow Design Patterns** - Use established visualization patterns

## License

This Training Visualization System is part of the Chess RL project and follows the same licensing terms.
