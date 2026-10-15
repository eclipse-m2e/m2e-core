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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.mockito.Mockito;


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

  @Test
  public void testPhase2CapabilityRevisitsRequeueOnlyOnChange() throws Exception {
    assertPhase2RevisitsRequeueOnlyOnChange(true);
  }

  @Test
  public void testPhase2RequirementRevisitsRequeueOnlyOnChange() throws Exception {
    assertPhase2RevisitsRequeueOnlyOnChange(false);
  }

  private void assertPhase2RevisitsRequeueOnlyOnChange(boolean changeCapabilities) throws Exception {
    IProject dependencyProject = createExisting("dependency", "resources/projects/dependency/dependency", true);
    IProject dependentProject = createExisting("dependent", "resources/projects/dependency/dependent", true);
    IProject unrelatedProject = createExisting("unrelated", "resources/projects/simplePomOK", true);
    waitForJobsToComplete(monitor);
    ProjectRegistryManager registryManager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();
    ProjectRegistryManager refreshManager = Mockito.spy(registryManager);
    IFile dependencyPom = dependencyProject.getFile("pom.xml");
    IFile dependentPom = dependentProject.getFile("pom.xml");
    ArtifactKey dependencyKey = registryManager.create(dependencyProject, monitor).getArtifactKey();
    ArtifactKey changedKey = new ArtifactKey(dependencyKey.groupId(), dependencyKey.artifactId(), "0.0.2", null);
    ArtifactKey parentKey = new ArtifactKey("test", "parent", "1", null);
    List<Boolean> firstVisits = new ArrayList<>();

    try(MutableProjectRegistry state = registryManager.newMutableProjectRegistry()) {
      if(changeCapabilities) {
        state.setCapabilities(dependencyPom,
            Set.of(MavenCapability.createMavenArtifact(changedKey), MavenCapability.createMavenParent(changedKey)));
      } else {
        state.setRequirements(dependencyPom, Set.of(MavenRequiredCapability.createResolvedMavenParent(parentKey)));
      }

      Mockito.doAnswer(invocation -> {
        DependencyResolutionContext context = invocation.getArgument(1);
        Map<IFile, Set<Capability>> originalCapabilities = invocation.getArgument(2);
        Map<IFile, Set<RequiredCapability>> originalRequirements = invocation.getArgument(3);
        IFile pom = invocation.getArgument(4);
        MavenProjectFacade facade = invocation.getArgument(5);
        boolean firstVisit = invocation.getArgument(6);
        IProgressMonitor progress = invocation.getArgument(7);
        if(!dependencyPom.equals(pom)) {
          return invocation.callRealMethod();
        }

        firstVisits.add(firstVisit);
        int visit = firstVisits.size();
        Assert.assertTrue("The forced revisits must converge", visit <= 3);
        Assert.assertFalse("The dependent must have been processed before this visit",
            context.getCurrent().contains(dependentPom));
        MavenProject mavenProject = refreshManager.getMavenProject(facade);
        Artifact parentArtifact = mavenProject.getParentArtifact();
        if(visit == 3) {
          if(changeCapabilities) {
            facade = Mockito.spy(facade);
            Mockito.doReturn(changedKey).when(facade).getArtifactKey();
            refreshManager.putMavenProject(facade, mavenProject);
          } else {
            mavenProject.setParentArtifact(new DefaultArtifact(parentKey.groupId(), parentKey.artifactId(),
                parentKey.version(), null, "pom", null, new DefaultArtifactHandler("pom")));
          }
        }

        try {
          registryManager.refreshPhase2(state, context, originalCapabilities, originalRequirements, pom, facade,
              firstVisit, progress);
        } finally {
          mavenProject.setParentArtifact(parentArtifact);
        }
        assertEquals("Only a changed phase-2 result should re-enqueue the dependent on visit " + visit,
            visit != 2, context.getCurrent().contains(dependentPom));
        if(visit < 3) {
          context.forcePomFile(pom);
        }
        return null;
      }).when(refreshManager).refreshPhase2(Mockito.eq(state), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(),
          Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

      // The unrelated project lets ProjectProcessingTracker make progress and retry the forced revisit
      List<IFile> poms = List.of(dependentPom, dependencyPom, unrelatedProject.getFile("pom.xml"));
      MavenImpl.execute(MavenPlugin.getMaven(), false, false, (context, progress) -> {
        refreshManager.refresh(state, poms, progress);
        return null;
      }, monitor);
      assertEquals("Exercise the actual first-visit tracking across three phase-2 iterations",
          List.of(true, false, false), firstVisits);
    }
  }
}
