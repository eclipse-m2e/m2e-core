/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich
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

import java.util.List;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.MavenTargetLocation;

public class MavenTargetTreeContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) parentElement;
			List<DependencyNode> nodes = location.getDependencyNodes();
			if (nodes != null) {
				for (DependencyNode dependencyNode : nodes) {
					if (dependencyNode.getData().containsKey(MavenTargetLocation.DEPENDENCYNODE_IS_ROOT)) {
						dependencyNode.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parentElement);
						return getChildren(dependencyNode);
					}
				}
			}
		} else if (parentElement instanceof DependencyNode) {
			DependencyNode[] dependencyNodes = ((DependencyNode) parentElement).getChildren()
					.toArray(new DependencyNode[0]);
			for (DependencyNode dependencyNode : dependencyNodes) {
				dependencyNode.setData(MavenTargetLocation.DEPENDENCYNODE_PARENT, parentElement);
			}
			return dependencyNodes;
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof DependencyNode) {
			DependencyNode dependencyNode = (DependencyNode) element;
			Object parent = dependencyNode.getData().get(MavenTargetLocation.DEPENDENCYNODE_PARENT);
			if (parent instanceof DependencyNode) {
				DependencyNode dp = (DependencyNode) parent;
				if (dp.getData().containsKey(MavenTargetLocation.DEPENDENCYNODE_IS_ROOT)) {
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
			List<DependencyNode> dependencyNodes = location.getDependencyNodes();
			return dependencyNodes != null && !dependencyNodes.isEmpty();
		} else if (element instanceof DependencyNode) {
			DependencyNode node = (DependencyNode) element;
			return !node.getChildren().isEmpty();
		}
		return false;
	}

}
