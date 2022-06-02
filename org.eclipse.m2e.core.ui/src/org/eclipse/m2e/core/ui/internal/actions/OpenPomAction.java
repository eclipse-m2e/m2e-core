/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.actions;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;


/**
 * Open POM Action
 *
 * @author Eugene Kuleshov
 */
public class OpenPomAction extends ActionDelegate implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

  private static final Logger log = LoggerFactory.getLogger(OpenPomAction.class);

  String type = IIndex.SEARCH_ARTIFACT;

  private IStructuredSelection selection;

  private MavenProject mavenProject;

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  @Override
  public void init(IWorkbenchWindow window) {
  }

  @Override
  public void selectionChanged(IAction action, ISelection selection) {
    this.selection = selection instanceof IStructuredSelection sel ? sel : null;
  }

  @Override
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    mavenProject = targetPart.getAdapter(MavenProject.class);
  }

  protected MavenProject getMavenProject() {
    return mavenProject;
  }

  @Override
  public void run(IAction action) {
    //TODO mkleint: this asks for rewrite.. having one action that does 2 quite different things based
    // on something as vague as selection passed in is unreadable..
    if(selection != null) {
      Object element = this.selection.getFirstElement();
      if(IIndex.SEARCH_ARTIFACT.equals(type) && element != null) {
        final ArtifactKey ak = SelectionUtil.getArtifactKey(element);
        if(ak != null) {
          new Job(Messages.OpenPomAction_job_opening) {
              @Override
              protected IStatus run(IProgressMonitor monitor) {
              openEditor(ak.getGroupId(), ak.getArtifactId(), ak.getVersion(), getMavenProject(), monitor);
              return Status.OK_STATUS;
            }
          }.schedule();
          return;
        }
      }
    }

    String title = Messages.OpenPomAction_title_pom;

    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createOpenPomDialog(shell, title);
    if(dialog.open() == Window.OK) {
      final IndexedArtifactFile iaf = (IndexedArtifactFile) dialog.getFirstResult();
      new Job(Messages.OpenPomAction_job_opening) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
          if(iaf != null) {
            openEditor(iaf.group, iaf.artifact, iaf.version, monitor);
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    }
  }

  public static IEditorPart openEditor(String groupId, String artifactId, String version, IProgressMonitor monitor) {
    return openEditor(groupId, artifactId, version, null, monitor);
  }

  public static IEditorPart openEditor(String groupId, String artifactId, String version, MavenProject project,
      IProgressMonitor monitor) {
    return new OpenPomAction().openPomEditor(groupId, artifactId, version, project, monitor);
  }

  public IEditorPart openPomEditor(String groupId, String artifactId, String version, MavenProject project,
      IProgressMonitor monitor) {
    if(groupId.length() > 0 && artifactId.length() > 0) {
      final String name = groupId + ":" + artifactId + ":" + version + ".pom"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      try {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(groupId, artifactId, version);
        if(projectFacade != null) {
          final IFile pomFile = projectFacade.getPom();
          return openPomEditor(new FileEditorInput(pomFile), name);
        }

        IMaven maven = MavenPlugin.getMaven();

        List<ArtifactRepository> artifactRepositories;
        if(project != null) {
          artifactRepositories = project.getRemoteArtifactRepositories();
        } else {
          artifactRepositories = maven.getArtifactRepositories();
        }

        Artifact artifact = maven.resolve(groupId, artifactId, version, "pom", null, artifactRepositories, monitor); //$NON-NLS-1$

        File file = artifact.getFile();
        if(file != null) {
          IFileStore store = EFS.getStore(file.toURI());
          if (store != null) {
            return openPomEditor(
                new FileStoreEditorInput(store),
                name);
          }
        }

        openDialog(NLS.bind(Messages.OpenPomAction_error_download, name));

      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
        openDialog(ex.getMessage() + "\n" + ex.toString()); //$NON-NLS-1$
      }
    }

    return null;
  }

  public static IEditorPart openEditor(final IEditorInput editorInput, final String name) {
    return new OpenPomAction().openPomEditor(editorInput, name);
  }

  public IEditorPart openPomEditor(final IEditorInput editorInput, final String name) {
    final IEditorPart[] part = new IEditorPart[1];
    PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
      IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
      IContentType contentType = contentTypeManager.findContentTypeFor(name);
      IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
      IEditorDescriptor editor = editorRegistry.getDefaultEditor(name, contentType);
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if(window != null) {
        IWorkbenchPage page = window.getActivePage();
        if(page != null) {
          try {
            part[0] = page.openEditor(editorInput, editor.getId());
          } catch(PartInitException ex) {
            openDialog(NLS.bind(Messages.OpenPomAction_33, editorInput.getName(), ex.toString()));
          }
        }
      }
    });
    return part[0];
  }

  protected void openDialog(final String msg) {
    Runnable r = () -> MessageDialog.openInformation(Display.getDefault().getActiveShell(), //
        Messages.OpenPomAction_open_title, msg);
    Display display = PlatformUI.getWorkbench().getDisplay();
    if(display == Display.getCurrent()) {
      r.run();
    } else {
      display.asyncExec(r);
    }
  }

}
