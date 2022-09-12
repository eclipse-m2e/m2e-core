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
package org.eclipse.m2e.core.internal.project;

import java.io.File;

import org.eclipse.core.resources.IFile;

import org.eclipse.m2e.core.embedder.IMavenExecutableLocation;

public class ResourcePomFacade implements IMavenExecutableLocation {

  protected final IFile pom;

  public ResourcePomFacade(IFile pom) {
    this.pom = pom;
  }

  @Override
  public File getPomFile() {
    return pom.getLocation().toFile();
  }
}