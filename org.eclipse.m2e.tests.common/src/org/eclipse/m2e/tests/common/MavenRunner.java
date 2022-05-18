/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;


/**
 * MavenRunner
 */
public class MavenRunner extends BlockJUnit4ClassRunner {

  public MavenRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    return new Statement() {
      @SuppressWarnings("synthetic-access")
      public void evaluate() throws Throwable {
        Throwable catchedThrowable = MavenPlugin.getMaven().createExecutionContext().execute((c, m) -> {
          try {
            MavenRunner.super.methodInvoker(method, test);
          } catch(Throwable ex) {
            return ex;
          }
          return null;
        }, new NullProgressMonitor());
        if(catchedThrowable != null) {
          throw catchedThrowable;
        }
      }
    };
  }
}
