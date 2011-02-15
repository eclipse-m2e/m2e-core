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

package org.eclipse.m2e.refactoring.exclude;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer.RequiredProjectWrapper;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * This action is intended to be used in popup menus
 * 
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public class DependencyExcludeAction implements IActionDelegate {

  public static final String ID = "org.eclipse.m2e.refactoring.DependencyExclude"; //$NON-NLS-1$
  
  private IFile file;

  private ArtifactKey[] keys;

  private Model model;

  private IMavenProjectFacade projectFacade;

  private EditingDomain editingDomain;

  public void run(IAction action) {
    if(keys != null && file != null) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      ExcludeArtifactRefactoring r = new ExcludeArtifactRefactoring(projectFacade, model, editingDomain, keys, file);
      MavenExcludeWizard wizard = new MavenExcludeWizard(r);
      try {
        String titleForFailedChecks = ""; //$NON-NLS-1$
        RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
        op.run(shell, titleForFailedChecks);
      } catch(InterruptedException e) {
        // XXX
      }
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    file = null;
    keys = null;
    model = null;
    editingDomain = null;
    
    // TODO move logic into adapters
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;

      List<ArtifactKey> keys = new ArrayList<ArtifactKey>(structuredSelection.size());
      for(Object selected : structuredSelection.toArray()) {
        if (selected instanceof Artifact) {
          file = getFileFromEditor();
          keys.add(new ArtifactKey((Artifact) selected));
          model = getModelFromEditor();
          projectFacade = getFacade(file);
          editingDomain = getEditingDomain();
        } else if (selected instanceof org.sonatype.aether.graph.DependencyNode) {
          file = getFileFromEditor();
          keys.add(new ArtifactKey(((org.sonatype.aether.graph.DependencyNode) selected).getDependency().getArtifact()));
          model = getModelFromEditor();
          projectFacade = getFacade(file);
          editingDomain = getEditingDomain();
        } else if (selected instanceof RequiredProjectWrapper) {
          RequiredProjectWrapper w = (RequiredProjectWrapper) selected;
          file = getFileFromProject(w.getParentClassPathContainer().getJavaProject());
          projectFacade = getFacade(file);
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
        } else {
          keys.add(SelectionUtil.getType(selected, ArtifactKey.class));
          if (selected instanceof IJavaElement) {
            IJavaElement el = (IJavaElement) selected;
            file = getFileFromProject(el.getParent().getJavaProject());
            projectFacade = getFacade(file);
          }
        }
      }
      this.keys = keys.toArray(new ArtifactKey[keys.size()]);
    }
    if(keys.length > 0 && file != null) {
      action.setEnabled(true);
    } else {
      action.setEnabled(false);
    }
  }

  private IFile getFileFromProject(IJavaProject javaProject) {
    return javaProject.getProject().getFile("pom.xml"); //$NON-NLS-1$
  }

  private IFile getFileFromEditor() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if (part != null && part.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) part.getEditorInput();
      return input.getFile();
    }
    return null;
  }

  private Model getModelFromEditor() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if(part != null && part instanceof MavenPomEditor) {
      try {
        return ((MavenPomEditor) part).readProjectDocument();
      } catch(CoreException ex) {
        // TODO Should we do something here, or do we not care
      }
    }
    return null;
  }

  private EditingDomain getEditingDomain() {
    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if(part != null && part instanceof MavenPomEditor) {
      return ((MavenPomEditor) part).getEditingDomain();
    }
    return null;
  }

  private IMavenProjectFacade getFacade(IFile file) {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    return projectManager.create(file, true, new NullProgressMonitor());
  }

}
