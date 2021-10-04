
The Maven-Runtime bundles are ordinary Eclipse-Plugins that facilitate Maven to fetch Maven-dependency jars (and their sources) from a Maven-repository
and to generate corresponding MANIFEST.MF and .classpath files that include all fetched jars accordingly.
Consequently the content of those files and folders is fully controlled by the project's pom.xml (and its parent) and should only be adjusted via the pom.
Those files/folders are ignored by git and direct changes are therefore lost upon the next metadata-generation.

Therefore two builds are necessary to build m2e from scratch:
	1. A pure Maven build (tycho.mode=maven), to fetch all jars and to generate the MANIFEST.MF and .classpath file for each Maven-runtime project.
	2. An ordinary 'Eclipse-Tycho' build, where the Maven-runtime plug-ins are build like ordinary Eclipse plug-ins together with all other m2e Eclipse-Plugins.

In the Eclipse-IDE the first build is executed by the Oomph setup on every start-up or when triggered manually in order to re-generate the the mentioned files.
To re-generate the OSGi metadata of the Maven-runtime bundles you can either run the first step of the Maven build as mentioned [in the Build section of the CONTRIBUTING guide](../CONTRIBUTING.md#🏗️ Build), launch the corresponding Run-configuration or perform the Oomph-setup tasks for m2e (manually or by restarting your Eclipse-IDE).

Compared to the previous approach this one has the advantage, that the Maven-runtime bundles can be build together with all other m2e plug-ins.
The Maven-runtime projects then participate directly in the PDE build and are directly included when launching another Eclipse from the IDE (e.g. for testing).
They don't have to be included by installing them into the local .m2 repo and loading into the target-platform anymore, which simplifies their handling in the IDE and during build.
Furthermore sources of the embedded jars are also available in the IDE.
