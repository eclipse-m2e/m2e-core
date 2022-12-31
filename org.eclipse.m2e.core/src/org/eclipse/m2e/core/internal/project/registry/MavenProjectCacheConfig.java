/********************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * @author christoph
 *
 */
@ObjectClassDefinition(id = "maven.project.cache")
public @interface MavenProjectCacheConfig {

  static final int DEFAULT_CACHE_SIZE = 50;

  @AttributeDefinition(type = AttributeType.INTEGER, description = "%maven.project.cache.size.description", name = "%maven.project.cache.size", defaultValue = DEFAULT_CACHE_SIZE
      + "")
  int cacheSize() default -1;
}
