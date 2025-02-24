/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;

/**
 * represents a resolved set of {@link Artifact} -> {@link TargetBundle}
 */
class TargetBundles {
	private final Map<Artifact, TargetBundle> bundles = new HashMap<>();
	private final Map<File, Artifact> artifacts = new HashMap<>();
	private final Map<Artifact, MavenSourceBundle> sourceBundles = new HashMap<>();
	final Set<Artifact> ignoredArtifacts = new HashSet<>();
	final List<TargetFeature> features = new ArrayList<>();
	final Map<MavenTargetDependency, List<DependencyNode>> dependencyNodes = new HashMap<>();

	Optional<DependencyNode> getDependencyNode(Artifact artifact) {
		return dependencyNodes.values().stream().flatMap(List::stream)
				.filter(node -> artifact.equals(node.getArtifact())).findAny();
	}

	Optional<MavenTargetBundle> getMavenTargetBundle(Artifact artifact) {
		return Optional.ofNullable(bundles.get(artifact)).filter(MavenTargetBundle.class::isInstance)
				.map(MavenTargetBundle.class::cast);
	}

	Optional<TargetBundle> getTargetBundle(Artifact artifact, boolean source) {
		Map<Artifact, ? extends TargetBundle> bundlesMap = source ? sourceBundles : bundles;
		return Optional.ofNullable(bundlesMap.get(artifact));
	}

	Optional<MavenTargetBundle> getTargetBundle(MavenTargetDependency dependency) {
		List<DependencyNode> list = dependencyNodes.get(dependency);
		if (list != null) {
			Optional<Artifact> artifact = list.stream()
					.filter(node -> node.getData().get(MavenTargetLocation.DEPENDENCYNODE_ROOT) == dependency)
					.findFirst().map(DependencyNode::getArtifact);
			return artifact.flatMap(this::getMavenTargetBundle);
		}
		return Optional.empty();
	}

	public void addBundle(Artifact artifact, TargetBundle bundle) {

		File file = artifact.getFile();
		if (file == null) {
			bundles.put(artifact, bundle);
		} else {
			if (artifacts.put(file, artifact) == null) {
				bundles.put(artifact, bundle);
			}
		}
	}

	public void addSourceBundle(Artifact artifact, MavenSourceBundle sourceBundle) {
		sourceBundles.put(artifact, sourceBundle);
	}

	public Stream<Entry<Artifact, TargetBundle>> bundles() {
		return bundles.entrySet().stream();
	}

	public Optional<Artifact> getArtifact(File file) {
		return Optional.ofNullable(artifacts.get(file));
	}

}
