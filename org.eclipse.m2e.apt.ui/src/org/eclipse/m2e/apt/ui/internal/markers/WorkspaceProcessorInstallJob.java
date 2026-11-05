/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.apt.ui.internal.markers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.apt.internal.IMavenAptConstants;
import org.eclipse.m2e.apt.ui.internal.Messages;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IProjectConfiguration;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;


@SuppressWarnings("restriction")
final class WorkspaceProcessorInstallJob extends Job {

  private final String artifactCoordinates;

  private final String processorProjectName;

  WorkspaceProcessorInstallJob(IMarker marker) {
    this(marker.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_ARTIFACT_ATTRIBUTE, "?"), //$NON-NLS-1$
        marker.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_PROJECT_ATTRIBUTE, "?")); //$NON-NLS-1$
  }

  private WorkspaceProcessorInstallJob(String artifactCoordinates, String processorProjectName) {
    super(NLS.bind(Messages.WorkspaceProcessorQuickFix_job, artifactCoordinates));
    this.artifactCoordinates = artifactCoordinates;
    this.processorProjectName = processorProjectName;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IProject processorProject = workspace.getRoot().getProject(processorProjectName);
    IMavenProjectFacade processorFacade = MavenPlugin.getMavenProjectRegistry().getProject(processorProject);
    if(!processorProject.isOpen() || processorFacade == null) {
      return Status.error(NLS.bind(Messages.WorkspaceProcessorQuickFix_missing_project, processorProjectName));
    }

    try {
      Set<IProject> consumers = findConsumers(workspace);
      SubMonitor progress = SubMonitor.convert(monitor, consumers.size() + 2);
      IStatus installStatus = install(processorFacade, progress.split(1));
      if(!installStatus.isOK()) {
        return installStatus;
      }

      IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();
      workspace.run(updateMonitor -> {
        SubMonitor updates = SubMonitor.convert(updateMonitor, consumers.size() + 1);
        processorProject.refreshLocal(IResource.DEPTH_INFINITE, updates.split(1));
        for(IProject consumer : consumers) {
          if(consumer.isOpen()) {
            configurationManager.updateProjectConfiguration(consumer, updates.split(1));
          }
        }
      }, configurationManager.getRule(), IWorkspace.AVOID_UPDATE, progress.split(consumers.size() + 1));
      return Status.OK_STATUS;
    } catch(CoreException | RuntimeException ex) {
      return Status.error(NLS.bind(Messages.WorkspaceProcessorQuickFix_failure, processorProjectName), ex);
    }
  }

  @Override
  public boolean belongsTo(Object family) {
    return IMavenAptConstants.WORKSPACE_PROCESSOR_MARKER_ID.equals(family);
  }

  private IStatus install(IMavenProjectFacade processorFacade, IProgressMonitor monitor) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
    ILaunchConfigurationWorkingCopy workingCopy = launchType.newInstance(null,
        launchManager.generateLaunchConfigurationName("annotation-processor-install")); //$NON-NLS-1$
    IProjectConfiguration configuration = processorFacade.getConfiguration();
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR,
        processorFacade.getPomFile().getParentFile().getAbsolutePath());
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, "install"); //$NON-NLS-1$
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_RUNTIME, "EMBEDDED"); //$NON-NLS-1$
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_BATCH, true);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_SAVE_BEFORE_LAUNCH, false);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_WORKSPACE_RESOLUTION, false);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, configuration.getSelectedProfiles());
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROPERTIES, getProperties(configuration));

    ILaunch launch = null;
    try {
      // Keep the configuration available to asynchronous console listeners without
      // creating a persistent launch configuration for this one-off operation.
      launch = workingCopy.launch(ILaunchManager.RUN_MODE, monitor);
      while(!launch.isTerminated()) {
        if(monitor.isCanceled()) {
          launch.terminate();
          return Status.CANCEL_STATUS;
        }
        try {
          Thread.sleep(100);
        } catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
          launch.terminate();
          return Status.CANCEL_STATUS;
        }
      }
      IProcess[] processes = launch.getProcesses();
      if(processes.length == 0) {
        return Status.error(NLS.bind(Messages.WorkspaceProcessorQuickFix_failure, processorProjectName));
      }
      for(IProcess process : processes) {
        if(process.getExitValue() != 0) {
          return Status.error(NLS.bind(Messages.WorkspaceProcessorQuickFix_failure, processorProjectName));
        }
      }
      launchManager.removeLaunch(launch);
      return Status.OK_STATUS;
    } catch(DebugException ex) {
      return Status.error(NLS.bind(Messages.WorkspaceProcessorQuickFix_failure, processorProjectName), ex);
    }
  }

  private static List<String> getProperties(IProjectConfiguration configuration) {
    Map<String, String> properties = new LinkedHashMap<>(configuration.getUserProperties());
    properties.putAll(configuration.getConfigurationProperties());
    List<String> result = new ArrayList<>(properties.size());
    properties.forEach((key, value) -> {
      if(key != null && !key.isBlank()) {
        result.add(key.strip() + (value == null ? "" : "=" + value)); //$NON-NLS-1$ //$NON-NLS-2$
      }
    });
    return result;
  }

  private Set<IProject> findConsumers(IWorkspace workspace) throws CoreException {
    Set<IProject> consumers = new LinkedHashSet<>();
    IMarker[] markers = workspace.getRoot().findMarkers(IMavenAptConstants.WORKSPACE_PROCESSOR_MARKER_ID,
        false, IResource.DEPTH_INFINITE);
    for(IMarker candidate : markers) {
      if(artifactCoordinates.equals(
          candidate.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_ARTIFACT_ATTRIBUTE, null))
          && processorProjectName.equals(
              candidate.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_PROJECT_ATTRIBUTE, null))) {
        consumers.add(candidate.getResource().getProject());
      }
    }
    return consumers;
  }

}
