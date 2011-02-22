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
package org.eclipse.m2e.internal.udc;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/*
 * Listen for the maven core bundle starting to avoid early activation by the usage data collector
 */
public class Activator implements BundleActivator, BundleListener {

	private static final String ORG_ECLIPSE_M2E_CORE = "org.eclipse.m2e.core"; //$NON-NLS-1$

	private static Activator instance;

	private volatile MavenListener listener;

	private PomImportMonitor monitor;

	private BundleContext context;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		instance = this;
	}

	public void stop(BundleContext context) throws Exception {
		context.removeBundleListener(this);
		if (listener != null) {
			listener.stopListener();
			listener = null;
		}
	}

	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STARTED && ORG_ECLIPSE_M2E_CORE.equals(event.getBundle().getSymbolicName())) {
			createListener();
			// Don't care about future events so the listener can be removed
			context.removeBundleListener(this);
		}
	}

	public static Activator getDefault() {
		return instance;
	}

	void setMonitor(PomImportMonitor monitor) {
		this.monitor = monitor;

		// If the m2e core bundle is already active create listener for project
		// changes, otherwise listen for bundle started events
		Bundle b = Platform.getBundle(ORG_ECLIPSE_M2E_CORE);
		if (b.getState() == Bundle.ACTIVE) {
			createListener();
		} else {
			context.addBundleListener(this);
		}
	}

	private synchronized void createListener() {
		if (listener == null) {
			listener = new MavenListener(monitor);
		}
	}
}
