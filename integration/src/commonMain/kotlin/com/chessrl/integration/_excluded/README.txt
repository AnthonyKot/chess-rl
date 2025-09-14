These files were temporarily excluded to unblock core compilation and alpha training:

- TrainingVisualizationSystem.kt (dashboard + visualization types conflicted with shared models)
- AdvancedTrainingController.kt (referenced DeterministicTrainingController and other drifting APIs)
- TrainingControlInterfaceDemo.kt (depended on InteractiveTrainingDashboard and advanced controller)

Return plan (see TODO.md):
- Re-enable after core metrics stabilize; align to consolidated data classes in SharedDataClasses.kt.
- Ensure exhaustive enums; remove mismatched named params; wire to real metrics.
