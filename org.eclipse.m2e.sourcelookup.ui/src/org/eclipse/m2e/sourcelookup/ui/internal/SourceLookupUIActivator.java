/*******************************************************************************
 * Copyright (c) 2011-2023 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.ui.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SourceLookupUIActivator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.m2e.sourcelookup.ui"; //$NON-NLS-1$

	private static SourceLookupUIActivator plugin;

	public SourceLookupUIActivator() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static SourceLookupUIActivator getDefault() {
		return plugin;
	}

}
