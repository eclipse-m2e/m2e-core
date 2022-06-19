/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.embedder;

import java.io.File;

import org.eclipse.core.resources.IFile;


/**
 * The {@link IPomFacade} is a wrapper over the maven representation of a pom that represents something that could be
 * executed as a maven execution. You can expect it to do something similar to
 * <code>mvn -f {@link IPomFacade#getPomFile()}</code> on the commandline.
 */
public interface IPomFacade {

  /**
   * @return the pom file location in the current workspace
   */
  IFile getPom();

  /**
   * @return the underlying java file
   */
  default File getPomFile() {
    return getPom().getLocation().toFile();
  }
}
