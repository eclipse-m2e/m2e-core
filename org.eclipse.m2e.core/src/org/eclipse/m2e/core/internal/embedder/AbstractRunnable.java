/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


/**
 * @since 1.5
 */
public abstract class AbstractRunnable implements ICallable<Void> {
  @Override
  public final Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
    run(context, monitor);
    return null;
  }

  protected abstract void run(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException;
}
