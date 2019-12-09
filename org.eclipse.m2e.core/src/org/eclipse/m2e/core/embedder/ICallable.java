/*******************************************************************************
 * Copyright (c) 2013, 2018 Igor Fedorenko and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *      Mickael Istria (Red Hat Inc.) - @FunctionInterface
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * @see IMavenExecutionContext
 * @since 1.4
 */
@FunctionalInterface
public interface ICallable<V> {
  V call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException;
}
