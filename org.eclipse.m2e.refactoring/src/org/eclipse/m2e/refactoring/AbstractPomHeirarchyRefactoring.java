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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractPomHeirarchyRefactoring extends Refactoring {

  public static final String PLUGIN_ID = "org.eclipse.m2e.refactoring";

  protected IMavenProjectFacade projectFacade;

  protected EditingDomain editingDomain;

  protected Model model;

  protected IFile file;

  protected List<MavenProject> hierarchy;

  protected List<MavenProject> targets;

  private Map<IFile, Model> modelCache = new HashMap<IFile, Model>();

  public AbstractPomHeirarchyRefactoring(IMavenProjectFacade projectFacade, Model model, EditingDomain editingDomain, IFile file) {
    this.editingDomain = editingDomain;
    this.file = file;
    this.model = model;
    this.projectFacade = projectFacade;
    modelCache.put(file, model);
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
   * Is the project a target for this refactoring
   */
  protected abstract boolean isChanged(EditingDomain editingDomain, MavenProject project, IProgressMonitor pm)
      throws CoreException, OperationCanceledException, IOException;

  /*
   * Change associated with the MavenProject
   */
  protected abstract Change getChange(MavenProject project, IProgressMonitor pm) throws CoreException;

  /* (non-Javadoc)
   * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
   */
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    SubMonitor monitor = SubMonitor.convert(pm, 103);
    try {
      checkInitial(monitor.newChild(1));
      RefactoringStatus status = new RefactoringStatus();
      if(model == null && file == null) {
        status.addEntry(new RefactoringStatusEntry(RefactoringStatus.FATAL,
            Messages.AbstractPomHeirarchyRefactoring_noModelOrPom));
        return status;
      }
      loadWorkspaceAncestors(monitor.newChild(1));
      if(monitor.isCanceled()) {
        throw new OperationCanceledException();
      }

      targets = new ArrayList<MavenProject>();
      for(MavenProject project : hierarchy) {
        IMavenProjectFacade facade = getMavenProjectFacade(project);
        if(isChanged(getEditingDomain(facade), project, monitor.newChild(100 / hierarchy.size()))) {
          targets.add(project);
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
    } catch(IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
          Messages.AbstractPomHeirarchyRefactoring_failedToLoadModel,
          e));
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
      for(MavenProject project : targets) {
        Change change = getChange(project, monitor.newChild(1));
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

  /*
   * Get the EditingDomain for a given project
   */
  protected EditingDomain getEditingDomain(IMavenProjectFacade facade) {
    if(facade.getMavenProject().equals(hierarchy.get(0)) && editingDomain != null) {
      return editingDomain;
    }
    // Check if an editor is open
    MavenPomEditor editor = getOpenEditor(facade);
    if(editor != null) {
      return editor.getEditingDomain();
    }
    // Create a fake one
    List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
    factories.add(new ResourceItemProviderAdapterFactory());
    factories.add(new ReflectiveItemProviderAdapterFactory());

    ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(factories);
    BasicCommandStack commandStack = new BasicCommandStack();
    return new AdapterFactoryEditingDomain(adapterFactory, //
        commandStack, new HashMap<Resource, Boolean>());
  }

  private MavenPomEditor getOpenEditor(IMavenProjectFacade facade) {
    for(IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      for(IWorkbenchPage page : window.getPages()) {
        for(IEditorReference editor : page.getEditorReferences()) {
          if(MavenPomEditor.EDITOR_ID.equals(editor.getId())) {
            IEditorPart part = editor.getEditor(false);
            if(part instanceof MavenPomEditor) {
              MavenPomEditor mpe = (MavenPomEditor) part;
              if(facade.getPom().equals(mpe.getPomFile())) {
                return mpe;
              }
            }
          }
        }
      }
    }
    return null;
  }

  /*
   * Get the model associated with the Project
   */
  protected Model getModel(MavenProject mavenProject) throws CoreException, IOException {
    IFile pomFile = getMavenProjectFacade(mavenProject).getPom();
    Model m = modelCache.get(pomFile);
    if(m == null) {
      MavenPomEditor editor = getOpenEditor(getMavenProjectFacade(mavenProject));
      if(editor != null) {
        m = editor.readProjectDocument();
      } else {
        PomResourceImpl resource = MavenPlugin.getDefault().getMavenModelManager().loadResource(pomFile);
        resource.load(Collections.EMPTY_MAP);
        m = resource.getModel();
      }
      modelCache.put(pomFile, m);
    }
    return m;
  }

  protected IMavenProjectFacade getMavenProjectFacade(MavenProject mavenProject) {
    return MavenPlugin.getDefault().getMavenProjectManager()
        .getMavenProject(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion());
  }

  protected IFile getPomFile(MavenProject project) {
    IMavenProjectFacade facade = getMavenProjectFacade(project);
    if(facade.equals(projectFacade)) {
      return file;
    }
    return facade.getPom();
  }

  /*
   * Get the heirarchy of parents that exist in the workspace
   */
  private List<MavenProject> loadWorkspaceAncestors(IProgressMonitor progressMonitor) throws CoreException {
    SubMonitor monitor = SubMonitor.convert(progressMonitor);
    try {
      IMaven maven = MavenPlugin.getDefault().getMaven();
      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();

      MavenProject project = projectFacade.getMavenProject();
      maven.detachFromSession(project);

      hierarchy = new LinkedList<MavenProject>();
      hierarchy.add(project);
      while(project.getModel().getParent() != null) {
        if(monitor.isCanceled()) {
          return null;
        }
        MavenExecutionRequest request = projectManager.createExecutionRequest(projectFacade, monitor);
        project = maven.resolveParentProject(request, project, monitor);
        if(getMavenProjectFacade(project) != null) {
          hierarchy.add(project);
        }
      }

      return hierarchy;
    } finally {
      monitor.done();
    }
  }

  /*
   * Wraps a {@link org.eclipse.emf.common.command.Command} to the pom in a Resource
   */
  protected static class PomResourceChange extends ResourceChange {
    private Command command;

    private IFile pom;

    private EditingDomain domain;

    private PomResourceChange redo;

    private String name;

    private PomResourceChange(PomResourceChange redo, EditingDomain domain, Command command, IFile pom, String name) {
      this(domain, command, pom, name);
      this.redo = redo;
    }

    public PomResourceChange(EditingDomain domain, Command command, IFile pom, String changes) {
      this.command = command;
      this.domain = domain;
      this.pom = pom;
      this.name = pom.getFullPath().toString() + " - " + changes;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.resource.ResourceChange#getModifiedResource()
     */
    protected IResource getModifiedResource() {
      return pom;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#getName()
     */
    public String getName() {
      return name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
     */
    public Change perform(IProgressMonitor pm) throws CoreException {
      SubMonitor monitor = SubMonitor.convert(pm, 3);
      try {
        Display.getDefault().syncExec(new Runnable() {
          public void run() {
            domain.getCommandStack().execute(command);
          }
        });
        monitor.worked(2);

        if(redo == null && domain.getCommandStack().canUndo()) {
          redo = new PomResourceChange(this, domain, domain.getCommandStack().getUndoCommand(), pom, name);
        }
        monitor.worked(1);
        return redo;
      } finally {
        monitor.done();
      }
    }
  }
}
