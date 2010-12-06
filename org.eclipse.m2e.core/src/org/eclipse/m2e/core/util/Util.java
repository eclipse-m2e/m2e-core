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

package org.eclipse.m2e.core.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;

import org.eclipse.m2e.core.core.MavenLogger;


/**
 * Utility methods
 * 
 * @author Eugene Kuleshov
 */
public class Util {

  public static boolean isEclipseVersion(int major, int minor) {
    Bundle bundle = ResourcesPlugin.getPlugin().getBundle();
    String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
    Version v = Version.parseVersion(version);
    return v.getMajor() == major && v.getMinor() == minor;
  }

  /**
   * Proxy factory for compatibility stubs
   */
  @SuppressWarnings("unchecked")
  public static <T> T proxy(final Object o, Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), //
        new Class[] {type}, //
        new InvocationHandler() {
          public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            try {
              Method mm = o.getClass().getMethod(m.getName(), m.getParameterTypes());
              return mm.invoke(o, args);
            } catch(final NoSuchMethodException e) {
              return null;
            }
          }
        });
  }

  /**
   * Stub interface for FileStoreEditorInput
   * 
   * @see Util#proxy(Object, Class)
   */
  public static interface FileStoreEditorInputStub {
    public java.net.URI getURI();
  }

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

  public static void setDerived(IFolder folder, boolean derived) throws CoreException {
    if(folder.isAccessible()) {
      folder.setDerived(derived);
    }
  }

  /**
   * Substitute any variable
   */
  public static String substituteVar(String s) {
    if(s == null) {
      return s;
    }
    try {
      return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(s);
    } catch(CoreException e) {
      MavenLogger.log(e);
      return null;
    }
  }

  public static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }
}
