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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class DependencyNodeLabelProvider implements ILabelProvider {
	private Image image;

	@Override
	public String getText(Object element) {
		if (element instanceof DependencyNode) {
			Artifact artifact = ((DependencyNode) element).getArtifact();
			return artifact.getGroupId() + ":" + artifact.getArtifactId() + " (" + artifact.getVersion() + ")";
		}
		return String.valueOf(element);
	}

	@Override
	public Image getImage(Object element) {
		Display current = Display.getCurrent();
		if (image == null && current != null) {

			image = new Image(current,
					DependencyNodeLabelProvider.class.getResourceAsStream("/icons/show_inherited_dependencies.gif"));
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
