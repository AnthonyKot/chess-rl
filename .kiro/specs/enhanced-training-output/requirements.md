# Requirements Document - Enhanced Training Output System

## Introduction

This specification addresses the need for a high-quality, user-friendly training output system for the Chess RL Bot. The current training output suffers from excessive noise, inconsistent formatting, missing trend information, and poor user experience. Users need clear, actionable information about training progress without being overwhelmed by spam or debugging information.

The goal is to implement a structured, configurable logging system that provides essential information concisely while offering detailed insights when needed, with proper trend analysis and professional output formatting.

## Requirements

### Requirement 1 - Structured Training Output

**User Story:** As a machine learning engineer, I want structured, consistent training output that shows essential progress information in a clear format, so that I can quickly assess training status and performance trends.

#### Acceptance Criteria

1. WHEN training starts THEN the system SHALL display a config summary once showing profile(s), overrides, and key parameters
2. WHEN each training cycle completes THEN the system SHALL display a one-line summary with cycle number, games played, win/draw/loss ratios, average game length, reward metrics, buffer status, training metrics, duration, and checkpoint events
3. WHEN training completes THEN the system SHALL display a final summary with total cycles, duration, best performance achieved, total games/experiences, and final averages
4. WHEN displaying metrics THEN the system SHALL use consistent decimal formatting (1-2 decimals, consistent locale like 0.25 not 0,25)
5. WHEN showing performance data THEN the system SHALL include trend indicators comparing recent performance to previous periods
6. IF training involves checkpoints THEN checkpoint events SHALL be clearly indicated with performance deltas and best performance tracking

### Requirement 2 - Noise Reduction and Spam Control

**User Story:** As a machine learning engineer, I want training output that eliminates spam and repetitive messages, so that I can focus on important information without being distracted by noise.

#### Acceptance Criteria

1. WHEN multi-process fallback occurs THEN the system SHALL print the suggestion once, not every cycle
2. WHEN displaying phase information THEN the system SHALL show only per-cycle headers and hide "Phase 1/2/3" banners unless verbose mode is enabled
3. WHEN initializing with seeds THEN the system SHALL print exactly one line showing either deterministic OR random mode, not both, with per-component seeds moved to debug level
4. WHEN saving checkpoints THEN the system SHALL ensure no duplicate logging and consistent performance reporting between saves
5. WHEN validation issues occur THEN the system SHALL aggregate per-cycle issues into short bullet lists with repeat counts to avoid spamming identical advice
6. IF the same validation message would repeat THEN the system SHALL show it once with a count rather than repeating the full message

### Requirement 3 - Trend Analysis and Progress Tracking

**User Story:** As a machine learning engineer, I want to see training trends and progress indicators, so that I can understand if training is improving and estimate completion time.

#### Acceptance Criteria

1. WHEN displaying cycle summaries THEN the system SHALL include reward trend showing last-N average vs previous-N average
2. WHEN showing game outcomes THEN the system SHALL display win/draw/loss trends using visual indicators (arrows or sparklines)
3. WHEN reporting training metrics THEN the system SHALL show loss and gradient norm trends using smoothed values
4. WHEN training is in progress THEN the system SHALL provide ETA for remaining cycles based on moving average of cycle duration
5. WHEN best performance improves THEN the system SHALL show previous best performance and delta improvement
6. IF performance metrics are available THEN trend indicators SHALL use appropriate symbols (↑↓→) or simple text indicators

### Requirement 4 - Configurable Output Modes

**User Story:** As a machine learning engineer, I want configurable output verbosity and formats, so that I can choose the appropriate level of detail for different use cases.

#### Acceptance Criteria

1. WHEN using the system THEN it SHALL support --log-level with options: info (default), warn, debug
2. WHEN using --log-interval N THEN the system SHALL show detailed blocks every N cycles with one-line summaries otherwise
3. WHEN using --summary-only THEN the system SHALL show only final summary and checkpoint results
4. WHEN using --json-logs or --metrics-file THEN the system SHALL write per-cycle metrics as JSON/CSV alongside checkpoints for plotting
5. WHEN in info mode THEN the system SHALL show the ideal one-liner format for each cycle
6. IF verbose mode is enabled THEN the system SHALL show detailed blocks with episode metrics, training details, Q-value stats, efficiency metrics, and validation information

### Requirement 5 - Professional Output Formatting

**User Story:** As a machine learning engineer, I want professionally formatted output with consistent styling and clear information hierarchy, so that the training logs are easy to read and understand.

#### Acceptance Criteria

1. WHEN displaying cycle information THEN the system SHALL use the format: "Cycle X/Y | games=Z | win/draw/loss=A/B/C | avgLen=D.E | reward=F.GH | batches=I | loss=J.KL | grad=M.NO | buf=P.Qk/Rs | T.Us | bestΔ=±V.WXY (saved)"
2. WHEN showing detailed blocks THEN the system SHALL include episode metrics (range, avg length, termination breakdown), training metrics (batch updates, loss, grad norm, entropy), Q-value statistics (mean, range, variance), and efficiency metrics (games/sec, total counts)
3. WHEN displaying validation information THEN the system SHALL show only new or changed issues since the last detailed block
4. WHEN formatting numbers THEN the system SHALL use consistent decimal places and locale formatting throughout
5. WHEN showing checkpoint information THEN the system SHALL log exact paths once and show clear performance deltas
6. IF profile composition is used THEN the system SHALL show which domain profiles were applied and list CLI overrides clearly

### Requirement 6 - Evaluation and Play Output Enhancement

**User Story:** As a machine learning engineer, I want clear, informative output during evaluation and play sessions, so that I can understand agent performance and game progression.

#### Acceptance Criteria

1. WHEN running evaluation THEN the system SHALL include actual average game length (not hardcoded 0.0), color alternation information, and confidence intervals for win-rate if possible
2. WHEN comparing agents THEN the system SHALL summarize statistical significance and performance deltas clearly
3. WHEN in play mode THEN the system SHALL show a concise header with profile used and max steps, then display moves cleanly with engine diagnostics hidden unless verbose
4. WHEN evaluation completes THEN the system SHALL provide clear summary of results with statistical confidence measures
5. WHEN games are played THEN the system SHALL track and report actual step counts for accurate average game length calculation
6. IF evaluation involves multiple opponents THEN results SHALL be clearly separated and summarized by opponent type

### Requirement 7 - Bug Fixes and Data Accuracy

**User Story:** As a machine learning engineer, I want accurate training metrics and bug-free output, so that I can trust the information displayed and make informed decisions.

#### Acceptance Criteria

1. WHEN calculating average game length THEN the system SHALL track actual step counts instead of using hardcoded values
2. WHEN saving checkpoints THEN the system SHALL avoid duplicate "Saved regular checkpoint" messages and ensure consistent performance reporting
3. WHEN initializing seeds THEN the system SHALL show only the active seeding mode (deterministic OR random) with proper guards in SeedManager
4. WHEN displaying performance metrics THEN all values SHALL be calculated from actual training data, not placeholder values
5. WHEN reporting buffer utilization THEN the system SHALL show accurate current size vs maximum capacity
6. IF checkpoint saving occurs multiple times THEN the final checkpoint SHALL be logged as a distinct finalization step to avoid confusion

### Requirement 8 - Aggregated Validation and Error Reporting

**User Story:** As a machine learning engineer, I want consolidated validation messages and error reporting, so that I can quickly identify and address issues without being overwhelmed by repetitive warnings.

#### Acceptance Criteria

1. WHEN validation issues occur THEN the system SHALL aggregate identical issues with repeat counts rather than showing each occurrence
2. WHEN displaying validation results THEN the system SHALL present issues as a concise bullet list with clear action items
3. WHEN errors occur THEN the system SHALL provide clear, actionable error messages with suggested solutions
4. WHEN the same validation advice would repeat THEN the system SHALL increment a counter and show the advice once with the count
5. WHEN multiple validation categories have issues THEN the system SHALL group them logically and prioritize by severity
6. IF validation passes without issues THEN the system SHALL indicate successful validation concisely without verbose "all good" messages

### Requirement 9 - Metrics Export and Analysis Support

**User Story:** As a machine learning engineer, I want to export training metrics in structured formats, so that I can create visualizations and perform detailed analysis of training progress.

#### Acceptance Criteria

1. WHEN --json-logs flag is used THEN the system SHALL write per-cycle metrics as JSON files alongside checkpoints
2. WHEN --metrics-file flag is used THEN the system SHALL export metrics in CSV format suitable for plotting and analysis
3. WHEN exporting metrics THEN the system SHALL include all key performance indicators: rewards, win rates, loss values, gradient norms, buffer utilization, and timing information
4. WHEN metrics are exported THEN the system SHALL use consistent field names and data types across export formats
5. WHEN training completes THEN exported metrics SHALL include summary statistics and final performance measures
6. IF metrics export is enabled THEN the system SHALL ensure exported data matches what is displayed in the console output

### Requirement 10 - Backward Compatibility and Migration

**User Story:** As a machine learning engineer, I want the enhanced output system to work with existing training configurations and workflows, so that I can adopt the improvements without breaking current processes.

#### Acceptance Criteria

1. WHEN using existing training commands THEN the system SHALL work with current CLI interfaces and configuration files
2. WHEN legacy log parsing tools are used THEN essential information SHALL remain accessible in a compatible format
3. WHEN migrating from old output THEN the system SHALL provide clear documentation on format changes and new features
4. WHEN using default settings THEN the system SHALL provide improved output without requiring configuration changes
5. WHEN advanced features are needed THEN the system SHALL offer new flags and options without breaking existing usage
6. IF existing automation depends on log parsing THEN the system SHALL maintain key output patterns or provide migration guidance