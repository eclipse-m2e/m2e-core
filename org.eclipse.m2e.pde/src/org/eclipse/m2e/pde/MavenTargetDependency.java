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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public final class MavenTargetDependency extends Dependency {

	private MavenTargetLocation location;

	public MavenTargetDependency(String groupId, String artifactId, String version,
			String artifactType, String classifier) {
		setGroupId(groupId);
		setArtifactId(artifactId);
		setVersion(version);

		if (artifactType != null && !artifactType.isBlank()) {
			setType(artifactType);
		} else {
			setType(MavenTargetLocation.DEFAULT_PACKAGE_TYPE);
		}
		setClassifier(classifier);
	}

	public MavenTargetLocation getTargetLocation() {
		return location;
	}

	public List<DependencyNode> getDependencyNodes() {
		if (location == null) {
			return null;
		}
		return location.getDependencyNodes(this);
	}

	public String getKey() {
		String key = getGroupId() + ":" + getArtifactId();
		String classifier = getClassifier();
		if (classifier != null && !classifier.isBlank()) {
			key += ":" + classifier;
		}
		String type = getType();
		if (type != null && !type.isBlank()) {
			key += ":" + type;
		}
		key += ":" + getVersion();
		return key;
	}

	public void bind(MavenTargetLocation mavenTargetLocation) {
		if (this.location == null) {
			this.location = mavenTargetLocation;
		} else {
			throw new IllegalStateException("already bound!");
		}
	}

	public MavenTargetDependency copy() {
		return new MavenTargetDependency(getGroupId(), getArtifactId(), getVersion(), getType(),
				getClassifier());
	}

}
