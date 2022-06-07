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
package org.eclipse.m2e.core.ui.internal.archetype;

import org.apache.maven.archetype.catalog.Archetype;

import org.eclipse.m2e.core.project.IArchetype;


@SuppressWarnings("restriction")
public class MavenArchetype implements IArchetype {

  private Archetype archetype;

  public MavenArchetype(Archetype archetype) {
    this.archetype = archetype;
  }

  @Override
  public String getGroupId() {
    return archetype.getGroupId();
  }

  @Override
  public String getArtifactId() {
    return archetype.getArtifactId();
  }

  @Override
  public String getVersion() {
    return archetype.getVersion();
  }

  @Override
  public String getRepository() {
    return archetype.getRepository();
  }

}
