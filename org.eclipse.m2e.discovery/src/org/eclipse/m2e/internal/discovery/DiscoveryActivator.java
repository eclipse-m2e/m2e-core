/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.internal.discovery;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


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