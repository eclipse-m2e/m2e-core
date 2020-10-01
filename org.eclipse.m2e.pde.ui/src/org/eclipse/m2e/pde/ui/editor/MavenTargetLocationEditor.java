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
package org.eclipse.m2e.pde.ui.editor;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationEditor;

public class MavenTargetLocationEditor implements ITargetLocationEditor {

	@Override
	public boolean canEdit(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation instanceof MavenTargetLocation;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, ITargetLocation targetLocation) {
		MavenTargetLocationWizard wizard = new MavenTargetLocationWizard((MavenTargetLocation) targetLocation);
		wizard.setTarget(target);
		return wizard;
	}

}
