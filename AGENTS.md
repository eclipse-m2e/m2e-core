# Eclipse m2e Copilot Instructions

Eclipse m2e (Maven Integration for Eclipse) is an Eclipse IDE plugin that integrates Apache Maven into the Eclipse workspace.
It is built as an OSGi/Eclipse plugin project using **Tycho** (Maven extension for building Eclipse bundles and features).

## Build Commands

```bash
# Full build (compile + unit tests, no integration tests)
mvn clean verify

# Skip all tests
mvn clean verify -DskipTests

# Include integration tests (m2e-core-tests submodule)
mvn clean verify -Pits

# Build a single module (e.g., only the core bundle)
mvn clean verify -pl org.eclipse.m2e.core

# Build a module and its dependencies
mvn clean verify -am -pl org.eclipse.m2e.core.tests
```

Tests run inside a live Eclipse workbench via Tycho Surefire.
On Linux, tests require a display — the CI uses `xvfb`.
Use `DISPLAY=:1 mvn ...` locally if you have Xvfb running.

Running a **single test class** can be done by building only its bundle module with `-pl` and filtering with `-Dtest=MyTestClass` (Tycho Surefire supports `-Dtest`).

## Architecture

### Module Structure

Each directory is a self-contained OSGi bundle (Eclipse plug-in).
Naming conventions:

| Pattern | Purpose |
| --------------------------------- | -------------------------------------------------- |
| `org.eclipse.m2e.core`            | Core OSGi bundle: Maven embedding, project registry, lifecycle mapping |
| `org.eclipse.m2e.core.ui`         | Core UI (SWT/JFace): wizards, preferences, workspace actions |
| `org.eclipse.m2e.jdt`             | JDT integration: classpath containers, Java project configurator |
| `org.eclipse.m2e.launching`       | Maven launch support |
| `org.eclipse.m2e.editor`          | pom.xml form editor |
| `org.eclipse.m2e.editor.lemminx`  | LemMinX/LSP-based XML editor integration |
| `org.eclipse.m2e.pde.*`           | PDE (Eclipse plugin dev) connector and target platform support |
| `org.eclipse.m2e.apt.*`           | Annotation processing support |
| `org.eclipse.m2e.maven.runtime`   | Embeds a specific Apache Maven version for use at runtime |
| `org.eclipse.m2e.tests.common`    | Shared base classes and helpers for all test bundles |
| `*.tests`                         | Unit/integration test bundles (Fragment-Host or standalone) |
| `*.feature`                       | p2 feature descriptors |
| `org.eclipse.m2e.repository`      | p2 update site aggregator |
| `target-platform/`                | `.target` files defining all Eclipse/OSGi dependencies |
| `m2e-core-tests/`                 | Git submodule with additional integration tests (activated by `-Pits`) |

### Key Concepts

**Entry points:**
- `MavenPlugin` — static facade providing access to all core services (project registry, Maven embedder, configuration, etc.).
  Backed by `MavenPluginActivator` which is the OSGi `BundleActivator`.
- `IMaven` — OSGi DS service for interacting directly with the embedded Maven (model reading, dependency resolution, execution).
- `IMavenProjectRegistry` / `IMavenProjectFacade` — Eclipse workspace view of Maven projects; the facade wraps a `MavenProject`.
- `AbstractProjectConfigurator` — extension point base class; implementations configure Eclipse projects from Maven plugin executions (e.g., JDT classpath, APT settings).

**Lifecycle mapping:**
Maven build lifecycle phases are mapped to Eclipse build operations via `lifecycle-mapping-metadata.xml` files in bundle roots and the `org.eclipse.m2e.core.lifecycleMappings` / `projectConfigurators` extension points in `plugin.xml`.

**OSGi Declarative Services (DS):**
Components are declared in `OSGI-INF/*.xml` files (hand-authored, not annotation-processed).
The `MANIFEST.MF` `Service-Component:` header lists them.
Some newer code uses `@Component` / `@Reference` annotations; most existing code uses XML.

**Target platform:**
All Eclipse/OSGi dependencies are resolved from `target-platform/target-platform.target` and `m2e-runtimes.target`.
Do not add Maven `<dependency>` elements for Eclipse platform bundles; use `Require-Bundle` or `Import-Package` in `MANIFEST.MF`.

## Key Conventions

### OSGi Bundle Metadata

Every bundle's public API, internal packages, and friend-accessible internals are declared in `MANIFEST.MF`:

```
Export-Package: org.eclipse.m2e.core,                     # public API
 org.eclipse.m2e.core.internal;x-friends:="...",          # visible to listed bundles
 org.eclipse.m2e.core.internal.builder;x-internal:=true,  # private
```

New packages must be explicitly exported.
Do not add classes to the default package.

### Version Bumping

OSGi Semantic Versioning applies per bundle — bump only the bundle(s) you actually change, and only once per release cycle:

- Bugfix / internal change: `+0.0.100` (micro)
- New API (addition): `+0.1.0` (minor)
- API break: `+1.0.0` (major — discuss on mailing list first)

The `tycho-baseline-plugin` enforces this at build time against the latest release.
If your binary output changes but the version was already bumped, add a note to `forceQualifierUpdate.txt` in the bundle root.
**Never** bump the parent `m2e-parent` pom version or bundles you did not change.

### Testing

All tests run inside a live Eclipse workbench (Tycho Surefire UI harness).
Test classes extend `AbstractMavenProjectTestCase` from `org.eclipse.m2e.tests.common`.

Key helpers inherited from the base class:
- `importProject("resources/projects/my-project/pom.xml")` — imports a Maven project from the test bundle's `resources/` folder into the test workspace.
- `waitForJobsToComplete(monitor)` — waits for all background Eclipse jobs to finish before asserting results.
- `monitor` field — a pre-created `IProgressMonitor`.

Test Maven projects live under `resources/projects/` inside the test bundle directory.

Test bundles are typically declared as `Fragment-Host` of the bundle under test (so they share its classloader and can access package-private code).

### Copyright Header

Every Java source file must start with:

```java
/*******************************************************************************
 * Copyright (c) <year> <Author> and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
```

### Java

- Minimum Java version: **Java 21** (`JavaSE-21` in `MANIFEST.MF`).
- Use public or package-private top-level types instead of inner classes, interfaces, records, or enums where possible.
- Logging is done via SLF4J (`LoggerFactory.getLogger(MyClass.class)`), not `System.out` or Eclipse `ILog`.

### `plugin.xml` and Extension Points

New extension point contributions go into `plugin.xml` of the contributing bundle.
Extension point *definitions* (schemas) go into the `schema/` directory of the defining bundle.
Changes to marker IDs must be mirrored between `org.eclipse.m2e.core/plugin.xml` and `org.eclipse.m2e.core.ui/plugin.xml` (noted in comments there).
