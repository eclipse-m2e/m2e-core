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

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.eclipse.m2e.core.internal.lifecycle.model.PluginExecutionFilter;

/**
 * PluginExecutionMetadata
 *
 * @author Administrator
 */
public class PluginExecutionMetadata {
  private PluginExecutionFilter filter;

  private PluginExecutionAction action;

  private Xpp3Dom configuration;

  public PluginExecutionMetadata(PluginExecutionFilter filter, PluginExecutionAction action, Xpp3Dom configuration) {
    this.filter = filter;
    this.action = action;
    this.configuration = configuration;
  }

  public PluginExecutionFilter getFilter() {
    return filter;
  }

  public PluginExecutionAction getAction() {
    return action;
  }

  public Xpp3Dom getConfiguration() {
    return configuration;
  }
}
