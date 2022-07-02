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

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;


/**
 * A {@link Predicate} that returns true if the given {@link LifecycleMappingMetadata} is filtered for the packaging
 * type by any of the filters.
 */
public class PackagingTypeFilter implements Predicate<LifecycleMappingMetadata> {
  private final List<LifecycleMappingFilter> filters;

  public PackagingTypeFilter(List<MappingMetadataSource> metadataSources, String packagingType) {
    filters = metadataSources.stream().flatMap(s -> s.getFilters().stream())
        .filter(filter -> filter.getPackagingTypes().contains(packagingType)).toList();
  }

  @Override
  public boolean test(LifecycleMappingMetadata metadata) {
    return MojoExecutionFilter.anyMatch(metadata.getSource(), filters);
  }

}
