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
import java.util.Collections;
import java.util.HashMap;
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
import org.osgi.framework.Bundle;

public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {

	private static IMavenConfigurationChangeListener mavenConfigurationlistener;

	public MavenRuntimeClasspathProvider() {
		if (mavenConfigurationlistener == null) {
			final IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
			final LanguageServerDefinition lemminxDefinition = LanguageServersRegistry.getInstance().getDefinition("org.eclipse.wildwebdeveloper.xml");
			if (mavenConfiguration != null && lemminxDefinition != null) {
				mavenConfigurationlistener = event -> {
					Map<String, String> prefs = new HashMap<>(2);
					prefs.put("maven.globalSettings", mavenConfiguration.getGlobalSettingsFile());
					prefs.put("maven.userSettings", mavenConfiguration.getUserSettingsFile());
					DidChangeConfigurationParams params = new DidChangeConfigurationParams(Collections.singletonMap("xml", prefs));
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
		File jarsDir = BundleResolver.getBundleResource("org.eclipse.m2e.maven.runtime", "/jars/");
		if (jarsDir != null) {
			for (File jar : jarsDir.listFiles()) {
				if (!jar.isDirectory()) {
					mavenRuntimeJars.add(jar);
				}
			}
		}
		// Indexer jars
		File indexerJarsDir = BundleResolver.getBundleResource("org.eclipse.m2e.editor.lemminx", "/indexer-jars/");
		if (indexerJarsDir != null) {
			for (File jar : indexerJarsDir.listFiles()) {
				if (!jar.isDirectory()) {
					mavenRuntimeJars.add(jar);
				}
			}
		}
		// Libraries that are also required and not included in org.eclipse.m2e.maven.runtime
		Bundle injectBundle = Platform.getBundle("javax.inject");
		if (injectBundle != null) {
			try {
				mavenRuntimeJars.add(FileLocator.getBundleFile(injectBundle));
			} catch (IOException e) {
				// TODO
			}
		}
		Bundle slf4jApiBundle = Platform.getBundle("org.slf4j.api");
		if (slf4jApiBundle != null) {
			try {
				mavenRuntimeJars.add(FileLocator.getBundleFile(slf4jApiBundle));
			} catch (IOException e) {
				// TODO
			}
		}
		return mavenRuntimeJars;
	}
}
