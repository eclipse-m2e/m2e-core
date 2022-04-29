/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.profiles.ui.internal;

import org.osgi.framework.BundleContext;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * The activator class controls the plug-in life cycle
 *
 * @since 1.5.0
 */
public class MavenProfilesUIActivator extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.profiles.ui";

  private static IEclipseContext serviceContext;

  /**
   * The constructor
   */
  public MavenProfilesUIActivator() {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    super.start(context);
    serviceContext = EclipseContextFactory.getServiceContext(context);
  }

  /**
   * TODO should be provided by AbstractUIPlugin
   */
  public static IEclipseContext getServiceContext() {
    return serviceContext;
  }
}
