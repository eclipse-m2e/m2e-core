# How to... develop and debug m2e and lemminx-maven integration

This document gathers answers to main questions about technical process to troubleshot or improve m2e and lemminx-maven integration

## How to try getting more information logged of lemminx-maven while developing m2e

XML Language Server and Lemminx-maven extension used in Lemminx POM Editor are able to log more information than only severe errors and exceptions that may occur during the Editor work.

See [Enable java.util.logging in the XML Language Server process](https://github.com/eclipse/wildwebdeveloper/blob/master/TIPS_and_FAQ.md#enable-javautillogging-in-the-xml-language-server-process) for details.
 
## How to try a different build of lemminx-maven while developing m2e

In your development IDE, make sure the org.eclipse.m2e.editor.lemminx plugin is part of the workspace and can be resolved and built normally (no error marker). Then simply add/replace the `lemminx-maven.jar` file in its root with the particular lemminx-maven jar you're willing to work with. Then running as Eclipse Application will use the new jar for lemminx-maven.

Note that this process needs to be performed after any Maven build: running `mvn verify` will restore the maven-lemminx.jar to the version that's defined in the pom.xml, overriding your changes.

## How to build m2e master with the latest builds from lemminx-maven

`mvn install` to install maven-lemminx then `mvn verify` on m2e-core does a nice part of the job for testing *from the IDE*.
Unfortunately, it's currently a bit more complex for the Tycho build because of Git-based qualifiers and baseline replacement replacing the newly built bundle (which contains local lemminx-maven jar) by the baseline one because they have the exact same version. To avoid this replacement and get the latest lemminx-maven cascading to a newer org.eclipse.m2e.editor.lemminx bundle and so on, you need to make an extra commit somewhere under the `org.eclipse.m2e.editor.lemminx` bundle. In Eclipse Platform, there is usually a file named `forceQualifierUpdate.txt` that's committed and updated whenever we want to enforce usage of local build vs baseline.
(From https://github.com/eclipse/lemminx-maven/issues/43#issuecomment-622462629)

## How can I debug Eclipse IDE with m2e and the LemMinX-Maven instance it's using, at the same time

0. You need Eclipse Plugin Development Environment installed in your IDE.
1. Get the code of lemminx-maven in your workspace, in a version that matches the one in org.eclipse.m2e.editor.lemminx. You can see the version of lemminx-maven in the pom.xml of this module.
2. Put breakpoints in code of Lemminx-Maven to see what step is going wrong.
3. Create an "Eclipse Application" launch configuration for a child Eclipse IDE which does contain org.eclipse.m2e.editor.lemminx plugin and its depenndencies
4. Tweak the launch configuration Java settings to add `-Dorg.eclipse.wildwebdeveloper.xml.internal.XMLLanguageServer.debugPort=8000`
5. Launch it
6. Ensure all XML files are closed (Language Server is stopped)
7. Configure the child Eclipse IDE properly for your test (Maven settings and so on)
8. Open a pom.xml file of your choice
9. In the development Eclipse IDE instance (where you have the source code of lemminx-maven), create a Remote Debug Config for lemminx-maven on port 8080 and start it
=> You're now ready to debug the language server in typical m2e use-case
10. To repeat the initialization steps without restarting the whole child Eclipse IDE, just close all XML editors from child Eclipse IDE (that will stop the language server) and reopen a XML file and reconnect via remote debugger
