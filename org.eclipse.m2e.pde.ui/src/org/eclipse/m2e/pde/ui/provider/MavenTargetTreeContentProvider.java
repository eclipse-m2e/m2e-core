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
package org.eclipse.m2e.pde.ui.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.MavenTargetDependency;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.pde.core.target.TargetFeature;

public class MavenTargetTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@SuppressWarnings("restriction")
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) parentElement;
			List<Object> childs = new ArrayList<Object>();
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
		} else if (parentElement instanceof DependencyNode) {
			DependencyNode[] dependencyNodes = ((DependencyNode) parentElement).getChildren().stream()
					.filter(d -> d.getArtifact().getFile() != null).toArray(DependencyNode[]::new);
			for (DependencyNode dependencyNode : dependencyNodes) {
				dependencyNode.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parentElement);
			}
			return dependencyNodes;
		} else if (parentElement instanceof MavenTargetDependency) {
			return getDependencyChilds((MavenTargetDependency) parentElement, parentElement);
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
		if (element instanceof DependencyNode) {
			DependencyNode dependencyNode = (DependencyNode) element;
			Object parent = dependencyNode.getData().get(MavenTargetLocation.DEPENDENCYNODE_PARENT);
			if (parent instanceof DependencyNode) {
				DependencyNode dp = (DependencyNode) parent;
				if (dp.getData().containsKey(MavenTargetLocation.DEPENDENCYNODE_ROOT)) {
					return getParent(dp);
				}
			}
			return parent;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) element;
			return location.getRoots().size() > 0;
		} else if (element instanceof DependencyNode) {
			DependencyNode node = (DependencyNode) element;
			return !node.getChildren().isEmpty();
		} else if (element instanceof MavenTargetDependency) {
			MavenTargetDependency targetDependency = (MavenTargetDependency) element;
			List<DependencyNode> dependencyNodes = targetDependency.getDependencyNodes();
			return dependencyNodes != null && !dependencyNodes.isEmpty();
		}
		return false;
	}

}
