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

package org.eclipse.m2e.sourcelookup.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Helper to find and extract information from META-INF/maven pom.properties files.
 */
public abstract class MetaInfMavenScanner<T> {

  private static final String META_INF_MAVEN = "META-INF/maven";

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
}
