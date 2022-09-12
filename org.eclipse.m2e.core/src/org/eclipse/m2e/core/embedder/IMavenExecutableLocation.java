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

import java.io.File;


/**
 * The {@link IMavenExecutableLocation} is a wrapper over the maven representation of a pom that represents something
 * that could be executed as a maven execution. You can expect it to do something similar to
 * <code>mvn -f {@link IMavenExecutableLocation#getPomFile()}</code> on the commandline.
 */
public interface IMavenExecutableLocation {

  /**
   * @return the executable Maven file, never {@code null}.
   */
  File getPomFile();
}
