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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.embedder.AbstractRunnable;


/**
 * MavenRunner
 */
public class MavenRunner extends BlockJUnit4ClassRunner {

  public MavenRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    return new Statement() {

      public void evaluate() throws Throwable {
        class WrappedThrowable extends RuntimeException {
          public WrappedThrowable(Throwable ex) {
            super(ex);
          }
        }
        try {
          MavenPlugin.getMaven().execute(new AbstractRunnable() {
            @SuppressWarnings("synthetic-access")
            protected void run(IMavenExecutionContext context, IProgressMonitor monitor) {
              try {
                MavenRunner.super.methodInvoker(method, test);
              } catch(Throwable ex) {
                throw new WrappedThrowable(ex);
              }
            }
          }, new NullProgressMonitor());
        } catch(WrappedThrowable e) {
          throw e.getCause();
        }
      }

    };
  }

}
