/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.lemminx;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleResolver {
	protected static final Logger LOG = LoggerFactory.getLogger(BundleResolver.class);

	private BundleResolver() {
		// Utility class, not meant to be instantiated
	}

	public static Bundle getBundle(String bundleID) {
		return Platform.getBundle(bundleID);
	}

	public static File getBundleResource(String bundleID, String resourceName) {
		Bundle bundle = getBundle(bundleID);
		if (bundle == null) {
			LOG.debug("Bundle {} was not found!", bundleID);
			return null;
		}
		try {
			URL resource = bundle.getResource(resourceName);
			if (resource == null) {
				LOG.debug("resource {} was not found in bundle {}!", resourceName, bundle.getSymbolicName());
				return null;
			}
			return new java.io.File(
					FileLocator
					.toFileURL(FileLocator.find(bundle, new Path(resource.getPath())))
					.getPath());
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return null;
		}
	}
}
