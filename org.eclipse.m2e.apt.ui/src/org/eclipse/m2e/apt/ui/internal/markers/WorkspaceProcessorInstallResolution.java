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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.m2e.apt.internal.IMavenAptConstants;
import org.eclipse.m2e.apt.ui.internal.Messages;


final class WorkspaceProcessorInstallResolution implements IMarkerResolution2 {

  private final IMarker marker;

  WorkspaceProcessorInstallResolution(IMarker marker) {
    this.marker = marker;
  }

  @Override
  public String getLabel() {
    return NLS.bind(Messages.WorkspaceProcessorQuickFix_label, getProcessorProjectName());
  }

  @Override
  public void run(IMarker ignored) {
    WorkspaceProcessorInstallJob job = new WorkspaceProcessorInstallJob(marker);
    job.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        IStatus result = event.getResult();
        if(result != null && !result.isOK()) {
          StatusManager.getManager().handle(result, StatusManager.LOG | StatusManager.SHOW);
        }
      }
    });
    job.schedule();
  }

  @Override
  public String getDescription() {
    return Messages.WorkspaceProcessorQuickFix_description;
  }

  @Override
  public Image getImage() {
    return null;
  }

  private String getProcessorProjectName() {
    return marker.getAttribute(IMavenAptConstants.WORKSPACE_PROCESSOR_PROJECT_ATTRIBUTE, "?"); //$NON-NLS-1$
  }
}
