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


public interface IPomFacade {

  IFile getPom();

  default File getPomFile() {
    return getPom().getLocation().toFile();
  }
}
