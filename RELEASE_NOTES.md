# Eclipse m2e - Release notes

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

m2e mavenarchiver connector so far part of some external repository were migrated into m2e and are shipped by default with m2e installations.

### Stop caching of Maven-projects for legacy clients

For for clients that request setup MojoExecution outside of MavenBuilder context the MavenProject is not longer cached any more.
In general MojoExecutions should be set up within the scope of `MavenExecutionContext`.

### Multiple API breakage

This major release improve (and cleans up) various legacy APIs. Some clients may require to update their code if they use removed APIs. [This commit](https://github.com/eclipse-m2e/m2e-wtp/commit/0705044047ec83124f7f3905431d0027ad4112e8) can be used as an example of how to adapt to newer APIs. Usually, calling `mavenProjectFacade.createExecutionContext().execute(...)` is a good replacement to removed APIs.

## 1.20.1

* 📅 Release Date: March 04th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.20.0...1.20.1

### Sign *.jnilib files for macOS ###

Embedded *.jnilib files are now signed for macOS to fulfill macOS-notarization requirements.


## 1.20.0

* 📅 Release Date: February 11th 2022
* All changes: https://github.com/eclipse-m2e/m2e-core/compare/1.19.0...1.20.0

📢 This is most probably the last 1.x release of m2e. m2e will then start working on a 2.0 version, with some disruptive changes. If you're using m2e in your application and rely on its API, please consider following closely future development to facilitate integration of further versions.

#### Automatically launch and attach Remote-Application-Debugger when Maven plug-in starts a forked JVM that waits for a debugger

If a Maven plug-in like `Maven-Surefire`, `Tycho-Surefire` or `Tycho-Eclipserun` launches a separate ("forked") JVM process in debug mode, so that it waits for a debugger being attached and prints a line like `Listening for transport dt_socket at address: <port-number>`,
M2E now detects such lines and automatically launches a `Remote-Application-Debugger` in the context of the workspace project being build at that time and using the printed port. As usual the debugger stops at encountered break-points so one can step through the code or analyze the state of variables.

For example to debug a test executed by the Maven-surefire plug-in within a Maven build, one only has to specify the property `maven.surefire.debug=true` in the Maven launch-configuration and the debugger will just stop at the break-points in the executed tests:

![grafik](https://user-images.githubusercontent.com/44067969/152249878-c0e0e5bd-1a72-4772-8554-75d97be3bc33.png)

Further information, how to activate the debug mode of forked JVMs for the plug-ins mentioned above, can be found at the following links:
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

In the wizard it is now possible to request generation of a feature

![grafik](https://user-images.githubusercontent.com/1331477/139412713-e0218ff7-642c-4c19-afac-55fca49ef325.png)

If the option is checked the wizard contains a new page to enter basic infos

![grafik](https://user-images.githubusercontent.com/1331477/139412847-5268aaae-de32-472c-b30e-44a8e88728cd.png)

If one likes he can add additional plugins as well that should be mentioned in the feature (but this step is optional)

![grafik](https://user-images.githubusercontent.com/1331477/139413014-ff0463e2-cd89-41c4-89dd-fe4402be05b2.png)

Afterwards this will end up in the target source and one can add/edit/adjust additional data, effectivly everything the feature.xml supports is allowed here:

![grafik](https://user-images.githubusercontent.com/1331477/139413236-f04d9b5f-54a5-4240-b83f-86167c7519b2.png)

#### Include and use Maven 3.8.4

Maven 3.8.4 is now used internally in m2e. This allows to benefit from various improvements of recent Maven versions

#### Improved LemMinX-based editor with newer LemMinX-Maven

LemMinX-Maven 0.5.1 is now used and provide many major improvements. A noticeable one is the search.maven.org engine is now used instead of the indexer. This will greatly improve the "warmup" time of the editor. Other bugfixes and performance improvements have a very positive and visible impact on the user experience when using the Generic Editor.

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

```
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

```
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

#### Multiple fixes and improvement in LemMinX based editor

With upgrade to newer LemMinX-Maven, the edition of pom.xml with the Generic Editor receives several comfortable fixes and improvements.

#### One way synchronization for jpms directives from maven compiler arguments to .classpath file

Extract jpms arguments (--add-exports,--add-opens,--add-reads,--patch-module) from the maven-compiler-plugin compiler arguments if any.
Dispatch the arguments in the right container (if the target module is part of JRE then in JreContainer else in M2eContainer) and
transform them into eclipse classpath attributes (add-exports, add-opens, add-reads, patch-module)

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
