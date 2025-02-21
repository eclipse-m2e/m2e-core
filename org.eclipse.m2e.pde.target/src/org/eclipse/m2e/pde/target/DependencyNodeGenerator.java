/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.util.Collection;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.pde.target.shared.DependencyDepth;
import org.eclipse.m2e.pde.target.shared.DependencyResult;
import org.eclipse.m2e.pde.target.shared.MavenDependencyCollector;

final class DependencyNodeGenerator {
	private DependencyNodeGenerator() {
	}

	static ICallable<DependencyResult> create(MavenTargetDependency root, Artifact artifact,
			DependencyDepth dependencyDepth, Collection<String> dependencyScopes,
			@SuppressWarnings("deprecation") List<ArtifactRepository> repositories,
			MavenTargetLocation parent) {
		return (context, monitor) -> {
			try {
				MavenDependencyCollector collector = new MavenDependencyCollector(
						MavenPluginActivator.getDefault().getRepositorySystem(), context.getRepositorySession(),
						RepositoryUtils.toRepos(repositories), dependencyDepth, dependencyScopes);
				DependencyResult result = collector.collect(new Dependency(artifact, null));
				DependencyNode node = result.root();
				node.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parent);
				node.setData(MavenTargetLocation.DEPENDENCYNODE_ROOT, root);
				return result;
			} catch (RepositoryException e) {
				throw new CoreException(Status.error("Resolving dependencies failed", e));
			} catch (RuntimeException e) {
				throw new CoreException(Status.error("Internal error", e));
			}
		};
	}

}