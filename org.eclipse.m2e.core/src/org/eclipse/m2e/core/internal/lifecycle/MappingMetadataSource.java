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

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.lifecycle.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionMetadata;


/**
 * MappingMetadataSource
 * 
 * @author igor
 */
public interface MappingMetadataSource {
  public LifecycleMappingMetadata getLifecycleMappingMetadata(String packagingType) throws DuplicateMappingException;

  public PluginExecutionMetadata getPluginExecutionMetadata(MojoExecution execution) throws DuplicateMappingException;

}
