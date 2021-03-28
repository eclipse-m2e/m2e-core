/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui;

import org.eclipse.m2e.pde.ui.adapter.DependencyNodeAdapterFactory;
import org.eclipse.m2e.pde.ui.adapter.MavenTargetAdapterFactory;
import org.eclipse.m2e.pde.ui.adapter.MavenTargetBundleAdapterFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.m2e.pde.ui";

	@Override
	public void start(BundleContext context) throws Exception {

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		DependencyNodeAdapterFactory.LABEL_PROVIDER.dispose();
		DependencyNodeAdapterFactory.TREE_CONTENT_PROVIDER.dispose();
		MavenTargetAdapterFactory.LABEL_PROVIDER.dispose();
		MavenTargetAdapterFactory.TREE_CONTENT_PROVIDER.dispose();
		MavenTargetBundleAdapterFactory.LABEL_PROVIDER.dispose();
	}

}
