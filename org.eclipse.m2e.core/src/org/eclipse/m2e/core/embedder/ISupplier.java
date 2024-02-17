/*******************************************************************************
 * Copyright (c) 2024 Georg Tsakumagos and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Georg Tsakumagos - initial declaration
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import org.eclipse.core.runtime.CoreException;


/**
 * Represents a supplier of results that throws a {@link CoreException}
 * <p>
 * There is no requirement that a new or distinct result be returned each time the supplier is invoked.
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is {@link #get()}.
 *
 * @param <T> the type of results supplied by this supplier
 * @since 2.1.0
 * @author Georg Tsakumagos
 */
@FunctionalInterface
public interface ISupplier<T> {

  /**
   * Gets a result.
   *
   * @return a result
   * @throws CoreException If something went wrong.
   */
  T get() throws CoreException;
}
