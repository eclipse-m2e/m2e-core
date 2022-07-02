/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - #549 - Improve conflict handling of lifecycle mappings
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecyclemapping;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingFilter;
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

  private final List<LifecycleMappingFilter> mappingFilters = new ArrayList<>();

  public SimpleMappingMetadataSource(LifecycleMappingMetadataSource source) {
    this.sources.add(source);
    this.lifecycleMappings.addAll(source.getLifecycleMappings());
    this.pluginExecutions.addAll(source.getPluginExecutions());
    this.mappingFilters.addAll(source.getLifecycleMappingFilters());
  }

  public SimpleMappingMetadataSource(List<LifecycleMappingMetadataSource> sources) {
    this.sources.addAll(sources);
    for(LifecycleMappingMetadataSource source : sources) {
      this.lifecycleMappings.addAll(source.getLifecycleMappings());
      this.pluginExecutions.addAll(source.getPluginExecutions());
      this.mappingFilters.addAll(source.getLifecycleMappingFilters());
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
  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType,
      Predicate<LifecycleMappingMetadata> filter) throws DuplicateLifecycleMappingMetadataException {
    if(packagingType == null) {
      return null;
    }

    Stream<LifecycleMappingMetadata> stream = lifecycleMappings.stream()
        .filter(mapping -> packagingType.equals(mapping.getPackagingType()));
    if(filter != null) {
      stream = stream.filter(Predicate.not(filter));
    }
    List<LifecycleMappingMetadata> matching = stream.toList();
    if(matching.isEmpty()) {
      return null;
    }
    if(matching.size() == 1) {
      return matching.get(0);
    }
    throw new DuplicateLifecycleMappingMetadataException(matching.toArray(LifecycleMappingMetadata[]::new));
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

  @Override
  public List<LifecycleMappingFilter> getFilters() {
    return mappingFilters;
  }

}
