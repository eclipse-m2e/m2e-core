/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.MappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;


/**
 * @author igor
 */
public class CustomLifecycleMapping extends AbstractCustomizableLifecycleMapping {

  @Override
  protected Map<MojoExecutionKey, List<PluginExecutionMetadata>> getEffectiveMapping(
      List<MojoExecution> mojoExecutions, MappingMetadataSource originalMapping,
      List<MappingMetadataSource> inheritedMapping) {

    return super.getEffectiveMapping(mojoExecutions, originalMapping, Arrays.asList(originalMapping));
  }

}
