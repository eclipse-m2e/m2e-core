/*******************************************************************************
 * Copyright (c) 2010, 2023 Sonatype, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Christoph Läubrich - enhance values and add javadoc
 *******************************************************************************/

package org.eclipse.m2e.core.lifecyclemapping.model;

public enum PluginExecutionAction {

  /**
   * The execution of this plugin is suppressed with no further actions
   */
  ignore,
  /**
   * The plugin is marked as executed
   */
  execute,
  /**
   * The execution of this plugin is handled by an explicit configured configurator.
   */
  configurator,
  /**
   * The execution results always in an error but the plugin is never executed
   */
  error,
  /**
   * The execution results always in a warning but the plugin is never executed
   */
  warn;

  public static final PluginExecutionAction DEFAULT_ACTION = execute;
}
