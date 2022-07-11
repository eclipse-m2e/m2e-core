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
 *      Hannes Wellmann - Generalize and improve artifact identification and source locating
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;


public class MavenArtifactIdentifier {
  private MavenArtifactIdentifier() { // static use only
  }

  private static final ILog LOG = Platform.getLog(MavenArtifactIdentifier.class);

  public static Collection<ArtifactKey> identify(File classesLocation) {
    // GAV extracted from pom.properties
    Path location = classesLocation.toPath();
    Set<ArtifactKey> classesArtifacts = MetaInfMavenScanner.scanForPomProperties(location);
    if(classesArtifacts.isEmpty()) {
      // GAV extracted from pom.xml
      classesArtifacts = MetaInfMavenScanner.scanForPomXml(location);
      if(classesArtifacts.isEmpty()) {
        // checksum-based lookup in central
        classesArtifacts = identifyCentralSearch(location);
      }
    }
    return classesArtifacts;
  }

  private static Set<ArtifactKey> identifyCentralSearch(Path file) {
    if(!Files.isRegularFile(file)) {
      return Set.of();
    }
    try {
      String sha1;
      try (InputStream fis = Files.newInputStream(file)) {
        sha1 = DigestUtils.sha1Hex(fis); // TODO use Locations for caching
      }
      URL url = new URL("https://search.maven.org/solrsearch/select?q=1:" + sha1);
      try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
        Set<ArtifactKey> result = new LinkedHashSet<>();
        JsonObject container = new Gson().fromJson(reader, JsonObject.class);
        JsonArray docs = container.get("response").getAsJsonObject().get("docs").getAsJsonArray();
        for(JsonElement element : docs) {
          JsonObject doc = element.getAsJsonObject();
          String g = doc.get("g").getAsString();
          String a = doc.get("a").getAsString();
          String v = doc.get("v").getAsString();
          result.add(new ArtifactKey(g, a, v, null));
        }
        return !result.isEmpty() ? Collections.unmodifiableSet(result) : Collections.emptySet();
      }
    } catch(IOException e) {
      LOG.log(Status.error("Failed to identify file by its hash using search.maven.org: " + file));
      return Set.of();
    }
  }

  public static Path resolveSourceLocation(ArtifactKey artifact, IProgressMonitor monitor) {
    if(artifact == null) {
      return null;
    }
    String groupId = artifact.groupId();
    String artifactId = artifact.artifactId();
    String version = artifact.version();
    try {
      List<ArtifactRepository> repositories = new ArrayList<>();
      IMaven maven = MavenPlugin.getMaven();
      repositories.addAll(maven.getArtifactRepositories());
      repositories.addAll(maven.getPluginArtifactRepositories());

      if(!maven.isUnavailable(groupId, artifactId, version, "jar", "sources", repositories)) {
        Artifact resolve = maven.resolve(groupId, artifactId, version, "jar", "sources", null, monitor);
        return resolve.getFile().toPath().toAbsolutePath();
      }
    } catch(CoreException e) {
      LOG.error("Failed to obtain source for artifact " + artifact, e);
    }
    return null;
  }

}
