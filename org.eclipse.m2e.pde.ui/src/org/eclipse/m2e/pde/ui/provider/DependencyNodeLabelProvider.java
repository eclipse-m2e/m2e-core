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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.m2e.pde.MavenTargetBundle;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.swt.graphics.Image;

public class DependencyNodeLabelProvider implements ILabelProvider {

	private ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
	private ImageDescriptor jarDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/jar_obj.gif"));
	private ImageDescriptor inheritedDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/show_inherited_dependencies.gif"));
	private ImageDescriptor inheritedJarDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/jar_dep.png"));
	private ImageDescriptor inheritedJarDefaultDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/jar_dep_default.png"));
	private ImageDescriptor errorDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/error_st_obj.gif"));
	private ImageDescriptor disabledDescriptor = ImageDescriptor.createFromURLSupplier(true,
			() -> DependencyNodeLabelProvider.class.getResource("/icons/clear.gif"));

	@Override
	public String getText(Object element) {
		if (element instanceof DependencyNode) {
			DependencyNode node = (DependencyNode) element;
			Artifact artifact = node.getArtifact();
			MavenTargetLocation location = getTargetLocation(node);
			String baseLabel = artifact.getGroupId() + ":" + artifact.getArtifactId() + " (" + artifact.getVersion()
					+ ")";
			if (location != null) {
				if (location.isDisabled(artifact)) {
					return "(excluded) " + baseLabel;
				} else if (location.isIgnored(artifact)) {
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
			if (location != null) {
				if (location.isDisabled(node.getArtifact())) {
					return resourceManager.createImage(disabledDescriptor);
				} else if (location.isIgnored(node.getArtifact())) {
					return resourceManager.createImage(jarDescriptor);
				} else if (location.isFailed(node.getArtifact())) {
					return resourceManager.createImage(errorDescriptor);
				}
				MavenTargetBundle targetBundle = location.getTargetBundle(node.getArtifact());
				if (targetBundle != null && targetBundle.isWrapped()) {
					BNDInstructions instructions = location.getInstructions(node.getArtifact());
					if (instructions.isEmpty()) {
						return resourceManager.createImage(inheritedJarDefaultDescriptor);
					} else {
						return resourceManager.createImage(inheritedJarDescriptor);
					}

				}
			}
			return resourceManager.createImage(inheritedDescriptor);
		}
		return null;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {
		resourceManager.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {

	}

}
