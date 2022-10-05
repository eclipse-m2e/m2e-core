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
package org.eclipse.m2e.core.ui.internal.archetype;

import org.apache.maven.archetype.catalog.Archetype;

import org.eclipse.m2e.core.project.IArchetype;


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

}
