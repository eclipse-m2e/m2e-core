/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others
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

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.util.Collection;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.m2e.pde.target.shared.DependencyDepth;

public class MavenTargetDependencyFilter implements DependencyFilter {

	private static final String[] VALID_EXTENSIONS = { "jar", "pom" };
	private Collection<String> locationScopes;
	private DependencyDepth dependencyDepth;

	public MavenTargetDependencyFilter(DependencyDepth dependencyDepth, Collection<String> dependencyScopes) {
		this.dependencyDepth = dependencyDepth;
		this.locationScopes = dependencyScopes;
	}

	@Override
	public boolean accept(DependencyNode node, List<DependencyNode> parents) {
		String extension = node.getArtifact().getExtension();
		for (String valid : VALID_EXTENSIONS) {
			// only for a valid extension...
			if (valid.equalsIgnoreCase(extension) && (dependencyDepth == DependencyDepth.INFINITE
					|| (dependencyDepth == DependencyDepth.DIRECT && parents.size() <= 1))) {
				Dependency dependency = node.getDependency();
				if (dependency == null) {
					Artifact artifact = node.getArtifact();
					if (artifact == null) {
						return false;
					}
					dependency = new Dependency(artifact, null);
				}
				return isValidScope(dependency);
			}
		}
		return false;
	}

	private boolean isValidScope(Dependency dependency) {
		String dependecyScope = dependency.getScope();
		if (dependecyScope == null || dependecyScope.isBlank()) {
			return true;
		}
		if (locationScopes.isEmpty()) {
			return SCOPE_COMPILE.equalsIgnoreCase(dependecyScope);
		}
		return locationScopes.stream().anyMatch(dependecyScope::equalsIgnoreCase);
	}

	static final Collection<String> expandScope(String scope) {
		if (scope == null || scope.isBlank() || SCOPE_COMPILE.equalsIgnoreCase(scope)) {
			return List.of(SCOPE_COMPILE);
		}
		if (SCOPE_PROVIDED.equalsIgnoreCase(scope)) {
			return List.of(SCOPE_PROVIDED, SCOPE_COMPILE, SCOPE_SYSTEM, SCOPE_RUNTIME);
		}
		if (SCOPE_TEST.equalsIgnoreCase(scope)) {
			return List.of(SCOPE_TEST, SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_SYSTEM, SCOPE_RUNTIME);
		}
		return List.of();
	}
}
