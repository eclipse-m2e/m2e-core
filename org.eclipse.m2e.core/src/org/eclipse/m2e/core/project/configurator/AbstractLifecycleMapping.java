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

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.builder.MavenBuilderImpl;
import org.eclipse.m2e.core.internal.embedder.MavenProjectMutableState;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * AbstractLifecycleMapping
 * 
 * @author igor
 */
public abstract class AbstractLifecycleMapping implements ILifecycleMapping {

  private String name;

  protected String id;

  /**
   * Calls #configure method of all registered project configurators
   */
  public void configure(ProjectConfigurationRequest request, IProgressMonitor mon) throws CoreException {
    final SubMonitor monitor = SubMonitor.convert(mon, 5);
    try {
      MavenPlugin.getProjectConfigurationManager().addMavenBuilder(request.getProject(), null /*description*/,
          monitor.newChild(1));

      IMavenProjectFacade projectFacade = request.getMavenProjectFacade();
      MavenProject mavenProject = request.getMavenProject();

      Build build = mavenProject.getBuild();
      if(build != null) {
        String directory = build.getDirectory();
        if(directory != null) {
          IContainer container = projectFacade.getProject().getFolder(projectFacade.getProjectRelativePath(directory));
          if(container != null) {
            if(!container.exists() && container instanceof IFolder) {
              M2EUtils.createFolder((IFolder) container, true, monitor.newChild(1));
            } else {
              container.setDerived(true, monitor.newChild(1));
            }
          }
        }
      }

      MavenProjectMutableState snapshot = MavenProjectMutableState.takeSnapshot(mavenProject);

      try {
        //run pre-configuration build
        Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants = new LinkedHashMap<MojoExecutionKey, List<AbstractBuildParticipant>>();
        for(Map.Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : getBuildParticipants(projectFacade,
            monitor).entrySet()) {
          List<AbstractBuildParticipant> participants2 = new ArrayList<AbstractBuildParticipant>();
          for(AbstractBuildParticipant participant : entry.getValue()) {
            if(participant instanceof AbstractBuildParticipant2) {
              participants2.add(participant);
            }
          }

          if(!participants2.isEmpty()) {
            // @TODO do we want mapping for all executions???
            participants.put(entry.getKey(), participants2);
          }
        }
        new MavenBuilderImpl().build(request.getMavenSession(), projectFacade,
            AbstractBuildParticipant2.PRECONFIGURE_BUILD, participants, monitor);

        //perform configuration
        for(AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, monitor.newChild(1))) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          configurator.configure(request, monitor.newChild(1));
        }
      } finally {
        snapshot.restore(mavenProject);
      }
    } finally {
      monitor.done();
    }
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade projectFacade = request.getMavenProjectFacade();

    for(AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.unconfigure(request, monitor);
    }
  }

  /**
   * @return Returns the name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @param name The name to set.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return Returns the id.
   */
  public String getId() {
    return this.id;
  }

  /**
   * @param id The id to set.
   */
  public void setId(String id) {
    this.id = id;
  }

  public abstract boolean hasLifecycleMappingChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldConfiguration, IProgressMonitor monitor);

}
