/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.registry;

import org.apache.maven.model.Plugin;

import org.eclipse.m2e.core.project.IPluginFacade;


/**
 * Default {@link IPluginFacade} implementation simply wrapping a Maven-core {@link Plugin}.
 */
public class PluginFacade implements IPluginFacade {

  private final Plugin plugin;

  public PluginFacade(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getGroupId() {
    return plugin.getGroupId();
  }

  @Override
  public String getArtifactId() {
    return plugin.getArtifactId();
  }

  @Override
  public String getVersion() {
    return plugin.getVersion();
  }

  @Override
  public String getKey() {
    return plugin.getKey();
  }

  @Override
  public String toString() {
    return getKey();
  }

}
