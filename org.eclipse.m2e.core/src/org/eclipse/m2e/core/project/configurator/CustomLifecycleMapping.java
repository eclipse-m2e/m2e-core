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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.util.NLS;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.lifecycle.DuplicateMappingException;
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
    Map<MojoExecutionKey, List<PluginExecutionMetadata>> result = new LinkedHashMap<MojoExecutionKey, List<PluginExecutionMetadata>>();

    for(MojoExecution mojoExecution : mojoExecutions) {
      try {
        PluginExecutionMetadata executionMetadata = originalMapping.getPluginExecutionMetadata(mojoExecution);
        if (executionMetadata != null) {
          List<PluginExecutionMetadata> executionMappings = new ArrayList<PluginExecutionMetadata>();
          executionMappings.add(executionMetadata);
          result.put(new MojoExecutionKey(mojoExecution), executionMappings);
        }
      } catch (DuplicateMappingException e) {
        addProblem(1, NLS.bind(Messages.PluginExecutionMappingDuplicate, mojoExecution.toString()));
      }
    }

    return result;
  }

}
