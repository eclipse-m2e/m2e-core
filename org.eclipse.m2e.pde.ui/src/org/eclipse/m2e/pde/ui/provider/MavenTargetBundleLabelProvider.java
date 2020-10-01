/*******************************************************************************
 * Copyright (c) 2018 Christoph Läubrich
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

import org.eclipse.m2e.pde.MavenTargetBundle;
import org.eclipse.m2e.pde.ui.adapter.MavenTargetAdapterFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

@SuppressWarnings("restriction")
public class MavenTargetBundleLabelProvider
		extends org.eclipse.pde.internal.ui.shared.target.StyledBundleLabelProvider {

	private Image image;

	public MavenTargetBundleLabelProvider() {
		super(true, false);
	}

	@Override
	public org.eclipse.swt.graphics.Image getImage(Object element) {
		if (element instanceof MavenTargetBundle) {
			if (((MavenTargetBundle) element).isWrapped()) {
				Display current = Display.getCurrent();
				if (image == null && current != null) {
					image = new Image(current,
							MavenTargetAdapterFactory.class.getResourceAsStream("/icons/jar_obj.gif"));
				}
				return image;
			}
		}
		return super.getImage(element);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (image != null) {
			image.dispose();
		}
	}

}
