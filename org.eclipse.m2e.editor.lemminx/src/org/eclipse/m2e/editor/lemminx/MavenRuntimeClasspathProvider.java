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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider;

public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {

	@Override
	public List<File> get() {
		List<File> mavenRuntimeJars = new ArrayList<>();
		File jarDir = BundleResolver.getBundleResource("org.eclipse.m2e.maven.runtime", "/jars/");
		for (File jar : jarDir.listFiles()) {
			if (!jar.isDirectory()) {
				mavenRuntimeJars.add(jar);
			}
		}
		// Indexer jars
		jarDir = BundleResolver.getBundleResource("org.eclipse.m2e.editor.lemminx", "/indexer-jars/");
		for (File jar : jarDir.listFiles()) {
			if (!jar.isDirectory()) {
				mavenRuntimeJars.add(jar);
			}
		}
		// Libraries that are also required and not included in org.eclipse.m2e.maven.runtime
		try {
			mavenRuntimeJars.add(FileLocator.getBundleFile(Platform.getBundle("javax.inject")));
			mavenRuntimeJars.add(FileLocator.getBundleFile(Platform.getBundle("org.slf4j.api")));
		} catch (IOException e) {
			// TODO
		}
		return mavenRuntimeJars;
	}
}
