/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import org.osgi.framework.BundleContext;

import org.eclipse.ui.plugin.AbstractUIPlugin;


public class DiscoveryActivator extends AbstractUIPlugin {

  // Should match bundle name
  public static final String PLUGIN_ID = "org.eclipse.m2e.discovery"; //$NON-NLS-1$

  private static DiscoveryActivator plugin;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    DiscoveryActivator.plugin = this;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    DiscoveryActivator.plugin = null;
  }

  public static DiscoveryActivator getDefault() {
    return plugin;
  }
}
