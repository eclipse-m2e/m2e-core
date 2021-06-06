
The Maven-Runtime bundles are ordinary Eclipse-Plugins that facilitate Maven to fetch Maven-dependency jars from a Maven-repository
and to generate corresponding Manifest and .classpath files that include all fetched jars accordingly.

Therefore two builds are necessary:
	1. A pure Maven build (tycho.mode=maven), to fetch all jars and to generate the MANIFEST.MF and .classpath file for each Maven-runtime project.
	2. An ordinary 'Eclipse-Tycho' build, where the Maven-runtime plug-ins are build like ordinary Eclipse plug-ins together with all other m2e Eclipse-Plugins.

In the Eclipse-IDE the first build is executed by the Oomph setup on every start-up or when triggered manually in order to re-generate the the mentioned files.
In order to re-generate the OSGi metadata of the Maven-runtime bundles you can either run the first step of the Maven build as mentioned [in the Build section of the CONTRIBUTING guide](../CONTRIBUTING.md#🏗️ Build), launch the corresponding Run-configuration or perform the Oomph-setup tasks for m2e (manually or by restarting your Eclipse-IDE).

This approach has the advantage, that the Maven-runtime bundles can be build together with all other m2e plug-ins.
This avoids the need to include them via the target-platform, thus simplifies their handling in the IDE and during build.
Additionally the Maven-runtime projects participate directly in the PDE build and are directly included when launching another Eclipse from the IDE (e.g. for testing) and don't have to be included by installing in the local .m2 repo and then including them into the target-platform.
