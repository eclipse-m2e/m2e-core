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

package org.eclipse.m2e.pde.target;

import org.eclipse.pde.core.target.TargetFeature;

public class MavenTargetFeature extends TargetFeature {

	MavenTargetFeature(@SuppressWarnings("restriction") org.eclipse.pde.internal.core.ifeature.IFeatureModel model) {
		super(model);
	}

}
