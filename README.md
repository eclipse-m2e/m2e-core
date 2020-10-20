Maven integration with Eclipse JDT Annotation Processor Toolkit.
================================================================

[![Build Status](https://travis-ci.org/jbosstools/m2e-apt.svg?branch=master)](https://travis-ci.org/jbosstools/m2e-apt)

m2e-apt aims at providing automatic Annotation Processing configuration in Eclipse based on your project's pom.xml and its classpath dependencies. m2e-apt requires Java >= 1.8 to run, but will work on Java 6 projects.

Due to some rogue annotation processors at large, we decided to disable automatic annotation processing by default. By default, your manual settings for Eclipse JDT APT apply.

Go to `Window > Preferences > Maven > Annotation Processing` or right-click on your project `Properties > Maven > Annotation Processing` to select the Annotation Processing strategy of your choice.

m2e-apt supports both Annotation Processing set on the [maven-compiler-plugin](http://maven.apache.org/plugins/maven-compiler-plugin/index.html) or the [maven-processor-plugin](http://code.google.com/p/maven-annotation-plugin/) (the latter takes precedence over the former).

When Annotation Processing is managed by Eclipse JDT APT, any change in your source classes triggers incremental processing automatically, however subtle differences in Annotation classpath might lead to unexpected results. Moreover, main and test sources separation only works in Eclipse Photon or more recent versions. 
You can change the workspace or project preferences to delegate annotation processing to maven, instead of JDT APT. This will result in slower incremental builds (all classes will be processed) but will provide identical results to maven command line builds. Note this only works when using maven-processor-plugin. When annotation processing is driven by maven-compiler-plugin, JDT APT is always used in eclipse.

See this [blog post](https://community.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out) for more informations.

Default m2e-apt activation mode (from workspace preferences) can be overridden by an `<m2e.apt.activation>` Maven property in a project's pom.xml `settings` section. Valid values are :

* `disabled` : m2e-apt activation is disabled for this project
* `jdt_apt` : enable m2e-apt on this project and delegate Annotation Processing to JDT APT
* `maven_plugin` : enable m2e-apt on this project and delegate Annotation Processing to the proper maven plugin execution
Project preferences supercede existing pom properties.

The toolkit uses Eclipse JDK, that's why additional configuration may needed to be done for legacy projects which depend on outdated JDK.
For example in Eclipse 2020-09 (which runs on JDK11+ only), in case a project needing m2e-apt is using JDK8, additional annotation processing paths must be added to the project's pom.xml for successful compilation
(to the plugin's `annotationProcessorPaths` section):
```
<path>
	<groupId>jakarta.xml.bind</groupId>
	<artifactId>jakarta.xml.bind-api</artifactId>
	<version>2.3.3</version>
</path>
<path>
	<groupId>javax.annotation</groupId>
	<artifactId>jsr250-api</artifactId>
	<version>1.0</version>
</path>
```


m2e-apt can be installed from :

* the m2e discovery catalog : Window > Preferences > Maven > Discovery > Open Catalog
* the Stable update site :  http://download.jboss.org/jbosstools/updates/m2e-extensions/m2e-apt
* the Dev builds update site : http://download.jboss.org/jbosstools/photon/snapshots/builds/m2e-apt/latest/all/repo/

m2e-apt **requires [m2e 1.5+](http://download.eclipse.org/technology/m2e/releases/)**
