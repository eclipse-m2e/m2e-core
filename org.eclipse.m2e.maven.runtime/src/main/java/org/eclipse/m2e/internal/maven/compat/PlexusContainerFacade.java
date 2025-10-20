/********************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.internal.maven.compat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.maven.cli.internal.BootstrapCoreExtensionManager;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.apache.maven.cli.internal.extension.model.io.xpp3.CoreExtensionsXpp3Reader;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.scope.internal.MojoExecutionScopeModule;
import org.apache.maven.extension.internal.CoreExtensionEntry;
import org.apache.maven.session.scope.internal.SessionScopeModule;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

public class PlexusContainerFacade {

	public static final String CONTAINER_CONFIGURATION_NAME = "maven";

	public static final String MVN_FOLDER = ".mvn";

	public static final String EXTENSIONS_FILENAME = MVN_FOLDER + "/extensions.xml";

	private PlexusContainer container;

	public PlexusContainerFacade(PlexusContainer container) {
		this.container = container;
	}

	public static List<CoreExtensionEntry> loadCoreExtensions(ClassRealm coreRealm, CoreExtensionEntry coreEntry,
			File multiModuleProjectDirectory, LoggerManager loggerManager, MavenExecutionRequesFactory requestFactory)
			throws Exception {
		if (multiModuleProjectDirectory == null) {
			return Collections.emptyList();
		}
		File extensionsXml = new File(multiModuleProjectDirectory, EXTENSIONS_FILENAME);
		if (!extensionsXml.isFile()) {
			return Collections.emptyList();
		}
		List<CoreExtension> extensions;
		try (InputStream is = new FileInputStream(extensionsXml)) {
			extensions = new CoreExtensionsXpp3Reader().read(is).getExtensions();
		}
		if (extensions.isEmpty()) {
			return Collections.emptyList();
		}

		ContainerConfiguration cc = new DefaultContainerConfiguration().setClassWorld(coreRealm.getWorld())
				.setRealm(coreRealm).setClassPathScanning(PlexusConstants.SCANNING_INDEX).setAutoWiring(true)
				.setJSR250Lifecycle(true).setName(CONTAINER_CONFIGURATION_NAME);

		DefaultPlexusContainer container = new DefaultPlexusContainer(cc, new AbstractModule() {
			@Override
			protected void configure() {
				bind(ILoggerFactory.class).toInstance(LoggerFactory.getILoggerFactory());
			}
		});

		Thread thread = Thread.currentThread();
		ClassLoader ccl = thread.getContextClassLoader();
		try {
			container.setLookupRealm(null);
			container.setLoggerManager(loggerManager);
			thread.setContextClassLoader(container.getContainerRealm());

			BootstrapCoreExtensionManager resolver = container.lookup(BootstrapCoreExtensionManager.class);
			return resolver.loadCoreExtensions(requestFactory.createFor(container), coreEntry.getExportedArtifacts(),
					extensions);
		} finally {
			thread.setContextClassLoader(ccl);
			container.dispose();
		}
	}

	public static interface MavenExecutionRequesFactory {
		public MavenExecutionRequest createFor(PlexusContainer container) throws Exception;
	}

	public void loadExtension(CoreExtensionEntry extension) throws ComponentLookupException {
		if (container instanceof DefaultPlexusContainer) {
			((DefaultPlexusContainer) container).discoverComponents(extension.getClassRealm(),
					new SessionScopeModule(container),
				new MojoExecutionScopeModule(container));
		}
	}

}
