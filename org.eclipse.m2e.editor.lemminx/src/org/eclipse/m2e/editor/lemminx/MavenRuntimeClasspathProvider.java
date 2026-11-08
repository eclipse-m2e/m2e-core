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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServersRegistry.LanguageServerDefinition;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenConfigurationChangeListener;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;
import org.eclipse.wildwebdeveloper.xml.LemminxClasspathExtensionProvider;

@SuppressWarnings("restriction")
public class MavenRuntimeClasspathProvider implements LemminxClasspathExtensionProvider {

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
		MavenRuntimeManagerImpl mavenRuntimeManager = MavenPluginActivator.getDefault().getMavenRuntimeManager();
		MavenEmbeddedRuntime runtime = (MavenEmbeddedRuntime) mavenRuntimeManager
				.getRuntime(MavenRuntimeManagerImpl.EMBEDDED);
		Set<Path> collectMavenRuntimeClasspath = runtime.collectMavenRuntimeClasspath(false);
		return collectMavenRuntimeClasspath.stream().map(Path::toFile).sorted().toList();
	}

}
