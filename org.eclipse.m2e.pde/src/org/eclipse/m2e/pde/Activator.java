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
package org.eclipse.m2e.pde;

import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		CacheManager.setBasedir(context.getBundle().getDataFile(""));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// clear all locations older than 14 days, this can be improved by
		// 1) watch for changes in the workspace -> if target is deleted/removed from
		// workspace we can clear the cache
		// 2) we can add a preference page where the user can force clearing the cache
		// or set the cache days
		CacheManager.clearFilesOlderThan(14, TimeUnit.DAYS);
		CacheManager.setBasedir(null);
	}

}
