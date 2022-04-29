/*************************************************************************************
 * Copyright (c) 2008-2011 Red Hat, Inc. and others.
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

package org.eclipse.m2e.profiles.core.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The activator class controls the plug-in life cycle
 *
 * @author Fred Bricon
 * @since 1.5.0
 */
public class MavenProfilesCoreActivator implements BundleActivator {

  private static MavenProfilesCoreActivator instance;

  private ServiceTracker<IProfileManager, IProfileManager> profileManagerTracker;

  private BundleContext context;

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    this.context = context;
    instance = this;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    instance = null;
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static MavenProfilesCoreActivator getDefault() {
    return instance;
  }

  @Deprecated(forRemoval = true) //only used for test code at the moment...
  public synchronized IProfileManager getProfileManager() {
    if(profileManagerTracker == null) {
      profileManagerTracker = new ServiceTracker<>(context, IProfileManager.class, null);
    }
    return profileManagerTracker.getService();
  }
}
