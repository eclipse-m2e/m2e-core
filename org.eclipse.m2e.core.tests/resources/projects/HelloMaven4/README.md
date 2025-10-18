# Hello Maven 4 Test Project

## Overview
This is a demonstration project showcasing Maven 4 features for testing m2e compatibility.
The project is based on real-world usage patterns from [jline3](https://github.com/jline/jline3).

## Project Structure
```
HelloMaven4/
├── pom.xml (parent)
├── hello-core/ (library subproject)
│   └── src/main/java/org/eclipse/m2e/tests/demo/core/
│       └── GreetingService.java
└── hello-app/ (application subproject)
    └── src/main/java/org/eclipse/m2e/tests/demo/app/
        └── HelloWorldApp.java
```

## Maven 4 Features Used

This project demonstrates the following Maven 4 features from [What's New in Maven 4](https://maven.apache.org/whatsnewinmaven4.html):

### 1. ✅ New Model Version 4.1.0
- **Feature**: Maven 4 introduces a new POM model version `4.1.0`
- **Usage**: All POMs use `<modelVersion>4.1.0</modelVersion>` and `xmlns="http://maven.apache.org/POM/4.1.0"` namespace
- **Location**: All `pom.xml` files
- **Status**: Demonstrated

### 2. ✅ Subprojects Instead of Modules
- **Feature**: Maven 4 uses `<subprojects>` instead of `<modules>` for better semantics
- **Usage**: Parent POM uses `<subprojects>` to declare child projects
- **Location**: `pom.xml` (parent)
- **Status**: Demonstrated

### 3. ✅ Inheritance of GroupId and Version
- **Feature**: Child projects can omit `<groupId>` and `<version>` when inheriting from parent
- **Usage**: Child POMs (`hello-core` and `hello-app`) omit these elements
- **Location**: `hello-core/pom.xml` and `hello-app/pom.xml`
- **Status**: Demonstrated

### 4. ⏳ Build/Consumer POM Split
- **Feature**: Maven 4 separates build-time and consumption-time concerns
- **Usage**: Not explicitly demonstrated in this simple project
- **Status**: Not applicable for this simple test case

### 5. ⏳ Better Java Version Handling
- **Feature**: Improved handling of Java versions with `maven.compiler.release`
- **Usage**: Uses `maven.compiler.release` property set to 11
- **Location**: Parent POM properties
- **Status**: Partially demonstrated

## Testing with Maven 4

This project is designed to be built with **Maven 4.0.0-rc-2** or later (Preview release).

To verify Maven 4 compatibility:
```bash
# Requires Maven 4.0.0-rc-2 or later
mvn clean install
```

## Purpose

This test project serves two main goals:

1. **Demonstrate Incompatibility**: Show that current m2e does NOT support Maven 4 features
2. **Validation Target**: Provide a test case to verify Maven 4 support as it's implemented

## References

- [Apache Maven 4 Downloads](https://maven.apache.org/download.cgi)
- [What's New in Maven 4](https://maven.apache.org/whatsnewinmaven4.html)
- [JLine3 Project](https://github.com/jline/jline3) - Real-world Maven 4 usage example
- [Maven 4 Announcement](https://lists.apache.org/thread/jnb3snhdm4b564gz8hbctp9rfk8fc67n)
