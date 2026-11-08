![M2E-Banner](assets/m2e-Banner.svg)
# Eclipse IDE integration for Maven (Eclipse m2e project)
[![Build m2e-core](https://github.com/eclipse-m2e/m2e-core/actions/workflows/maven.yml/badge.svg)](https://github.com/eclipse-m2e/m2e-core/actions/workflows/maven.yml)

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/eclipse-m2e/m2e-core?label=Version&sort=semver)
[![GitHub license](https://img.shields.io/github/license/eclipse-m2e/m2e-core?label=License)](https://github.com/eclipse-m2e/m2e-core/blob/main/LICENSE)
[![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.eclipse.org%2Fm2e%2Fjob%2Fm2e%2Fjob%2Fmain%2F&label=Build)](https://ci.eclipse.org/m2e/job/m2e/)
![Jenkins tests](https://img.shields.io/jenkins/tests?jobUrl=https%3A%2F%2Fci.eclipse.org%2Fm2e%2Fjob%2Fm2e%2Fjob%2Fmain%2F&label=Tests)

M2Eclipse provides tight integration for Apache Maven into the Eclipse IDE with the following features:
* Rich editor for pom.xml files leveraging
  * [LemMinX-Maven](https://github.com/eclipse/lemminx-maven) (extension for LemMinX [LSP](https://github.com/Microsoft/language-server-protocol) plugin)
  * [LemMinX (XML Language Server)](https://github.com/eclipse/lemminx)
  * [lsp4e (Language Server Protocol client support in Eclipse)](https://github.com/eclipse/lsp4e)
  * [lsp4j (Language Server Protocol Java binding)](https://github.com/eclipse-lsp4j/lsp4j)
  * [tm4e (Text document tokenization and syntax highlighting support)](https://github.com/eclipse/tm4e)
  * [Wild Web Developer (XML editing support)](https://github.com/eclipse/tm4e)
* Launching Maven builds from within Eclipse
* Dependency management for Eclipse build path based on Maven's pom.xml
* Resolving Maven dependencies from the Eclipse workspace without installing to local Maven repository
* Automatic downloading of the required dependencies from the remote Maven repositories
* Wizards for creating new Maven projects, pom.xml and to enable Maven support on plain Java project
* Quick search for dependencies in Maven remote repositories

See also https://projects.eclipse.org/projects/technology.m2e

## 📥 Installation
The recommended way to install Eclipse-m2e is using the Eclipse marketplace. Either click on

[![Eclipse Maven integration Marketplace entry](https://img.shields.io/static/v1?logo=eclipseide&label=Marketplace&message=Install%20Eclipse%20m2e&style=for-the-badge&logoColor=white&labelColor=darkorange&color=grey)](https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirectToMarketplace.html?entryId=5321178 "Install with Marketplace client")
&nbsp;&nbsp;&nbsp;or&nbsp;&nbsp;&nbsp;
[![Eclipse Maven integration Marketplace entry](https://img.shields.io/static/v1?logo=eclipseide&label=Marketplace&message=View%20Eclipse%20m2e&style=for-the-badge&logoColor=white&labelColor=darkorange&color=grey)](https://marketplace.eclipse.org/content/eclipse-m2e-maven-support-eclipse-ide "Open Eclipse Marketplace entry")

into your Eclipse-IDE, or use the Eclipse Marketplace Client directly from within the IDE.

⚠️ _Some other entries exist that look like m2e. They're usually outdated or incorrect. Please use the official one, linked above._

Alternatively, you can install the latest M2Eclipse release by using the _Install New Software_ dialog in Eclipse IDE, pointing it to this p2 repository:<br>
https://download.eclipse.org/technology/m2e/releases/latest/

To use the latest snapshot build, you can use this p2 repository:<br>
https://download.eclipse.org/technology/m2e/snapshots/latest/

To use snapshots only for the current version under development, you can add a p2 repository like the following:<br>
`https://download.eclipse.org/technology/m2e/snapshots/<version-under-development>`<br>
This version-specific snapshot repository is deleted at the next release so you will automatically fallback to the regular release updates.
The variable `<version-under-development>` has to be replaced by the current version under development version.

## 📢 Release notes

See [RELEASE_NOTES.md](RELEASE_NOTES.md)

### m2e 3 and Maven 4 support (under development)

The next evolution of the Eclipse Maven integration, named m2e 3, is currently under development and has the goal to prepare for Maven 4.
Maven 4 is not supported by the current m2e releases.
Decoupling m2e from the concrete Maven runtime is substantial work, and **the main blocker is currently a lack of funding and contributors**, not a lack of interest.

Interested parties can obtain the latest development snapshot at:
- `https://download.eclipse.org/technology/m2e/snapshots/3.0.0`

Status and discussion:
- [Open issues and pull requests labeled `Maven 4`](https://github.com/eclipse-m2e/m2e-core/issues?q=is%3Aopen+label%3A%22Maven+4%22)
- [Discussion: Maven 4 support](https://github.com/eclipse-m2e/m2e-core/discussions/2010)
- [Funding proposal to the IDE-WG Development Funding Initiative](https://gitlab.eclipse.org/eclipse-wg/ide-wg/ide-wg-dev-funded-efforts/ide-wg-dev-funded-program-planning-council-top-issues/-/work_items/47) (voicing your interest there helps)

m2e is not only used by the Eclipse IDE, it is also the foundation for Maven support in JDT-LS and therefore in many other Java tools.
If Maven 4 matters to you or your organization, please consider:
- [sponsoring the work](https://github.com/sponsors/laeubi), or contact us first if the funding should go to a concrete goal or issue (see also [sponsored issues](https://github.com/eclipse-m2e/m2e-core/issues?q=is%3Aissue+label%3Asponsored))
- joining the [IDE-WG](https://eclipseide.org/working-group/#about) to have a say in prioritizing funded efforts
- contributing code, testing the snapshots, or reviewing pull requests

## 🙋 Getting support

Just get in contact with us through the [discussions](https://github.com/eclipse-m2e/m2e-core/discussions) and share your [ideas](https://github.com/eclipse-m2e/m2e-core/discussions/new) or report [issues](https://github.com/eclipse-m2e/m2e-core/issues).

Please bear in mind that this is a community project and not a product you contracted support for.
Some contributors may or may not look at your support requests on demand, and if you do not provide the fix/implementation yourself, the issue might even never get fixed.

If you require dedicated help with m2e or want to make sure a bugfix or feature is handled with priority, the following individuals and companies offer commercial support:
* [Christoph Läubrich](https://github.com/sponsors/laeubi)
* [Läubisoft GmbH](https://xn--lubisoft-0za.gmbh/en/)

Others who offer support for m2e are welcome to be listed here, just open a pull request.

## 🤝 Help wanted

m2e is looking for help!
The project depends on the time and contributions of a small number of people, and more contributors are needed to keep it healthy and moving forward, especially for the upcoming m2e 3 (see the [discussion](https://github.com/eclipse-m2e/m2e-core/discussions/2010)).
If m2e is a key technology for you or your organization, you can help with the following:

* **Help testing:** try the latest [snapshot builds](#-installation) early and often, so regressions are found before a release.
* **Donate some time:** fix bugs, improve documentation, review open pull requests, answer questions in the [discussions](https://github.com/eclipse-m2e/m2e-core/discussions), or provide tests/sample projects for the features that are important to you.
* **Sponsor contributors:** if you can't contribute time, you can sponsor individual contributors, e.g. [Christoph Läubrich](https://github.com/sponsors/laeubi).

## ⌨️ Contributing
[![Create Eclipse Development Environment for m2e](https://download.eclipse.org/oomph/www/setups/svg/m2e.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-m2e/m2e-core/main/setup/m2eDevelopmentEnvironmentConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")
&nbsp;&nbsp;&nbsp;or just&nbsp;&nbsp;&nbsp;
[![Clone to Eclipse IDE](https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png)](https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html)

For detailed information about development, testing and builds, see [CONTRIBUTING.md](CONTRIBUTING.md).
