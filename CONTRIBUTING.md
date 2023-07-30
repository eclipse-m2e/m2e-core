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

Java 17 and Maven 3.8.6 (only if you want to build from the command-line), or newer.

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
9. Once the *Executing startup task* job is finished you should have all the *m2-core* and *m2-core-tests* projects imported into three working sets called *m2-core*  and *m2-core-tests*.
10. Remaining errors are resolved after a restart of Eclipse.
11. Happy coding!

### ⌨️ Setting up the Development Environment manually

* Clone this repository <a href="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html"><img src="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png" alt="Clone to Eclipse IDE"/></a> for m2e-core.
Some tests are in a separate repository which is referenced as a Git submodule in the `m2e-core-tests` folder. You can use typical Git submodules comment to initialie the content of this folder.

* Run a build via command-line as mentioned below, since m2e relies on some code-generation that's not well integrated in the Eclipse IDE.
* Use latest release of the Eclipse SDK or Eclipse IDE with the Plugin Development Environment installed.
* Make sure m2e is installed in this IDE, including the "m2e PDE" feature,
* _File > Open Projects from Filesystem..._ , select the path to m2e-core Git repo and the relevant children projects you want to import; approve m2e connectors installation if prompted
* Open the project modules you want to work on (right-click > Open project) and their dependencies; approve m2e connectors installation if prompted
* Happy coding!

### ⌨️ Setting up the RCPTT Test Environment

* Download and install the RCPTT IDE from [here](https://www.eclipse.org/rcptt/download/).

* Create an executable application .
    * Right-click on [m2e-ide.product](products/m2e-ide/m2e-ide.product), *Export... > Plug-in Development > Eclipse product*
    * Execute `mvn clean verify -D skipTests` in the command line. The application is located under `products/m2e-ide/target/products`.
 
 * Launch the RCPTT IDE and import [org.eclipse.m2e.rcptt.tests](org.eclipse.m2e.rcptt.tests) into your workspace.
 
 * Add m2e to your applications.
 
 * Right-click on the test case you want to execute, *Run As... > Test Cases*. Select the m2e application.
 
Alternatively, you may also install RCPTT directly into your Eclipse IDE via the update site.

When creating new test cases, it is recommended to use the *Record* functionality, to track your actions in the test application. Check the [User Guide](https://www.eclipse.org/rcptt/documentation/userguide/getstarted/) for more information.

### 🏗️ Build

In order to build m2e on the command line, run the following command from the root of this repo's clone

`mvn clean verify`

Within the Eclipse-IDE the build can be run using the Maven Launch-Configuration *m2e-core--build*.
The (long-running) integration tests are skipped by default, add `-Pits` to your command in order to run them; adding `-DskipTests` will skip all tests, within Eclipse one can run *m2e-core--build-with-integration-tests*.

If you have unresolved errors refreshing the `m2e-maven-runtime` project and/or performing a `Clean` + `Full` build may solve them.
If are going to modify the Maven runtime components in `m2e-maven-runtime` folder you may want to delete the `target` folder of the affected sub-project and refresh and `Clean` + `Full` build it in order to ensure consistency.

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
