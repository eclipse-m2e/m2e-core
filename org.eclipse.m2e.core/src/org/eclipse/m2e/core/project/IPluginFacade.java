/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import org.apache.maven.model.Plugin;

import org.eclipse.m2e.core.internal.project.registry.PluginFacade;


/**
 * Facade for the (build) plugin backing a {@link IMojoExecutionFacade}. This decouples consumers that just need the
 * plugin's coordinates from depending directly on Maven-core API such as {@link Plugin}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.9
 */
public interface IPluginFacade {

  /**
   * @return the group id of the plugin
   */
  String getGroupId();

  /**
   * @return the artifact id of the plugin
   */
  String getArtifactId();

  /**
   * @return the version of the plugin
   */
  String getVersion();

  /**
   * @return the {@code groupId:artifactId} key uniquely identifying the plugin (independent of its version)
   */
  String getKey();

  /**
   * Wraps a raw {@link Plugin} into an {@link IPluginFacade}.
   * <p>
   * This is intended as a transitional helper for client code that still acquires or receives (e.g. through a
   * framework callback) a raw {@link Plugin} instance, until such code can be migrated to acquire an
   * {@link IPluginFacade} directly from {@link IMojoExecutionFacade#getPlugin()}.
   * </p>
   *
   * @param plugin the raw plugin to wrap
   * @return a facade wrapping the given plugin, or {@code null} if {@code plugin} is {@code null}
   */
  static IPluginFacade wrap(Plugin plugin) {
    if(plugin == null) {
      return null;
    }
    return new PluginFacade(plugin);
  }

}
