/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui.target.adapter;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.ui.target.editor.MavenTargetLocationEditor;
import org.eclipse.m2e.pde.ui.target.provider.DependencyNodeLabelProvider;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetTreeContentProvider;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = IAdapterFactory.class, property = {
		IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.aether.graph.DependencyNode",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.jface.viewers.ILabelProvider",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.jface.viewers.ITreeContentProvider",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.pde.ui.target.ITargetLocationHandler" })
public class DependencyNodeAdapterFactory implements IAdapterFactory {

	private final ITreeContentProvider TREE_CONTENT_PROVIDER = new MavenTargetTreeContentProvider();
	private final ILabelProvider LABEL_PROVIDER = new DependencyNodeLabelProvider();
	private final MavenTargetLocationEditor LOCATION_EDITOR = new MavenTargetLocationEditor();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof DependencyNode) {
			if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			} else if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			} else if (adapterType == ITargetLocationHandler.class) {
				return adapterType.cast(LOCATION_EDITOR);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ITreeContentProvider.class, ILabelProvider.class, ITargetLocationHandler.class };
	}

	@Deactivate
	void dispose() {
		TREE_CONTENT_PROVIDER.dispose();
		LABEL_PROVIDER.dispose();
	}

}
