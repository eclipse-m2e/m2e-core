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

package org.eclipse.m2e.core.internal.project.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Assert;
import org.junit.Test;


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

  @Test
  public void testMissingParentCapabilityStored() throws IOException, CoreException, InterruptedException {
    IProject project = createExisting(getClass().getSimpleName(), "resources/projects/missingParent", true);
    waitForJobsToComplete(monitor);
    MutableProjectRegistry registry = MavenPluginActivator.getDefault().getMavenProjectManagerImpl()
        .newMutableProjectRegistry();
    MavenCapability parentCapability = MavenCapability
        .createMavenParent(new ArtifactKey("missingGroup", "missingArtifactId", "1", null));
    assertEquals(Collections.singleton(project.getFile("pom.xml")), registry.getDependents(parentCapability, false));
  }

  public void testMultiRefreshKeepsCapabilities() throws IOException, CoreException, InterruptedException {
    IProject dependentProject = createExisting("dependent", "resources/projects/dependency/dependent", true);
    IProject dependencyProject = createExisting("dependency", "resources/projects/dependency/dependency", true);
    waitForJobsToComplete(monitor);
    ProjectRegistryManager registryManager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    Collection<IFile> pomFiles = new ArrayList<>(2);
    pomFiles.add(dependentProject.getFile("pom.xml"));
    pomFiles.add(dependencyProject.getFile("pom.xml"));
    MutableProjectRegistry state = MavenPluginActivator.getDefault().getMavenProjectManagerImpl().newMutableProjectRegistry();
    state.clear();
    registryManager.getMaven().execute(false, false, (context, aMonitor) -> {
      registryManager.refresh(state, pomFiles, aMonitor);
      return null;
    }, monitor);
    Assert.assertNotEquals(Collections.emptyMap(), state.requiredCapabilities);
  }
}
