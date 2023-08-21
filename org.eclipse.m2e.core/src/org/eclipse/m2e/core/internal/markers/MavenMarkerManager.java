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

package org.eclipse.m2e.core.internal.markers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.internal.Messages;


@Component(service = {IMavenMarkerManager.class})
public class MavenMarkerManager implements IMavenMarkerManager {

  private static Logger log = LoggerFactory.getLogger(MavenMarkerManager.class);

  @Reference
  private IMavenConfiguration mavenConfiguration;


  @Override
  public void addMarkers(IResource pomResource, String type, MavenExecutionResult result) {
    SourceLocation defaultSourceLocation = new SourceLocation(1, 0, 0);
    List<MavenProblemInfo> allProblems = new ArrayList<>();

    allProblems.addAll(toMavenProblemInfos(pomResource, defaultSourceLocation, result.getExceptions()));

    MavenProject mavenProject = result.getProject();
    DependencyResolutionResult resolutionResult = result.getDependencyResolutionResult();
    if(resolutionResult != null) {
      allProblems
          .addAll(toMavenProblemInfos(pomResource, defaultSourceLocation, resolutionResult.getCollectionErrors()));
      for(org.eclipse.aether.graph.Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        List<Exception> exceptions = resolutionResult.getResolutionErrors(dependency);
        if(exceptions != null && exceptions.size() > 0) {
          SourceLocation sourceLocation = SourceLocationHelper.findLocation(mavenProject, dependency);
          allProblems.addAll(toMavenProblemInfos(pomResource, sourceLocation, exceptions));
        }
      }
    }

    if(mavenProject != null) {
      addMissingArtifactProblemInfos(mavenProject, defaultSourceLocation, allProblems);
    }

    addErrorMarkers(pomResource, type, allProblems);
  }

  @Override
  public IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity) {
    return addMarker(resource, type, message, lineNumber, severity, false /*isTransient*/);
  }

  private IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity,
      boolean isTransient) {
    IMarker marker = null;
    try {
      if(resource.isAccessible()) {
        if(lineNumber == -1) {
          lineNumber = 1;
        }

        //mkleint: this strongly smells like some sort of workaround for a problem with bad marker cleanup.
        //adding is adding and as such shall always be performed.
        marker = findMarker(resource, type, message, lineNumber, severity, isTransient);
        if(marker != null) {
          // This marker already exists
          return marker;
        }
        marker = resource.createMarker(type);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.TRANSIENT, isTransient);

        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        log.debug("Created marker '{}' on resource '{}'.", message, resource.getFullPath());
      }
    } catch(CoreException ex) {
      log.error("Unable to add marker; " + ex.toString(), ex); //$NON-NLS-1$
    }
    return marker;
  }

  private static <T> boolean eq(T a, T b) {
    if(a == null) {
      if(b == null) {
        return true;
      }
      return false;
    }
    return a.equals(b);
  }

  private IMarker findMarker(IResource resource, String type, String message, int lineNumber, int severity,
      boolean isTransient) throws CoreException {
    IMarker[] markers = resource.findMarkers(type, false /*includeSubtypes*/, IResource.DEPTH_ZERO);
    if(markers == null || markers.length == 0) {
      return null;
    }
    for(IMarker marker : markers) {
      if(eq(message, marker.getAttribute(IMarker.MESSAGE)) && eq(lineNumber, marker.getAttribute(IMarker.LINE_NUMBER))
          && eq(severity, marker.getAttribute(IMarker.SEVERITY))
          && eq(isTransient, marker.getAttribute(IMarker.TRANSIENT))) {
        return marker;
      }
    }
    return null;
  }

  private String getArtifactId(AbstractArtifactResolutionException rex) {
    String id = rex.getGroupId() + ":" + rex.getArtifactId() + ":" + rex.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
    if(rex.getClassifier() != null) {
      id += ":" + rex.getClassifier(); //$NON-NLS-1$
    }
    if(rex.getType() != null) {
      id += ":" + rex.getType(); //$NON-NLS-1$
    }
    return id;
  }

  private String getRootErrorMessage(Throwable ex) {
    return getRootCause(ex).getMessage();
  }

  private String getErrorMessage(Throwable ex) {
    String exMessage = ex.getMessage();
    StringWriter stackTrace = new StringWriter();
    ex.printStackTrace(new PrintWriter(stackTrace));
    String stackString = stackTrace.toString().replace("\r\n", "\n");
    return exMessage != null ? exMessage + "\n\n" + stackString : stackString;
  }

  private Throwable getRootCause(Throwable ex) {
    Throwable lastCause = ex;
    Throwable cause = lastCause.getCause();
    while(cause != null && cause != lastCause) {
      if(cause instanceof ArtifactNotFoundException) {
        cause = null;
      } else {
        lastCause = cause;
        cause = cause.getCause();
      }
    }
    return cause == null ? lastCause : cause;
  }

  private List<MavenProblemInfo> toMavenProblemInfos(IResource pomResource, SourceLocation location,
      List<? extends Throwable> exceptions) {
    List<MavenProblemInfo> result = new ArrayList<>();
    if(exceptions == null) {
      return result;
    }

    for(Throwable ex : exceptions) {
      if(ex instanceof org.eclipse.aether.transfer.ArtifactNotFoundException artifactNotFoundException) {
        ArtifactNotFoundProblemInfo problem = new ArtifactNotFoundProblemInfo(artifactNotFoundException.getArtifact(),
            mavenConfiguration.isOffline(), location);
        result.add(problem);
      } else if(ex instanceof AbstractArtifactResolutionException abstractArtifactResolutionException) {
        String errorMessage = getArtifactId(abstractArtifactResolutionException) + " " + getRootErrorMessage(ex); //$NON-NLS-1$
        result.add(new MavenProblemInfo(errorMessage, location));
      } else if(ex instanceof ProjectBuildingException projectBuildingException) {
        Collection<ModelProblem> modelProblems = getModelProblems(projectBuildingException);
        if (modelProblems != null && !modelProblems.isEmpty()) {
          for(ModelProblem problem : modelProblems) {
            String message = NLS.bind(Messages.pluginMarkerBuildError, problem.getMessage());
            int severity = (Severity.WARNING == problem.getSeverity()) ? IMarker.SEVERITY_WARNING
                : IMarker.SEVERITY_ERROR;
            SourceLocation problemLocation = SourceLocationHelper.findLocation(pomResource, problem);
            result.add(new MavenProblemInfo(message, severity, problemLocation));
          }
        } else {
          result.add(new MavenProblemInfo(getErrorMessage(ex), location));
        }
      } else {
        result.add(new MavenProblemInfo(getErrorMessage(ex), location));
      }
    }

    return result;
  }

  private Collection<ModelProblem> getModelProblems(ProjectBuildingException ex) {
    if(ex.getCause() instanceof ModelBuildingException modelBuildingException) {
      return modelBuildingException.getProblems();
    }
    Set<ModelProblem> problems = new HashSet<>();
    for(ProjectBuildingResult projectBuildingResult : ex.getResults()) {
      Collection<ModelProblem> current = projectBuildingResult.getProblems();
      if(current != null) {
        problems.addAll(projectBuildingResult.getProblems());
      }
    }
    if(!problems.isEmpty()) {
      return problems;
    }
    return null;
  }

  @Override
  public void deleteMarkers(IResource resource, String type) throws CoreException {
    deleteMarkers(resource, true /*includeSubtypes*/, type);
  }

  @Override
  public void deleteMarkers(IResource resource, boolean includeSubtypes, String type) throws CoreException {
    if(resource != null && resource.exists()) {
      resource.deleteMarkers(type, includeSubtypes, IResource.DEPTH_INFINITE);
    }
  }

  @Override
  public void deleteMarkers(IResource resource, String type, String attrName, String attrValue) throws CoreException {
    if(resource == null || !resource.exists()) {
      return;
    }

    IMarker[] markers = resource.findMarkers(type, false /*includeSubtypes*/, IResource.DEPTH_ZERO);
    for(IMarker marker : markers) {
      if(eq(attrValue, marker.getAttribute(attrName))) {
        marker.delete();
      }
    }
  }

  private void addMissingArtifactProblemInfos(MavenProject mavenProject, SourceLocation location,
      List<MavenProblemInfo> knownProblems) {
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    all_artifacts_loop: for(Artifact mavenArtifact : artifacts) {
      if(!mavenArtifact.isResolved()) {
        org.eclipse.aether.artifact.Artifact artifact = RepositoryUtils.toArtifact(mavenArtifact);
        for(MavenProblemInfo problem : knownProblems) {
          if(problem instanceof ArtifactNotFoundProblemInfo artifactNotFoundProblemInfo) {
            if(equals(artifactNotFoundProblemInfo.getArtifact(), artifact)) {
              continue all_artifacts_loop;
            }
          }
        }

        knownProblems.add(new ArtifactNotFoundProblemInfo(artifact, mavenConfiguration.isOffline(), location));
      }
    }
  }

  @Override
  public void addErrorMarkers(IResource resource, String type, Throwable ex) {
    Throwable cause = getRootCause(ex);
    if(cause instanceof CoreException cex) {
      IStatus status = cex.getStatus();
      if(status != null) {
        addMarker(resource, type, status.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/);
        IStatus[] children = status.getChildren();
        if(children != null) {
          for(IStatus childStatus : children) {
            addMarker(resource, type, childStatus.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/);
          }
        }
      }
    } else {
      addMarker(resource, type, cause.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/);
    }
  }

  @Override
  public void addErrorMarkers(IResource resource, String type, Exception ex) {
    addErrorMarkers(resource, type, (Throwable) ex);
  }

  @Override
  public void addErrorMarkers(IResource resource, String type, List<MavenProblemInfo> problems) {
    for(MavenProblemInfo problem : problems) {
      addErrorMarker(resource, type, problem);
    }
  }

  @Override
  public void addErrorMarker(IResource resource, String type, MavenProblemInfo problem) {
    IMarker marker = addMarker(resource, type, problem.getMessage(), problem.getLocation().getLineNumber(),
        problem.getSeverity());
    if(marker == null) {
      //resource is no longer accessible (eg. project being closed)
      return;
    }
    try {
      problem.processMarker(marker);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
    MarkerUtils.decorateMarker(marker);
  }

  private static boolean equals(org.eclipse.aether.artifact.Artifact a1, org.eclipse.aether.artifact.Artifact a2) {
    if(a1 == a2) {
      return true;
    }

    return a1.getArtifactId().equals(a2.getArtifactId()) && a1.getGroupId().equals(a2.getGroupId())
        && a1.getVersion().equals(a2.getVersion()) && a1.getExtension().equals(a2.getExtension())
        && a1.getClassifier().equals(a2.getClassifier());
  }
}
