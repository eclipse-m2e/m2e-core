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
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * FailedMappingMetadataSource
 *
 */
public class FailedMappingMetadataSource implements MappingMetadataSource {

  private DuplicateMappingException failure;

  private MappingMetadataSource source;

  /**
   * @param source
   * @param failure
   */
  public FailedMappingMetadataSource(MappingMetadataSource source, DuplicateMappingException e) {
    this.source = source;
    this.failure = e;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource#getLifecycleMappingMetadata(java.lang.String)
   */
  @Override
  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType) throws DuplicateMappingException {
    throw failure;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.internal.lifecyclemapping.MappingMetadataSource#getPluginExecutionMetadata(org.eclipse.m2e.core.project.configurator.MojoExecutionKey)
   */
  @Override
  public List<PluginExecutionMetadata> getPluginExecutionMetadata(MojoExecutionKey execution) {
    return source.getPluginExecutionMetadata(execution);
  }

  /**
   * @return Returns the failure.
   */
  public DuplicateMappingException getFailure() {
    return this.failure;
  }

  /**
   * @return Returns the source.
   */
  public MappingMetadataSource getSource() {
    return this.source;
  }

}
