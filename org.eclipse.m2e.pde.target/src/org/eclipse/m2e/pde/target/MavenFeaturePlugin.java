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
package org.eclipse.m2e.pde.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

@SuppressWarnings("restriction")
class MavenFeaturePlugin extends FeaturePlugin {

	private static final long serialVersionUID = -4864755319910409580L;

	MavenFeaturePlugin(TargetBundle child, IFeatureModel featureModel) throws CoreException {
		setModel(featureModel);
		BundleInfo bundleInfo = child.getBundleInfo();
		setId(bundleInfo.getSymbolicName());
		setVersion(bundleInfo.getVersion());
		setFragment(child.isFragment());
		setUnpack(false);
	}

	@Override
	public boolean exists() {
		return true;
	}
}
