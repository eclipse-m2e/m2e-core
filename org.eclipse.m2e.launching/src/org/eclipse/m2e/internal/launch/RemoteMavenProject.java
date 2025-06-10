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

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.codehaus.plexus.build.connect.messages.ProjectMessage;
import org.codehaus.plexus.build.connect.messages.ProjectMessage.EventType;
import org.codehaus.plexus.build.connect.messages.ProjectsMessage.ProjectInfo;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;

import org.eclipse.m2e.core.embedder.ArtifactKey;


/**
 * Represents a maven project from a remote maven launch
 */
public class RemoteMavenProject {

  private ArtifactKey artifactKey;

  private Path baseDir;

  private String modelString;

  private Model model;

  private CompletableFuture<?> starting = new CompletableFuture<>();

  private CompletableFuture<Result> finished = new CompletableFuture<>();

  RemoteMavenProject(ProjectInfo projectInfo) {
    artifactKey = new ArtifactKey(projectInfo.getGroupId(), projectInfo.getArtifactId(), projectInfo.getVersion(),
        null);
    this.baseDir = projectInfo.getBaseDir();
    this.modelString = projectInfo.getModel();
  }

  /**
   * @return the artifactKey
   */
  public ArtifactKey getArtifactKey() {
    return this.artifactKey;
  }

  /**
   * @return the baseDir
   */
  public Path getBaseDir() {
    return this.baseDir;
  }

  public synchronized Model getModel() {
    if(model != null) {
      return model;
    }
    try {
      if(modelString == null) {
        return model = new Model();
      }
      try {
        return model = new DefaultModelReader().read(new StringReader(modelString), null);
      } catch(IOException ex) {
        return model = new Model();
      }
    } finally {
      model = null;
    }
  }

  public CompletableFuture<Void> onStart(Runnable runnable) {
    return starting.thenAcceptAsync(nil -> runnable.run());
  }

  public CompletableFuture<Void> onFinish(Consumer<Result> consumer) {
    return finished.thenAcceptAsync(result -> consumer.accept(result));
  }

  void handleEvent(ProjectMessage project) {
    if(project.getType() == EventType.ProjectStarted) {
      starting.complete(null);
    } else if(project.getType() == EventType.ProjectSucceeded) {
      finished.complete(Result.SUCCESS);
    } else if(project.getType() == EventType.ProjectSkipped) {
      finished.complete(Result.SKIPPED);
    } else if(project.getType() == EventType.ProjectFailed) {
      finished.complete(Result.FAILED);
    }
  }

  void cancel() {
    starting.cancel(true);
    finished.cancel(true);
  }

  public static enum Result {
    SUCCESS, FAILED, SKIPPED;
  }

}
