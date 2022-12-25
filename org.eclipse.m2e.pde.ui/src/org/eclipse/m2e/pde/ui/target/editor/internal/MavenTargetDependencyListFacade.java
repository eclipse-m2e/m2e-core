/*******************************************************************************
 * Copyright (c) 2023 Patrick Ziegler and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui.target.editor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.m2e.pde.target.MavenTargetDependency;

/**
 * This class adds the ability to replace all values of an observable list
 * within a single operation and to modify the fields of a single
 * {@link MavenTargetDependency}.<br>
 * Former is necessary when e.g. initializing the model with the dependencies of
 * the target location. This operation can already be imitated by calling
 * {@link #clear()}, followed by an {@link #addAll(Collection)}, except that
 * this fires two change events, instead of just one.<br>
 * Latter is required when editing the content of table cells. This modification
 * should cause the {@code Update} button to re-evaluluate its
 * {@link enablement} property, because the currently selected element might now
 * have a valid GAV.
 *
 * Calling {@link #modify(MavenTargetDependency, BiConsumer, String),
 * {@link #set(Collection)} or {@link #set(MavenTargetDependency)} counts as a
 * {@code set} operation.
 */
public class MavenTargetDependencyListFacade extends WritableList<MavenTargetDependency> {
	public void modify(MavenTargetDependency source, BiConsumer<MavenTargetDependency, String> setter, String value) {
		setter.accept(source, value);
		// The ModifyPropertyOperation keeps track of the old and new value.
		fireListChange(Diffs.computeListDiff(this, this));
	}

	public void set(MavenTargetDependency newValue) {
		set(Collections.singleton(newValue));
	}

	public void set(Collection<MavenTargetDependency> newValue) {
		// Make sure the list is mutable
		updateWrappedList(new ArrayList<>(newValue));
	}
}
