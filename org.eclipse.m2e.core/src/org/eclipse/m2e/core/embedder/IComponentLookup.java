/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 *
 * This program and the accompanying materials
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

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;


/**
 * A {@link IComponentLookup} looks up a component in a given context
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IComponentLookup {

  /**
   * Lookup a single component from this context.
   * 
   * @param clazz the requested role
   * @return The component instance requested.
   * @throws CoreException if the requested component is not available
   */
  <C> C lookup(Class<C> type) throws CoreException;

  /**
   * Look up a list of components in a given context
   * 
   * @param <C>
   * @param extensionType
   * @return a (possibly empty) list of the requested components
   * @throws CoreException
   */
  <C> Collection<C> lookupCollection(Class<C> type) throws CoreException;
}
