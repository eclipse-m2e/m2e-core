/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
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
package org.eclipse.m2e.pde.ui.target.adapter;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.ui.Activator;
import org.eclipse.m2e.pde.ui.target.editor.MavenTargetLocationEditor;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetDependencyLabelProvider;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetTreeContentProvider;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class MavenTargetDependencyAdapterFactory implements IAdapterFactory {

	public static final ILabelProvider LABEL_PROVIDER = new MavenTargetDependencyLabelProvider();
	public static final ITreeContentProvider TREE_CONTENT_PROVIDER = new MavenTargetTreeContentProvider();
	private static final MavenTargetLocationEditor LOCATION_EDITOR = new MavenTargetLocationEditor();
	static {
		Activator.runOnBundleStop(LABEL_PROVIDER::dispose);
		Activator.runOnBundleStop(TREE_CONTENT_PROVIDER::dispose);
	}
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof MavenTargetDependency) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			} else if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			} else if (adapterType == ITargetLocationHandler.class) {
				return adapterType.cast(LOCATION_EDITOR);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ILabelProvider.class, ITreeContentProvider.class, ITargetLocationHandler.class };
	}

}
