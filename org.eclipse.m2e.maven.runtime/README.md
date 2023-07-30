
The Maven-Runtime bundles are ordinary Maven-Projects that facilitate Maven to fetch Maven-dependency jars (and their sources) from a Maven-repository
and use the bnd-maven-plugin to generate a corresponding MANIFEST.MF that has all fetched jars on the `Bundle-ClassPath`.
Consequently the content of those files and folders is fully controlled by the project's pom.xml (and its parent) and should only be adjusted via the pom.
Those files/folders are ignored by git and direct changes are therefore lost upon the next metadata-generation.

This one has the advantage, that the Maven-runtime bundles can be build together with all other m2e plug-ins.
The children of this project then participate directly in the PDE workspace build in the IDE and are directly included when launching another Eclipse from the IDE (e.g. for testing).
This is possible due to the connector for the bnd-maven-plugin which is part of m2e.
