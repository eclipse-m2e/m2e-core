/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;


/**
 * Registry of all Maven workspace projects and their inter-dependencies. Dependencies are modelled as generic
 * requirement/capability match and can represent any dependencies, not just Maven. The only way to change registry
 * contents is via {@link #apply(MutableProjectRegistry)} call. This class is thread safe.
 * 
 * @author Igor Fedorenko
 */
public class ProjectRegistry extends BasicProjectRegistry implements Serializable, IProjectRegistry {

  private static final long serialVersionUID = -5813062417528479711L;

  private transient int version;

  public synchronized MavenProjectFacade getProjectFacade(IFile pom) {
    return super.getProjectFacade(pom);
  }

  public synchronized MavenProjectFacade getProjectFacade(String groupId, String artifactId, String version) {
    return super.getProjectFacade(groupId, artifactId, version);
  }

  public synchronized MavenProjectFacade[] getProjects() {
    return super.getProjects();
  }

  public synchronized IFile getWorkspaceArtifact(ArtifactKey key) {
    return super.getWorkspaceArtifact(key);
  }

  public synchronized List<MavenProjectChangedEvent> apply(MutableProjectRegistry newState)
      throws StaleMutableProjectRegistryException {
    if(newState.isStale()) {
      throw new StaleMutableProjectRegistryException();
    }

    ArrayList<MavenProjectChangedEvent> events = new ArrayList<MavenProjectChangedEvent>();

    // removed projects
    for(MavenProjectFacade facade : workspacePoms.values()) {
      if(!newState.workspacePoms.containsKey(facade.getPom())) {
        MavenProjectChangedEvent event = new MavenProjectChangedEvent( //
            facade.getPom(), //
            MavenProjectChangedEvent.KIND_REMOVED, //
            MavenProjectChangedEvent.FLAG_NONE, //
            facade /*old*/, //
            null /*new*/);
        events.add(event);
      }
    }

    // changed and new projects
    for(MavenProjectFacade facade : newState.workspacePoms.values()) {
      MavenProjectFacade old = workspacePoms.get(facade.getPom());
      if(facade != old) { // not the same instance!
        MavenProjectChangedEvent event;
        if(old != null) {
          int flags = hasDependencyChange(old.getPom(), newState) ? MavenProjectChangedEvent.FLAG_DEPENDENCIES
              : MavenProjectChangedEvent.FLAG_NONE;
          event = new MavenProjectChangedEvent(facade.getPom(), //
              MavenProjectChangedEvent.KIND_CHANGED, //
              flags, //
              old /*old*/, //
              facade /*new*/);

        } else {
          event = new MavenProjectChangedEvent(facade.getPom(), //
              MavenProjectChangedEvent.KIND_ADDED, //
              MavenProjectChangedEvent.FLAG_NONE, //
              null /*old*/, //
              facade /*new*/);
        }
        events.add(event);
      }
    }

    replaceWith(newState);

    version++ ;

    return events;
  }

  public synchronized int getVersion() {
    return version;
  }

  private boolean hasDependencyChange(IFile pom, MutableProjectRegistry newState) {
    Set<RequiredCapability> oldRequirements = getProjectRequirements(pom);
    Set<RequiredCapability> requirements = newState.getProjectRequirements(pom);

    return ProjectRegistryManager.hasDiff(oldRequirements, requirements);
  }
}
