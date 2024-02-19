/*******************************************************************************
 * Copyright (c) 2019-2023 Red Hat Inc. and others.
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {
	private static final ILog LOG = Platform.getLog(MavenRuntimeClasspathProvider.class);

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

					LanguageServers.forProject(null).withPreferredServer(definition).excludeInactive()
							.collectAll((w, ls) -> CompletableFuture.completedFuture(ls)).thenAccept(lss -> lss.stream()
									.forEach(ls -> ls.getWorkspaceService().didChangeConfiguration(params)));

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
		// Libraries that are also required and not included in
		// org.eclipse.m2e.maven.runtime
		Stream.of(javax.inject.Inject.class, org.slf4j.Logger.class, CommandLine.class)//
				.map(FrameworkUtil::getBundle).map(FileLocator::getBundleFileLocation)//
				.flatMap(Optional::stream).forEach(mavenRuntimeJars::add);
		return mavenRuntimeJars;
	}

	private static void addJarsFromBundle(Bundle bundle, String folder, List<File> jarFiles) {
		try {
			URL fileURL = FileLocator.toFileURL(bundle.getResource(folder));
			Path jarDir = Path.of(URIUtil.toURI(fileURL));
			try (Stream<Path> paths = Files.walk(jarDir, 1)) {
				paths.filter(Files::isRegularFile).map(Path::toFile).forEach(jarFiles::add);
			}
		} catch (IOException | URISyntaxException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
