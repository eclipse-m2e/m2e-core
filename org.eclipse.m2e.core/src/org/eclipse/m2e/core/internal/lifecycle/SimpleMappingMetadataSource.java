/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;


/**
 * SimpleMappingMetadataSource
 * 
 * @author igor
 */
class SimpleMappingMetadataSource implements MappingMetadataSource {

  private final List<LifecycleMappingMetadataSource> sources = new ArrayList<LifecycleMappingMetadataSource>();

  public SimpleMappingMetadataSource(LifecycleMappingMetadataSource source) {
    this.sources.add(source);
  }

  public SimpleMappingMetadataSource(List<LifecycleMappingMetadataSource> sources) {
    this.sources.addAll(sources);
  }

  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType) throws DuplicateMappingException {
    if(packagingType == null) {
      return null;
    }
    LifecycleMappingMetadata mapping = null;
    for(LifecycleMappingMetadataSource source : sources) {
      for(LifecycleMappingMetadata _mapping : source.getLifecycleMappings()) {
        if(packagingType.equals(_mapping.getPackagingType())) {
          if(mapping != null) {
            throw new DuplicateMappingException();
          }
          mapping = _mapping;
        }
      }
    }
    return mapping;
  }

  public PluginExecutionMetadata getPluginExecutionMetadata(MojoExecution execution) throws DuplicateMappingException {
    List<PluginExecutionMetadata> pluginExecutions = new ArrayList<PluginExecutionMetadata>();
    for(LifecycleMappingMetadataSource source : sources) {
      pluginExecutions.addAll(source.getPluginExecutions());
    }
    return getPluginExecution(execution, pluginExecutions);
  }

  public static PluginExecutionMetadata getPluginExecution(MojoExecution execution,
      List<PluginExecutionMetadata> pluginExecutions) throws DuplicateMappingException {
    if(execution == null) {
      return null;
    }
    PluginExecutionMetadata mapping = null;
    for(PluginExecutionMetadata _mapping : pluginExecutions) {
      if(_mapping.getFilter().match(execution)) {
        if(mapping != null) {
          throw new DuplicateMappingException();
        }
        mapping = _mapping;
      }
    }
    return mapping;
  }

}
