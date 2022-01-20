/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
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
package org.eclipse.m2e.pde.connector;

import java.util.Optional;

import org.eclipse.pde.core.project.IBundleProjectService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static volatile ServiceTracker<IBundleProjectService, IBundleProjectService> bundleProjectServiceTracker;

	@Override
	public void start(BundleContext context) throws Exception {
		bundleProjectServiceTracker = new ServiceTracker<>(context, IBundleProjectService.class, null);
		bundleProjectServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleProjectServiceTracker.close();
	}

	public static Optional<IBundleProjectService> getBundleProjectService() {
		if (bundleProjectServiceTracker == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(bundleProjectServiceTracker.getService());
	}

}
