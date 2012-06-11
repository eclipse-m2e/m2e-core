Maven integration with Eclipse JDT Annotation Processor Toolkit.
================================================================

m2e-apt aims at providing automatic Annotation Processing configuration in Eclipse based on your project's pom.xml and its classpath dependencies  (Requires Java >= 1.6). 

Due to some rogue annotation processors at large, we decided to disable automatic annotation processing by default. By default, your manual settings for Eclipse JDT APT apply.

Go to Window > Preferences > Maven > Annotation processing or right-click on your project > Properties > Maven > Annotation processing to select the Annotation Processing strategy of your choice.

m2e-apt supports both Annotation Processing set on the [maven-compiler-plugin](http://maven.apache.org/plugins/maven-compiler-plugin/index.html) or the [maven-processor-plugin](http://code.google.com/p/maven-annotation-plugin/) (the latter takes precedence over the former).

When Annotation Processing is managed by Eclipse JDT APT, any change in your source classes triggers incremental processing automatically. The downside of using JDT APT is, there's no separation between main and test classes (the way maven-processor-plugin handles them).
To mitigate that limitation, you can change the workspace or project preferences to delegate annotation processing to maven, instead of JDT APT. This will result in slower incremental builds (all classes will be processed) but will provide identical results to maven command line builds. Note this only works when using maven-processor-plugin. When annotation processing is driven by maven-compiler-plugin, JDT APT is always used in eclipse.

See this [blog post](https://community.jboss.org/en/tools/blog/2012/05/20/annotation-processing-support-in-m2e-or-m2e-apt-100-is-out) for more informations. 

m2e-apt can be installed from :

* the m2e discovery catalog : Window > Preferences > Maven > Discovery > Open Catalog
* the Stable update site :  http://download.jboss.org/jbosstools/updates/m2e-extensions/m2e-apt
* the Dev builds update site : http://download.jboss.org/jbosstools/builds/staging/m2e-apt/all/repo/

