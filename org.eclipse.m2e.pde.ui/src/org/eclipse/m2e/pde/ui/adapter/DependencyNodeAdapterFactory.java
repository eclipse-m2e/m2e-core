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

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.ui.provider.DependencyNodeLabelProvider;
import org.eclipse.m2e.pde.ui.provider.MavenTargetTreeContentProvider;

public class DependencyNodeAdapterFactory implements IAdapterFactory {

	public static final ITreeContentProvider TREE_CONTENT_PROVIDER = new MavenTargetTreeContentProvider();
	public static final ILabelProvider LABEL_PROVIDER = new DependencyNodeLabelProvider();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof DependencyNode) {
			if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			} else if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ITreeContentProvider.class, ILabelProvider.class };
	}

}
