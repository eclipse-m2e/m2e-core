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

package org.eclipse.m2e.core.internal.project;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * @author igor
 */
public class IgnoreMojoProjectConfigurator extends AbstractProjectConfigurator {

  public IgnoreMojoProjectConfigurator(PluginExecutionFilter pluginExecutionFilter) {
    addPluginExecutionFilter(pluginExecutionFilter);
  }

  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
  }
}
