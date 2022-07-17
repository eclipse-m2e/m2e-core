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