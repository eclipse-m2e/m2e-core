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
package org.eclipse.m2e.core;

import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;

/**
 * Represents an operation that accepts two input arguments, returns no
 * result and throws a {@link CoreException}. 
 * This is the two-arity specialization of {@link Consumer}.
 * Unlike most other functional interfaces, {@code BiConsumer} is expected
 * to operate via side-effects.
 *
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * 
 * @see Consumer
 * @author Georg Tsakumagos
 */
@FunctionalInterface
public interface IBiConsumer<T, U> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws CoreException If something went wrong.
     */
    void accept(T t, U u) throws CoreException;
}