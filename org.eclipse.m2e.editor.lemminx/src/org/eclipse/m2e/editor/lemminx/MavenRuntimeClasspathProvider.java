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
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider;

public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {

	private static IMavenConfigurationChangeListener mavenConfigurationlistener;

	public MavenRuntimeClasspathProvider() {
		if (mavenConfigurationlistener == null) {
			final IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
			final LanguageServerDefinition lemminxDefinition = LanguageServersRegistry.getInstance().getDefinition("org.eclipse.wildwebdeveloper.xml");
			if (mavenConfiguration != null && lemminxDefinition != null) {
				mavenConfigurationlistener = event -> {
					DidChangeConfigurationParams params = new DidChangeConfigurationParams(Map.of("xml", InitializationOptionsProvider.toLemMinXOptions(mavenConfiguration)));
					LanguageServiceAccessor.getActiveLanguageServers(null).stream().filter(server -> lemminxDefinition.equals(LanguageServiceAccessor.resolveServerDefinition(server).get()))
						.forEach(ls -> ls.getWorkspaceService().didChangeConfiguration(params));
				};
				MavenPlugin.getMavenConfiguration().addConfigurationChangeListener(mavenConfigurationlistener);
			}
		}
	}

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
