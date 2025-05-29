/********************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich  - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.plexus.build.connect.Configuration;
import org.codehaus.plexus.build.connect.messages.InitMessage;
import org.codehaus.plexus.build.connect.messages.Message;
import org.codehaus.plexus.build.connect.messages.MojoMessage;
import org.codehaus.plexus.build.connect.messages.ProjectMessage;
import org.codehaus.plexus.build.connect.messages.ProjectsMessage;
import org.codehaus.plexus.build.connect.messages.SessionMessage;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Captures the run state of a maven process
 */
public class MavenRunState {

  private String mavenName;

  private String mavenBuildNumber;

  private String mavenVersion;

  private String workingDirectory;

  private final Map<ArtifactKey, CompletableFuture<RemoteMavenProject>> projects = new ConcurrentHashMap<>();

  Map<String, String> handleMessage(Message msg) {
    if(msg instanceof InitMessage init) {
      this.mavenName = init.getProperty("versionProperties.distributionName");
      this.mavenBuildNumber = init.getProperty("versionProperties.buildNumber");
      this.mavenVersion = init.getProperty("versionProperties.version");
      this.workingDirectory = init.getProperty("workingDirectory");
      Map<String, String> config = new HashMap<String, String>();
      config.put(Configuration.CONFIG_SEND_PROJECTS, "true");
      return config;
    }
    if(msg instanceof ProjectsMessage projects) {
      projects.projects().forEach(pi -> {
        RemoteMavenProject project = new RemoteMavenProject(pi);
        getProject(project.getArtifactKey()).complete(project);
      });
    }
    if(msg instanceof ProjectMessage project) {
      ArtifactKey key = new ArtifactKey(project.getGroupId(), project.getArtifactId(), project.getVersion(), null);
      getProject(key).thenAccept(rp -> rp.handleEvent(project));
      System.out.println("--- " + project.getType() + " --- " + key);
      System.out.println();
      System.out.println(project.getBaseDir());
      return null;
    }
    if(msg instanceof MojoMessage mojo) {
      System.out.println("--- " + mojo.getType() + " ---");
      System.out.println(mojo.getGroupId() + ":" + mojo.getArtifactId() + ":" + mojo.getVersion() + " - "
          + mojo.getLifecyclePhase() + " [" + mojo.getExecutionId() + "] " + mojo.getGoal());
      return null;
    }
    if(msg instanceof SessionMessage session) {
      if(!session.isSessionStart()) {
        projects.values().forEach(cf -> {
          cf.thenAccept(rp -> rp.cancel());
          cf.cancel(true);
        });
        projects.clear();
      }
    }
    return null;
  }

  public CompletableFuture<RemoteMavenProject> getProject(ArtifactKey key) {
    CompletableFuture<RemoteMavenProject> computeIfAbsent = projects.computeIfAbsent(key,
        nil -> new CompletableFuture<>());
    return computeIfAbsent;
  }

}
