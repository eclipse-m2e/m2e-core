/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;


/**
 * This class holds the context of a lifecycle mapping execution (typically on a set of {@link MavenProject}s
 *
 * @author caroso-de
 */
public class LifecycleMappingContext {

  private static final Logger LOG = LoggerFactory.getLogger(LifecycleMappingContext.class);

  private Map<String, MavenProject> mavenParentCache = new ConcurrentHashMap<>();

  /**
   * determines the maven parent for the given project either by leveraging a built-in cache or by using {@link IMaven}
   * 
   * @param project a {@link MavenProject} to determine the parent for
   * @return the maven parent or <b>null</b> if none can be determined
   */
  public MavenProject determineParentFor(MavenProject project, IProgressMonitor monitor) {
    if(project == null) {
      return null;
    }
    String parentId;
    if(project.getParent() != null) {
      parentId = project.getParent().getId();
    } else if(project.getParentArtifact() != null) {
      parentId = project.getParentArtifact().getId();
    } else {
      return null;
    }

    return mavenParentCache.computeIfAbsent(parentId, id -> {
      try {
        IMaven maven = MavenPlugin.getMaven();
        return maven.resolveParentProject(project, monitor);
      } catch(CoreException ex) {
        LOG.error("Failed to resolve parent project of " + project.getId(), ex);
        return null;
      }
    });
  }
}
