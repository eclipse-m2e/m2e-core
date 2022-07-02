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

import java.util.Arrays;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;


/**
 * DuplicatePluginExecutionMetadataException
 */
public class DuplicatePluginExecutionMetadataException extends DuplicateMappingException {

  private static final long serialVersionUID = 1L;

  private final PluginExecutionMetadata[] pluginExecutionMetadatas;

  public DuplicatePluginExecutionMetadataException(PluginExecutionMetadata... pluginExecutionMetadatas) {
    super(Arrays.stream(pluginExecutionMetadatas).map(PluginExecutionMetadata::getSource)
        .toArray(LifecycleMappingMetadataSource[]::new));
    this.pluginExecutionMetadatas = pluginExecutionMetadatas;
  }

  /**
   * @return Returns the sources.
   */
  public PluginExecutionMetadata[] getConflictingMetadata() {
    return this.pluginExecutionMetadatas;
  }
}
