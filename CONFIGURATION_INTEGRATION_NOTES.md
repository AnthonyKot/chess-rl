# Configuration System Integration Notes

## Current Status

The central configuration system has been implemented with the following architecture:

### Multiplatform Design
- **ConfigParser** (commonMain): Handles built-in profiles, CLI parsing, and direct YAML/JSON content parsing
- **JvmConfigParser** (jvmMain): Integrates with existing ProfilesLoader for actual file-based profile loading

### Integration with Existing System
- **ProfilesLoader** (existing): Handles the nested YAML structure in profiles.yaml
- **JvmConfigParser** (new): Bridges ProfilesLoader output to ChessRLConfig
- **ConfigParser** (new): Provides fallback built-in profiles and CLI parsing

## Known Limitations and TODOs

### 1. File Loading Architecture
**Current State**: 
- ConfigParser.loadProfileFromFile() is a placeholder that falls back to built-in profiles
- JvmConfigParser.loadProfileFromFile() provides actual file loading via ProfilesLoader

**Reason**: ConfigParser is in commonMain for multiplatform support, but file I/O is JVM-specific

**Future Enhancement**: 
- Consider expect/actual pattern for platform-specific file loading
- Or document that file loading should use JvmConfigParser on JVM platform

### 2. YAML Parsing Duplication
**Current State**:
- ProfilesLoader has robust YAML parsing for nested structure
- ConfigParser has basic YAML parsing for flat content

**Integration Plan**:
- JvmConfigParser bridges the gap by using ProfilesLoader then converting to ChessRLConfig
- ConfigParser YAML parsing is for direct content, not file-based profiles

### 3. Parameter Mapping
**Current State**:
- New ChessRLConfig uses 19 essential parameters
- Existing profiles.yaml may have legacy parameter names
- JvmConfigParser includes legacy parameter mapping (episodes → maxCycles, etc.)

**Migration Strategy**:
- JvmConfigParser handles backward compatibility
- New profiles should use ChessRLConfig parameter names
- Legacy profiles continue to work via parameter mapping

## Usage Patterns

### For New Code (Recommended)
```kotlin
// Use JvmConfigParser for file-based profiles
val config = JvmConfigParser.loadProfile("fast-debug")

// Use ConfigParser for CLI and built-in profiles
val config = ConfigParser.parseArgs(args)
```

### For CLI Integration
```kotlin
// Parse CLI args first, then override with profile if specified
var config = ConfigParser.parseArgs(args)
val profileName = extractProfileFromArgs(args)
if (profileName != null) {
    config = JvmConfigParser.loadProfile(profileName)
    // Re-apply CLI overrides on top of profile
    config = ConfigParser.parseArgs(args, baseConfig = config)
}
```

### For Testing
```kotlin
// Use built-in profiles for consistent testing
val config = ConfigParser.loadProfile("fast-debug")

// Or create custom configs programmatically
val config = ChessRLConfig().forFastDebug().withSeed(12345L)
```

## Migration Path

### Phase 1: Current Implementation ✅
- ChessRLConfig with 19 essential parameters
- ConfigParser with built-in profiles and CLI parsing
- JvmConfigParser with ProfilesLoader integration
- Backward compatibility for legacy parameter names

### Phase 2: Integration (Next Steps)
- Update existing CLI code to use JvmConfigParser for file loading
- Update training pipeline to use ChessRLConfig instead of TrainingConfiguration
- Add expect/actual pattern for multiplatform file loading if needed

### Phase 3: Cleanup (Future)
- Remove old TrainingConfiguration once fully migrated
- Simplify profiles.yaml to use new parameter names
- Remove legacy parameter mapping once migration is complete

## Testing Strategy

### Unit Tests ✅
- ChessRLConfig validation and methods
- ConfigParser CLI parsing and built-in profiles
- JvmConfigParser profile data parsing and integration

### Integration Tests (Needed)
- End-to-end profile loading from actual profiles.yaml
- CLI integration with profile loading
- Training pipeline integration with new configuration system

### Backward Compatibility Tests (Needed)
- Verify existing profiles.yaml still works
- Verify legacy parameter names are mapped correctly
- Verify CLI compatibility with existing scripts

## Risk Mitigation

### 1. Misleading Method Names
**Risk**: ConfigParser.loadProfileFromFile() doesn't actually read files
**Mitigation**: 
- Clear documentation and TODO comments
- JvmConfigParser provides actual file loading
- Consider renaming to loadBuiltInProfile() in future

### 2. YAML Parsing Inconsistency
**Risk**: ConfigParser YAML parsing doesn't handle nested structure
**Mitigation**:
- JvmConfigParser uses ProfilesLoader for file-based profiles
- ConfigParser YAML parsing is for direct content only
- Clear documentation of usage patterns

### 3. Parameter Mapping Complexity
**Risk**: Legacy parameter mapping adds complexity
**Mitigation**:
- Comprehensive tests for parameter mapping
- Clear migration path documented
- Backward compatibility maintained during transition

## Recommendations

1. **Use JvmConfigParser for production code** that needs file-based profile loading
2. **Use ConfigParser for CLI parsing** and built-in profiles
3. **Document the architecture clearly** in code and README
4. **Plan migration timeline** for existing code to use new configuration system
5. **Add integration tests** to verify end-to-end functionality
6. **Consider expect/actual pattern** for true multiplatform file loading in future
