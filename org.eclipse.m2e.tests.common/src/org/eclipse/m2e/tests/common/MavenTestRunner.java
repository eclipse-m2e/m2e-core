/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Aleksandar Kurtakov - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;


/**
 * Runs tests requiring a Maven execution context, as declared with {@link RequireMavenExecutionContext} on the test
 * method, the test class or one of its superclasses, inside one. The context is created for the test method only,
 * after the {@code @Before} methods have set up the test.
 */
public class MavenTestRunner extends BlockJUnit4ClassRunner {

  public MavenTestRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    Statement statement = super.methodInvoker(method, test);
    if(!requiresMavenExecutionContext(method)) {
      return statement;
    }
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Throwable throwable = MavenPlugin.getMaven().createExecutionContext().execute((c, m) -> {
          try {
            statement.evaluate();
          } catch(Throwable ex) {
            return ex;
          }
          return null;
        }, new NullProgressMonitor());
        if(throwable != null) {
          throw throwable;
        }
      }
    };
  }

  private boolean requiresMavenExecutionContext(FrameworkMethod method) {
    RequireMavenExecutionContext annotation = method.getAnnotation(RequireMavenExecutionContext.class);
    for(Class<?> type = getTestClass().getJavaClass(); annotation == null && type != null; type = type
        .getSuperclass()) {
      annotation = type.getAnnotation(RequireMavenExecutionContext.class);
    }
    return annotation != null && annotation.require();
  }
}
