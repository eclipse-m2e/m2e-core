# Eclipse m2e - Release notes

## 2.7.0

* 📅 Release Date: 26th November 2024

Various minor bug-fixes, enhancements and dependency updates.

### Require Java 21 or above

Running m2e now requires a Java-21 (or higher) compliant JRE.

## 2.6.2

* 📅 Release Date: 04th September 2024

### Embedded and use Maven 3.9.9

Updated the embedded Maven from version 3.9.7 to 3.9.9; [Maven 3.9.9 Release Notes](https://maven.apache.org/docs/3.9.9/release-notes.html).

### Surefire/Failsafe plugin configuration propagated to Junit/TestNG launch configuration

The following arguments are supported:
* `<argLine>`
* `<environmentVariables>`
* `<systemPropertyVariables>`
* `<workingDirectory>`
* `<enableAssertions>`

Configuration is propagated on unit test launch configuration creation and also when executing `Maven > Update Project`.

## 2.6.1

* 📅 Release Date: 04th June 2024

Various minor bug-fixes, enhancements and dependency updates.

### Embedded and use Maven 3.9.7

Updated the embedded Maven from version 3.9.6 to 3.9.7; [Maven 3.9.7 Release Notes](https://maven.apache.org/docs/3.9.7/release-notes.html).

## 2.6.0

* 📅 Release Date: 21th February 2024

### Embedded and use Maven 3.9.6

Updated the embedded Maven from version 3.9.5 to 3.9.6; [Maven 3.9.6 Release Notes](https://maven.apache.org/docs/3.9.6/release-notes.html).

### Improved toolchain.xml handling

In the Preferences under `Maven > User Settings` the user `toolchain.xml` used in workspace builds can now be specified explicitly.
At the same time the `maven-toolchains-plugin` is now disabled by default for workspace builds.

### Improved resource linking

Source or resource folders of Maven-projects outside of the project's base directory are now considered in the workspace and are added to the project as linked resources.

### Project preference for automated Maven project configuration updates

Automatic configuration updates for Maven projects can now be disabled in the project preferences.
This allows to disable these updates individually per project and to store the setting in a preference file under version control,
which is useful for projects that require special workspace configuration that doesn't exactly match the configuration in the `pom.xml`.

![grafik](https://github.com/eclipse-m2e/m2e-core/assets/44067969/7d27ceda-5d13-4f0e-97f0-ff34c94d7493)

### Support of global and user settings in .mvn/maven.config

The `.mvn/maven.config` allows to specify [global and user settings](https://maven.apache.org/settings.html#quick-overview), m2e now also takes these into account.

This improvement was gently sponsored by <img src="https://www.sigasi.com/img/logoSquare.png"  width="16" height="16"> [Sigasi](https://www.sigasi.com/). 

## 2.5.0

* 📅 Release Date: 27th November 2023

### Embedded and use Maven 3.9.5

Updated the embedded Maven from version 3.9.4 to 3.9.5; [Maven 3.9.5 Release Notes](https://maven.apache.org/docs/3.9.5/release-notes.html).

### Dropped aether-okhttp-connector

Previously, m2e embedded [`aether-okhttp-connector`](https://github.com/takari/aether-connector-okhttp), an alternative to Wagon HTTP connector, based on [OkHttp](https://square.github.io/okhttp/), which was developed at a time when Maven 2's HTTP Connector didn't leverage HTTP/2 and parallel downloads.

However, the usage of this alternative connector introduced certain inconsistencies when compared to regular Maven CLI builds.
These discrepancies, often revolving around matters of authentication and proxies, posed challenges.
Maven 3.x significantly improved its resolver implementations, largely mitigating the advantages of `aether-okhttp-connector` and bringing new features.
This shift left the `aether-okhttp-connector` outdated and that project has now been abandoned.

m2e 2.4 has been adjusted to better align with the Maven 3.9 runtime.
This adjustment is expected to result in fewer issues pertaining to artifact resolution and proxy authentication.
However, due to its removal from the runtime, there exists a potential risk that third-party Plug-ins dependent on m2e's integrated `OkHttp` functionality might experience disruptions.

## 2.4.0

* 📅 Release Date: 29th August 2023

### Embedded and use Maven 3.9.4

Updated the embedded Maven from version 3.9.1 to 3.9.4; [Maven 3.9.4 Release Notes](https://maven.apache.org/docs/3.9.4/release-notes.html).

### Support for multiple embedded runtimes

You can now install additional embedded Maven Runtimes using the m2eclipse update site

![grafik](https://github.com/eclipse-m2e/m2e-core/assets/1331477/8ef45a1b-e2bf-46e3-909e-275ac3c21510)

After the installation you can now select the desired default runtime, keep in mind that m2e internally still uses the `EMBEDDED` runtime even if another is selected. 

![grafik](https://github.com/eclipse-m2e/m2e-core/assets/1331477/a849a70c-e7a6-4049-ac55-3338596dca7b)

If you want to switch back to an older runtime you currently need to modify you eclipse installation:

- Make sure the older runtime is already installed
- close eclipse (and maybe make a backup)
- go to the `plugins` folder and locate the folders starting with `org.eclipse.m2e.maven.runtime_<version>`
- delete higher runtime versions (e.g. 3.9)
- start eclipse and check that now an older runtime is used by default

![grafik](https://github.com/eclipse-m2e/m2e-core/assets/1331477/ef04e7f4-e36b-4bbc-a4d3-ff92e6a5f9f4)

### m2e.archetype.common changed it structure

_Only relevant for developers of Plug-ins based on m2e_

The single `org.eclipse.m2e.archetype.common` Bundle that used to embed the maven-archetype jars was replaced by a set of multiple bundles, where each corresponds to one of the embedded archetype jars and has reduced OSGi metadata just sufficient for the use with m2e.
One of those bundles is again named `org.eclipse.m2e.archetype.common` and serves as host bundle for the others, which are fragments to it.

## 2.3.0

* 📅 Release Date: 23th May 2023

### Embedded and use Maven 3.9.1

Updated the embedded Maven from version 3.8.7 to 3.9.1; [Maven 3.9.1 Release Notes](https://maven.apache.org/docs/3.9.1/release-notes.html).

### Support for Java 20 and later

Projects that compile with Java 20 and therefore have a corresponding configuration of in their `maven-compiler-plugin` are now supported.
Furthermore the processing has been enhanced to support future Java versions as soon as Eclipse JDT supports them.

### Enhanced M2E Maven Target support

* OSGi metadata generated for artifacts that don't contain a OSGi compliant MANIFEST.MF have been enhanced to also contain version ranges for `Import-Package` headers by default.
* Source Bundles of excluded artifacts are now excluded too.

## 2.2.1

* 📅 Release Date: 7th March 2023

### Regression fixes

This release fixes several regressions from the previous release.

## 2.2.0

* 📅 Release Date: 28th February 2023

### Mojos without a mapping are now executed by default in incremental builds

Previously, to participate in the incremental maven build it was necessary to
* explicitly configure a mapping, or
* there had to be a connector, or
* the plugin itself had to contain a mapping for a mojo
This often leads to a poor user experience and we think that users are adding mojos on purpose because they perform valuable tasks.

Because of this, M2E now automatically enables the execution of mojos if there is no mapping configured. In case you want to change this there is a new configuration option to control the behavior:

![grafik](https://user-images.githubusercontent.com/1331477/211298610-0fa92418-246a-4377-913a-60d02d63013b.png)

### Updated Dependency Editor

The dependencies editor has been adapted to show all artifacts within a target location as a single table, instead of multiple tabs. This change also includes support for updating only a selected number of artifacts to their latest version, as well as undo/redo functionality.

![grafik](https://user-images.githubusercontent.com/70652978/212153011-160fa96a-1c06-4092-9b89-fcd7a3c2859e.png)


### Ignore Test Sources and Resources compilation if `maven.test.skip=true` is specified

The property `<maven.test.skip>true</maven.test.skip>` and the `skip` property in configurations of the `maven-compiler-plugin` and `maven-resources-plugin` are now taken into account by M2E. If enabled, M2E ignores the corresponding folder. It will no longer appear in the Package Explorer as a "Java" folder, but as a regular folder.
This allows, depending on the need (especially compilation time), to either not compile tests or not copy test resources.

In general, it is not recommended to use the mentioned properties but to use `-DskipTests` instead:
https://maven.apache.org/surefire/maven-surefire-plugin/examples/skipping-tests.html


### Configuration of Maven Execution JRE

In the past, the project's build JRE was also used by default to execute Maven itself.
Now the default Java version for executing Maven is determined from the configuration of the `maven-enforcer-plugin` rule [`requireJavaVersion`](https://maven.apache.org/enforcer/enforcer-rules/requireJavaVersion.html) when creating or updating the Maven configuration. This value is no longer considered for configuring the project's build JRE.
In case this plugin configuration is not found, M2E falls back to either the project's build JRE or the workspace's default JRE.

For each Maven build configuration, you can overwrite the default execution JRE in the Maven Launch configuration's JRE tab:

![Maven Launch Configuration JRE Tab](https://user-images.githubusercontent.com/185025/208966517-7d847058-23b9-4e2e-8b1a-7a86df4836bd.png)


## 2.1.0

* 📅 Release Date: November 24th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/2.0.2...2.1.0

### Automatic configuration updates of Maven projects enabled by default

The previously experimental feature of M2E to update Maven projects automatically on configuration changes matured and is now enabled by default.

### Performance and memory consumption improvements
For large projects, the build performance has been improved and the memory consumption has been reduced.

### Improved connectors for bnd-maven-plugin and maven-bundle-plugin

The connector for the `bnd-maven-plugin` and `maven-bundle-plugin`, which is included into M2E, has been improved to consider the jars specified on the `Bundle-Classpath` of the generated MANIFEST.MF, when updating the Plugins JDT `.classpath`.

### Console support for polyglot Maven projects and projects without Maven nature

The M2E Maven-Console now also supports tracking of so called _polyglot_ Maven-projects. Those are projects that don't have a standard `pom.xml` and whose Maven-model is instead created from another source. One prominent example in the Eclipse world are Eclipse-PDE projects that are build with Tycho(-pomless).

Due to this new support, polyglot Maven projects now also benefit from the
[Improved links to JUnit test-reports and project file in the Console](https://github.com/eclipse-m2e/m2e-core/blob/master/RELEASE_NOTES.md#improved-links-to-junit-test-reports-and-project-pomxml-in-the-console-of-a-maven-build)
as well as the capability to
[Automatically launch and attach Remote-Application-Debugger when Maven plug-in starts a forked JVM that waits for a debugger](https://github.com/eclipse-m2e/m2e-core/blob/master/RELEASE_NOTES.md#automatically-launch-and-attach-remote-application-debugger-when-maven-plug-in-starts-a-forked-jvm-that-waits-for-a-debugger) introduced in previous releases.

## 2.0.2

* 📅 Release Date: August 30th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/2.0.1...2.0.2

### Support for colored Maven console printouts

M2E now supports colored console printouts for Maven builds launched in the IDE out of the box.

This is built on top of the new support for colored Console content, which is added to Eclipse-Platform in the 2022-09/4.25 release (which is therefore required).
In the Run/Debug-configuration of a `Maven Build` launch it can be controlled if the printout is colored or not (i.e. the value of Maven's `style.color` property). The default is `Auto`, which enables colored print-outs if colored Console printout is generally enabled in the workspace.

## 2.0.1

* 📅 Release Date: August 5th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.20.1...2.0.1

### Require Java 17 or above

Running m2e now requires a Java-17 (or higher) compliant JRE.

#### Removed legacy WTP-SSE based editor

The legacy textual editor is removed as it doesn't receive enough update/maintenance. The editor based on Generic Editor and LemMinX-Maven is now the only one distributed by m2e.

#### Inclusion of m2e-apt plugins for annotation processors

m2e-apt plugins that were so far included in JBoss Tools were migrated into m2e and are shipped by default with m2e installations. So annotation processing should be better supported out of the box.

#### Inclusion of maven archiver connector

The m2e mavenarchiver connector, so far part of some external repository, was migrated into m2e and is shipped by default with m2e installations.

### Updated embedded maven

Updated the embedded maven from 3.8.4 to 3.8.6.

### Stop caching of Maven-projects for legacy clients

For clients that request setup MojoExecution outside of MavenBuilder context the MavenProject is no longer cached anymore.
In general, MojoExecutions should be set up within the scope of `MavenExecutionContext`.

### Improved support for Maven archetypes

Maven archetypes can use Groovy scripts for the processing of input parameters since Maven 3, which is now also supported via m2e. In addition, validation of parameters with regular expressions is now also supported:

![archetype parameter validation](https://user-images.githubusercontent.com/17798/189828315-2deb2fd4-c310-4e75-a83b-9603acfb4198.png)

Any additional inputs required by the Groovy script are handled in the Eclipse console:

![archetype in Eclipse console](https://user-images.githubusercontent.com/17798/189828638-2bb545b8-bbaf-4d72-a8fb-ba798df9894f.png)

This feature was sponsored by [Faktor Zehn](https://faktorzehn.org)

### Multiple API breakage

This major release improves (and cleans up) various legacy APIs. Some clients may require to update their code if they use removed APIs. [This commit](https://github.com/eclipse-m2e/m2e-wtp/commit/0705044047ec83124f7f3905431d0027ad4112e8) can be used as an example of how to adapt to newer APIs. Usually, calling `mavenProjectFacade.createExecutionContext().execute(...)` is a good replacement for removed APIs.

## 1.20.1

* 📅 Release Date: March 04th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.20.0...1.20.1

### Sign *.jnilib files for macOS ###

Embedded *.jnilib files are now signed for macOS to fulfill macOS-notarization requirements.


## 1.20.0

* 📅 Release Date: February 11th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.19.0...1.20.0

📢 This is most probably the last 1.x release of m2e. m2e will then start working on a 2.0 version, with some disruptive changes. If you're using m2e in your application and rely on its API, please consider following closely future development to facilitate the integration of further versions.

#### Automatically launch and attach Remote-Application-Debugger when Maven plug-in starts a forked JVM that waits for a debugger

If a Maven plug-in like `Maven-Surefire`, `Tycho-Surefire` or `Tycho-Eclipserun` launches a separate ("forked") JVM process in debug mode, so that it waits for a debugger being attached and prints a line like `Listening for transport dt_socket at address: <port-number>`,
M2E now detects such lines and automatically launches a `Remote-Application-Debugger` in the context of the workspace project being build at that time and using the printed port. As usual the debugger stops at encountered break-points so one can step through the code or analyze the state of variables.

For example to debug a test executed by the Maven-surefire plug-in within a Maven build, one only has to specify the property `maven.surefire.debug=true` in the Maven launch-configuration and the debugger will just stop at the break-points in the executed tests:

![grafik](https://user-images.githubusercontent.com/44067969/152249878-c0e0e5bd-1a72-4772-8554-75d97be3bc33.png)

Further information on how to activate the debug mode of forked JVMs for the plug-ins mentioned above, can be found at the following links:
- [Maven-Surefire](https://maven.apache.org/surefire/maven-surefire-plugin/examples/debugging.html)
- [Tycho-Surefire](https://www.eclipse.org/tycho/sitedocs/tycho-surefire-plugin/test-mojo.html#debugPort)
- [Tycho-Eclipserun](https://www.eclipse.org/tycho/sitedocs/tycho-extras/tycho-eclipserun-plugin/eclipse-run-mojo.html#jvmArgs)

#### Improved links to JUnit test-reports and project pom.xml in the Console of a Maven build

Clicking on the link placed at the name of a running test-class now opens the `JUnit` view that displays the test-reports of the executed tests:

![grafik](https://user-images.githubusercontent.com/44067969/152246278-b9066258-fb99-44bf-afd7-795ec0967ca5.png)

For each project build a link is now added to the project's headline, which opens the project's `pom.xml` file when clicked:

![grafik](https://user-images.githubusercontent.com/44067969/152246296-e2feb457-136a-425d-ae04-455a2044cc47.png)

In case of a build failure another link, that opens the `pom.xml` of the failed project, is added to the line that that describes the failure at the very end of the Maven build print-out:

![grafik](https://user-images.githubusercontent.com/44067969/152247987-01ee209a-ad6c-454e-92c1-e2aa75388931.png)

#### the m2e-pde editor now supports generation of a feature from a location:

In the wizard, it is now possible to request the generation of a feature

![grafik](https://user-images.githubusercontent.com/1331477/139412713-e0218ff7-642c-4c19-afac-55fca49ef325.png)

If the option is checked the wizard contains a new page to enter basic info

![grafik](https://user-images.githubusercontent.com/1331477/139412847-5268aaae-de32-472c-b30e-44a8e88728cd.png)

If one likes he can add additional plugins as well that should be mentioned in the feature (but this step is optional)

![grafik](https://user-images.githubusercontent.com/1331477/139413014-ff0463e2-cd89-41c4-89dd-fe4402be05b2.png)

Afterward this will end up in the target source and one can add/edit/adjust additional data, effectively everything the feature.xml supports is allowed here:

![grafik](https://user-images.githubusercontent.com/1331477/139413236-f04d9b5f-54a5-4240-b83f-86167c7519b2.png)

#### Include and use Maven 3.8.4

Maven 3.8.4 is now used internally in m2e. This allows benefiting from various improvements of recent Maven versions

#### Improved LemMinX-based editor with newer LemMinX-Maven

LemMinX-Maven 0.5.1 is now used and provides many major improvements. A noticeable one is the search.maven.org engine is now used instead of the indexer. This will greatly improve the "warmup" time of the editor. Other bugfixes and performance improvements have a very positive and visible impact on the user experience when using the Generic Editor.

#### Other noticeable changes

* Performance boost in parent project resolution https://github.com/eclipse-m2e/m2e-core/commit/ec12bd6222c377f93e21af0dc1988fba2134123d
* Downgrade the "Plugin Execution not covered by lifecycle configuration" error to warning #424 https://github.com/eclipse-m2e/m2e-core/commit/e13899b1345da44fd888d851bb249daefc044d20
* `*.pom` files are treated as regular Maven `pom.xml` files and should now benefit from similar edition assistance
* Overview page in the Pom Editor now also lists the packaging types provided by Maven extensions that are accessible from the current project.

## 1.19.0

* 📅 Release Date: November 22nd 2021
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.18.2...1.19.0

#### Generic Editor with LemMinX-Maven used as default source editor

The legacy WTP-based pom editor is not included by default anymore. The LemMinX-Maven based editor is now referenced by default from the main feature, but as optional, so it can still be uninstalled, and the legacy editor can be manually installed instead.

The new editor brings more powerful pom.xml understanding and edition features; and evolves much faster than the WTP-based one.

## 1.18.2

* 📅 Release Date: October 15th 2021
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.18.1...1.18.2

#### the m2e-pde editor now supports adding more than one dependency per target location:

![grafik](https://user-images.githubusercontent.com/1331477/126075863-ee075afb-c4e1-423d-acc0-8174905378dd.png)

![grafik](https://user-images.githubusercontent.com/1331477/126106751-892626dc-46d5-45a5-841b-beff82085de0.png)

```xml
<target name="multipleElements">
<locations>
	<location includeDependencyScope="compile" includeSource="true" missingManifest="generate" type="Maven">
		<dependency>
			<groupId>org.eclipse.jetty</groupId>
			<artifactId>jetty-server</artifactId>
			<version>11.0.3</version>
			<type>jar</type>
		</dependency>
		<dependency>
			<groupId>org.eclipse.jetty</groupId>
			<artifactId>jetty-servlet</artifactId>
			<version>11.0.3</version>
			<type>jar</type>
		</dependency>
	</location>
</locations>
</target>
```

Old target formats are automatically converted.

#### the m2e-pde editor now supports adding additional maven repoistories for a target location:

![grafik](https://user-images.githubusercontent.com/1331477/126276711-8e42165c-01bd-4d79-a28b-441bbc7c9fc7.png)

```xml
<target name="extraRepository">
	<locations>
		<location includeDependencyScope="compile" includeSource="true" missingManifest="generate" type="Maven">
			<dependencies>
				<dependency>
				  <groupId>edu.ucar</groupId>
				  <artifactId>cdm</artifactId>
				  <version>5.0.0</version>
				</dependency>
			</dependencies>
			<repositories>
				<repository>
					<id>unidata-all</id>
					<url>https://artifacts.unidata.ucar.edu/repository/unidata-all/</url>
				</repository>
			</repositories>
		</location>
	</locations>
</target>
```

#### Multiple fixes and improvements in LemMinX based editor

With the upgrade to the newer LemMinX-Maven, the edition of pom.xml with the Generic Editor receives several comfortable fixes and improvements.

#### One way synchronization for jpms directives from maven compiler arguments to .classpath file

Extract jpms arguments (`--add-exports`, `--add-opens`, `--add-reads`, `--patch-module`) from the `maven-compiler-plugin` compiler arguments if any.
Dispatch the arguments in the right container (if the target module is part of JRE then in JreContainer else in M2eContainer) and
transform them into eclipse classpath attributes (`add-exports`, `add-opens`, `add-reads`, `patch-module`)

#### Improved m2e development workflow

Many improvements happened in m2e to facilitate the setup and maintenance of the development environment. See details in [CONTRIBUTING.md](CONTRIBUTING.md).

### Older releases

## 1.18.1

* 📅 Release Date: June 23rd, 2021
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.18.0...1.18.1

Main changes:
* Use newer guava 30.1
* Code cleanups
* Improve project structure, documentation and other files to ease contributions

## 1.18.0

* 📅 Release Date: June 3rd, 2021
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.17.2...1.18.0


## 1.17.2

* 📅 Release Date: March 2nd, 2021
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.17.1...1.17.2

Main contents:
* Upgrade dependencies and build toolchain: Most noticeably, Guava 30.1 is now used
* Improvements to the LemMinX-Based pom editor, mainly through upgrade to newer Wild Web Developer and LemMinX-Maven releases
* Improvements & fixes to Maven PDE Target Platform location editor
* High-resolution icons [➡️🐛📝](https://bugs.eclipse.org/bugs/show_bug.cgi?id=570473)
* Performance improvement/fix in the Run Configuration with _Verifying launch attributes_ job [➡️🐛📝](https://bugs.eclipse.org/bugs/show_bug.cgi?id=563742)

📝 Release notes for 1.17.1 and former releases are available at https://projects.eclipse.org/projects/technology.m2e/releases/
