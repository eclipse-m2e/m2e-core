#!/bin/sh

LOCALREPO=/tmp/m2e-core.localrepo

mvn -f m2e-maven-runtime/pom.xml clean install -Dmaven.repo.local=$LOCALREPO
mvn clean install -Dmaven.repo.local=$LOCALREPO
