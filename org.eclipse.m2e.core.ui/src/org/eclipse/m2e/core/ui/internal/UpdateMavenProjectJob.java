/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.ui.internal.actions.OpenMavenConsoleAction;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;


public class UpdateMavenProjectJob extends MavenWorkspaceJob {

  private final Collection<IProject> projects;

  private final boolean offline;

  private final boolean forceUpdateDependencies;

  private final boolean updateConfiguration;

  private final boolean cleanProjects;

  private final boolean refreshFromLocal;

  public UpdateMavenProjectJob(Collection<IProject> projects) {
    this(projects, MavenPlugin.getMavenConfiguration().isOffline(), false /*forceUpdateDependencies*/,
        true /*updateConfiguration*/, true /*rebuild*/, true /*refreshFromLocal*/);
  }

  public UpdateMavenProjectJob(Collection<IProject> projects, boolean offline, boolean forceUpdateDependencies,
      boolean updateConfiguration, boolean cleanProjects, boolean refreshFromLocal) {

    super(Messages.UpdateSourcesAction_job_update_conf);

    this.projects = projects;
    this.offline = offline;
    this.forceUpdateDependencies = forceUpdateDependencies;
    this.updateConfiguration = updateConfiguration;
    this.cleanProjects = cleanProjects;
    this.refreshFromLocal = refreshFromLocal;

    setRule(MavenPlugin.getProjectConfigurationManager().getRule());
  }

  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor) {
    ProjectConfigurationManager configurationManager = (ProjectConfigurationManager) MavenPlugin
        .getProjectConfigurationManager();

    setProperty(IProgressConstants.ACTION_PROPERTY, new OpenMavenConsoleAction());

    MavenUpdateRequest request = new MavenUpdateRequest(projects, offline, forceUpdateDependencies);
    Map<String, IStatus> updateStatus = configurationManager.updateProjectConfiguration(request, updateConfiguration,
        cleanProjects, refreshFromLocal, monitor);

    Map<String, Throwable> errorMap = new LinkedHashMap<>();
    ArrayList<IStatus> errors = new ArrayList<>();

    for(Map.Entry<String, IStatus> entry : updateStatus.entrySet()) {
      if(!entry.getValue().isOK()) {
        errors.add(entry.getValue());
        errorMap.put(entry.getKey(), new CoreException(entry.getValue()));
      }
    }

    if(errorMap.size() > 0) {
      handleErrors(errorMap);
    }

    IStatus status = Status.OK_STATUS;
    if(errors.size() == 1) {
      status = errors.get(0);
    } else {
      status = new MultiStatus(M2EUIPluginActivator.PLUGIN_ID, -1, errors.toArray(new IStatus[errors.size()]),
          Messages.UpdateSourcesAction_error_cannot_update, null);
    }

    return status;
  }

  private void handleErrors(final Map<String, Throwable> updateErrors) {
    final Display display = Display.getDefault();
    if(display != null) {
      display.asyncExec(() -> M2EUIUtils.showErrorsForProjectsDialog(display.getActiveShell(),
          Messages.UpdateSourcesAction_error_title, Messages.UpdateSourcesAction_error_message, updateErrors));
    }
  }
}
