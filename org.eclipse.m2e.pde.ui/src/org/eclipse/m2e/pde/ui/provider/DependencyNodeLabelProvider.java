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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class DependencyNodeLabelProvider implements ILabelProvider {
	private Image inheritedImage;
	private Image jarImage;
	private Image errorImage;

	@Override
	public String getText(Object element) {
		if (element instanceof DependencyNode) {
			DependencyNode node = (DependencyNode) element;
			Artifact artifact = node.getArtifact();
			MavenTargetLocation location = getTargetLocation(node);
			String baseLabel = artifact.getGroupId() + ":" + artifact.getArtifactId() + " (" + artifact.getVersion()
					+ ")";
			if (location != null) {
				if (location.isIgnored(artifact)) {
					return "(ignored) " + baseLabel;
				} else if (location.isFailed(artifact)) {
					return "(failed) " + baseLabel;
				}
			}
			return baseLabel;
		}
		return String.valueOf(element);
	}

	private MavenTargetLocation getTargetLocation(DependencyNode node) {
		Object object = node.getData().get(MavenTargetLocation.DEPENDENCYNODE_PARENT);
		if (object instanceof DependencyNode) {
			return getTargetLocation((DependencyNode) object);
		} else if (object instanceof MavenTargetLocation) {
			return (MavenTargetLocation) object;
		} else {
			return null;
		}
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof DependencyNode) {
			DependencyNode node = (DependencyNode) element;
			MavenTargetLocation location = getTargetLocation(node);
			Display current = Display.getCurrent();
			if (location != null) {
				if (location.isIgnored(node.getArtifact())) {
					if (jarImage == null && current != null) {
						jarImage = new Image(current, DependencyNodeLabelProvider.class
								.getResourceAsStream("/icons/jar_obj.gif"));
					}
					return jarImage;
				} else if (location.isFailed(node.getArtifact())) {
					if (errorImage == null && current != null) {
						errorImage = new Image(current,
								DependencyNodeLabelProvider.class.getResourceAsStream("/icons/error_st_obj.gif"));
					}
					return errorImage;
				}
			}
			if (inheritedImage == null && current != null) {
				inheritedImage = new Image(current, DependencyNodeLabelProvider.class
						.getResourceAsStream("/icons/show_inherited_dependencies.gif"));
			}
			return inheritedImage;
		}
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {
		if (inheritedImage != null) {
			inheritedImage.dispose();
		}
		if (jarImage != null) {
			jarImage.dispose();
		}
		if (errorImage != null) {
			errorImage.dispose();
		}
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {

	}

}
