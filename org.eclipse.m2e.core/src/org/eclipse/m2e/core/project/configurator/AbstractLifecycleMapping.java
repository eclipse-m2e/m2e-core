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
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * AbstractLifecycleMapping
 * 
 * @author igor
 */
public abstract class AbstractLifecycleMapping implements ILifecycleMapping {

  private String name;

  private String id;

  private boolean showConfigurators;

  private IMavenProjectFacade mavenProjectFacade;

  /**
   * Calls #configure method of all registered project configurators
   */
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    addMavenBuilder(request.getProject(), monitor);

    for(AbstractProjectConfigurator configurator : getProjectConfigurators(monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.configure(request, monitor);
    }
  }

  public void unconfigure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    for(AbstractProjectConfigurator configurator : getProjectConfigurators(monitor)) {
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }
      configurator.unconfigure(request, monitor);
    }
  }

  protected static void addMavenBuilder(IProject project, IProgressMonitor monitor) throws CoreException {
    IProjectDescription description = project.getDescription();

    // ensure Maven builder is always the last one
    ICommand mavenBuilder = null;
    ArrayList<ICommand> newSpec = new ArrayList<ICommand>();
    for(ICommand command : description.getBuildSpec()) {
      if(IMavenConstants.BUILDER_ID.equals(command.getBuilderName())) {
        mavenBuilder = command;
      } else {
        newSpec.add(command);
      }
    }
    if(mavenBuilder == null) {
      mavenBuilder = description.newCommand();
      mavenBuilder.setBuilderName(IMavenConstants.BUILDER_ID);
    }
    newSpec.add(mavenBuilder);
    description.setBuildSpec(newSpec.toArray(new ICommand[newSpec.size()]));

    project.setDescription(description, monitor);
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

  /**
   * @param show Set whether the project configurators should show. Default is true.
   */
  public void setShowConfigurators(boolean show) {
    this.showConfigurators = show;
  }

  /**
   * Returns whether the project configurators will be shown in the UI. Default is true.
   */
  public boolean showConfigurators() {
    return this.showConfigurators;
  }

  public List<AbstractBuildParticipant> getBuildParticipants(IProgressMonitor monitor) throws CoreException {
    List<AbstractBuildParticipant> participants = new ArrayList<AbstractBuildParticipant>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      Set<AbstractProjectConfigurator> projectConfigurators = getProjectConfiguratorsForMojoExecution(mojoExecution,
          monitor);
      if(projectConfigurators == null) {
        continue;
      }
      for(AbstractProjectConfigurator configurator : projectConfigurators) {
        AbstractBuildParticipant participant = configurator.getBuildParticipant(mojoExecution);
        if(participant != null) {
          participants.add(participant);
        }
      }
    }

    return participants;
  }

  public List<MojoExecution> getNotCoveredMojoExecutions(IProgressMonitor monitor) throws CoreException {
    List<MojoExecution> result = new ArrayList<MojoExecution>();

    MavenExecutionPlan mavenExecutionPlan = mavenProjectFacade.getExecutionPlan(monitor);
    List<MojoExecution> allMojoExecutions = mavenExecutionPlan.getMojoExecutions();
    for(MojoExecution mojoExecution : allMojoExecutions) {
      if(!isInterestingPhase(mojoExecution.getLifecyclePhase())) {
        continue;
      }
      Set<AbstractProjectConfigurator> projectConfigurators = getProjectConfiguratorsForMojoExecution(mojoExecution,
          monitor);
      if(projectConfigurators == null || projectConfigurators.size() == 0) {
        result.add(mojoExecution);
      }
    }
    return result;
  }

  private static final String[] INTERESTING_PHASES = {"validate", //
      "initialize", //
      "generate-sources", //
      "process-sources", //
      "generate-resources", //
      "process-resources", //
      "compile", //
      "process-classes", //
      "generate-test-sources", //
      "process-test-sources", //
      "generate-test-resources", //
      "process-test-resources", //
      "test-compile", //
      "process-test-classes", //
  // "test", //
  // "prepare-package", //
  // "package", //
  //"pre-integration-test", //
  // "integration-test", //
  // "post-integration-test", //
  // "verify", //
  // "install", //
  // "deploy", //
  };

  public boolean isInterestingPhase(String phase) {
    for(String interestingPhase : INTERESTING_PHASES) {
      if(interestingPhase.equals(phase)) {
        return true;
      }
    }
    return false;
  }

  public abstract List<AbstractProjectConfigurator> getProjectConfigurators(IProgressMonitor monitor)
      throws CoreException;

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.configurator.ILifecycleMapping#initialize(org.eclipse.m2e.core.project.IMavenProjectFacade)
   */
  public void initialize(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) throws CoreException {
    setMavenProjectFacade(mavenProjectFacade);
  }

  public IMavenProjectFacade getMavenProjectFacade() {
    return mavenProjectFacade;
  }

  protected void setMavenProjectFacade(IMavenProjectFacade mavenProjectFacade) {
    if(this.mavenProjectFacade != null) {
      throw new IllegalStateException("Cannot change the maven project facade for a lifecycle mapping instance."); //$NON-NLS-1$
    }
    this.mavenProjectFacade = mavenProjectFacade;
  }
}
