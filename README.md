![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/eclipse-m2e/m2e-core?label=Version&sort=semver)
[![GitHub license](https://img.shields.io/github/license/eclipse-m2e/m2e-core?label=License)](https://github.com/eclipse-m2e/m2e-core/blob/master/LICENSE)
[![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.eclipse.org%2Fm2e%2Fjob%2Fm2e%2Fjob%2Fmaster%2F&label=Build)](https://ci.eclipse.org/m2e/job/m2e/)
![Jenkins tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fci.eclipse.org%2Fm2e%2Fjob%2Fm2e%2Fjob%2Fmaster%2F&label=Tests)

# Eclipse IDE integration for Maven (Eclipse m2e project)

M2Eclipse provides tight integration for Apache Maven into the Eclipse IDE with the following features:
* Rich editor for pom.xml files
* Launching Maven builds from within Eclipse
* Dependency management for Eclipse build path based on Maven's pom.xml
* Resolving Maven dependencies from the Eclipse workspace without installing to local Maven repository
* Automatic downloading of the required dependencies from the remote Maven repositories
* Wizards for creating new Maven projects, pom.xml and to enable Maven support on plain Java project
* Quick search for dependencies in Maven remote repositories

See also https://projects.eclipse.org/projects/technology.m2e

## 📥 Installation
The recommended way to install Eclipse-m2e is using the Eclipse marketplace. Either click on

[![Eclipse Maven integration Marketplace entry](https://img.shields.io/static/v1?logo=eclipseide&label=Marketplace&message=Eclipse%20m2e&style=for-the-badge&logoColor=white&labelColor=darkorange&color=grey)](https://marketplace.eclipse.org/content/eclipse-m2e-maven-support-eclipse-ide)
&nbsp;&nbsp;&nbsp;or&nbsp;&nbsp;&nbsp;
[![Eclipse Maven integration Marketplace entry](https://img.shields.io/static/v1?logo=eclipseide&label=Drag%20to%20install&message=Eclipse%20m2e&style=for-the-badge&logoColor=white&labelColor=4B0082&color=grey)](https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=5321178 "Drag to your running Eclipse workspace. Requires Eclipse Marketplace Client")

into your Eclipse-IDE, or use the Eclipse Marketplace Client directly from within the IDE.

⚠️ _Some other entries exist that look like m2e. They're usually outdated or incorrect. Please use the official one, linked above._

Alternatively, you can install the lastest M2Eclipse release by using the _Install New Software_ dialog in Eclipse IDE, pointing it to this p2 repository: https://download.eclipse.org/technology/m2e/releases/latest/

## 📢 Release notes

See [RELEASE_NOTES.md](RELEASE_NOTES.md)

## ⌨️ Contributing
[![Create Eclipse Development Environment for m2e](https://img.shields.io/static/v1?logo=eclipseide&label=Create%20Development%20Environment&message=M2E&style=for-the-badge&logoColor=white&labelColor=darkorange&color=gray)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-m2e/m2e-core/master/setup/m2eDevelopmentEnvironmentConfiguration.setup&show=true)
 or just 
[![Clone to Eclipse IDE](https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png)](https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html)

For detailed information about development, testing and builds, see [CONTRIBUTING.md](CONTRIBUTING.md).
