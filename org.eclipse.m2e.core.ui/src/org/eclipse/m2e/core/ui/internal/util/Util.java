/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * Utility methods
 *
 * @author Eugene Kuleshov
 */
public class Util {

  /**
   * Proxy factory for compatibility stubs
   */
  @SuppressWarnings("unchecked")
  public static <T> T proxy(final Object o, Class<T> type) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), //
        new Class[] {type}, //
        (InvocationHandler) (proxy, m, args) -> {
          try {
            Method mm = o.getClass().getMethod(m.getName(), m.getParameterTypes());
            return mm.invoke(o, args);
          } catch(final NoSuchMethodException e) {
            return null;
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

  public static String nvl(String s) {
    return s == null ? "" : s; //$NON-NLS-1$
  }
}
