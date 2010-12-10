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

/**
 * PluginExecutionMetadata
 *
 * @author Administrator
 */
public class PluginExecutionMetadata {
  private PluginExecutionFilter filter;

  private PluginExecutionAction action;

  private String configuratorId;

  public PluginExecutionMetadata(PluginExecutionFilter filter, PluginExecutionAction action) {
    this.filter = filter;
    this.action = action;
  }

  public PluginExecutionMetadata(PluginExecutionFilter filter, String configuratorId) {
    this.filter = filter;
    this.action = PluginExecutionAction.CONFIGURATOR;
    this.configuratorId = configuratorId;
  }

  public PluginExecutionFilter getFilter() {
    return this.filter;
  }

  public PluginExecutionAction getAction() {
    return this.action;
  }

  public String getConfiguratorId() {
    return this.configuratorId;
  }
}
