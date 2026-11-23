/*******************************************************************************
 * Copyright (c) 2018, 2026 Christoph Läubrich
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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.ui.target.editor.Messages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("restriction")
public class MavenTargetLocationLabelProvider implements ILabelProvider {
	private final ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

	@Override
	public String getText(Object element) {
		if (element instanceof MavenTargetLocation location) {
			String name = location.getLabel();
			if (name != null && !name.isBlank()) {
				return name;
			}
			IFeature featureTemplate = location.getFeatureTemplate();
			if (featureTemplate != null) {
				String label = featureTemplate.getLabel();
				if (label != null && !label.isBlank()) {
					return label;
				}
				String id = featureTemplate.getId();
				if (id != null && !id.isBlank()) {
					return id;
				}
			}
			List<MavenTargetDependency> roots = location.getRoots();
			if (roots.size() == 1) {
				MavenTargetDependency dependency = roots.get(0);
				return MessageFormat.format(Messages.MavenTargetLocationLabelProvider_1, dependency.getGroupId(),
						dependency.getArtifactId(), dependency.getVersion());
			} else {
				return MessageFormat.format(Messages.MavenTargetLocationLabelProvider_2, roots.size());
			}
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		return resourceManager.create(
				ResourceLocator.imageDescriptorFromBundle("org.eclipse.m2e.pde.ui", "icons/m2.gif").orElseThrow());
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
