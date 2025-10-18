# Maven 4 Upgrade Notes for m2e

This document describes the changes made to upgrade m2e's embedded Maven runtime from Maven 3.9.11 to Maven 4.0.0-rc-4.

## Changes Made

### 1. Maven Runtime Bundle (org.eclipse.m2e.maven.runtime)

#### Version Update
- Updated `maven-core.version` from `3.9.11` to `4.0.0-rc-4`
- Updated bundle version from `3.9.1100-SNAPSHOT` to `4.0.004-SNAPSHOT`

#### Dependency Changes

**Removed Dependencies:**
- `org.fusesource.jansi:jansi` - Replaced with `org.jline:jansi-core` (Maven 4 uses JLine's jansi)
- `org.apache.maven:maven-slf4j-provider` - Replaced with `org.apache.maven:maven-logging`
- `org.apache.maven.resolver:maven-resolver-transport-http` - Replaced with `maven-resolver-transport-jdk`

**Added/Updated Dependencies:**
- `org.apache.maven:maven-logging:4.0.0-rc-4` - New Maven 4 logging module
- `org.apache.maven:maven-compat:4.0.0-rc-4` - Backward compatibility layer for Maven 3 APIs
- `org.jline:jansi-core:3.30.4` - JLine's jansi implementation
- `org.apache.maven.resolver:maven-resolver-transport-jdk:2.0.9` - Java 11+ HTTP client-based transport
- Updated all resolver dependencies to version `2.0.9`
- Updated `com.google.guava:failureaccess` to version `1.0.1`

#### Removed Source Files
- `org.eclipse.m2e.slf4j2.provider.MavenSLF4JProvider` - No longer needed as Maven 4 provides its own SLF4J provider
- `META-INF/services/org.slf4j.spi.SLF4JServiceProvider` - Service registration for the removed provider

#### BND Configuration Updates
- Updated SLF4J binding jar name from `maven-slf4j-provider-${maven-core.version}.jar` to `maven-logging-${maven-core.version}.jar`

### 2. Runtime Target Platform (target-platform/m2e-runtimes.target)

- Updated to reference Maven runtime `3.9.1100.20241011-1318` from m2e release 2.2.1 for backward compatibility
- This allows running tests with both Maven 3.9.11 and Maven 4.0.0-rc-4

### 3. Root POM (pom.xml)

- Updated maven.runtime dependency version from `3.9.1100-SNAPSHOT` to `4.0.004-SNAPSHOT`

## Maven 4 Architecture Changes

### New Modules in Maven 4
Maven 4 introduces several new API modules:
- `maven-api-core` - Core Maven 4 API
- `maven-api-model` - Model API
- `maven-api-plugin` - Plugin API
- `maven-api-settings` - Settings API
- `maven-api-toolchain` - Toolchain API
- `maven-api-xml` - XML processing API
- `maven-di` - Dependency injection module
- `maven-impl` - Implementation module
- `maven-jline` - JLine integration
- `maven-logging` - Logging module (replaces maven-slf4j-provider)

### Backward Compatibility

Maven 4 provides the `maven-compat` module that maintains compatibility with Maven 3 APIs. This includes:
- Support for Plexus Container API
- Legacy Maven APIs from Maven 3.x
- Compatibility layer for plugins built against Maven 3

## Code Compatibility Assessment

### PlexusContainer Usage

The following areas of m2e use PlexusContainer and related Plexus APIs:

1. **org.eclipse.m2e.core.internal.embedder.PlexusContainerManager**
   - Creates and manages PlexusContainer instances
   - Uses DefaultPlexusContainer, ContainerConfiguration
   - **Status**: Should work with maven-compat module

2. **org.eclipse.m2e.core.internal.embedder.IMavenPlexusContainer**
   - Interface for wrapping PlexusContainer
   - **Status**: Should work with maven-compat module

3. **Plexus Utility Classes**
   - `org.codehaus.plexus.util.xml.Xpp3Dom` - Used extensively for plugin configuration
   - `org.codehaus.plexus.util.dag.*` - DAG utilities for dependency ordering
   - `org.codehaus.plexus.util.Scanner` - Build context scanning
   - `org.codehaus.plexus.classworlds.ClassRealm` - ClassLoader management
   - **Status**: These are still available in Maven 4 (plexus-utils, plexus-classworlds)

4. **org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase**
   - Test utilities that lookup PlexusContainer
   - **Status**: Should work with maven-compat module

5. **org.eclipse.m2e.core.ui.internal.archetype.ArchetypePlugin**
   - Uses DefaultPlexusContainer for archetype operations
   - **Status**: Should work with maven-compat module

## Testing Requirements

The following areas should be tested to ensure Maven 4 compatibility:

### Core Functionality
1. [ ] Project import from existing Maven projects
2. [ ] POM editing and validation
3. [ ] Dependency resolution
4. [ ] Plugin execution during builds
5. [ ] Workspace resolution
6. [ ] Maven lifecycle execution
7. [ ] External Maven launches

### PlexusContainer Compatibility
1. [ ] Container creation and disposal
2. [ ] Component lookup
3. [ ] Extension loading from .mvn/extensions.xml
4. [ ] ClassRealm management
5. [ ] Plugin realm creation

### Build and Test Infrastructure
1. [ ] Maven runtime bundle builds successfully
2. [ ] All m2e modules compile against Maven 4 APIs
3. [ ] Unit tests pass
4. [ ] Integration tests pass
5. [ ] UI tests pass

### Maven 4 Specific Features
1. [ ] New Maven 4 API usage (if any)
2. [ ] Maven 4 resolver transport (JDK HTTP client)
3. [ ] Maven 4 logging integration with Eclipse
4. [ ] Maven 4 dependency injection (Guice/Sisu)

## Known Issues and Limitations

### Build Environment
- The current build requires access to repo.eclipse.org which may not be available in all environments
- Tycho 4.0.13 may have issues with JavaSE-21 execution environment

### Potential API Changes
The following Maven APIs may have changed between Maven 3 and Maven 4:
1. **Resolver APIs** - Updated from 1.x to 2.x
   - New transport mechanism (JDK HTTP client vs Apache HttpClient)
   - May affect custom repository configurations

2. **Logging** - New maven-logging module
   - Uses SLF4J 2.x
   - May affect log filtering and configuration

3. **Plugin API** - New maven-api-plugin module
   - May affect plugin descriptor reading
   - May affect mojo parameter injection

## Recommendations

### Short Term
1. Complete full build and test cycle to identify any runtime issues
2. Test with real-world projects to validate Maven 4 compatibility
3. Document any API incompatibilities found during testing
4. Consider keeping Maven 3.9.11 runtime as an alternative for users

### Long Term
1. Consider migrating to Maven 4 APIs where beneficial
2. Deprecate Maven 3-specific workarounds that are no longer needed
3. Take advantage of Maven 4's improved dependency injection
4. Explore Maven 4's new features (e.g., build cache, consumer POM)

## References

- Maven 4.0.0-rc-4 Release: https://repo.maven.apache.org/maven2/org/apache/maven/maven/4.0.0-rc-4/
- Maven Resolver 2.0.9: https://maven.apache.org/resolver/
- Maven 4 Migration Guide: https://maven.apache.org/docs/4.0.0/
