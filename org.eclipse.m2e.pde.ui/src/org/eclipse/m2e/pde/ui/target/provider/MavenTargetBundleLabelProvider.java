/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich
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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2e.pde.target.MavenTargetBundle;
import org.eclipse.m2e.pde.ui.target.adapter.MavenTargetAdapterFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class MavenTargetBundleLabelProvider extends LabelProvider {

	private Image image;

	@Override
	public String getText(Object element) {
		return null;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof MavenTargetBundle targetBundle && targetBundle.isWrapped()) {
			Display current = Display.getCurrent();
			if (image == null && current != null) {
				image = new Image(current, MavenTargetAdapterFactory.class.getResourceAsStream("/icons/jar_obj.gif"));
			}
			return image;
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (image != null) {
			image.dispose();
		}
	}

}
