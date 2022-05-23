/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.embedder;

/**
 * a {@link IMavenExecutionContextFactory} is a supplier for new {@link IMavenExecutionContext}s
 * 
 * @since 2.0
 */
public interface IMavenExecutionContextFactory {

  /**
   * @return a fresh {@link IMavenExecutionContext}
   */
  IMavenExecutionContext createExecutionContext();

}
