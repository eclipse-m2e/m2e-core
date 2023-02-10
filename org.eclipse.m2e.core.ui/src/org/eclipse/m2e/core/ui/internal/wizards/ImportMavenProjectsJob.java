/*******************************************************************************
 * Copyright (c) 2010-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation (in o.e.m.c.u.i.w.MavenImportWizard)
 *      Red Hat, Inc. - Extracted import workflow as standalone job
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.jobs.MavenWorkspaceJob;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * Workspace Job for importing {@link MavenProjectInfo}s into the workspace. After the projects are imported, if
 * lifecycle mappings errors have been detected on the imported projects, the Lifecycle Mapping wizard is shown to help
 * users fix these errors.
 *
 * @author Eugene Kuleshov
 * @author Fred Bricon
 */
public class ImportMavenProjectsJob extends MavenWorkspaceJob {

  private final List<IWorkingSet> workingSets;

  private final Collection<MavenProjectInfo> projects;

  private final ProjectImportConfiguration importConfiguration;

  public ImportMavenProjectsJob(Collection<MavenProjectInfo> projects, List<IWorkingSet> workingSets,
      ProjectImportConfiguration importConfiguration) {
    super(Messages.MavenImportWizard_job);
    this.projects = projects;
    this.workingSets = workingSets;
    this.importConfiguration = importConfiguration;
    setRule(MavenPlugin.getProjectConfigurationManager().getRule());
  }

  @Override
  public IStatus runInWorkspace(final IProgressMonitor monitor) {

    final AbstractCreateMavenProjectsOperation importOperation = new AbstractCreateMavenProjectsOperation() {

      @Override
      protected List<IProject> doCreateMavenProjects(IProgressMonitor progressMonitor) throws CoreException {
        SubMonitor monitor = SubMonitor.convert(progressMonitor, 101);
        try {
          List<IMavenProjectImportResult> results = MavenPlugin.getProjectConfigurationManager().importProjects(
              projects, importConfiguration, new MavenProjectWorkspaceAssigner(workingSets), monitor.newChild(100));
          return toProjects(results);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      importOperation.run(monitor);
      List<IProject> createdProjects = importOperation.getCreatedProjects();
      MappingDiscoveryJob discoveryJob = new MappingDiscoveryJob(createdProjects, true);
      discoveryJob.schedule();
    } catch(InvocationTargetException e) {
      return AbstractCreateMavenProjectsOperation.toStatus(e);
    }
    return Status.OK_STATUS;
  }

}
