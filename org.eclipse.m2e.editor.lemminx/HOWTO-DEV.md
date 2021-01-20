# [Question] Steps to build m2e master with the latest builds from lemminx-maven

`mvn install` to install maven-lemminx then `mvn verify` on m2e-core does a nice part of the job for testing *from the IDE*.
Unfortunately, it's currently a bit more complex for the Tycho build because of Git-based qualifiers and baseline replacement replacing the newly built bundle (which contains local lemminx-maven jar) by the baseline one because they have the exact same version. To avoid this replacement and get the latest lemminx-maven cascading to a newer org.eclipse.m2e.editor.lemminx bundle and so on, you need to make an extra commit somewhere under the `org.eclipse.m2e.editor.lemminx` bundle. In Eclipse Platform, there is usually a file named `forceQualifierUpdate.txt` that's committed and updated whenever we want to enforce usage of local build vs baseline.
(From https://github.com/eclipse/lemminx-maven/issues/43#issuecomment-622462629)
