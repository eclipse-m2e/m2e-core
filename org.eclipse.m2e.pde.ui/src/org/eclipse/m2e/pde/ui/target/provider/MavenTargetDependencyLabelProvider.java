/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich
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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.ui.target.adapter.MavenTargetAdapterFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class MavenTargetDependencyLabelProvider implements ILabelProvider {

	private Image image;

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

	@Override
	public Image getImage(Object element) {
		Display current = Display.getCurrent();
		if (image == null && current != null) {
			image = new Image(current, MavenTargetAdapterFactory.class.getResourceAsStream("/icons/m2.gif"));
		}
		return image;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof MavenTargetDependency targetDependency) {
			return targetDependency.getKey();
		}
		return String.valueOf(element);
	}

}
