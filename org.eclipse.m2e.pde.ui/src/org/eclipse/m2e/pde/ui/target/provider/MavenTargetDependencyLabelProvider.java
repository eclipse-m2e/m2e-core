/*******************************************************************************
 * Copyright (c) 2021, 2026 Christoph Läubrich
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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.swt.graphics.Image;

public class MavenTargetDependencyLabelProvider implements ILabelProvider {

	private final ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

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

	@Override
	public Image getImage(Object element) {
		return resourceManager.create(
				ResourceLocator.imageDescriptorFromBundle("org.eclipse.m2e.pde.ui", "icons/m2.gif").orElseThrow());
	}

	@Override
	public String getText(Object element) {
		if (element instanceof MavenTargetDependency targetDependency) {
			return targetDependency.getKey();
		}
		return String.valueOf(element);
	}

}
