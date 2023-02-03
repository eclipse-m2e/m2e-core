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

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffectFactory;
import org.eclipse.jface.databinding.swt.WidgetSideEffects;
import org.eclipse.jface.databinding.viewers.IViewerObservableList;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.m2e.core.ui.internal.components.TextComboBoxCellEditor;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.ui.target.editor.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * This editor part displays all Maven dependencies of the selected target
 * locations. The elements are stored in a simple table.
 */
@SuppressWarnings("restriction")
public class DependencyTable {
	private static final String[] DEFAULT_TYPES = new String[] { "jar", "bundle", "pom" };
	private final TargetDependencyModel model;
	private TableViewer viewer;

	public DependencyTable(Composite parent, TargetDependencyModel model) {
		this.model = model;
		createContent(parent);
	}

	private void createContent(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.swtDefaults().margins(5, 0).create());
		composite.setLayoutData(new BorderData(SWT.RIGHT));

		createButtons(composite);

		TableColumnLayout layout = new TableColumnLayout();
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(layout);
		composite.setLayoutData(new BorderData(SWT.CENTER, SWT.DEFAULT, 0));

		createTable(composite);
		createTableColumns(layout);
		fillTable(composite);
	}

	private void createTable(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().addKeyListener(KeyListener.keyPressedAdapter(event -> {
			if (event.keyCode == SWT.DEL && !viewer.getSelection().isEmpty()) {
				model.remove();
			}
		}));
	}

	private void createTableColumns(TableColumnLayout layout) {
		Function<MavenTargetDependency, String> getter = MavenTargetDependency::getGroupId;
		BiConsumer<MavenTargetDependency, String> setter = MavenTargetDependency::setGroupId;
		TableViewerColumn tableColumn = newColumn(layout, Messages.MavenTargetDependencyEditor_1, getter, true);
		tableColumn.setEditingSupport(newEditingSupport(getter, setter, TextCellEditor::new));

		getter = MavenTargetDependency::getArtifactId;
		setter = MavenTargetDependency::setArtifactId;
		tableColumn = newColumn(layout, Messages.MavenTargetDependencyEditor_2, getter, true);
		tableColumn.setEditingSupport(newEditingSupport(getter, setter, TextCellEditor::new));

		getter = MavenTargetDependency::getVersion;
		setter = MavenTargetDependency::setVersion;
		tableColumn = newColumn(layout, Messages.MavenTargetDependencyEditor_3, getter, true);
		tableColumn.setEditingSupport(newEditingSupport(getter, setter, TextCellEditor::new));

		getter = MavenTargetDependency::getClassifier;
		setter = MavenTargetDependency::setClassifier;
		tableColumn = newColumn(layout, Messages.MavenTargetDependencyEditor_4, getter, false);
		tableColumn.setEditingSupport(newEditingSupport(getter, setter, TextCellEditor::new));

		getter = MavenTargetDependency::getType;
		tableColumn = newColumn(layout, Messages.MavenTargetDependencyEditor_5, getter, true);
		tableColumn.setEditingSupport(newTypeEditingSupport());
	}

	private void createButtons(Composite parent) {
		ISideEffectFactory factory = WidgetSideEffects.createFactory(parent);

		newButton(parent, Messages.MavenTargetDependencyEditor_7, Messages.MavenTargetDependencyEditor_12, model::add);

		Button removeButton = newButton(parent, Messages.MavenTargetDependencyEditor_8,
				Messages.MavenTargetDependencyEditor_13, model::remove);
		factory.create(() -> removeButton.setEnabled(model.canRemove()));

		Button updateButton = newButton(parent, Messages.MavenTargetDependencyEditor_9,
				Messages.MavenTargetDependencyEditor_14, model::update);
		factory.create(() -> updateButton.setEnabled(model.canUpdate()));

		Button undoButton = newButton(parent, Messages.MavenTargetDependencyEditor_10, null, model.getHistory()::undo);
		factory.create(() -> undoButton.setEnabled(model.getHistory().canUndo()));

		Button redoButton = newButton(parent, Messages.MavenTargetDependencyEditor_11, null, model.getHistory()::redo);
		factory.create(() -> redoButton.setEnabled(model.getHistory().canRedo()));
	}

	private void fillTable(Composite parent) {
		ISideEffectFactory factory = WidgetSideEffects.createFactory(parent);

		// Input and selection have to be set first!
		factory.create(() -> {
			List<MavenTargetDependency> input = model.getTargetDependencies();
			viewer.setInput(input);

			resizeColumns();

			ViewerComparator comparator = viewer.getComparator();
			if (comparator != null) {
				comparator.sort(viewer, input.toArray());
			}
		});

		factory.create(() -> {
			IStructuredSelection newSelection = new StructuredSelection(model.getCurrentSelection());
			// Some fun stuff happens when we update the selection with itself...
			if (!newSelection.equals(viewer.getSelection())) {
				viewer.setSelection(newSelection, shouldReveal());
			}
		});

		IViewerObservableList<MavenTargetDependency> multipleSelection = ViewerProperties
				.multipleSelection(MavenTargetDependency.class).observe(viewer);

		factory.create(() -> model.setCurrentSelection(multipleSelection));
	}

	/**
	 * Recalculates the preferred size of each table column to ensure that none of
	 * the entries exceed the column width. Called after adding/removing elements
	 * from the table or after editing a table cell..
	 */
	private void resizeColumns() {
		for (TableColumn tableColumn : viewer.getTable().getColumns()) {
			tableColumn.pack();
		}
	}

	private Button newButton(Composite parent, String text, String tooltip, Runnable onSelection) {
		Button button = new Button(parent, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		button.setText(text);
		button.setToolTipText(tooltip);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> onSelection.run()));
		return button;
	}

	private TableViewerColumn newColumn(TableColumnLayout layout, String text,
			Function<MavenTargetDependency, String> getter, boolean required) {
		TableViewerColumn tableColumn = new TableViewerColumn(viewer, SWT.NONE);
		tableColumn.getColumn().addSelectionListener(newSortAdapter(getter));
		tableColumn.getColumn().setText(text);
		tableColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String text = getter.apply((MavenTargetDependency) element);
				return isValid(element) ? text : Messages.MavenTargetDependencyEditor_16;
			}

			@Override
			public Color getForeground(Object element) {
				Color errorColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
				return isValid(element) ? null : errorColor;
			}

			private boolean isValid(Object element) {
				return !required || !getter.apply((MavenTargetDependency) element).isBlank();
			}
		});
		layout.setColumnData(tableColumn.getColumn(), new ColumnWeightData(0, false));
		return tableColumn;
	}

	private EditingSupport newTypeEditingSupport() {
		Function<Composite, CellEditor> editorFactory = composite -> {
			TextComboBoxCellEditor cellEditor = new TextComboBoxCellEditor(composite, SWT.NONE);
			cellEditor.setItems(DEFAULT_TYPES);
			return cellEditor;
		};

		return newEditingSupport(MavenTargetDependency::getType, MavenTargetDependency::setType, editorFactory);
	}

	private EditingSupport newEditingSupport(Function<MavenTargetDependency, String> getter,
			BiConsumer<MavenTargetDependency, String> setter, Function<Composite, CellEditor> editorFactory) {
		return new EditingSupport(viewer) {
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editorFactory.apply((Composite) getViewer().getControl());
			}

			@Override
			protected boolean canEdit(Object element) {
				return element instanceof MavenTargetDependency;
			}

			@Override
			protected String getValue(Object element) {
				return getter.apply((MavenTargetDependency) element);
			}

			@Override
			protected void setValue(Object element, Object value) {
				MavenTargetDependency dependency = (MavenTargetDependency) element;
				String oldValue = getter.apply(dependency);
				String newValue = (String) value;

				// The table column needs to be redrawn. Otherwise we still see the old value...
				BiConsumer<MavenTargetDependency, String> realSetter = (k, v) -> {
					setter.accept(k, v);
					getViewer().refresh();
					resizeColumns();
				};

				model.modify(dependency, realSetter, oldValue, newValue);
			}
		};
	}

	/**
	 * <p>
	 * Creates a new column-specific comparator, wrapped around a selection adapter.
	 * Whenever the column header is clicked, the table is sorted lexicographically
	 * by this column.
	 * </p>
	 * <p>
	 * If the viewer is sorted by a different column or if this column is clicked
	 * for the first time, the elements are sorted in ascending order. If the column
	 * is clicked again, elements are sorted in descending order.
	 * </p>
	 * 
	 * @param getter The accessor for the property shown in the current column.
	 * @return A selection adapter for the column header.
	 */
	private SelectionListener newSortAdapter(Function<MavenTargetDependency, String> getter) {
		return SelectionListener.widgetSelectedAdapter(e -> {
			Table table = viewer.getTable();
			Comparator<MavenTargetDependency> comparator;
			Comparator<MavenTargetDependency> remainingColumns = Comparator.comparing(MavenTargetDependency::getKey);

			// When we don't currently sort by this column, default to ascending order
			if (table.getSortColumn() != e.widget || table.getSortDirection() == SWT.UP) {
				table.setSortDirection(SWT.DOWN);
				comparator = Comparator.comparing(getter).thenComparing(remainingColumns);
			} else {
				table.setSortDirection(SWT.UP);
				comparator = Comparator.comparing(getter).thenComparing(remainingColumns).reversed();
			}

			table.setSortColumn((TableColumn) e.widget);
			viewer.setComparator(new ViewerComparator() {
				@Override
				public int compare(Viewer viewer, Object e1, Object e2) {
					return comparator.compare((MavenTargetDependency) e1, (MavenTargetDependency) e2);
				}
			});
			viewer.refresh();
		});
	}

	/**
	 * Checks whether all selected rows are within the rendered client area.
	 * 
	 * @return {@code true}, if all rows are visible.
	 */
	private boolean shouldReveal() {
		for (MavenTargetDependency dependency : model.getCurrentSelection()) {
			int index = model.getTargetDependencies().indexOf(dependency);
			int rowHeight = viewer.getTable().getItemHeight();
			int rowOffset = index * rowHeight;
			Rectangle clientArea = viewer.getTable().getClientArea();

			// Check whether the row is below or above the visible (client) area
			if ((rowOffset < clientArea.y) || (rowOffset + rowHeight) > (clientArea.y + clientArea.height)) {
				return true;
			}
		}

		return false;
	}
}
