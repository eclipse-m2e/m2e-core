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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.maven.shared.utils.StringUtils;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffectFactory;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.ui.target.editor.ClipboardParser;
import org.eclipse.swt.widgets.Display;

/**
 * This class represents the data model used by the dependency editor. It keeps
 * track of target dependencies, as well as all of the currently selected
 * elements. The for the editor relevant fields are stored in
 * {@link IObersableValue}, so that they can react to model changes via the
 * {@link ISideEffectFactory}.
 */
public class TargetDependencyModel {
	private static final ILog LOGGER = Platform.getLog(TargetDependencyModel.class);
	/**
	 * Target location over which the editor is defined. Required for requesting the
	 * latest version of a Maven dependency, as they may be hosted on a 3rd party
	 * repository. May be null.
	 */
	private final MavenTargetLocation targetLocation;
	/**
	 * All Maven dependencies of the target location. Initialized with the root
	 * elements of {@link targetLocation}. If this is empty, it is initialized with
	 * the dependencies in the clipboard. If this is empty, it is initialized with a
	 * blank dependency. May be empty.
	 */
	private final MavenTargetDependencyListFacade targetDependencies = new MavenTargetDependencyListFacade();
	/**
	 * All currently selected elements. Initialized with the first element of
	 * {@link #targetDependencies} or alternatively the currently selected root
	 * element in the target editor. May be empty.
	 */
	private final MavenTargetDependencyListFacade currentSelection = new MavenTargetDependencyListFacade();
	/**
	 * The history facade used to handle undo/redo operations.
	 */
	private final OperationHistoryFacade history;
	/**
	 * Each dependency must contain a valid group id, artifact id, version and type.
	 */
	private final IObservableValue<Boolean> hasErrors = new WritableValue<>();

	public TargetDependencyModel(MavenTargetLocation targetLocation, MavenTargetDependency selectedRoot) {
		this.history = new OperationHistoryFacade(this);
		this.targetLocation = targetLocation;

		int itemToSelect;

		if (targetLocation == null || targetLocation.getRoots().isEmpty()) {
			itemToSelect = 0;
			targetDependencies.set(getDefaultClipboardDependencies());
		} else {
			itemToSelect = selectedRoot == null ? 0 : targetLocation.getRoots().indexOf(selectedRoot);
			// DON'T WORK DIRECTLY ON THE TARGET LOCATION!
			targetDependencies.set(deepClone(targetLocation.getRoots()));
		}

		currentSelection.set(targetDependencies.get(itemToSelect));

		// The target location might be invalid if e.g. the XML file was edited directly
		check();
	}

	public void setCurrentSelection(List<MavenTargetDependency> newCurrentSelection) {
		currentSelection.set(newCurrentSelection);
	}

	public List<MavenTargetDependency> getCurrentSelection() {
		return List.copyOf(currentSelection);
	}

	public void setTargetDependencies(List<MavenTargetDependency> newTargetDependencies) {
		targetDependencies.set(newTargetDependencies);
	}

	public List<MavenTargetDependency> getTargetDependencies() {
		return List.copyOf(new ArrayList<>(targetDependencies));
	}

	public OperationHistoryFacade getHistory() {
		return history;
	}

	/**
	 * Modifies the property of the selected element. For example the group id or
	 * version. Does nothing of the old and new value are identical. An error is
	 * logged if the element is not selected.<br>
	 * This operation is undoable.
	 * 
	 * @param source   The original, unmodified dependency.
	 * @param setter   The accessor used to apply the given values. Must not be
	 *                 null.
	 * @param oldValue The original value. Restored after an {@code undo} operation.
	 *                 May be null.
	 * @param newValue The new value applied after an {@code execute} or
	 *                 {@code redo} operation. May be null.
	 */
	public void modify(MavenTargetDependency source, BiConsumer<MavenTargetDependency, String> setter, String oldValue,
			String newValue) {
		if (Objects.equals(oldValue, newValue)) {
			return;
		}

		history.propertyChange(value -> currentSelection.modify(source, setter, value), oldValue, newValue);
	}

	/**
	 * Adds the current clipboard dependencies to the list of target dependencies.
	 * If the clipboard is empty its content doesn't contain any valid Maven
	 * coordinates, a blank dependency is added. The newly added elements are
	 * selected.<br>
	 * This operation is undoable.
	 */
	public void add() {
		List<MavenTargetDependency> newCurrentSelection = getDefaultClipboardDependencies();
		List<MavenTargetDependency> newTargetDependencies = new ArrayList<>();
		newTargetDependencies.addAll(deepClone(getTargetDependencies()));
		newTargetDependencies.addAll(newCurrentSelection);

		history.modelChange(newTargetDependencies, newCurrentSelection);
	}

	/**
	 * Removes the current selection from the list of target dependencies. The new
	 * selection is calculated using the following rules:
	 * <ul>
	 * <li>If one element has been removed, the element at the same position (in the
	 * new list) is selected. If this position is out-of-bounds, the last element in
	 * the list is selected instead.</li>
	 * <li>If the more than one element is removed or if the target list is empty,
	 * the selection is cleared.</li>
	 * </ul>
	 * An error is logged if {@link canRemove()} returns {@code false}.<br>
	 * This operation is undoable.
	 * 
	 * @see #canRemove()
	 */
	public void remove() {
		if (!canRemove()) {
			LOGGER.error("The remove operation is currently not possible! Ignore...");
			return;
		}

		List<MavenTargetDependency> oldCurrentSelection = getCurrentSelection();
		List<MavenTargetDependency> oldTargetDependencies = getTargetDependencies();

		List<MavenTargetDependency> newCurrentSelection = Collections.emptyList();
		List<MavenTargetDependency> newTargetDependencies = new ArrayList<>(oldTargetDependencies);
		newTargetDependencies.removeAll(oldCurrentSelection);
		newTargetDependencies = deepClone(newTargetDependencies);

		if (oldCurrentSelection.size() == 1 && !newTargetDependencies.isEmpty()) {
			// Get the position of the removed element in the OLD list of dependencies
			int index = oldTargetDependencies.indexOf(oldCurrentSelection.get(0));

			// Is the index still valid? If not, select the last element instead...
			if (index >= newTargetDependencies.size()) {
				index = newTargetDependencies.size() - 1;
			}

			MavenTargetDependency itemToSelect = newTargetDependencies.get(index);
			newCurrentSelection = Collections.singletonList(itemToSelect);
		}

		history.modelChange(newTargetDependencies, newCurrentSelection);
	}

	/**
	 * Updates the versions of all given artifacts to the latest one available on
	 * Maven Central (or any additional repository specified in the target
	 * location). If all dependencies are already up-to-date, nothing is done. An
	 * error is logged if {@link canUpdate()} returns {@code false}.<br>
	 * This operation is undoable.
	 * 
	 * @see #canUpdate()
	 */
	public void update() {
		if (!canUpdate()) {
			LOGGER.error("The update operation is currently not possible! Ignore...");
			return;
		}

		List<MavenTargetDependency> oldCurrentSelection = getCurrentSelection();
		List<MavenTargetDependency> oldTargetDependencies = getTargetDependencies();

		List<MavenTargetDependency> newCurrentSelection = new ArrayList<>();
		List<MavenTargetDependency> newTargetDependencies = new ArrayList<>(oldTargetDependencies);

		try {
			int updated = 0;

			for (MavenTargetDependency dependency : oldCurrentSelection) {
				int index = oldTargetDependencies.indexOf(dependency);

				MavenTargetDependency newDependency = targetLocation.update(dependency, null);

				if (!dependency.matches(newDependency)) {
					updated++;
				}

				newTargetDependencies.set(index, newDependency);
				newCurrentSelection.add(newDependency);
			}

			// An "empty" update should not show up on the command stack...
			if (updated > 0) {
				history.modelChange(newTargetDependencies, newCurrentSelection);
			}
		} catch (CoreException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Checks whether more than one element is selected.
	 * 
	 * @return {@code true}, if the update remove can be performed.
	 * @see #remove()
	 */
	public boolean canRemove() {
		return !currentSelection.isEmpty();
	}

	/**
	 * Checks whether all selected element contain a valid GAV.
	 * 
	 * @return {@code true}, if the update operation can be performed.
	 * @see #update()
	 */
	public boolean canUpdate() {
		if (targetLocation == null) {
			return false;
		}

		Stream<String> allFields = currentSelection.stream().mapMulti((dependency, downStream) -> {
			downStream.accept(dependency.getGroupId());
			downStream.accept(dependency.getArtifactId());
			downStream.accept(dependency.getVersion());
		});

		return allFields.noneMatch(StringUtils::isBlank) && !currentSelection.isEmpty();
	}

	public boolean hasErrors() {
		return hasErrors.getValue();
	}

	public void setErrors(boolean hasErrors) {
		this.hasErrors.setValue(hasErrors);
	}

	private static List<MavenTargetDependency> getDefaultClipboardDependencies() {
		List<MavenTargetDependency> dependencies = ClipboardParser.getClipboardDependencies(Display.getCurrent());

		if (dependencies.isEmpty()) {
			return List.of(new MavenTargetDependency("", "", "", "", ""));
		} else {
			return dependencies;
		}
	}

	/**
	 * Checks if all target dependencies contain valid Maven coordinates. Each
	 * dependency requires a non-empty group id, artifact id, version and type.
	 */
	public void check() {
		Stream<String> allFields = targetDependencies.stream().mapMulti((dependency, downStream) -> {
			downStream.accept(dependency.getGroupId());
			downStream.accept(dependency.getArtifactId());
			downStream.accept(dependency.getVersion());
			downStream.accept(dependency.getType());
		});
		hasErrors.setValue(allFields.anyMatch(StringUtils::isBlank));
	}

	private static List<MavenTargetDependency> deepClone(List<MavenTargetDependency> dependencies) {
		List<MavenTargetDependency> copy = new ArrayList<>();

		for (MavenTargetDependency dependency : dependencies) {
			copy.add(dependency.copy());
		}

		return new ArrayList<>(copy);
	}
}
