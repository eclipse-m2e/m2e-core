/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.markers.SourceLocation;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


@SuppressWarnings("restriction")
public class WorkspaceHelpers {
  public static String getModelId(MavenProject mavenProject) {
    return mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ":" + mavenProject.getVersion();
  }

  public static void cleanWorkspace() throws InterruptedException, CoreException {
    Exception cause = null;
    int i;
    for(i = 0; i < 10; i++ ) {
      try {
        System.gc();
        doCleanWorkspace();
      } catch(InterruptedException e) {
        throw e;
      } catch(OperationCanceledException e) {
        throw e;
      } catch(Exception e) {
        cause = e;
        e.printStackTrace();
        System.out.println(i);
        Thread.sleep(6 * 1000);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(
        new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Could not delete workspace resources (after " + i
            + " retries): " + Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()), cause));
  }

  private static void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run((IWorkspaceRunnable) monitor -> {
      IProject[] projects = workspace.getRoot().getProjects();
      for(IProject project : projects) {
        project.delete(true, true, monitor);
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete(new NullProgressMonitor());

    File[] files = workspace.getRoot().getLocation().toFile().listFiles();
    if(files != null) {
      for(File file : files) {
        if(!".metadata".equals(file.getName())) {
          if(file.isDirectory()) {
            FileUtils.deleteDirectory(file);
          } else {
            if(!file.delete()) {
              throw new IOException("Could not delete file " + file.getCanonicalPath());
            }
          }
        }
      }
    }
  }

  public static String toString(IMarker[] markers) {
    if(markers != null) {
      return toString(Arrays.asList(markers));
    }
    return "";
  }

  public static String toString(List<IMarker> markers) {
    String sep = "";
    StringBuilder sb = new StringBuilder();
    if(markers != null) {
      for(IMarker marker : markers) {
        try {
          sb.append(sep).append(toString(marker));
        } catch(CoreException ex) {
          // ignore
        }
        sep = ", ";
      }
    }
    return sb.toString();
  }

  protected static String toString(IMarker marker) throws CoreException {
    return "Type=" + marker.getType() + ":Message=" + marker.getAttribute(IMarker.MESSAGE) + ":LineNumber="
        + marker.getAttribute(IMarker.LINE_NUMBER);
  }

  public static List<IMarker> findMarkers(IProject project, int targetSeverity) throws CoreException {
    return findMarkers(project, targetSeverity, null /*withAttribute*/);
  }

  public static List<IMarker> findMarkers(IProject project, int targetSeverity, String withAttribute)
      throws CoreException {
    SortedMap<IMarker, IMarker> errors = new TreeMap<IMarker, IMarker>((o1, o2) -> {
      int lineNumber1 = o1.getAttribute(IMarker.LINE_NUMBER, -1);
      int lineNumber2 = o2.getAttribute(IMarker.LINE_NUMBER, -1);
      if(lineNumber1 < lineNumber2) {
        return -1;
      }
      if(lineNumber1 > lineNumber2) {
        return 1;
      }
      // Markers on the same line
      String message1 = o1.getAttribute(IMarker.MESSAGE, "");
      String message2 = o2.getAttribute(IMarker.MESSAGE, "");
      return message1.compareTo(message2);
    });
    for(IMarker marker : project.findMarkers(null /* all markers */, true /* subtypes */, IResource.DEPTH_INFINITE)) {
      int severity = marker.getAttribute(IMarker.SEVERITY, 0);
      if(severity != targetSeverity) {
        continue;
      }
      if(withAttribute != null) {
        String attribute = marker.getAttribute(withAttribute, null);
        if(attribute == null) {
          continue;
        }
      }
      errors.put(marker, marker);
    }
    List<IMarker> result = new ArrayList<IMarker>();
    result.addAll(errors.keySet());
    return result;
  }

  public static List<IMarker> findWarningMarkers(IProject project) throws CoreException {
    return findMarkers(project, IMarker.SEVERITY_WARNING);
  }

  public static List<IMarker> findErrorMarkers(IProject project) throws CoreException {
    return findMarkers(project, IMarker.SEVERITY_ERROR);
  }

  public static void assertNoErrors(IProject project) throws CoreException {
    List<IMarker> markers = findErrorMarkers(project);
    Assert.assertEquals("Unexpected error markers " + toString(markers), 0, markers.size());
  }

  public static void assertNoWarnings(IProject project) throws CoreException {
    List<IMarker> markers = findWarningMarkers(project);
    Assert.assertEquals("Unexpected warning markers " + toString(markers), 0, markers.size());
  }

  public static IMarker assertErrorMarker(String type, String message, Integer lineNumber, String resourceRelativePath,
      IProject project) throws Exception {
    return assertMarker(type, IMarker.SEVERITY_ERROR, message, lineNumber, resourceRelativePath, project);
  }

  public static IMarker assertWarningMarker(String type, String message, Integer lineNumber,
      String resourceRelativePath, IProject project) throws Exception {
    return assertMarker(type, IMarker.SEVERITY_WARNING, message, lineNumber, resourceRelativePath, project);
  }

  private static IMarker findMarker(String type, String message, Integer lineNumber, String resourceRelativePath,
      List<IMarker> markers) throws Exception {
    for(IMarker marker : markers) {
      if(type != null && !type.equals(marker.getType())) {
        continue;
      }
      if(message != null && !marker.getAttribute(IMarker.MESSAGE, "").startsWith(message)) {
        continue;
      }
      if(lineNumber != null && !lineNumber.equals(marker.getAttribute(IMarker.LINE_NUMBER))) {
        continue;
      }
      if(type != null && type.startsWith(IMavenConstants.MARKER_ID)) {
        Assert.assertEquals("Marker not persistent:" + toString(marker), false, marker.getAttribute(IMarker.TRANSIENT));
      }

      if(resourceRelativePath == null) {
        resourceRelativePath = "";
      }
      Assert.assertEquals("Marker not on the expected resource:" + toString(marker), resourceRelativePath,
          marker.getResource().getProjectRelativePath().toString());

      return marker;
    }
    return null;
  }

  public static IMarker assertErrorMarker(String type, String message, Integer lineNumber, IProject project)
      throws Exception {
    return assertMarker(type, IMarker.SEVERITY_ERROR, message, lineNumber, "pom.xml", project);
  }

  public static IMarker assertWarningMarker(String type, String message, Integer lineNumber, IProject project)
      throws Exception {
    return assertMarker(type, IMarker.SEVERITY_WARNING, message, lineNumber, "pom.xml", project);
  }

  public static IMarker assertMarker(String type, int severity, String message, Integer lineNumber,
      String resourceRelativePath, IProject project) throws Exception {
    List<IMarker> markers = findMarkers(project, severity);
    IMarker marker = findMarker(type, message, lineNumber, resourceRelativePath, markers);
    if(marker == null) {
      Assert.fail(
          "Expected marker not found. Found " + (markers.isEmpty() ? "no markers" : "markers :") + toString(markers));
    }
    Assert.assertTrue("Marker type " + type + " is not a subtype of " + IMarker.PROBLEM,
        marker.isSubtypeOf(IMarker.PROBLEM));
    return marker;
  }

  public static void assertLifecycleIdErrorMarkerAttributes(IProject project, String lifecycleId) throws CoreException {
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    Assert.assertNotNull(errorMarkers);
    Assert.assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    assertLifecycleIdErrorMarkerAttributes(errorMarkers.get(0), lifecycleId);
  }

  public static void assertConfiguratorErrorMarkerAttributes(IProject project, String configuratorId)
      throws CoreException {
    List<IMarker> errorMarkers = WorkspaceHelpers.findErrorMarkers(project);
    Assert.assertNotNull(errorMarkers);
    Assert.assertEquals(WorkspaceHelpers.toString(errorMarkers), 1, errorMarkers.size());
    assertConfiguratorErrorMarkerAttributes(errorMarkers.get(0), configuratorId);
  }

  public static void assertLifecycleIdErrorMarkerAttributes(IMarker marker, String lifecycleId) {
    Assert.assertEquals("Marker's editor hint", IMavenConstants.EDITOR_HINT_UNKNOWN_LIFECYCLE_ID,
        marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null));
    Assert.assertEquals("Marker's lifecycle", lifecycleId,
        marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, null));
  }

  public static void assertConfiguratorErrorMarkerAttributes(IMarker marker, String configuratorId) {
    Assert.assertEquals("Marker's editor hint", IMavenConstants.EDITOR_HINT_MISSING_CONFIGURATOR,
        marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null));
    Assert.assertEquals("Marker's ConfiguratorID", configuratorId,
        marker.getAttribute(IMavenConstants.MARKER_ATTR_CONFIGURATOR_ID, null));
  }

  public static void assertErrorMarkerAttributes(IMarker marker, MojoExecutionKey mojoExecution) {
    Assert.assertEquals(IMavenConstants.EDITOR_HINT_NOT_COVERED_MOJO_EXECUTION,
        marker.getAttribute(IMavenConstants.MARKER_ATTR_EDITOR_HINT, null));
    //TODO what parameters are important here for the hints?
    Assert.assertEquals("Marker's groupID", mojoExecution.getGroupId(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_GROUP_ID, null));
    Assert.assertEquals("Marker's artifactId", mojoExecution.getArtifactId(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_ARTIFACT_ID, null));
    Assert.assertEquals("Marker's executionId", mojoExecution.getExecutionId(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_EXECUTION_ID, null));
    Assert.assertEquals("Marker's goal", mojoExecution.getGoal(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_GOAL, null));
    Assert.assertEquals("Marker's version", mojoExecution.getVersion(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_VERSION, null));
    Assert.assertEquals("Marker's lifecyclePhase", mojoExecution.getLifecyclePhase(),
        marker.getAttribute(IMavenConstants.MARKER_ATTR_LIFECYCLE_PHASE, null));
  }

  public static void assertMarkerLocation(SourceLocation markerLocation, IMarker marker) throws CoreException {
    Assert.assertEquals("Wrong line number", markerLocation.getLineNumber(), marker.getAttribute(IMarker.LINE_NUMBER));
    Assert.assertEquals("Wrong column start", markerLocation.getColumnStart(),
        marker.getAttribute(IMavenConstants.MARKER_COLUMN_START));
    Assert.assertEquals("Wrong column end", markerLocation.getColumnEnd(),
        marker.getAttribute(IMavenConstants.MARKER_COLUMN_END));

    markerLocation = markerLocation.getLinkedLocation();
    if(markerLocation == null) {
      return;
    }

    Assert.assertEquals("Wrong cause resource path", markerLocation.getResourcePath(),
        marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_PATH));
    Assert.assertEquals("Wrong cause resource id", markerLocation.getResourceId(),
        marker.getAttribute(IMavenConstants.MARKER_CAUSE_RESOURCE_ID));
    Assert.assertEquals("Wrong cause line number", markerLocation.getLineNumber(),
        marker.getAttribute(IMavenConstants.MARKER_CAUSE_LINE_NUMBER));
    Assert.assertEquals("Wrong cause column start", markerLocation.getColumnStart(),
        marker.getAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_START));
    Assert.assertEquals("Wrong cause column end", markerLocation.getColumnEnd(),
        marker.getAttribute(IMavenConstants.MARKER_CAUSE_COLUMN_END));
  }
}
