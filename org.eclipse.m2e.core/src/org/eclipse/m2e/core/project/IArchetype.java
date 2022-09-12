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

package org.eclipse.m2e.core.project;

/**
 * Describes properties of an archetype.
 */
public interface IArchetype {

  /**
   * @return the group id of the archetype
   */
  String getGroupId();

  /**
   * @return the artifact id of the archetype
   */
  String getArtifactId();

  /**
   * @return the archetype version
   */
  String getVersion();

}
