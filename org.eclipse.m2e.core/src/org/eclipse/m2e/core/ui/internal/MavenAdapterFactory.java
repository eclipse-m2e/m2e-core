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

package org.eclipse.m2e.core.ui.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("unchecked")
public class MavenAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_TYPES = new Class[] { IActionFilter.class };

  public Class[] getAdapterList() {
    return ADAPTER_TYPES;
  }

  public Object getAdapter(final Object adaptable, Class adapterType) {
    return new IActionFilter() {
      public boolean testAttribute(Object target, String name, String value) {
        return "label".equals(name) // //$NON-NLS-1$
            && value.equals(getStub(adaptable, LabelProviderStub.class).getLabel());
      }

      private <T> T getStub(final Object o, Class<T> type) {
        // can't use IWorkbenchAdapter here because it can cause recursion
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {type}, //
            new InvocationHandler() {
              public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                try {
                  Method method = o.getClass().getDeclaredMethod(m.getName(), m.getParameterTypes());
                  return method.invoke(o, args);
                } catch(RuntimeException ex) {
                  return null;
                } catch(Exception ex) {
                  return null;
                }
              }
            });
      }
    };
  }
  
  /**
   * A stub interface to access org.eclipse.jdt.internal.ui.packageview.ClassPathContainer#getLabel() 
   */
  public interface LabelProviderStub {
    public String getLabel();
  }
  
}

