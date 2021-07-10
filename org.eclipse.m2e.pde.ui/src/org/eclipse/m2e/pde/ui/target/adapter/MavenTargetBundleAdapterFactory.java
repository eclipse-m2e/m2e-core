/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
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
import org.eclipse.m2e.pde.target.MavenTargetBundle;
import org.eclipse.m2e.pde.ui.target.provider.MavenTargetBundleLabelProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = IAdapterFactory.class, property = {
		IAdapterFactory.SERVICE_PROPERTY_ADAPTABLE_CLASS + "=org.eclipse.m2e.pde.target.MavenTargetBundle",
		IAdapterFactory.SERVICE_PROPERTY_ADAPTER_NAMES + "=org.eclipse.jface.viewers.ILabelProvider" })
public class MavenTargetBundleAdapterFactory implements IAdapterFactory {

	private final ILabelProvider labelProvider = new MavenTargetBundleLabelProvider();

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof MavenTargetBundle && adapterType == ILabelProvider.class) {
			return adapterType.cast(labelProvider);
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ILabelProvider.class };
	}

	@Deactivate
	void dispose() {
		labelProvider.dispose();
	}

}
