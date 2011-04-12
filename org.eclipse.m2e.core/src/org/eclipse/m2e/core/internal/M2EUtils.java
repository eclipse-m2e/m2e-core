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

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.maven.project.MavenProject;


public class M2EUtils {

  /**
   * Helper method which creates a folder and, recursively, all its parent folders.
   * 
   * @param folder The folder to create.
   * @param derived true if folder should be marked as derived
   * @throws CoreException if creating the given <code>folder</code> or any of its parents fails.
   */
  public static void createFolder(IFolder folder, boolean derived) throws CoreException {
    // Recurse until we find a parent folder which already exists.
    if(!folder.exists()) {
      IContainer parent = folder.getParent();
      // First, make sure that all parent folders exist.
      if(parent != null && !parent.exists()) {
        createFolder((IFolder) parent, false);
      }
      folder.create(true, true, null);
    }

    if(folder.isAccessible() && derived) {
      folder.setDerived(true);
    }
  }

  public static String getRootCauseMessage(Throwable t){
    Throwable root = getRootCause(t);
    if(t == null){
      return null;
    }
    return root.getMessage();
  }
  
  public static Throwable getRootCause(Throwable ex) {
    if(ex == null) {
      return null;
    }
    Throwable rootCause = ex;
    Throwable cause = rootCause.getCause();
    while(cause != null && cause != rootCause) {
      rootCause = cause;
      cause = cause.getCause();
    }
    return cause == null ? rootCause : cause;
  }

  /*
   * Find the pom associated with a MavenProject
   */
  public static IFile getPomFile(MavenProject project) {
    //XXX copied from XmlUtils.extractProject()
    File file = new File(project.getFile().toURI());
    IPath path = Path.fromOSString(file.getAbsolutePath());
    Stack<IFile> stack = new Stack<IFile>();
    //here we need to find the most inner project to the path.
    //we do so by shortening the path and remembering all the resources identified.
    // at the end we pick the last one from the stack. is there a catch to it?
    IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
    if(ifile != null) {
      stack.push(ifile);
    }
    while(path.segmentCount() > 1) {
      IResource ires = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
      if(ires != null && ires instanceof IFile) {
        stack.push((IFile) ires);
      }
      path = path.removeFirstSegments(1);
    }
    return stack.empty() ? null : stack.pop();
  }
}
