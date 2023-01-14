/*******************************************************************************
 * Copyright (c) 2011-2023 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.internal.launch;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.advanced.ISourceContainerResolver;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenArtifactIdentifier;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;

public class MavenSourceContainerResolver implements ISourceContainerResolver {

	@Override
	public Collection<ISourceContainer> resolveSourceContainers(File classesLocation, IProgressMonitor monitor) {
		Collection<ArtifactKey> classesArtifacts = MavenArtifactIdentifier.identify(classesLocation);

		if (classesArtifacts.isEmpty()) {
			return List.of();
		}
		return classesArtifacts.stream()//
				.map(classesArtifact -> resolveSourceContainer(classesArtifact, monitor))//
				.filter(Objects::nonNull).toList();
	}

	protected ISourceContainer resolveSourceContainer(ArtifactKey artifact, IProgressMonitor monitor) {
		String groupId = artifact.groupId();
		String artifactId = artifact.artifactId();
		String version = artifact.version();

		IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();

		IMavenProjectFacade mavenProject = projectRegistry.getMavenProject(groupId, artifactId, version);
		if (mavenProject != null) {
			return new JavaProjectSourceContainer(JavaCore.create(mavenProject.getProject()));
		}
		Path sourceLocation = MavenArtifactIdentifier.resolveSourceLocation(artifact, monitor);
		if (sourceLocation != null) {
			return new ExternalArchiveSourceContainer(sourceLocation.toString(), true);
		}
		return null;
	}
}
