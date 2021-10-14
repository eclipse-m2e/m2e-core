This is a Maven repository that contains test artifacts generated projects found in ../testrepo-src folder.

Do NOT add binary artifacts directly to this repository. Execute the following command from -src repo instead

   mvn clean deploy -DaltDeploymentRepository=default::default::file://<full-path-to-this-folder>
