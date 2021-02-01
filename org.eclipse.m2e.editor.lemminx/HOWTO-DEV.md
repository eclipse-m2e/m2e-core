# How to... develop and debug m2e and lemminx-maven integration

This document gathers answers to main questions about technical process to troubleshot or improve m2e and lemminx-maven integration

## How to build m2e master with the latest builds from lemminx-maven

`mvn install` to install maven-lemminx then `mvn verify` on m2e-core does a nice part of the job for testing *from the IDE*.
Unfortunately, it's currently a bit more complex for the Tycho build because of Git-based qualifiers and baseline replacement replacing the newly built bundle (which contains local lemminx-maven jar) by the baseline one because they have the exact same version. To avoid this replacement and get the latest lemminx-maven cascading to a newer org.eclipse.m2e.editor.lemminx bundle and so on, you need to make an extra commit somewhere under the `org.eclipse.m2e.editor.lemminx` bundle. In Eclipse Platform, there is usually a file named `forceQualifierUpdate.txt` that's committed and updated whenever we want to enforce usage of local build vs baseline.
(From https://github.com/eclipse/lemminx-maven/issues/43#issuecomment-622462629)

## How can I debug the LemMinX-Maven instance and m2e at the same time

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
