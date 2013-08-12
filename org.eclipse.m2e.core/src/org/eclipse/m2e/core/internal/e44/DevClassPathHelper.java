/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.e44;

import java.lang.reflect.Method;


/**
 * Reflection based adaptor that provides Equinox development classpath information and is meant to compensate for
 * implementation changes between Equinox 3.9 and 3.10.
 * 
 * @see http://dev.eclipse.org/mhonarc/lists/cross-project-issues-dev/msg09424.html
 * @since 1.5
 */
public class DevClassPathHelper {

  private static final String E43 = "org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper";

  private static final String E44 = "org.eclipse.core.internal.runtime.DevClassPathHelper";

  @SuppressWarnings("unchecked")
  private static <T> T invoke(String methodName, Class<T> returnType, Object... params) {
    try {
      ClassLoader cl = DevClassPathHelper.class.getClassLoader();
      Class<?> helper;
      try {
        helper = cl.loadClass(E44);
      } catch(ClassNotFoundException ex) {
        helper = cl.loadClass(E43);
      }
      Class<?>[] paramTypes = null;
      if(params != null) {
        paramTypes = new Class<?>[params.length];
        for(int i = 0; i < params.length; i++ ) {
          paramTypes[i] = params[i].getClass();
        }
      }
      Method method = helper.getMethod(methodName, paramTypes);
      return (T) method.invoke(null, params);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String[] getDevClassPath(String bundleSymbolicName) {
    return invoke("getDevClassPath", String[].class, bundleSymbolicName);
  }

  public static boolean inDevelopmentMode() {
    return invoke("inDevelopmentMode", boolean.class);
  }
}
