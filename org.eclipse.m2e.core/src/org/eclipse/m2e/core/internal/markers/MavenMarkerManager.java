/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.markers;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

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

import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;


public class MavenMarkerManager implements IMavenMarkerManager {

  private static Logger log = LoggerFactory.getLogger(MavenMarkerManager.class);

  private final IMavenConfiguration mavenConfiguration; 

  public MavenMarkerManager(IMavenConfiguration mavenConfiguration) {
    this.mavenConfiguration = mavenConfiguration;
  }
  
  public void addMarkers(IResource pomFile, String type, MavenExecutionResult result) {
    List<Throwable> exceptions = result.getExceptions();
    
    for(Throwable ex : exceptions) {
      if(ex instanceof ProjectBuildingException) {
        handleProjectBuildingException(pomFile, type, (ProjectBuildingException) ex);
      } else if(ex instanceof AbstractArtifactResolutionException) {
        AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
        String errorMessage = getArtifactId(rex) + " " + getRootErrorMessage(ex); //$NON-NLS-1$
        addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
      } else {
        handleBuildException(pomFile, type, ex);
      }
    }

    DependencyResolutionResult resolutionResult = result.getDependencyResolutionResult();
    if(resolutionResult != null) {
      // @see also addMissingArtifactMarkers
      addErrorMarkers(pomFile, type, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_metadata_resolution,
          resolutionResult.getCollectionErrors());
      for(org.sonatype.aether.graph.Dependency dependency : resolutionResult.getUnresolvedDependencies()) {
        addErrorMarkers(pomFile, type, org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_artifact,
            resolutionResult.getResolutionErrors(dependency));
      }
    }

    MavenProject mavenProject = result.getProject();
    if (mavenProject != null) {
      addMissingArtifactMarkers(pomFile, type, mavenProject);
    }
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.IMavenMarkerManager#addMarker(org.eclipse.core.resources.IResource, java.lang.String, int, int)
   */
  public IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity) {
    return addMarker(resource, type, message, lineNumber, severity, false /*isTransient*/);
  }

  private IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity, boolean isTransient) {
    IMarker marker = null;
    try {
      if(resource.isAccessible()) {
        //mkleint: this strongly smells like some sort of workaround for a problem with bad marker cleanup.
        //adding is adding and as such shall always be performed. 
        marker = findMarker(resource, type, message, lineNumber, severity, isTransient);
        if(marker != null) {
          // This marker already exists
          return marker;
        }
        marker= resource.createMarker(type);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        marker.setAttribute(IMarker.TRANSIENT, isTransient);
        
        if(lineNumber == -1) {
          lineNumber = 1;
        }
        marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
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

  private void handleProjectBuildingException(IResource pomFile, String type, ProjectBuildingException ex) {
    Throwable cause = ex.getCause();
    if(cause instanceof ModelBuildingException) {
      ModelBuildingException mbe = (ModelBuildingException) cause;
      for (ModelProblem problem : mbe.getProblems()) {
        String msg = Messages.getString("plugin.markerBuildError", problem.getMessage()); //$NON-NLS-1$
//      console.logError(msg);
        int severity = (Severity.WARNING == problem.getSeverity())? IMarker.SEVERITY_WARNING: IMarker.SEVERITY_ERROR;
        addMarker(pomFile, type, msg, 1, severity);
      }
    } else {
      handleBuildException(pomFile, type, ex);
    }
  }

  private void handleBuildException(IResource pomFile, String type, Throwable ex) {
    String msg = getErrorMessage(ex);
    addMarker(pomFile, type, msg, 1, IMarker.SEVERITY_ERROR);
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
    StringBuilder message = new StringBuilder();
    while(ex != null) {
      if(ex.getMessage() != null && message.indexOf(ex.getMessage()) < 0) {
        if(message.length() > 0) {
          message.append(": ");
        }
        message.append(ex.getClass().getSimpleName()).append(": ").append(ex.getMessage());
      }
      ex = ex.getCause();
    }
    return message.toString();
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

  
  private void addErrorMarkers(IResource pomFile, String type, String msg, List<? extends Exception> exceptions) {
    if(exceptions != null) {
      for(Exception ex : exceptions) {
        if(ex instanceof org.sonatype.aether.transfer.ArtifactNotFoundException) {
          // ignored here, handled by addMissingArtifactMarkers
        } else if(ex instanceof AbstractArtifactResolutionException) {
          AbstractArtifactResolutionException rex = (AbstractArtifactResolutionException) ex;
          String errorMessage = getArtifactId(rex) + " " + getRootErrorMessage(ex); //$NON-NLS-1$
          addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
//          console.logError(errorMessage);

        } else {
          addMarker(pomFile, type, ex.getMessage(), 1, IMarker.SEVERITY_ERROR);
//          console.logError(msg + "; " + ex.toString());
        }
      }
    }
  }

  public void deleteMarkers(IResource resource, String type) throws CoreException {
    deleteMarkers(resource, true /*includeSubtypes*/, type);
  }

  public void deleteMarkers(IResource resource, boolean includeSubtypes, String type) throws CoreException {
    if (resource != null && resource.exists()) {
      resource.deleteMarkers(type, includeSubtypes, IResource.DEPTH_INFINITE);
    }
  }

  public void deleteMarkers(IResource resource, String type, String attrName, String attrValue)
      throws CoreException {
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

  private void addMissingArtifactMarkers(IResource pomFile, String type, MavenProject mavenProject) {
//    Set<Artifact> directDependencies = mavenProject.getDependencyArtifacts();
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if (!artifact.isResolved()) {
        String errorMessage;
//        if (directDependencies.contains(artifact)) {
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_missing, artifact.toString());
//        } else {
//          errorMessage = "Missing indirectly referenced artifact " + artifact.toString();
//        }
        
        if(mavenConfiguration.isOffline()) {
          errorMessage = NLS.bind(org.eclipse.m2e.core.internal.Messages.MavenMarkerManager_error_offline, errorMessage); 
        }
        
        addMarker(pomFile, type, errorMessage, 1, IMarker.SEVERITY_ERROR);
        log.error(errorMessage);
      }
    }
  }

  public void addErrorMarkers(IResource resource, String type, Exception ex) {
    Throwable cause = getRootCause(ex);
    if(cause instanceof CoreException) {
      CoreException cex = (CoreException) cause;
      IStatus status = cex.getStatus();
      if(status != null) {
        addMarker(resource, type, status.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
        IStatus[] children = status.getChildren();
        if(children != null) {
          for(IStatus childStatus : children) {
            addMarker(resource, type, childStatus.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
          }
        }
      }
    } else {
      addMarker(resource, type, cause.getMessage(), 1, IMarker.SEVERITY_ERROR, false /*isTransient*/); //$NON-NLS-1$
    }
  }

  public void addErrorMarkers(IResource resource, String type, List<MavenProblemInfo> problems) throws CoreException {
    for(MavenProblemInfo problem : problems) {
      IMarker marker = addMarker(resource, type, problem.getMessage(), problem.getLine(), problem.getSeverity());
      problem.processMarker(marker);
      MarkerUtils.decorateMarker(marker);
    }
  }
}
