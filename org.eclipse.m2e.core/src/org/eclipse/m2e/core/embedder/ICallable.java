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

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * @see IMavenExecutionContext
 * @since 1.4
 */
public interface ICallable<V> {
  V call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException;
}
