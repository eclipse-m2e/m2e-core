/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde;

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;

import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

public class MavenTargetDependencyFilter implements DependencyFilter {

	private static final String[] VALID_EXTENSIONS = { "jar", "pom" };
	private String locationScope;
	private DependencyDepth dependencyDepth;

	public MavenTargetDependencyFilter(DependencyDepth dependencyDepth, String scope) {
		this.dependencyDepth = dependencyDepth;
		this.locationScope = scope;
	}

	@Override
	public boolean accept(DependencyNode node, List<DependencyNode> parents) {
		String extension = node.getArtifact().getExtension();
		for (String valid : VALID_EXTENSIONS) {
			// only for a valid extension...
			if (valid.equalsIgnoreCase(extension)) {
				if (dependencyDepth == DependencyDepth.INFINITE
						|| (dependencyDepth == DependencyDepth.DIRECT && parents.size() <= 1)) {
					return isValidScope(node.getDependency());
				}
			}
		}
		return false;
	}

	private boolean isValidScope(Dependency dependency) {
		String dependecyScope = dependency.getScope();
		if (dependecyScope == null || dependecyScope.isBlank()) {
			return true;
		}
		if (locationScope == null || locationScope.isBlank() || SCOPE_COMPILE.equalsIgnoreCase(locationScope)) {
			return SCOPE_COMPILE.equalsIgnoreCase(dependecyScope);
		}
		if (SCOPE_PROVIDED.equalsIgnoreCase(locationScope)) {
			return SCOPE_PROVIDED.equalsIgnoreCase(dependecyScope) || SCOPE_COMPILE.equalsIgnoreCase(dependecyScope)
					|| SCOPE_SYSTEM.equalsIgnoreCase(dependecyScope) || SCOPE_RUNTIME.equalsIgnoreCase(dependecyScope);
		}
		if (SCOPE_TEST.equalsIgnoreCase(locationScope)) {
			return SCOPE_TEST.equalsIgnoreCase(dependecyScope) || SCOPE_COMPILE.equalsIgnoreCase(dependecyScope)
					|| SCOPE_PROVIDED.equalsIgnoreCase(dependecyScope) || SCOPE_SYSTEM.equalsIgnoreCase(dependecyScope)
					|| SCOPE_RUNTIME.equalsIgnoreCase(dependecyScope);
		}
		return false;
	}
}
