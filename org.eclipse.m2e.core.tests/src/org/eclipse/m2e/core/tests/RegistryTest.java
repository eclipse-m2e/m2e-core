/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * - Mickael Istria (Red Hat, Inc.) - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.tests;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


public class RegistryTest extends AbstractMavenProjectTestCase {

  @Test
  public void testDeletedFacadeIsRemoved() throws IOException, CoreException, InterruptedException {
    IProject project = createExisting(getClass().getSimpleName(), "resources/projects/simplePomOK", true);
    waitForJobsToComplete(monitor);
    IMavenProjectFacade facade = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().create(project,
        monitor);
    Assert.assertNotNull(facade);
    project.delete(true, monitor);
    waitForJobsToComplete(new NullProgressMonitor());
    Assert.assertTrue(facade.isStale());
    project = createExisting(getClass().getSimpleName(), "resources/projects/emptyPom", true);
    waitForJobsToComplete(monitor);
    facade = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().create(project, monitor);
    Assert.assertNull(facade);
  }

}
