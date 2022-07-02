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

import java.util.List;
import java.util.function.Predicate;

import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * MappingMetadataSource
 *
 * @author igor
 */
public interface MappingMetadataSource {

  LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType, Predicate<LifecycleMappingMetadata> filter)
      throws DuplicateMappingException;

  List<PluginExecutionMetadata> getPluginExecutionMetadata(MojoExecutionKey execution);

  List<LifecycleMappingFilter> getFilters();

}
