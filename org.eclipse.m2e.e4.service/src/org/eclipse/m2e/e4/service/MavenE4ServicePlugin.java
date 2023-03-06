/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat, Inc.) - auto update project configuration
 *******************************************************************************/

package org.eclipse.m2e.e4.service;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

public class MavenE4ServicePlugin extends Plugin {

	public static final String PLUGIN_ID = "org.eclipse.m2e.e4.service";

	private static MavenE4ServicePlugin instance;

	public MavenE4ServicePlugin() {
		MavenE4ServicePlugin.instance = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Logs given message with given status.
	 *
	 * @param status  status of message
	 * @param message text of message
	 * @see IStatus
	 */
	public void log(int status, String message) {
		this.getLog().log(new Status(status, PLUGIN_ID, message));
	}

	/**
	 * Logs given message with given status and throwable.
	 *
	 * @param status    status of message
	 * @param message   text of message
	 * @param throwable Throwable
	 * @see IStatus
	 */
	public void log(int status, String message, Throwable throwable) {
		this.getLog().log(new Status(status, PLUGIN_ID, message, throwable));
	}

	public static MavenE4ServicePlugin getDefault() {
		return instance;
	}
}
