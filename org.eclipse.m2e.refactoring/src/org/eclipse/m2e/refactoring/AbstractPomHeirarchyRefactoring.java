/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.refactoring;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;

public abstract class AbstractPomHeirarchyRefactoring extends Refactoring {

  public static final String PLUGIN_ID = "org.eclipse.m2e.refactoring";

  protected IFile file;

  protected List<IFile> hierarchy;

  protected List<IFile> targets;

  public AbstractPomHeirarchyRefactoring(IFile file) {
    this.file = file;
  }

  /*
   * Called to notify checkInitialConditions has been called. Should be used to reset state not perform calculations
   */
  protected abstract void checkInitial(IProgressMonitor pm);

  /*
   * Called to notify checkFinalConditions has been called. Should be used to reset state not perform calculations
   */
  protected abstract void checkFinal(IProgressMonitor pm);

  /*
   * Called during checkInitialConditions, should be used to indicate missing targets, etc.
   */
  protected abstract RefactoringStatusEntry[] isReady(IProgressMonitor pm);

  /*
   * Is the pom a target for this refactoring
   */
  protected abstract boolean isAffected(IFile pom, IProgressMonitor monitor) throws CoreException;

  /*
   * Change associated with the MavenProject
   */
  protected abstract Change getChange(IFile file, IProgressMonitor pm) throws CoreException;

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    SubMonitor monitor = SubMonitor.convert(pm, 104);
    try {
      checkInitial(monitor.newChild(1));
      RefactoringStatus status = new RefactoringStatus();
      if(file == null) {
        status.addEntry(new RefactoringStatusEntry(RefactoringStatus.FATAL,
            Messages.AbstractPomHeirarchyRefactoring_noModelOrPom));
        return status;
      }
      gatherHeirarchy(monitor.newChild(1));
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      targets = new ArrayList<IFile>();
      for(IFile pom : hierarchy) {
        if(isAffected(pom, monitor.newChild(100 / hierarchy.size()))) {
          targets.add(pom);
        }
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
      }
      if(targets.isEmpty()) {
        status.addEntry(new RefactoringStatusEntry(RefactoringStatus.FATAL,
            Messages.AbstractPomHeirarchyRefactoring_noTargets));
      }
      for(RefactoringStatusEntry entry : isReady(monitor.newChild(1))) {
        status.addEntry(entry);
      }
      return status;
    } finally {
      monitor.done();
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    checkFinal(pm);
    return new RefactoringStatus();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
   */
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    CompositeChange compositeChange = new CompositeChange(getName());

    SubMonitor monitor = SubMonitor.convert(pm, targets.size() * 2);
    try {
      for(IFile file : targets) {
        Change change = getChange(file, pm);
        if(change != null) {
          compositeChange.add(change);
        }
        if(monitor.isCanceled()) {
          throw new OperationCanceledException();
        }
      }
      return compositeChange;
    } finally {
      monitor.done();
    }
  }

  protected IMavenProjectFacade getMavenProjectFacade(IFile pom) {
    return MavenPlugin.getDefault().getMavenProjectManager().create(pom, true, new NullProgressMonitor());
  }

  protected IMavenProjectFacade getMavenProjectFacade(MavenProject mavenProject) {
    return MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
  }

  /*
   * Get the heirarchy of parents that exist in the workspace
   */
  private List<IFile> gatherHeirarchy(IProgressMonitor progressMonitor) throws CoreException {
    SubMonitor monitor = SubMonitor.convert(progressMonitor, 3);
    try {
      IMaven maven = MavenPlugin.getDefault().getMaven();
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

      IMavenProjectFacade projectFacade = getMavenProjectFacade(file);
      MavenProject project = projectFacade.getMavenProject(monitor.newChild(1));
      maven.detachFromSession(project);

      hierarchy = new LinkedList<IFile>();
      hierarchy.add(file);

      gatherDescendants(projectFacade, projectManager, monitor);
      gatherAncestors(projectFacade, projectManager, monitor);

      return hierarchy;
    } finally {
      monitor.done();
    }
  }

  private void gatherAncestors(IMavenProjectFacade projectFacade, MavenProjectManager projectManager,
      IProgressMonitor pm)
      throws CoreException {
    MavenExecutionRequest request = projectManager.createExecutionRequest(projectFacade, pm);
    MavenProject project = MavenPlugin.getDefault().getMaven()
        .resolveParentProject(request, projectFacade.getMavenProject(pm), pm);
    pm.worked(1);
    if(project != null) {
      IMavenProjectFacade parentFacade = getMavenProjectFacade(project);
      if(parentFacade != null) {
        hierarchy.add(parentFacade.getPom());
        gatherDescendants(parentFacade, projectManager, pm);
        gatherAncestors(parentFacade, projectManager, pm);
      }
    }
  }

  private void gatherDescendants(IMavenProjectFacade projectFacade, MavenProjectManager projectManager,
      IProgressMonitor pm) throws CoreException {

    for(String module : projectFacade.getMavenProjectModules()) {
      IPath modulePath = projectFacade.getProject().getFullPath().append(module);
      modulePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(modulePath);
      ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(modulePath);

      for(IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        if(p.getLocation().equals(modulePath)) {
          IMavenProjectFacade facade = projectManager.getProject(p);
          if(facade != null && !hierarchy.contains(facade)) {
            if(!hierarchy.contains(facade.getPom())) {
              hierarchy.add(facade.getPom());
              gatherDescendants(facade, projectManager, pm);
            }
          }
        }
      }
    }
  }
}
