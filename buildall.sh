#!/bin/sh

mvn -f m2e-maven-runtime/pom.xml clean generate-sources -Dtycho.mode=maven -Pgenerate-osgi-metadata
mvn clean install
