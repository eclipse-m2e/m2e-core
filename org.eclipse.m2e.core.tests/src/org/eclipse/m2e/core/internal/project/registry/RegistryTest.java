/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat, Inc.) - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenUpdateRequest;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Assert;
import org.junit.Ignore;
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

  @Test
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
	MavenImpl.execute(MavenPlugin.getMaven(), false, false, (context, aMonitor) -> {
      registryManager.refresh(state, pomFiles, aMonitor);
      return null;
    }, monitor);
    Assert.assertNotEquals(Collections.emptyMap(), state.requiredCapabilities);
  }

  @Ignore(value = "This test doesn't manage to reproduce Bug 547172 while similar manual steps do lead to an error")
  @Test
  public void testInvalidParent() throws IOException, CoreException, InterruptedException {
    IProject childProject = importProject("invalidParent", "resources/projects/invalidParent/child/", new ProjectImportConfiguration());
    waitForJobsToComplete(monitor);
    Optional<IMarker> maybeErrorMarker = Arrays.stream(childProject.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE))
      .filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
      .findAny();
    assertEquals(Optional.empty(), maybeErrorMarker);
    // The main difference between manual and unit test is that MavenImpl#readMavenProjects populates some "problems"
    // for the manual case, but not in unit test. We didn't (yet?) manage to identify what cause this difference so
    // we couldn't automate a good test
  }

  @Test
  public void testResolvedParentConfiguration() throws Exception {
    setAutoBuilding(false);
    try {
        IProject parent = createExisting("parent", "resources/projects/bug548652/", false);
        IProjectDescription childProjectDescription = parent.getWorkspace().loadProjectDescription(parent.getFolder("child").getFile(IProjectDescription.DESCRIPTION_FILE_NAME).getLocation());
        IProject child = parent.getWorkspace().getRoot().getProject(childProjectDescription.getName());
        child.create(childProjectDescription, new NullProgressMonitor());
        child.open(new NullProgressMonitor());
        MavenUpdateRequest request = new MavenUpdateRequest(false, false);
        request.addPomFile(parent.getFile("pom.xml"));
        IFile childPom = child.getFile("pom.xml");
        request.addPomFile(childPom);
        MavenPluginActivator.getDefault().getProjectManagerRefreshJob().refresh(request);
        waitForJobsToComplete();
        IMarker[] markers = childPom.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        assertArrayEquals(new IMarker[0], markers);
    } finally {
        setAutoBuilding(isAutoBuilding());
    }
  }
}
