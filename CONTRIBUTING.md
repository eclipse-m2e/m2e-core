# Contributing to Eclipse m2eclipse

Welcome to the Eclipse m2e contributor land, and thanks in advance for your help in making Eclipse m2e better and better!

🏠 Official Eclipse m2e Git repo is [https://github.com/eclipse-m2e/m2e-core](https://github.com/eclipse-m2e/m2e-core) . (All other repositories, mirrors and so on are legacy repositories that should be removed at some point, so please don't use them!)

## ⚖️ Legal and Eclipse Foundation terms

The project license is available at [LICENSE](LICENSE).

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

Before your contribution can be accepted by the project team, 
contributors must have an Eclipse Foundation account and 
must electronically sign the Eclipse Contributor Agreement (ECA).

* [http://www.eclipse.org/legal/ECA.php](http://www.eclipse.org/legal/ECA.php)

For more information, please see the Eclipse Committer Handbook:
[https://www.eclipse.org/projects/handbook/#resources-commit](https://www.eclipse.org/projects/handbook/#resources-commit).

## 💬 Get in touch with the community

Eclipse m2e use mainly 2 channels for strategical and technical discussions

* 🐞 View and report issues through uses GitHub Issues at https://github.com/eclipse-m2e/m2e-core/issues. _📜 Migration to GitHub tracker took place in March 2021, for older tickets, see https://bugs.eclipse.org/bugs/buglist.cgi?product=m2e 📜_
* 📧 Join the m2e-dev@eclipse.org mailing-list to get in touch with other contributors about project organization and planning, and browse archive at 📜 [https://accounts.eclipse.org/mailing-list/m2e-dev](https://accounts.eclipse.org/mailing-list/m2e-dev)

## 🆕 Trying latest builds

Latest builds, for testing, can usually be found at `https://download.eclipse.org/technology/m2e/snapshots/latest/` .

## 🧑‍💻 Developer resources

### Prerequisites

Java 11 and Maven 3.6.3 (only if you want to build from the command-line), or newer.

### ⌨️ Setting up the Development Environment automatically, using the Eclipse Installer (Oomph)

[![Create Eclipse Development Environment for m2e](https://download.eclipse.org/oomph/www/setups/svg/m2e.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-m2e/m2e-core/master/setup/m2eDevelopmentEnvironmentConfiguration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")

Step by Step guide:

1. Download the [Eclipse Installer](https://wiki.eclipse.org/Eclipse_Installer).  
	1. If you are already in the workspace of an Eclipse provisioned by Oomph, go to *File > Import... > Oomph > Projects from catalog* and continue with step 6.
2. Start the installer using the `eclipse-inst` executable.
3. On the first page (*Product*), click the preference button in the top-right corner and select the *Advanced Mode*.
    1. If you are behind a proxy, at this point you might want to double check your network settings by clicking in the *Network proxy settings* at the bottom.
    2. If an SSH key is required to access the git-repository, make sure that this key is known by clicking on the *SSH2 settings* at the bottom and verify that *SS2 home* has the correct value and the key is listed in *Private keys*.
4. Select *Eclipse IDE for Eclipse Committers* (use *Product Version - latest* to use the latest builds of Eclipse). Click *Next* .
5. On the *Projects*-page under *Eclipse Projects*, select *m2e*. Make sure that *m2e* is shown in the table on the bottom. Click *Next*.
6. You can edit the *Installation location and folder name*, the *Workspace location and folder name* or the *Git clone location*, among others.
    1. Only the latter is available if you came here via the Import-projects dialog.
    2. By choosing *Show all variables* at the bottom of the page, you are able to change other values as well but you do not have to.
    3. Click *Next* .
7. Press *Finish* on the *Confirmation* page will start the installation process. 
8. The installer will download the selected Eclipse version, starts Eclipse and will perform all the additional steps (cloning the git repos, etc...). When the downloaded Eclispe started, the progress bar in the status bar shows the progress of the overall setup.
9. Once the *Executing startup task* job is finished you should have all the *m2-core*, *m2-core-tests* and *m2e-maven-runtime* projects imported into three working sets called *m2-core*, *m2-core-tests* and *m2e-maven-runtime*.
10. Remaining errors are resolved after a restart of Eclipse.
11. Happy coding!

### ⌨️ Setting up the Development Environment manually

* Clone this repository <a href="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html"><img src="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png" alt="Clone to Eclipse IDE"/></a> for m2e-core.
Some tests are in a separate repository which is referenced as a Git submodule in the `m2e-core-tests` folder. You can use typical Git submodules comment to initialie the content of this folder.

* Run a build via command-line as mentioned below, since m2e relies on some code-generation that's not well integrated in the Eclipse IDE.
* Use latest release of the Eclipse SDK or Eclipse IDE with the Plugin Development Environment installed.
* Make sure m2e is installed in this IDE, including the "m2e PDE" feature,
* _File > Open Projects from Filesystem..._ , select the path to m2e-core Git repo and the relevant children projects you want to import; approve m2e connectors installation if prompted
* Depending on the task you're planning to work on, multiple workflows are available to configure the [target-platform](https://help.eclipse.org/2021-03/topic/org.eclipse.pde.doc.user/concepts/target.htm?cp=4_1_5)
    * In many cases, this simplest workflow will be sufficient: Install latest m2e snapshot in your target-platform (can be your current IDE), or
    * If you don't want to mix versions of m2e, open  __target-platform/dev-worksace.target__  and  _Set as Target-Platform_  from the editor, or
    * In case you're working on the content of the `m2e-maven-runtime` folder, then run `mvn install -f m2e-maven-runtime/` after your changes to deploy them locally and then tweak the  _target-platform/dev-worksace.target_  to reference the versions of those artifacts you build locally and reload this target platform
* Open the project modules you want to work on (right-click > Open project) and their dependencies; approve m2e connectors installation if prompted
* Happy coding!


### 🏗️ Build

The full Maven build of Eclipse m2e is performed in two subsequent steps.
In order to build m2e on the command line, run the following commands subsequently from the root of this repo's clone

1. `mvn generate-sources -f m2e-maven-runtime -Pgenerate-osgi-metadata`
2. `mvn clean verify`

Within the Eclipse-IDE both builds can be run using the Maven Launch-Configurations *m2e-maven-runtime--generate-OSGi-metadata* respectively *m2e-core--build*. The Launch-Configuration *m2e-core--build-all* runs both builds subsequently.
The (long-running) integration tests are skipped by default, add `-Pits,uts` to your command in order to run them; adding `-DskipTests` will skip all tests, within Eclipse one can run *m2e-core--build-with-integration-tests*.

If you have unresolved errors or are going to modify the Maven runtime components in _m2e-maven-runtime_ folder (typically to change version of Maven runtime, indexer, archetypes... that are shipped by default with m2e), you may want to launch the `m2e-maven-runtime--generate-OSGi-metadata` Run-configuration or trigger the Oomph-setup manually. See `m2e-maven-runtime/README.md` for details.

### ⬆️ Version bump

m2e tries to use OSGi Semantic Version (to properly expose its API contracts and breakage) and Reproducible Version Qualifiers (to minimize the avoid producing multiple equivalent artifacts for identical source). This requires the developer to manually bump version from time to time. Somes rules are that:

* Versions are bumped on a __per module grain__ (bump version of individual bundles/features one by one when necessary), __DON'T bump version of parent pom, nor of other modules you don't change__
* __Versions are bumped maximum once per release__ (don't bump versions that were already bumped since last release)
* __Don't bump versions of what you don't change__
* __Bump version of the bundles you're modifying only if it's their 1st change since last release__
* Version bump may need to be cascaded to features that *include* the artifact you just changed, and then to features that *include* such features and so on (unless the version of those features were already bumped since last release).

The delta for version bumps are:

* `+0.0.1` (next micro) for a bugfix, or an internal change that doesn't surface to APIs
* `+0.1.0` (next minor) for an API addition
* `+1.0.0` (next major) for an API breakage (needs to be discussed on the mailing-list first)
* If some "smaller" bump already took place, you can replace it with your "bigger one". Eg, if last release has org.eclipse.m2e.editor 1.16.1; and someone already bumped version to 1.16.2 (for an internal change) and you're adding a new API, then you need to change version to 1.17.0

### ➕ Submit changes

m2e only accepts contributions via GitHub Pull Requests against [https://github.com/eclipse-m2e/m2e-core](https://github.com/eclipse-m2e/m2e-core) repository.
