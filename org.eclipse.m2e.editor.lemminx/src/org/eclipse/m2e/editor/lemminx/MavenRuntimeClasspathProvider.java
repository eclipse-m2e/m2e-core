/*******************************************************************************
 * Copyright (c) 2019-2021 Red Hat Inc. and others.
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {
	private static final Logger LOG = LoggerFactory.getLogger(MavenRuntimeClasspathProvider.class);

	private static IMavenConfigurationChangeListener mavenConfigurationlistener;
	private static final String LANGUAGE_SERVER = "org.eclipse.wildwebdeveloper.xml";

	public MavenRuntimeClasspathProvider() {
		if (mavenConfigurationlistener == null) {
			IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
			LanguageServerDefinition definition = LanguageServersRegistry.getInstance().getDefinition(LANGUAGE_SERVER);
			if (mavenConfiguration != null && definition != null) {
				mavenConfigurationlistener = event -> {
					Map<String, Object> options = InitializationOptionsProvider.toLemMinXOptions(mavenConfiguration);
					DidChangeConfigurationParams params = new DidChangeConfigurationParams(Map.of("xml", options));
					LanguageServiceAccessor.getActiveLanguageServers(null).stream()
							.filter(s -> definition.equals(LanguageServiceAccessor.resolveServerDefinition(s).get()))
							.forEach(ls -> ls.getWorkspaceService().didChangeConfiguration(params));
				};
				MavenPlugin.getMavenConfiguration().addConfigurationChangeListener(mavenConfigurationlistener);
			}
		}
	}

	@Override
	public List<File> get() {
		List<File> mavenRuntimeJars = new ArrayList<>();
		// Add all jars from org.eclipse.m2e.maven.runtime
		addJarsFromBundle(FrameworkUtil.getBundle(org.apache.maven.Maven.class), "/jars/", mavenRuntimeJars);
		// Add all jars from org.eclipse.m2e.maven.indexer
		addJarsFromBundle(FrameworkUtil.getBundle(org.apache.maven.index.Indexer.class), "/jars/", mavenRuntimeJars);
		// Libraries that are also required and not included in
		// org.eclipse.m2e.maven.runtime
		try {
			mavenRuntimeJars.add(FileLocator.getBundleFile(FrameworkUtil.getBundle(javax.inject.Inject.class)));
			mavenRuntimeJars.add(FileLocator.getBundleFile(FrameworkUtil.getBundle(org.slf4j.Logger.class)));
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		return mavenRuntimeJars;
	}

	private static void addJarsFromBundle(Bundle bundle, String resource, List<File> jarFiles) {
		try {
			URL fileURL = FileLocator.toFileURL(bundle.getResource(resource));
			Path jarDir = Path.of(fileURL.toURI());
			try (Stream<Path> paths = Files.walk(jarDir, 1)) {
				paths.filter(Files::isRegularFile).map(Path::toFile).forEach(jarFiles::add);
			}
		} catch (IOException | URISyntaxException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
