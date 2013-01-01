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

package org.eclipse.m2e.core.project.configurator;

import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;


/**
 * ILifecycleMappingConfiguration
 * 
 * @author igor
 * @noextend
 * @noimplement
 */
public interface ILifecycleMappingConfiguration {

  /**
   * @return
   */
  String getLifecycleMappingId();

  /**
   * @return
   */
  Map<MojoExecutionKey, List<IPluginExecutionMetadata>> getMojoExecutionMapping();

  /**
   * @param key
   * @return
   */
  Xpp3Dom getMojoExecutionConfiguration(MojoExecutionKey key);

}
