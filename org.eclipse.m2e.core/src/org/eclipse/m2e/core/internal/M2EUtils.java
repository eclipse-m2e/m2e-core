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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

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

}
