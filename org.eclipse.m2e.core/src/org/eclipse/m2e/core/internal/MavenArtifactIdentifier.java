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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


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
      if(classesArtifacts.isEmpty() && isQueryCentral()) {
        // checksum-based lookup in central. This can be really slow and the chances are low that, 
        // after we havn't found a pom.xml/.properties embedded into the jar that this exact 
        // same jar is on Maven-Central (jars on central usually have that).
        classesArtifacts = identifyCentralSearch(location);
      }
    }
    return classesArtifacts;
  }

  private static boolean isQueryCentral() {
    return InstanceScope.INSTANCE.getNode(IMavenConstants.PLUGIN_ID)
        .getBoolean(MavenPreferenceConstants.P_QUERY_CENTRAL_TO_IDENTIFY_ARTIFACT, false);
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
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://search.maven.org/solrsearch/select?q=1:" + sha1))
          .GET().build();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      JsonObject container = new Gson().fromJson(response.body(), JsonObject.class);
      JsonArray docs = container.get("response").getAsJsonObject().get("docs").getAsJsonArray();
      return docs.asList().stream().map(JsonElement::getAsJsonObject).map(obj -> {
        String g = obj.get("g").getAsString();
        String a = obj.get("a").getAsString();
        String v = obj.get("v").getAsString();
        return new ArtifactKey(g, a, v, null);
      }).collect(Collectors.toSet());
    } catch(IOException | InterruptedException e) {
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
