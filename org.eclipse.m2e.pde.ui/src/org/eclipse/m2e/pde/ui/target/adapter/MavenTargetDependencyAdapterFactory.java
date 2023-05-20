/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others
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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.ui.target.editor.MavenTargetLocationEditor;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetDependencyLabelProvider;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetTreeContentProvider;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = IAdapterFactory.class, property = {
		IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.m2e.pde.target.MavenTargetDependency",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.jface.viewers.ILabelProvider",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.jface.viewers.ITreeContentProvider",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.pde.ui.target.ITargetLocationHandler" })
public class MavenTargetDependencyAdapterFactory implements IAdapterFactory {

	private final ILabelProvider LABEL_PROVIDER = new MavenTargetDependencyLabelProvider();
	private final ITreeContentProvider TREE_CONTENT_PROVIDER = new MavenTargetTreeContentProvider();
	private final MavenTargetLocationEditor LOCATION_EDITOR = new MavenTargetLocationEditor();
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

	@Deactivate
	void dispose() {
		LABEL_PROVIDER.dispose();
		TREE_CONTENT_PROVIDER.dispose();
	}

}
