# Eclipse m2e - Release notes

### Next release

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
