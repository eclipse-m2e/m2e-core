/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others
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
package org.eclipse.m2e.pde.ui.target.editor;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.ui.target.editor.internal.DependencyTable;
import org.eclipse.m2e.pde.ui.target.editor.internal.TargetDependencyModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MavenTargetDependencyEditor {
	private final Composite composite;
	private final TargetDependencyModel model;

	public MavenTargetDependencyEditor(Composite parent, MavenTargetLocation targetLocation,
			MavenTargetDependency selectedRoot) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new BorderLayout());

		model = new TargetDependencyModel(targetLocation, selectedRoot);

		new DependencyTable(composite, model);
	}

	public Control getControl() {
		return composite;
	}

	public boolean hasErrors() {
		return model.hasErrors();
	}

	public Collection<MavenTargetDependency> getRoots() {
		return new ArrayList<>(model.getTargetDependencies());
	}
}
