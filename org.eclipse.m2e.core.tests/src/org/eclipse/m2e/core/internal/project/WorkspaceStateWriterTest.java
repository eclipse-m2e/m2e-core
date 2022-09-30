/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Jeff Maury (Red Hat, Inc.) - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;


public class WorkspaceStateWriterTest extends AbstractMavenProjectTestCase {

  @Test
  public void checkThatWorkspaceStateFileIsCreated() throws IOException, CoreException, InterruptedException {
    createExisting(getClass().getSimpleName(), "resources/projects/simplePomOK", true);
    waitForJobsToComplete(monitor);
    var manager = MavenPluginActivator.getDefault().getMavenProjectManager();
    assertNotNull(manager);
    assertTrue(manager instanceof MavenProjectManager);
    var file = ((MavenProjectManager) manager).getWorkspaceStateFile();
    assertNotNull(file);
    assertTrue(file.exists());
  }

}
