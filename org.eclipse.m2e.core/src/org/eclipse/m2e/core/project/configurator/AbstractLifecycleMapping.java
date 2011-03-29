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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.m2e.core.MavenPlugin;
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
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    MavenPlugin.getDefault().getProjectConfigurationManager()
        .addMavenBuilder(request.getProject(), null /*description*/, monitor);

    IMavenProjectFacade projectFacade = request.getMavenProjectFacade();

    for(AbstractProjectConfigurator configurator : getProjectConfigurators(projectFacade, monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.configure(request, monitor);
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
