/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


public class M2EUtils {

  /**
   * Helper method which creates a folder and, recursively, all its parent folders.
   *
   * @param folder The folder to create.
   * @param derived true if folder should be marked as derived
   * @param monitor the progress monitor
   * @throws CoreException if creating the given <code>folder</code> or any of its parents fails.
   */
  public static void createFolder(IContainer container, boolean derived, IProgressMonitor monitor)
      throws CoreException {
    if(!(container instanceof IFolder folder)) {
      return; // don't create projects
    }
    // Recurse until we find a parent folder which already exists.
    if(!folder.exists()) {
      IContainer parent = folder.getParent();
      // First, make sure that all parent folders exist.
      if(parent != null && !parent.exists()) {
        createFolder(parent, false, monitor);
      }
      try {
        if(!folder.exists()) {
          folder.create(true, true, monitor);
        }
      } catch(CoreException ex) {
        //Don't fail if the resource already exists, in case of a race condition
        if (!folder.exists()) {
          throw ex;
        }
      }
    }

    if(folder.isAccessible() && derived && !folder.isDerived()) {
      folder.setDerived(true, monitor);
    }
  }

  public static String getRootCauseMessage(Throwable t) {
    Throwable root = getRootCause(t);
    if(t == null) {
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
    if(project == null || project.getFile() == null) {
      return null;
    }
    //XXX copied from XmlUtils.extractProject()
    File file = new File(project.getFile().toURI());
    IPath path = IPath.fromOSString(file.getAbsolutePath());
    Stack<IFile> stack = new Stack<>();
    //here we need to find the most inner project to the path.
    //we do so by shortening the path and remembering all the resources identified.
    // at the end we pick the last one from the stack. is there a catch to it?
    IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(path);
    if(ifile != null) {
      stack.push(ifile);
    }
    while(path.segmentCount() > 1) {
      IResource ires = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
      if(ires instanceof IFile f) {
        stack.push(f);
      }
      path = path.removeFirstSegments(1);
    }
    return stack.empty() ? null : stack.pop();
  }

  public static Collection<MavenProject> getDefiningProjects(MojoExecutionKey key, Collection<MavenProject> projects) {
    Set<MavenProject> sourceProjects = new HashSet<>();
    for(MavenProject project : projects) {
      if(definesPlugin(project, key)) {
        sourceProjects.add(project);
        continue;
      }
      for(MavenProject parent = project.getParent(); parent != null; parent = parent.getParent()) {
        if(definesPlugin(parent, key)) {
          sourceProjects.add(parent);
          // Only the first instance is necessary
          break;
        }
      }
    }
    return sourceProjects;
  }

  public static boolean definesPlugin(MavenProject project, MojoExecutionKey key) {
    if(project.getOriginalModel().getBuild() == null) {
      return false;
    }
    for(Plugin p : project.getOriginalModel().getBuild().getPlugins()) {
      if(p.getGroupId().equals(key.groupId()) && p.getArtifactId().equals(key.artifactId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Thread-safe properties copy implementation.
   * <p>
   * {@link Properties#entrySet()} iterator is not thread safe and fails with {@link ConcurrentModificationException} if
   * the source properties "is structurally modified at any time after the iterator is created". The solution is to use
   * thread-safe {@link Properties#stringPropertyNames()} enumerate and copy properties.
   *
   * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=440696
   * @since 1.6
   */
  public static void copyProperties(Properties to, Properties from) {
    for(String key : from.stringPropertyNames()) {
      String value = from.getProperty(key);
      if(value != null) {
        to.put(key, value);
      }
    }
  }
}
