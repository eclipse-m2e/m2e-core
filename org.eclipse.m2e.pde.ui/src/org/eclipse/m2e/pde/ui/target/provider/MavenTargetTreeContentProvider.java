/*******************************************************************************
 * Copyright (c) 2018, 2021 Christoph Läubrich
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
package org.eclipse.m2e.pde.ui.target.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.pde.core.target.TargetFeature;

public class MavenTargetTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@SuppressWarnings("restriction")
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof MavenTargetLocation location) {
			List<Object> childs = new ArrayList<>();
			List<MavenTargetDependency> roots = location.getRoots();
			if (roots.size() == 1) {
				childs.addAll(Arrays.asList(getDependencyChilds(roots.get(0), parentElement)));
			} else {
				childs.addAll(roots);
			}
			TargetFeature[] features = location.getFeatures();
			if (features != null && features.length > 0) {
				childs.addAll(Arrays.asList(features));
			}
			return childs.toArray();
		} else if (parentElement instanceof DependencyNode dependency) {
			DependencyNode[] dependencyNodes = dependency.getChildren().stream()
					.filter(d -> d.getArtifact().getFile() != null).toArray(DependencyNode[]::new);
			for (DependencyNode dependencyNode : dependencyNodes) {
				dependencyNode.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parentElement);
			}
			return dependencyNodes;
		} else if (parentElement instanceof MavenTargetDependency dependency) {
			return getDependencyChilds(dependency, parentElement);
		}
		return new Object[0];
	}

	private Object[] getDependencyChilds(MavenTargetDependency targetDependency, Object parentElement) {

		List<DependencyNode> nodes = targetDependency.getDependencyNodes();
		if (nodes != null) {
			for (DependencyNode dependencyNode : nodes) {
				if (dependencyNode.getData().containsKey(MavenTargetLocation.DEPENDENCYNODE_ROOT)) {
					dependencyNode.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parentElement);
					return getChildren(dependencyNode);
				}
			}
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof DependencyNode dependencyNode) {
			Object parent = dependencyNode.getData().get(MavenTargetLocation.DEPENDENCYNODE_PARENT);
			if (parent instanceof DependencyNode dp
					&& dp.getData().containsKey(MavenTargetLocation.DEPENDENCYNODE_ROOT)) {
				return getParent(dp);
			}
			return parent;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof MavenTargetLocation location) {
			return !location.getRoots().isEmpty();
		} else if (element instanceof DependencyNode node) {
			return !node.getChildren().isEmpty();
		} else if (element instanceof MavenTargetDependency targetDependency) {
			List<DependencyNode> dependencyNodes = targetDependency.getDependencyNodes();
			return dependencyNodes != null && !dependencyNodes.isEmpty();
		}
		return false;
	}

}
