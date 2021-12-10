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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;

import aQute.bnd.version.Version;

/**
 * represents a resolved set of {@link Artifact} -> {@link TargetBundle}
 */
class TargetBundles {
	final Map<Artifact, TargetBundle> bundles = new HashMap<>();
	final Set<Artifact> ignoredArtifacts = new HashSet<>();
	final List<TargetFeature> features = new ArrayList<>();
	final Map<MavenTargetDependency, List<DependencyNode>> dependencyNodes = new HashMap<>();

	Optional<DependencyNode> getDependencyNode(Artifact artifact) {
		return dependencyNodes.values().stream().flatMap(l -> l.stream())
				.filter(node -> artifact.equals(node.getArtifact())).findAny();
	}

	Optional<MavenTargetBundle> getTargetBundle(Artifact artifact) {
		TargetBundle targetBundle = bundles.get(artifact);
		if (targetBundle instanceof MavenTargetBundle) {
			return Optional.of((MavenTargetBundle) targetBundle);
		}
		return Optional.empty();
	}

	Optional<MavenTargetBundle> getTargetBundle(MavenTargetDependency dependency) {
		List<DependencyNode> list = dependencyNodes.get(dependency);
		if (list != null) {
			Optional<Artifact> artifact = list.stream()
					.filter(node -> node.getData().get(MavenTargetLocation.DEPENDENCYNODE_ROOT) == dependency)
					.findFirst().map(DependencyNode::getArtifact);
			return artifact.flatMap(this::getTargetBundle);
		}
		return Optional.empty();
	}

	public static Version createOSGiVersion(Artifact artifact) {
		String version = artifact.getVersion();
		return createOSGiVersion(version);
	}

	public static Version createOSGiVersion(Model model) {
		return createOSGiVersion(model.getVersion());
	}

	public static Version createOSGiVersion(String version) {
		if (version == null || version.isEmpty()) {
			return new Version(0, 0, 1);
		}
		try {
			int index = version.indexOf('-');
			if (index > -1) {
				StringBuilder sb = new StringBuilder(version);
				sb.setCharAt(index, '.');
				return Version.parseVersion(sb.toString());
			}
			return Version.parseVersion(version);
		} catch (IllegalArgumentException e) {
			return new Version(0, 0, 1, version);
		}
	}
}
