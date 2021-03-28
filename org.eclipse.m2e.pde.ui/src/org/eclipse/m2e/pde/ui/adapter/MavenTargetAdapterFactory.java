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
package org.eclipse.m2e.pde.ui.adapter;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.m2e.pde.ui.MavenTargetLocationUpdater;
import org.eclipse.m2e.pde.ui.editor.MavenTargetLocationEditor;
import org.eclipse.m2e.pde.ui.provider.MavenTargetLocationLabelProvider;
import org.eclipse.m2e.pde.ui.provider.MavenTargetTreeContentProvider;
import org.eclipse.pde.ui.target.ITargetLocationEditor;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

public class MavenTargetAdapterFactory implements IAdapterFactory {

	public static final ILabelProvider LABEL_PROVIDER = new MavenTargetLocationLabelProvider();
	public static final ITreeContentProvider TREE_CONTENT_PROVIDER = new MavenTargetTreeContentProvider();
	private static final MavenTargetLocationEditor LOCATION_EDITOR = new MavenTargetLocationEditor();
	private static final MavenTargetLocationUpdater LOCATION_UPDATER = new MavenTargetLocationUpdater();

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ILabelProvider.class, ITreeContentProvider.class, ITargetLocationEditor.class,
				ITargetLocationUpdater.class };
	}

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof MavenTargetLocation) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			} else if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			} else if (adapterType == ITargetLocationEditor.class) {
				return adapterType.cast(LOCATION_EDITOR);
			} else if (adapterType == ITargetLocationUpdater.class) {
				return adapterType.cast(LOCATION_UPDATER);
			}
		}
		return null;
	}

}
