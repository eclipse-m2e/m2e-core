/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * SimpleMappingMetadataSource
 *
 * @author igor
 */
public class SimpleMappingMetadataSource implements MappingMetadataSource {

  private final List<LifecycleMappingMetadataSource> sources = new ArrayList<>();

  private final List<LifecycleMappingMetadata> lifecycleMappings = new ArrayList<>();

  private final List<PluginExecutionMetadata> pluginExecutions = new ArrayList<>();

  public SimpleMappingMetadataSource(LifecycleMappingMetadataSource source) {
    this.sources.add(source);
    this.lifecycleMappings.addAll(source.getLifecycleMappings());
    this.pluginExecutions.addAll(source.getPluginExecutions());
  }

  public SimpleMappingMetadataSource(List<LifecycleMappingMetadataSource> sources) {
    this.sources.addAll(sources);
    for(LifecycleMappingMetadataSource source : sources) {
      this.lifecycleMappings.addAll(source.getLifecycleMappings());
      this.pluginExecutions.addAll(source.getPluginExecutions());
    }
  }

  public SimpleMappingMetadataSource(LifecycleMappingMetadata lifecycleMapping) {
    //this.lifecycleMappings.add(lifecycleMapping);
    this.pluginExecutions.addAll(lifecycleMapping.getPluginExecutions());
  }

  public List<LifecycleMappingMetadataSource> getSources() {
    return this.sources;
  }

  @Override
  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType) throws DuplicateMappingException {
    if(packagingType == null) {
      return null;
    }
    LifecycleMappingMetadata mapping = null;
    for(LifecycleMappingMetadata _mapping : lifecycleMappings) {
      if(packagingType.equals(_mapping.getPackagingType())) {
        if(mapping != null) {
          throw new DuplicateMappingException();
        }
        mapping = _mapping;
      }
    }
    return mapping;
  }

  @Override
  public List<PluginExecutionMetadata> getPluginExecutionMetadata(MojoExecutionKey execution) {
    ArrayList<PluginExecutionMetadata> mappings = new ArrayList<>();
    if(execution != null) {
      for(PluginExecutionMetadata mapping : pluginExecutions) {
        if(mapping.getFilter().match(execution)) {
          mappings.add(mapping);
        }
      }
    }
    return mappings;
  }

}
