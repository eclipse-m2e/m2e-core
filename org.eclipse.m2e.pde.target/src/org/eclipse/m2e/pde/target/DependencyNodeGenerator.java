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
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;

final class DependencyNodeGenerator {
	private DependencyNodeGenerator() {
	}

	static ICallable<PreorderNodeListGenerator> create(MavenTargetDependency root, Artifact artifact,
			DependencyDepth dependencyDepth, Collection<String> dependencyScopes, List<ArtifactRepository> repositories,
			MavenTargetLocation parent) {

		return (IMavenExecutionContext context, IProgressMonitor monitor) -> {
			try {
				CollectRequest collectRequest = new CollectRequest();
				collectRequest.setRoot(new Dependency(artifact, null));
				collectRequest.setRepositories(RepositoryUtils.toRepos(repositories));

				RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
				DependencyNode node = repoSystem.collectDependencies(context.getRepositorySession(), collectRequest)
						.getRoot();
				node.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parent);
				node.setData(MavenTargetLocation.DEPENDENCYNODE_ROOT, root);
				DependencyRequest dependencyRequest = new DependencyRequest();
				dependencyRequest.setRoot(node);
				dependencyRequest.setFilter(new MavenTargetDependencyFilter(dependencyDepth, dependencyScopes));
				repoSystem.resolveDependencies(context.getRepositorySession(), dependencyRequest);
				PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
				node.accept(nlg);
				return nlg;
			} catch (RepositoryException e) {
				throw new CoreException(Status.error("Resolving dependencies failed", e));
			} catch (RuntimeException e) {
				throw new CoreException(Status.error("Internal error", e));
			}
		};
	}
}