/*******************************************************************************
 * Copyright (c) 2011-2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *      Hannes Wellmann - Generalize and improve artifact identification and source locating
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.CoreException;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Helper to find and extract information from META-INF/maven pom.properties files.
 */
public abstract class MetaInfMavenScanner<T> {

  private static final String POM_XML = "pom.xml";

  private static final String POM_PROPERTIES = "pom.properties";

  private static final String META_INF_MAVEN = "META-INF/maven";

//reads META-INF/maven/**/pom.properties
  private static final MetaInfMavenScanner<ArtifactKey> PROERTIES_SCANNER = new MetaInfMavenScanner<>() {
    @Override
    protected ArtifactKey visitFile(Path file) throws IOException {
      return loadPomProperties(Files.newInputStream(file), file);
    }

    @Override
    protected ArtifactKey visitJarEntry(JarFile jar, JarEntry entry) throws IOException {
      return loadPomProperties(jar.getInputStream(entry), Path.of(entry.getName()));
    }

    private ArtifactKey loadPomProperties(InputStream is, Path path) throws IOException {
      Properties properties = new Properties();
      try (is) {
        properties.load(is);
      }
      String groupId = properties.getProperty("groupId");
      String artifactId = properties.getProperty("artifactId");
      String version = properties.getProperty("version");
      return createKey(groupId, artifactId, version, path, POM_PROPERTIES);
    }
  };

  private static final MetaInfMavenScanner<ArtifactKey> XML_SCANNER = new MetaInfMavenScanner<>() {
    @Override
    protected ArtifactKey visitFile(Path file) throws IOException {
      return loadPomProperties(Files.newInputStream(file), file);
    }

    @Override
    protected ArtifactKey visitJarEntry(JarFile jar, JarEntry entry) throws IOException {
      return loadPomProperties(jar.getInputStream(entry), Path.of(entry.getName()));
    }

    private ArtifactKey loadPomProperties(InputStream is, Path path) throws IOException {
      Model model;
      try (is) {
        model = MavenPlugin.getMavenModelManager().readMavenModel(is);
      } catch(CoreException ex) {
        return null;
      }
      Parent parent = model.getParent();
      String groupId = model.getGroupId();
      if(groupId == null && parent != null) {
        groupId = parent.getGroupId();
      }
      String version = model.getVersion();
      if(version == null && parent != null) {
        version = parent.getVersion();
      }
      return createKey(groupId, model.getArtifactId(), version, path, POM_XML);
    }
  };

  private static ArtifactKey createKey(String groupId, String artifactId, String version, Path path, String source) {
    if(groupId != null && artifactId != null && version != null
        && path.endsWith(Path.of(groupId, artifactId, source))) { // validate that properties and path match
      return new ArtifactKey(groupId, artifactId, version, /* classifier= */null);
    }
    return null;
  }

  public List<T> scan(Path file, String filename) {
    if(file == null) {
      return List.of();
    }
    List<T> result = new ArrayList<>();
    try {
      if(Files.isDirectory(file)) {
        scanFilesystem(file.resolve(META_INF_MAVEN), filename, result);
      } else if(Files.isRegularFile(file)) {
        try (JarFile jar = new JarFile(file.toFile())) {
          scanJar(jar, "/" + filename, result);
        }
      }
    } catch(IOException e) {
      // fall through
    }
    return result;
  }

  private void scanJar(JarFile jar, String filename, List<T> result) {
    for(Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
      JarEntry entry = entries.nextElement();
      if(!entry.isDirectory()) {
        String name = entry.getName();
        if(name.startsWith(META_INF_MAVEN) && name.endsWith(filename)) {
          try {
            T t = visitJarEntry(jar, entry);
            if(t != null) {
              result.add(t);
            }
          } catch(IOException e) {
            // ignore
          }
        }
      }
    }
  }

  private void scanFilesystem(Path dir, String filename, List<T> result) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if(filename.equals(file.getFileName().toString())) {
          T t = MetaInfMavenScanner.this.visitFile(file);
          if(t != null) {
            result.add(t);
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE; // ignore
      }
    });
  }

  protected abstract T visitFile(Path file) throws IOException;

  protected abstract T visitJarEntry(JarFile jar, JarEntry entry) throws IOException;

  public static Set<ArtifactKey> scanForPomProperties(Path classesLocation) {
    return new LinkedHashSet<>(PROERTIES_SCANNER.scan(classesLocation, POM_PROPERTIES));
  }

  public static Set<ArtifactKey> scanForPomXml(Path classesLocation) {
    return new LinkedHashSet<>(XML_SCANNER.scan(classesLocation, POM_XML));
  }
}
