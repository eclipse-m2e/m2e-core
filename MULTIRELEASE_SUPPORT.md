# Multi-Release JAR Support in Eclipse m2e

## Overview

Eclipse m2e now supports Maven projects that use multi-release JARs. This feature automatically detects and configures projects that use the Maven Compiler Plugin's multi-release capabilities.

## What are Multi-Release JARs?

Multi-Release JARs (introduced in Java 9) allow a single JAR file to contain different versions of class files for different Java versions. This enables you to:
- Maintain a single codebase with version-specific implementations
- Use newer Java APIs when running on newer JVMs
- Maintain backward compatibility with older Java versions

## Maven Configuration

To use multi-release JARs in Maven, configure the maven-compiler-plugin like this:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <executions>
        <!-- Base compilation for Java 8 -->
        <execution>
          <id>default-compile</id>
          <configuration>
            <release>8</release>
          </configuration>
        </execution>
        
        <!-- Java 11 specific compilation -->
        <execution>
          <id>compile-java-11</id>
          <phase>compile</phase>
          <goals>
            <goal>compile</goal>
          </goals>
          <configuration>
            <release>11</release>
            <compileSourceRoots>
              <compileSourceRoot>${project.basedir}/src/main/java-11</compileSourceRoot>
            </compileSourceRoots>
            <multiReleaseOutput>true</multiReleaseOutput>
          </configuration>
        </execution>
      </executions>
    </plugin>
    
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-jar-plugin</artifactId>
      <version>3.3.0</version>
      <configuration>
        <archive>
          <manifestEntries>
            <Multi-Release>true</Multi-Release>
          </manifestEntries>
        </archive>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## Project Structure

A typical multi-release project structure:

```
src/
  main/
    java/                    # Base Java 8 code
      org/example/
        MyClass.java
    java-11/                 # Java 11 specific code
      org/example/
        MyClass.java
    java-17/                 # Java 17 specific code
      org/example/
        MyClass.java
```

## Eclipse Configuration

When you import a Maven project with multi-release configuration, m2e will:

1. **Detect Multi-Release Executions**: Automatically identify maven-compiler-plugin executions with `multiReleaseOutput=true`

2. **Add Source Folders**: Add all version-specific source folders (e.g., `src/main/java-11`) to the Eclipse classpath

3. **Configure Output Paths**: Set up correct output paths for versioned classes:
   - Base classes → `target/classes/`
   - Java 11 classes → `target/classes/META-INF/versions/11/`
   - Java 17 classes → `target/classes/META-INF/versions/17/`

4. **Set Attributes**: Add the `maven.compiler.release` attribute to each source folder for reference

## Result

The resulting JAR structure will be:

```
myproject.jar
├── org/example/MyClass.class          (Java 8)
├── META-INF/
│   ├── MANIFEST.MF                     (contains Multi-Release: true)
│   └── versions/
│       ├── 11/
│       │   └── org/example/MyClass.class  (Java 11)
│       └── 17/
│           └── org/example/MyClass.class  (Java 17)
```

## Requirements

- Maven Compiler Plugin 3.13.0 or later
- Eclipse IDE with m2e installed
- Java 9 or later (for multi-release JAR support)

## How It Works

The implementation works by:

1. Parsing all maven-compiler-plugin executions during project configuration
2. Checking for the `multiReleaseOutput` parameter
3. Extracting the `release` version and `compileSourceRoots` for each multi-release execution
4. Adding source folders with versioned output paths to the Eclipse classpath
5. Setting appropriate classpath attributes for IDE integration

## Example

See the test project in `org.eclipse.m2e.jdt.tests/projects/multirelease/` for a complete working example.

## References

- [JEP 238: Multi-Release JAR Files](https://openjdk.org/jeps/238)
- [Maven Compiler Plugin Documentation](https://maven.apache.org/plugins/maven-compiler-plugin/)
- [Eclipse JDT Multi-Release Support](https://wiki.eclipse.org/JDT_Core/Java9#Multi-release_Jars)
