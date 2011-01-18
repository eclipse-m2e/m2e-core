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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;


/**
 * @author igor
 */
public class CustomLifecycleMapping extends AbstractCustomizableLifecycleMapping {

  protected Map<MojoExecutionKey, List<PluginExecutionMetadata>> getMapping(
      List<PluginExecutionMetadata> configuration, Map<MojoExecutionKey, List<PluginExecutionMetadata>> mapping2,
      IProgressMonitor monitor) throws CoreException {

    Map<MojoExecutionKey, List<PluginExecutionMetadata>> result = new LinkedHashMap<MojoExecutionKey, List<PluginExecutionMetadata>>();

    MavenExecutionPlan executionPlan = getMavenProjectFacade().getExecutionPlan(monitor);
    for(MojoExecution mojoExecution : executionPlan.getMojoExecutions()) {
      MojoExecutionKey executionKey = new MojoExecutionKey(mojoExecution);
      List<PluginExecutionMetadata> executionMappings = new ArrayList<PluginExecutionMetadata>();
      for(PluginExecutionMetadata executionMetadata : configuration) {
        if(executionMetadata.getFilter().match(mojoExecution)) {
          executionMappings.add(executionMetadata);
        }
      }
      result.put(executionKey, executionMappings);
    }

    return result;
  }
}
