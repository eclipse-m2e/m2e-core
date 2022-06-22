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

	private final ILabelProvider LABEL_PROVIDER = new MavenTargetBundleLabelProvider();
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof MavenTargetBundle) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ILabelProvider.class };
	}

	@Deactivate
	void dispose() {
		LABEL_PROVIDER.dispose();
	}

}
