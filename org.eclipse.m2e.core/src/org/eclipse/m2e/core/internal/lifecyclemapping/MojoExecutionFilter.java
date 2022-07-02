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
import java.util.function.Predicate;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * A {@link Predicate} that returns true if the given {@link PluginExecutionMetadata} is filtered for the goal by any of
 * the filters.
 */
public class MojoExecutionFilter implements Predicate<PluginExecutionMetadata> {
  private final List<LifecycleMappingFilter> filters;

  public MojoExecutionFilter(List<MappingMetadataSource> metadataSources, MojoExecutionKey executionKey) {
    filters = metadataSources.stream().flatMap(s -> s.getFilters().stream())
        .filter(filter -> filter.getPluginExecutions().stream().anyMatch(f -> f.match(executionKey))).toList();
  }

  @Override
  public boolean test(PluginExecutionMetadata metadata) {
    return anyMatch(metadata.getSource(), filters);
  }

  static boolean anyMatch(LifecycleMappingMetadataSource source, List<LifecycleMappingFilter> filters) {
    return source != null && source.getSource() instanceof Bundle bundle && anyFilterMatches(bundle, filters);
  }

  private static boolean anyFilterMatches(Bundle bundle, List<LifecycleMappingFilter> filters) {
    Version version = bundle.getVersion();
    return filters.stream() //
        .filter(f -> f.getSymbolicName().equals(bundle.getSymbolicName()))
        .anyMatch(filter -> filter.matches(version.getMajor() + "." + version.getMinor() + "." + version.getMicro()));
  }
}
