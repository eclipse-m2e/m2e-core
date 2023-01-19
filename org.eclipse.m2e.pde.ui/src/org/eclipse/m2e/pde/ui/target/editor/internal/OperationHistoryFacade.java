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

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.DefaultOperationHistory;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.UndoContext;
import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.pde.target.MavenTargetDependency;

/**
 * This class wraps the {@link DefaultOperationHistory} and makes it available
 * to the {@link IObservable} framework.<br>
 * Calling {@link #undo(IUndoContext, IProgressMonitor, IAdaptable)},
 * {@link #redo(IUndoContext, IProgressMonitor, IAdaptable)} or
 * {@link #execute(IUndoableOperation, IProgressMonitor, IAdaptable)}
 * corresponds to a {@code set} operation.<br>
 * Calling {@link #canUndo(IUndoContext)} or {@link #canRedo(IUndoContext)}
 * corresponds to a {@code get} operation.
 */
public class OperationHistoryFacade extends AbstractObservable {
	private static final ILog LOGGER = Platform.getLog(TargetDependencyModel.class);
	/**
	 * Undo context used for all user modifications on the Maven dependency table
	 * (e.g. delete, add, update...) which have to be put on the command stack.
	 */
	private static final IUndoContext USER_CONTEXT = new UndoContext();
	/**
	 * The command stack keeps track of all modifications done to the viewer.
	 * Necessary to perform undo/redo operations.
	 */
	private final IOperationHistory commandStack = new DefaultOperationHistory();

	private final TargetDependencyModel model;

	public OperationHistoryFacade(TargetDependencyModel model) {
		super(Realm.getDefault());
		this.model = model;
	}

	/**
	 * Reverts all model changes done by the most recent operation. An error is
	 * logged if {@link canUndo()} returns {@code false}.
	 * 
	 * @see #canUndo()
	 */
	public void undo() {
		if (!canUndo()) {
			LOGGER.error("The undo operation is currently not possible! Ignore...");
			return;
		}

		try {
			commandStack.undo(USER_CONTEXT, null, null);
			fireChange();
		} catch (ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Re-applies all changes done by the most recent {@code undo} operation. An
	 * error is logged if {@link doRedo()} returns {@code false}.
	 * 
	 * @see #canRedo()
	 */
	public void redo() {
		if (!canRedo()) {
			LOGGER.error("The redo operation is currently not possible! Ignore...");
			return;
		}

		try {
			commandStack.redo(USER_CONTEXT, null, null);
			fireChange();
		} catch (ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Checks whether the command stack contains a valid {@code undo} operation.
	 * 
	 * @return {@code true}, if the {@code undo} operation can be performed.
	 * @see #undo()
	 */
	public boolean canUndo() {
		getterCalled();
		return commandStack.canUndo(USER_CONTEXT);
	}

	/**
	 * Checks whether the command stack contains a valid {@code redo} operation.
	 * 
	 * @return {@code true}, if the {@code redo} operation can be performed.
	 * @see #redo()
	 */
	public boolean canRedo() {
		getterCalled();
		return commandStack.canRedo(USER_CONTEXT);
	}

	/**
	 * Performs an undoable modification on one of the properties of the data
	 * model.This operation doesn't cause an UI update.<br>
	 * This behavior is required e.g. when editing a table cell, as a model change
	 * would break the current selection and therefore close the cell editor.
	 * 
	 * @param setter   The property accessor.
	 * @param oldValue The properties old value.
	 * @param newValue The properties new value.
	 */
	public void propertyChange(Consumer<String> setter, String oldValue, String newValue) {
		IUndoableOperation command = new ModifyPropertyOperation(setter, oldValue, newValue);
		command.addContext(USER_CONTEXT);

		try {
			commandStack.execute(command, null, null);
			fireChange();
		} catch (ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/**
	 * Performs an undoable on both the target dependencies and selection of the
	 * data model. This operation causes an update of the table and buttons.
	 * 
	 * @param newTargetDependencies The models new target dependencies.
	 * @param newCurrentSelection   The models new selection.
	 */
	public void modelChange(List<MavenTargetDependency> newTargetDependencies,
			List<MavenTargetDependency> newCurrentSelection) {
		IUndoableOperation command = new ModifyModelOperation(newTargetDependencies, newCurrentSelection);
		command.addContext(USER_CONTEXT);

		try {
			commandStack.execute(command, null, null);
			fireChange();
		} catch (ExecutionException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean isStale() {
		getterCalled();
		return false;
	}

	private void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	/**
	 * This class updates both the list of all dependencies of the underlying target
	 * location, as well as the list of all currently selected elements. This change
	 * operation triggers all registered side-effects over those attributes.
	 */
	private class ModifyModelOperation extends AbstractOperation {
		private final List<MavenTargetDependency> oldTargetDependencies;
		private final List<MavenTargetDependency> newTargetDependencies;
		private final List<MavenTargetDependency> oldCurrentSelection;
		private final List<MavenTargetDependency> newCurrentSelection;

		public ModifyModelOperation(List<MavenTargetDependency> newTargetDependencies,
				List<MavenTargetDependency> newCurrentSelection) {
			super("Modify Model");
			this.oldTargetDependencies = model.getTargetDependencies();
			this.newTargetDependencies = newTargetDependencies;
			this.oldCurrentSelection = model.getCurrentSelection();
			this.newCurrentSelection = newCurrentSelection;
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			model.setTargetDependencies(newTargetDependencies);
			model.setCurrentSelection(newCurrentSelection);
			model.check();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			model.setTargetDependencies(oldTargetDependencies);
			model.setCurrentSelection(oldCurrentSelection);
			model.check();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			return execute(monitor, info);
		}
	}

	/**
	 * This class updates the property of a single element within the model. This
	 * change operation <b>does not</> trigger side-effects. This behavior is
	 * required when e.g. editing a cell in the table viewer. The data model should
	 * be updated with the new value but the input and selection should not be
	 * updated explicitly, as this is already handled by JFace.
	 */
	private class ModifyPropertyOperation extends AbstractOperation {
		private final Consumer<String> setter;
		private final String oldValue;
		private final String newValue;

		public ModifyPropertyOperation(Consumer<String> setter, String oldValue, String newValue) {
			super("Modify Model");
			this.setter = setter;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			setter.accept(newValue);
			model.check();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			setter.accept(oldValue);
			model.check();
			return Status.OK_STATUS;
		}

		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
			return execute(monitor, info);
		}
	}
}
