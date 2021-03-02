# Contributing to Eclipse m2eclipse

Welcome to the Eclipse m2e contributor land, and thanks in advance for your help in making Eclipse m2e better and better!

🏠 Official Eclipse m2e Git repo is [https://github.com/eclipse-m2e/m2e-core](https://github.com/eclipse-m2e/m2e-core) . (All other repositories, mirrors and so on are legacy repositories that should be removed at some point, so please don't use them!)

## ⚖️ Legal and Eclipse Foundation terms

The project license is available at [LICENSE](LICENSE).

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA).

* [http://www.eclipse.org/legal/ECA.php](http://www.eclipse.org/legal/ECA.php)

Commits that are provided by non-committers must have a `Signed-off-by` field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
[https://www.eclipse.org/projects/handbook/#resources-commit](https://www.eclipse.org/projects/handbook/#resources-commit).

## 💬 Get in touch with the community

Eclipse m2e use mainly 2 channels for strategical and technical discussions

* 📧 Mailing-list: Join the m2e-dev@eclipse.org mailing-list and browse archive at [https://accounts.eclipse.org/mailing-list/m2e-dev](https://accounts.eclipse.org/mailing-list/m2e-dev)
* 🐞 Issue management: m2e project uses Bugzilla to track ongoing development and issues.
    * Search for issues: [https://bugs.eclipse.org/bugs/buglist.cgi?product=m2e](https://bugs.eclipse.org/bugs/buglist.cgi?product=m2e)
    * Create a new report: [https://bugs.eclipse.org/bugs/enter_bug.cgi?product=m2e](https://bugs.eclipse.org/bugs/enter_bug.cgi?product=m2e)

## 🆕 Trying latest builds

Latest builds, for testing, can usually be found at https://download.eclipse.org/technology/m2e/snapshots/`${targetRelease}`/latest/ where `${targetRelease}` is the name of the **next** release.

## 🧑‍💻 Developer resources

 <a href="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/redirect.html"><img src="https://mickaelistria.github.io/redirctToEclipseIDECloneCommand/cloneToEclipseBadge.png" alt="Clone to Eclipse IDE"/></a>  

* Development Environment: [https://www.eclipse.org/m2e/documentation/m2e-development-environment.html](https://www.eclipse.org/m2e/documentation/m2e-development-environment.html) (documentation currently on wiki, but contributions to move it back into this Git repo are welcome!)
* m2e only accepts contributions via GitHub Pull Requests against [https://github.com/eclipse-m2e/m2e-core](https://github.com/eclipse-m2e/m2e-core) repository.

### 🏗️ Build

First `mvn install` from the _m2e-maven-runtime_ folder, then `mvn clean verify` from the root with typical usage of Maven+Tycho. The (long-running) integration tests are skipped by default, add `-Pits,uts` to yur command in order to run them.

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
