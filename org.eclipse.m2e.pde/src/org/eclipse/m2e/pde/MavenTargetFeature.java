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

package org.eclipse.m2e.pde;

import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

public class MavenTargetFeature extends TargetFeature {

	MavenTargetFeature(@SuppressWarnings("restriction") IFeatureModel model) {
		super(model);
	}

}
