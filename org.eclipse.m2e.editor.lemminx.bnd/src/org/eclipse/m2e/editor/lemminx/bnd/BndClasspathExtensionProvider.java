/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.lemminx.bnd;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import aQute.bnd.help.Syntax;

/**
 * register additional jars and the extension bundle
 */
@SuppressWarnings("restriction")
public class BndClasspathExtensionProvider
		implements org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider {

	@Override
	public List<File> get() {
		List<File> list = new ArrayList<>();
		Set<Bundle> bundleRequirements = new LinkedHashSet<>();
		bundleRequirements.add((FrameworkUtil.getBundle(getClass())));
		collectBundles(FrameworkUtil.getBundle(Syntax.class), bundleRequirements);
		for (Bundle bundle : bundleRequirements) {
			FileLocator.getBundleFileLocation(bundle).ifPresent(file -> {
				if (file.isDirectory()) {
					// For bundles from the workspace launch include the bin folder for classes
					File outputFolder = new File(file, "bin");
					if (outputFolder.exists()) {
						list.add(outputFolder);
					}
				}
				list.add(file);
			});
		}
		return list;
	}

	private void collectBundles(Bundle bundle, Set<Bundle> bundleRequirements) {
		if (isValid(bundle) && bundleRequirements.add(bundle)) {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			List<BundleWire> wires = wiring.getRequiredWires("osgi.wiring.package");
			for (BundleWire bundleWire : wires) {
				collectBundles(bundleWire.getProvider().getBundle(), bundleRequirements);
			}
		}

	}

	private boolean isValid(Bundle bundle) {
		if (bundle == null) {
			return false;
		}
		String bsn = bundle.getSymbolicName();
		if ("slf4j.api".equals(bsn)) {
			// slf4j is already provided
			return false;
		}
		return true;
	}

}
