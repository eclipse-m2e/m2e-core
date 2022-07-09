/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.List;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;


/**
 * DuplicateLifecycleMappingMetadataException
 */
public class DuplicateLifecycleMappingMetadataException extends DuplicateMappingException {

  private static final long serialVersionUID = 1L;

  private final List<LifecycleMappingMetadata> lifecyclemappings;

  public DuplicateLifecycleMappingMetadataException(List<LifecycleMappingMetadata> lifecyclemappings) {
    super(lifecyclemappings.stream().map(LifecycleMappingMetadata::getSource).toList());
    this.lifecyclemappings = lifecyclemappings;
  }

  /**
   * @return Returns the lifecyclemappings.
   */
  public List<LifecycleMappingMetadata> getConflictingMappings() {
    return this.lifecyclemappings;
  }

}
