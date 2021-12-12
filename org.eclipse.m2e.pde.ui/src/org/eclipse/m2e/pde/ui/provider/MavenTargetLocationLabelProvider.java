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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.MavenTargetDependency;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.m2e.pde.ui.adapter.MavenTargetAdapterFactory;
import org.eclipse.m2e.pde.ui.editor.Messages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class MavenTargetLocationLabelProvider implements ILabelProvider {
	private Image image;

	@SuppressWarnings("restriction")
	@Override
	public String getText(Object element) {
		if (element instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) element;
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
		Display current = Display.getCurrent();
		if (image == null && current != null) {
			image = new Image(current, MavenTargetAdapterFactory.class.getResourceAsStream("/icons/m2.gif"));
		}
		return image;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {

	}

	@Override
	public void dispose() {
		if (image != null) {
			image.dispose();
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
