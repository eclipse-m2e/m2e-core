/*************************************************************************************
 * Copyright (c) 2014-2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.core.ui.internal.project;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.project.ResolverConfigurationIO;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;


/**
 * Listens for Out-Of-Date project configuration {@link IMarker}s added to Maven projects. Triggers an
 * {@link UpdateMavenProjectJob} when such markers are found.
 *
 * @author Fred Bricon
 */
public class MavenUpdateConfigurationChangeListener implements IResourceChangeListener {

  private static Logger LOG = LoggerFactory.getLogger(MavenUpdateConfigurationChangeListener.class);

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if(isAutoConfigurationUpdateDisabled()) {
      return;
    }
    List<IProject> outOfDateProjects;
    try {
      OutOfDateConfigurationDeltaVisitor visitor = new OutOfDateConfigurationDeltaVisitor();
      event.getDelta().accept(visitor);
      outOfDateProjects = visitor.outOfDateProjects;
    } catch(CoreException e) {
      LOG.error("An error occurred while checking for out-of-date configuration markers", e);
      return;
    }
    outOfDateProjects = outOfDateProjects.stream() //
        .filter(ResolverConfigurationIO::isAutomaticallyUpdateConfiguration).toList();
    updateProjectConfiguration(outOfDateProjects);
  }

  public static boolean isAutoConfigurationUpdateDisabled() {
    return !MavenPlugin.getMavenConfiguration().isAutomaticallyUpdateConfiguration();
  }

  protected void updateProjectConfiguration(List<IProject> outOfDateProjects) {
    if(outOfDateProjects != null && !outOfDateProjects.isEmpty()) {
      LOG.debug("Automatic update of {}", outOfDateProjects);
      new UpdateMavenProjectJob(outOfDateProjects).schedule();
    }
  }
}
