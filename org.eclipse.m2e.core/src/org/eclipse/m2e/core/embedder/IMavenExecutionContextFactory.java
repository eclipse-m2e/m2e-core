/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Something that is able to provide an {@link IMavenExecutionContext} suitable to perform Maven related work, either
 * by joining an already running execution or by creating a new one of its own preferred flavor.
 * <p>
 * The main purpose of this interface is to allow library code that needs to perform work inside a Maven execution
 * context to simply call {@link #execute(ICallable, IProgressMonitor)} on whatever {@link IMavenExecutionContextFactory}
 * it already has at hand (typically an {@link IMaven} or an {@link org.eclipse.m2e.core.project.IMavenProjectFacade
 * IMavenProjectFacade} instance it was given or otherwise depends on), instead of always creating a brand new,
 * possibly less suited, execution context on its own. Depending on which concrete implementation is used, the
 * execution context that is ultimately used for the work is either:
 * </p>
 * <ul>
 * <li>a <b>project specific</b> context (provided by {@link org.eclipse.m2e.core.project.IMavenProjectFacade
 * IMavenProjectFacade}), that includes project specific configuration such as the project's repositories, mirrors and
 * settings, so results are as close as possible to what an actual build of that project would use, or</li>
 * <li>a <b>global</b> context (provided by {@link IMaven}), that only has access to globally configured repositories
 * and settings. Such a global context might therefore lack some project specific features (for example project
 * scoped repositories or extensions) and results obtained through it might differ from what an actual build of a
 * specific project would produce.</li>
 * </ul>
 *
 * @since 2.9
 */
public interface IMavenExecutionContextFactory {

  /**
   * Executes the given {@link ICallable} inside a Maven execution context, either by joining an already running
   * {@link IMavenExecutionContext} associated with the current thread, or, if none is running yet, by creating and
   * using a new one obtained through {@link #createExecutionContext()}.
   * <p>
   * This is the preferred way to run code that needs a Maven execution context: implementors and callers do not have
   * to care whether such a context is already active on the current thread (in which case it is transparently
   * (re)used) or still needs to be created (in which case this factory's own, preferred, kind of context - project
   * specific or global - is used). If more control over the used execution context is required (for example to
   * configure it before executing anything), use {@link #createExecutionContext()} directly.
   * </p>
   *
   * @param <V> the result type of the callable
   * @param callable the unit of work to perform inside the execution context
   * @param monitor the progress monitor to use, or <code>null</code> if progress reporting is not desired
   * @return the result of the callable
   * @throws CoreException if the callable throws a {@link CoreException}
   */
  default <V> V execute(ICallable<V> callable, IProgressMonitor monitor) throws CoreException {
    return IMavenExecutionContext.getThreadContext().orElseGet(this::createExecutionContext).execute(callable,
        monitor);
  }

  /**
   * Creates a new {@link IMavenExecutionContext} instance.
   * <p>
   * Each call returns a fresh instance that is <b>not yet</b> joined with any context that might already be running
   * on the current thread; only once {@link IMavenExecutionContext#execute(ICallable, IProgressMonitor)} is actually
   * invoked on it, it will, if applicable, join and inherit the configuration (execution request, repository session,
   * ...) of such an already running parent context. Implementors decide what configuration (repositories, mirrors,
   * settings, extensions, ...) is applied otherwise, i.e. when the returned context is used standalone. Depending on
   * the implementation this might be a project specific context that closely mirrors what an actual Maven build of a
   * specific project would use, or a global context that only has access to information that is not tied to any
   * specific project.
   * </p>
   *
   * @return a new {@link IMavenExecutionContext}
   */
  IMavenExecutionContext createExecutionContext();

}
