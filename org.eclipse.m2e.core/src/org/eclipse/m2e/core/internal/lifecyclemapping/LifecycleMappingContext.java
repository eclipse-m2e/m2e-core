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

import org.apache.maven.project.MavenProject;


/**
 * LifecycleMappingContext
 *
 * @author caroso-de
 */
public class LifecycleMappingContext {

  private static final Logger LOG = LoggerFactory.getLogger(LifecycleMappingContext.class);

  private Map<String, MavenProject> mavenParentCache = new ConcurrentHashMap<>();

  /**
   * @param project
   * @return
   */
  public MavenProject determineResolvedParentFor(MavenProject project) {
    if(project == null) {
      return null;
    }
    MavenProject parentProject = null;

    if(project.getParent() != null) {
      parentProject = mavenParentCache.get(project.getParent().getId());
    } else if(project.getParentArtifact() != null) {
      parentProject = mavenParentCache.get(project.getParentArtifact().getId());
    } else {
      return null;
    }

    LOG.info("parent cache {} for child {}", parentProject != null ? "HIT" : "MISS", project.getId());
    return parentProject;
  }

  /**
   * @param parent
   */
  public void registerResolvedParent(MavenProject parent) {
    if(parent != null) {
      mavenParentCache.put(parent.getId(), parent);
    }

  }

}
