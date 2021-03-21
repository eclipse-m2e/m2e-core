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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.WorkingSets;
import org.eclipse.m2e.core.ui.internal.util.Util;
import org.eclipse.m2e.core.ui.internal.util.Util.FileStoreEditorInputStub;


/**
 * Helper methods to deal with workspace resources passed as navigator selection to actions and wizards.
 */
public class SelectionUtil {
  private static final Logger log = LoggerFactory.getLogger(SelectionUtil.class);

  public static final int UNSUPPORTED = 0;

  public static final int PROJECT_WITH_NATURE = 1;

  public static final int PROJECT_WITHOUT_NATURE = 2;

  public static final int POM_FILE = 4;

  public static final int JAR_FILE = 8;

  public static final int WORKING_SET = 16;

  /** Checks which type the given selection belongs to. */
  public static int getSelectionType(IStructuredSelection selection) {
    int type = UNSUPPORTED;
    if(selection != null) {
      for(Object name : selection) {
        int elementType = getElementType(name);
        if(elementType == UNSUPPORTED) {
          return UNSUPPORTED;
        }
        type |= elementType;
      }
    }
    return type;
  }

  /** Checks which type the given element belongs to. */
  public static int getElementType(Object element) {
    IProject project = getType(element, IProject.class);
    if(project != null) {
      try {
        if(project.hasNature(IMavenConstants.NATURE_ID)) {
          return PROJECT_WITH_NATURE;
        }
        return PROJECT_WITHOUT_NATURE;
      } catch(CoreException e) {
        // ignored
      }
    }

    IFile file = getType(element, IFile.class);
    if(file != null) {
      if(IMavenConstants.POM_FILE_NAME.equals(file.getFullPath().lastSegment())) {
        return POM_FILE;
      }
    }

    ArtifactKey artifactKey = getType(element, ArtifactKey.class);
    if(artifactKey != null) {
      return JAR_FILE;
    }

    IWorkingSet workingSet = getType(element, IWorkingSet.class);
    if(workingSet != null) {
      return WORKING_SET;
    }

    return UNSUPPORTED;
  }

  /**
   * Checks if the object belongs to a given type and returns it or a suitable adapter.
   */
  public static <T> T getType(Object element, Class<T> type) {
    if(element == null) {
      return null;
    }
    if(type.isInstance(element)) {
      return type.cast(element);
    }
    if(element instanceof IAdaptable) {
      T adapter = ((IAdaptable) element).getAdapter(type);
      if(adapter != null) {
        return adapter;
      }
    }
    return Platform.getAdapterManager().getAdapter(element, type);
  }

  public static IPath getSelectedLocation(IStructuredSelection selection) {
    Object element = selection == null ? null : selection.getFirstElement();

    IPath path = getType(element, IPath.class);
    if(path != null) {
      return path;
    }

    IResource resource = getType(element, IResource.class);
    if(resource != null) {
      return resource.getLocation();
    }

//    IPackageFragmentRoot fragment = getType(element, IResource.class);
//    if(fragment != null) {
//      IJavaProject javaProject = fragment.getJavaProject();
//      if(javaProject != null) {
//        IResource resource = getType(javaProject, IResource.class);
//        if(resource != null) {
//          return resource.getProject().getProject().getLocation();
//        }
//      }
//    }

    return null;
  }

  public static IWorkingSet getSelectedWorkingSet(IStructuredSelection selection) {
    Object element = selection == null ? null : selection.getFirstElement();
    if(element == null) {
      return null;
    }

    IWorkingSet workingSet = getType(element, IWorkingSet.class);
    if(workingSet != null) {
      return workingSet;
    }

    IResource resource = getType(element, IResource.class);
    if(resource != null) {
      return WorkingSets.getAssignedWorkingSet(resource.getProject());
    }

    return null;

//    IResource resource = getType(element, IResource.class);
//    if(resource != null) {
//      return getWorkingSet(resource);
//    }

//    IPackageFragmentRoot fragment = getType(element, IPackageFragmentRoot.class);
//    if(fragment != null) {
//      IJavaProject javaProject = fragment.getJavaProject();
//      if(javaProject != null) {
//        IResource resource = getType(javaProject, IResource.class);
//        if(resource != null) {
//          return getWorkingSet(resource.getProject());
//        }
//      }
//    }
  }

  public static ArtifactKey getArtifactKey(Object element) {
    if(element instanceof Artifact) {
      return new ArtifactKey(((Artifact) element));

    } else if(element instanceof org.eclipse.aether.graph.DependencyNode) {
      org.eclipse.aether.artifact.Artifact artifact = ((org.eclipse.aether.graph.DependencyNode) element)
          .getDependency().getArtifact();
      return new ArtifactKey(artifact);

      //getArtifactKey() used only in a handful of actions, to my knowledge none of these are currently available on
      //model.edit.Dependency instances.
//    } else if(element instanceof Dependency) {
//      Dependency dependency = (Dependency) element;
//      String groupId = dependency.getGroupId();
//      String artifactId = dependency.getArtifactId();
//      String version = dependency.getVersion();
//      
//      if(version == null) {
//        //mkleint: this looks scary
//        IEditorPart editor = getActiveEditor();
//        if(editor!=null) {
//          MavenProject mavenProject = getMavenProject(editor.getEditorInput(), null);
//          if(mavenProject!=null) {
//            Artifact a = mavenProject.getArtifactMap().get(groupId + ":" + artifactId); //$NON-NLS-1$
//            version = a.getBaseVersion();
//          }
//        }
//      }
//      return new ArtifactKey(dependency.getGroupId(), dependency.getArtifactId(), version, null);
    }

    return SelectionUtil.getType(element, ArtifactKey.class);
  }

  public static MavenProject getMavenProject(IEditorInput editorInput, IProgressMonitor monitor) throws CoreException {
    if(editorInput instanceof IFileEditorInput) {
      IFile pomFile = ((IFileEditorInput) editorInput).getFile();
      IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
      IMavenProjectFacade facade = projectManager.create(pomFile, true, monitor);
      if(facade != null) {
        return facade.getMavenProject(monitor);
      }

    } else if(editorInput instanceof IStorageEditorInput) {
      IStorageEditorInput storageInput = (IStorageEditorInput) editorInput;
      IStorage storage = storageInput.getStorage();
      IPath path = storage.getFullPath();
      if(path == null || !new File(path.toOSString()).exists()) {
        File tempPomFile = null;
        InputStream is = null;
        OutputStream os = null;
        try {
          tempPomFile = File.createTempFile("maven-pom", ".pom"); //$NON-NLS-1$ //$NON-NLS-2$
          os = new FileOutputStream(tempPomFile);
          is = storage.getContents();
          IOUtil.copy(is, os);
          return readMavenProject(tempPomFile, monitor);
        } catch(IOException ex) {
          log.error("Can't close stream", ex); //$NON-NLS-1$
        } finally {
          IOUtil.close(is);
          IOUtil.close(os);
          if(tempPomFile != null) {
            tempPomFile.delete();
          }
        }
      } else {
        return readMavenProject(path.toFile(), monitor);
      }

    } else if(editorInput.getClass().getName().endsWith("FileStoreEditorInput")) { //$NON-NLS-1$
      return readMavenProject(new File(Util.proxy(editorInput, FileStoreEditorInputStub.class).getURI().getPath()),
          monitor);
    }

    return null;
  }

  private static MavenProject readMavenProject(final File pomFile, IProgressMonitor monitor) throws CoreException {
    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    final IMaven maven = MavenPlugin.getMaven();

    IMavenExecutionContext context = maven.createExecutionContext();
    MavenExecutionRequest request = context.getExecutionRequest();
    request.setOffline(false);
    request.setUpdateSnapshots(false);
    request.setRecursive(false);

    MavenExecutionResult result = context.execute((context1, monitor1) -> maven.readMavenProject(pomFile, context1.newProjectBuildingRequest()), monitor);

    MavenProject project = result.getProject();
    if(project != null) {
      return project;
    }

    if(result.hasExceptions()) {
      List<IStatus> statuses = new ArrayList<>();
      List<Throwable> exceptions = result.getExceptions();
      for(Throwable e : exceptions) {
        statuses.add(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, e.getMessage(), e));
      }

      throw new CoreException(new MultiStatus(IMavenConstants.PLUGIN_ID, IStatus.ERROR, //
          statuses.toArray(new IStatus[statuses.size()]), Messages.SelectionUtil_error_cannot_read, null));
    }

    throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
        Messages.SelectionUtil_error_cannot_read, null));
  }

  /**
   * Finds the pom.xml from the given selection or the current active pom editor.
   * 
   * @param selection
   * @return the first pom.xml from the given selection or the current active pom editor. returns
   *         <code>null</null> if no pom was found.
   * @since 1.4.0
   */
  public static IFile getPomFileFromPomEditorOrViewSelection(ISelection selection) {
    IFile file = null;

    //350136 we need to process the selection first! that's what is relevant for any popup menu action we have.
    //the processing of active editor first might have been only relevant when we had the actions in main menu, but even
    // then the popups were wrong..
    if(selection instanceof IStructuredSelection) {
      Object o = ((IStructuredSelection) selection).iterator().next();

      if(o instanceof IProject) {
        file = ((IProject) o).getFile(IMavenConstants.POM_FILE_NAME);
      } else if(o instanceof IFile) {
        file = (IFile) o;
      }
      if(file != null) {
        return file;
      }
    }
    //
    // If I am in the POM editor I want to get hold of the IFile that is currently in the buffer
    //
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    if(window != null) {
      IWorkbenchPage page = window.getActivePage();
      if(page != null) {
        IEditorPart editor = page.getActiveEditor();
        if(editor != null) {
          IEditorInput input = editor.getEditorInput();
          if(input instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) input;
            file = fileInput.getFile();
            if(file.getName().equals(IMavenConstants.POM_FILE_NAME)) {
              return file;
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns all the Maven projects found in the given selection. If no projects are found in the selection and
   * <code>includeAll</code> is true, all workspace projects are returned.
   * 
   * @param selection
   * @param includeAll flag to return all workspace projects if selection doesn't contain any Maven projects.
   * @return an array of {@link IProject} containing all the Maven projects found in the given selection, or all the
   *         workspace projects if no Maven project was found and <code>includeAll</code> is true.
   * @since 1.4.0
   */
  public static IProject[] getProjects(ISelection selection, boolean includeAll) {
    ArrayList<IProject> projectList = new ArrayList<>();
    if(selection instanceof IStructuredSelection) {
      for(Object o : ((IStructuredSelection) selection)) {
        if(o instanceof IProject) {
          safeAdd((IProject) o, projectList);
        } else if(o instanceof IWorkingSet) {
          IWorkingSet workingSet = (IWorkingSet) o;
          for(IAdaptable adaptable : workingSet.getElements()) {
            IProject project = adaptable.getAdapter(IProject.class);
            safeAdd(project, projectList);
          }
        } else if(o instanceof IResource) {
          safeAdd(((IResource) o).getProject(), projectList);
        } else if(o instanceof IAdaptable) {
          IAdaptable adaptable = (IAdaptable) o;
          IProject project = adaptable.getAdapter(IProject.class);
          safeAdd(project, projectList);
        }
      }
    }

    if(projectList.isEmpty() && includeAll) {
      return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    return projectList.toArray(new IProject[projectList.size()]);
  }

  private static void safeAdd(IProject project, List<IProject> projectList) {
    try {
      if(project != null && project.isAccessible() && project.hasNature(IMavenConstants.NATURE_ID)
          && !projectList.contains(project)) {
        projectList.add(project);
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }

}
