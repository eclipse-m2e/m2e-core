/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.apt.ui;

import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.m2e.apt.MavenJdtAptPlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class MavenJdtAptUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.m2e.apt.ui"; //$NON-NLS-1$

	// The shared instance
	private static MavenJdtAptUIPlugin plugin;

  private IPreferenceStore preferenceStore;
	
	/**
	 * The constructor
	 */
	public MavenJdtAptUIPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		preferenceStore = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MavenJdtAptUIPlugin getDefault() {
		return plugin;
	}

  @Override
  public IPreferenceStore getPreferenceStore() {
    // Create the preference store lazily.
    if(preferenceStore == null) {
      preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, MavenJdtAptPlugin.PLUGIN_ID);
    }
    return preferenceStore;
  }
}
