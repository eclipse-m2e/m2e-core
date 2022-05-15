/*******************************************************************************
 * Copyright (c) 2011, 2022 Igor Fedorenko and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.internal.launch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.m2e.core.embedder.ArtifactKey;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MavenArtifactIdentifier {

  // reads META-INF/maven/**/pom.properties
  private static final MetaInfMavenScanner<Properties> scanner = new MetaInfMavenScanner<>() {
    @Override
    protected Properties visitFile(File file) throws IOException {
      // TODO validate properties and path match
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
        return loadProperties(is);
      }
    }

    @Override
    protected Properties visitJarEntry(JarFile jar, JarEntry entry) throws IOException {
      // TODO validate properties and path match
      try (InputStream is = jar.getInputStream(entry)) {
        return loadProperties(is);
      }
    }

    private Properties loadProperties(InputStream is) throws IOException {
      Properties properties = new Properties();
      properties.load(is);
      return properties;
    }
  };

  public Collection<ArtifactKey> identify(File classesLocation) {
    // checksum-based lookup in nexus index
    // checksum-based lookup in central
    // GAV extracted from pom.properties

    Collection<ArtifactKey> classesArtifacts = identifyCentralSearch(classesLocation);
    if (classesArtifacts == null) {
      classesArtifacts = scanPomProperties(classesLocation);
    }

    return classesArtifacts;
  }

  protected Collection<ArtifactKey> identifyCentralSearch(File file) {
    if (!file.isFile()) {
      return null;
    }

    try {
      String sha1;
      try (InputStream fis = new FileInputStream(file)){
        sha1 = DigestUtils.sha1Hex(fis); // TODO use Locations for caching
      }
      URL url = new URL("https://search.maven.org/solrsearch/select?q=1:" + sha1);
      try (InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
        Set<ArtifactKey> result = new LinkedHashSet<>();
        JsonObject container = new Gson().fromJson(reader, JsonObject.class);
        JsonArray docs = container.get("response").getAsJsonObject().get("docs").getAsJsonArray();
        for (JsonElement element : docs) {
            JsonObject doc = element.getAsJsonObject();
          String g = doc.get("g").getAsString();
          String a = doc.get("a").getAsString();
          String v = doc.get("v").getAsString();
          result.add(new ArtifactKey(g, a, v, null));
        }
        return !result.isEmpty() ? Set.copyOf(result) : null;
      }
    } catch (IOException e) {
      // TODO maybe log, ignore otherwise
    }
    return null;
  }

  public Collection<ArtifactKey> scanPomProperties(File classesLocation) {
    Set<ArtifactKey> artifacts = new LinkedHashSet<>();
    for (Properties pomProperties : scanner.scan(classesLocation, "pom.properties")) {
      String groupId = pomProperties.getProperty("groupId");
      String artifactId = pomProperties.getProperty("artifactId");
      String version = pomProperties.getProperty("version");
      if (groupId != null && artifactId != null && version != null) {
        artifacts.add(new ArtifactKey(groupId, artifactId, version, /* classifier= */null));
      }
    }
    return Set.copyOf(artifacts);
  }
}
