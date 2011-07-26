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
package org.eclipse.m2e.core.ui.internal.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import org.codehaus.plexus.util.IOUtil;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.ui.internal.Messages;
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
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        int elementType = getElementType(it.next());
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

    ArtifactKey artifactKey  = getType(element, ArtifactKey.class);
    if(artifactKey != null) {
      return JAR_FILE;
    }

    IWorkingSet workingSet = getType(element, IWorkingSet.class);
    if(workingSet!=null) {
      return WORKING_SET;
    }
    
    return UNSUPPORTED;
  }

  /**
   * Checks if the object belongs to a given type and returns it or a suitable adapter.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getType(Object element, Class<T> type) {
    if(element==null) {
      return null;
    }
    if(type.isInstance(element)) {
      return (T) element;
    }
    if(element instanceof IAdaptable) {
      T adapter = (T) ((IAdaptable) element).getAdapter(type);
      if(adapter != null) {
        return adapter;
      }
    }
    return (T) Platform.getAdapterManager().getAdapter(element, type);
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
      return getWorkingSet(resource.getProject());
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

  public static IWorkingSet getWorkingSet(Object element) {
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      for(IAdaptable adaptable : workingSet.getElements()) {
        if(adaptable.getAdapter(IResource.class) == element) {
          return workingSet;
        }
      }
    }
    return null;
  }

  public static List<IWorkingSet> getAssignedWorkingSets(Object element) {
    List<IWorkingSet> list = new ArrayList<IWorkingSet>();
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      for(IAdaptable adaptable : workingSet.getElements()) {
        if(adaptable.getAdapter(IResource.class) == element) {
          list.add(workingSet);
        }
      }
    }
    return list;
  }
  
  public static ArtifactKey getArtifactKey(Object element) throws CoreException {
    if(element instanceof Artifact) {
      return new ArtifactKey(((Artifact) element));
      
    } else if(element instanceof org.sonatype.aether.graph.DependencyNode) {
      org.sonatype.aether.artifact.Artifact artifact = ((org.sonatype.aether.graph.DependencyNode) element)
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
      if(facade!=null) {
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
      return readMavenProject(new File(Util.proxy(editorInput, FileStoreEditorInputStub.class).getURI().getPath()), monitor);
    }
    
    return null;
  }
  
  private static MavenProject readMavenProject(File pomFile, IProgressMonitor monitor) throws CoreException {
    if(monitor==null) {
      monitor = new NullProgressMonitor();
    }
    
    IMaven maven = MavenPlugin.getMaven();

    MavenExecutionRequest request = maven.createExecutionRequest(monitor);
    request.setOffline(false);
    request.setUpdateSnapshots(false);
    request.setRecursive(false);
    request.setPom(pomFile);

    MavenExecutionResult result = maven.readProject(request, monitor);

    MavenProject project = result.getProject();
    if(project!=null) {
      return project;
    }

    if(result.hasExceptions()) {
      List<IStatus> statuses = new ArrayList<IStatus>();
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
}
