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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.project.IMavenMarkerManager;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectManager;


/**
 * Used to configure maven projects.
 * 
 * @author Igor Fedorenko
 */
public abstract class AbstractProjectConfigurator implements IExecutableExtension, IMavenProjectChangedListener {

  public static final String ATTR_ID = "id"; //$NON-NLS-1$

  public static final String ATTR_PRIORITY = "priority"; //$NON-NLS-1$

  public static final String ATTR_NAME = "name"; //$NON-NLS-1$

  public static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private int priority;

  private String id;

  private String name;

  /**
   * List of maven plugin goal patterns for which this project configurator is enabled automatically. Can be null, in
   * which case the project configurator can only be enabled explicitly in pom.xml.
   */
  protected Set<PluginExecutionFilter> pluginExecutionFilters;

  protected MavenProjectManager projectManager;

  protected IMavenConfiguration mavenConfiguration;

  protected IMavenMarkerManager markerManager;

  protected MavenConsole console;

  protected IMaven maven = MavenPlugin.getDefault().getMaven();

  public void setProjectManager(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }

  public void setMavenConfiguration(IMavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
  }

  public void setMarkerManager(IMavenMarkerManager markerManager) {
    this.markerManager = markerManager;
  }

  public void setConsole(MavenConsole console) {
    this.console = console;
  }

  /**
   * Configures Eclipse project passed in ProjectConfigurationRequest, using information from Maven project and other
   * configuration request parameters
   * <p>
   * <i>Should be implemented by subclass</i>
   * 
   * @param request a project configuration request
   * @param monitor a progress monitor
   */
  public abstract void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException;

  /**
   * Removes Maven specific configuration from the project passed in ProjectConfigurationRequest
   * 
   * @param request a project un-configuration request
   * @param monitor a progress monitor
   */
  @SuppressWarnings("unused")
  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
  }

  /**
   * Updates project configuration according project changes.
   * <p>
   * <i>Can be overwritten by subclass</i>
   * 
   * @param event a project change event
   * @param monitor a progress monitor
   */
  @SuppressWarnings("unused")
  public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
  }

  // IMavenProjectChangedListener

  public final void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    for(int i = 0; i < events.length; i++ ) {
      try {
        mavenProjectChanged(events[i], monitor);
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  public int getPriority() {
    return priority;
  }

  public String getId() {
    if(id == null) {
      id = getClass().getName();
    }
    return id;
  }

  public String getName() {
    return name;
  }

  // IExecutableExtension  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
    String priorityString = config.getAttribute(ATTR_PRIORITY);
    try {
      priority = Integer.parseInt(priorityString);
    } catch(Exception ex) {
      priority = Integer.MAX_VALUE;
    }
  }

  protected void addPluginExecutionFilter(String groupId, String artifactId, String versionRange, String goals) {
    addPluginExecutionFilter(new PluginExecutionFilter(groupId, artifactId, versionRange, goals));
  }

  public void addPluginExecutionFilter(PluginExecutionFilter filter) {
    // TODO validate
    if(pluginExecutionFilters == null) {
      pluginExecutionFilters = new LinkedHashSet<PluginExecutionFilter>();
    }
    pluginExecutionFilters.add(filter);
  }

  public Set<PluginExecutionFilter> getPluginExecutionFilters() {
    return pluginExecutionFilters;
  }

  // TODO move to a helper
  public static void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
    if(!project.hasNature(natureId)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
      newNatures[0] = natureId;
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }

  @Deprecated
  protected <T> T getParameterValue(MavenSession session, MojoExecution execution, String parameter, Class<T> asType)
      throws CoreException {
    return maven.getMojoParameterValue(session, execution, parameter, asType);
  }

  protected <T> T getParameterValue(String parameter, Class<T> asType, MavenSession session, MojoExecution mojoExecution)
      throws CoreException {
    PluginExecution execution = new PluginExecution();
    execution.setConfiguration(mojoExecution.getConfiguration());
    return maven.getMojoParameterValue(parameter, asType, session, mojoExecution.getPlugin(), execution,
        mojoExecution.getGoal());
  }

  protected void assertHasNature(IProject project, String natureId) throws CoreException {
    if(project.getNature(natureId) == null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.AbstractProjectConfigurator_error_missing_nature + natureId, null));
    }
  }

  @Override
  public String toString() {
    return getId() + ":" + name + "(" + priority + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  public AbstractBuildParticipant getBuildParticipant(MojoExecution execution) {
    return null;
  }

  public boolean isSupportedExecution(MojoExecution mojoExecution) {
    if(pluginExecutionFilters == null) {
      return false;
    }
    for(PluginExecutionFilter key : pluginExecutionFilters) {
      if(key.match(mojoExecution)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    AbstractProjectConfigurator other = (AbstractProjectConfigurator) obj;
    if(getId() == null) {
      if(other.getId() != null) {
        return false;
      }
    } else if(!getId().equals(other.getId())) {
      return false;
    }
    return true;
  }
}
