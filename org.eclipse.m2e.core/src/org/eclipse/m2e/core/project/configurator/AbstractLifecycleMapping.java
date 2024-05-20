/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.builder.InternalBuildParticipant;
import org.eclipse.m2e.core.internal.builder.MavenBuilderImpl;
import org.eclipse.m2e.core.internal.embedder.MavenProjectMutableState;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * AbstractLifecycleMapping
 *
 * @author igor
 */
public abstract class AbstractLifecycleMapping implements ILifecycleMapping {

  private static final Logger log = LoggerFactory.getLogger(AbstractLifecycleMapping.class);

  private String name;

  protected String id;

  private static final MavenBuilderImpl builder = new MavenBuilderImpl() {
    @Override
    protected boolean isApplicable(InternalBuildParticipant participant, int kind, IResourceDelta delta) {
      return true;
    }
  };

  /**
   * Calls #configure method of all registered project configurators
   */
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor mon) throws CoreException {
    final SubMonitor monitor = SubMonitor.convert(mon, 5);
    try {
      MavenPlugin.getProjectConfigurationManager().addMavenBuilder(request.mavenProjectFacade().getProject(),
          null /*description*/, monitor.newChild(1));

      IMavenProjectFacade projectFacade = request.mavenProjectFacade();
      MavenProject mavenProject = request.mavenProject();

      Build build = mavenProject.getBuild();
      if(build != null) {
        String directory = build.getDirectory();
        if(directory != null) {
          IPath targetPath = projectFacade.getProjectRelativePath(directory);
          if (targetPath != null) {
            IContainer container = projectFacade.getProject().getFolder(targetPath);
            if(container != null) {
              if(!container.exists() && container instanceof IFolder folder) {
                M2EUtils.createFolder(folder, true, monitor.newChild(1));
              } else {
                container.setDerived(true, monitor.newChild(1));
              }
            }
          }
        }
      }

      MavenProjectMutableState snapshot = MavenProjectMutableState.takeSnapshot(mavenProject);

      try {
        //run pre-configuration build
        Map<MojoExecutionKey, List<AbstractBuildParticipant>> participants = new LinkedHashMap<>();
        for(Map.Entry<MojoExecutionKey, List<AbstractBuildParticipant>> entry : getBuildParticipants(projectFacade,
            monitor).entrySet()) {
          List<AbstractBuildParticipant> participants2 = new ArrayList<>();
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
        projectFacade.createExecutionContext().execute((c, m) -> builder.build(c.getSession(), projectFacade,
            AbstractBuildParticipant2.PRECONFIGURE_BUILD, Collections.emptyMap(), participants, m), monitor);

        //perform configuration
        for(AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, monitor.newChild(1))) {
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          try {
            configurator.configure(request, monitor.newChild(1));
          } catch(RuntimeException e) {
            String message = NLS.bind(Messages.AbstractLifecycleMapping_could_not_update_project_configuration,
                projectFacade.getProject().getName());
            // oddly, CoreException stack trace is not shown in UI nor logged anywhere.
            log.warn(message, e);
            throw new CoreException(Status.error(message, e));
          }
        }
      } finally {
        snapshot.restore(mavenProject);
      }
    } finally {
      monitor.done();
    }
  }

  @Override
  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade projectFacade = request.mavenProjectFacade();

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
  @Override
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
  @Override
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
