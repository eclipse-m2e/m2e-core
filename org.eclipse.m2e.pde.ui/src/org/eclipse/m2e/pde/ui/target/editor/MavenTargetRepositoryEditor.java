/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others
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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.m2e.pde.target.MavenTargetRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class MavenTargetRepositoryEditor extends Dialog {

	private List<MavenTargetRepository> repositoryList;
	private List<MavenTargetRepository> editList;
	private TableViewer tableViewer;

	public MavenTargetRepositoryEditor(Shell shell, List<MavenTargetRepository> repositoryList) {
		super(shell);
		this.repositoryList = repositoryList;
		this.editList = repositoryList.stream().map(MavenTargetRepository::copy)
				.collect(Collectors.toCollection(ArrayList::new));

	}

	@Override
	public int open() {
		int open = super.open();
		if (open == OK) {
			this.repositoryList.clear();
			this.repositoryList.addAll(editList);
		}
		return open;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.CLIENT_ID + 1, Messages.MavenTargetRepositoryEditor_1,
				false);
		int[] counter = new int[] { 1 };
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			editList.add(new MavenTargetRepository("Id" + (counter[0]++), "https://"));
			tableViewer.refresh();
		}));
		super.createButtonsForButtonBar(parent);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		tableViewer = new TableViewer(container,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(manager -> {
			Object selected = tableViewer.getStructuredSelection().getFirstElement();
			if (selected instanceof MavenTargetRepository repository) {
				manager.add(new Action("Delete") {
					@Override
					public void run() {
						editList.remove(repository);
						tableViewer.refresh();
					}
				});
			}
		});

		tableViewer.getControl().setMenu(contextMenu.createContextMenu(tableViewer.getControl()));
		{
			TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
			column.getColumn().setText("Id");
			column.getColumn().setWidth(150);
			column.setLabelProvider(
					ColumnLabelProvider.createTextProvider(element -> ((MavenTargetRepository) element).getId()));
			column.setEditingSupport(
					new TextEditingSupport(tableViewer, MavenTargetRepository::getId, MavenTargetRepository::setId));
		}
		{
			TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
			column.getColumn().setText("URL");
			column.getColumn().setWidth(300);
			column.setLabelProvider(
					ColumnLabelProvider.createTextProvider(element -> ((MavenTargetRepository) element).getUrl()
			));
			column.setEditingSupport(
					new TextEditingSupport(tableViewer, MavenTargetRepository::getUrl, MavenTargetRepository::setUrl));
		}
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(editList);
		return container;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Edit repositories");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 300);
	}

	private static final class TextEditingSupport extends EditingSupport {

		private Function<MavenTargetRepository, String> getter;
		private BiConsumer<MavenTargetRepository, String> setter;

		public TextEditingSupport(ColumnViewer viewer, Function<MavenTargetRepository, String> getter,
				BiConsumer<MavenTargetRepository, String> setter) {
			super(viewer);
			this.getter = getter;
			this.setter = setter;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor((Composite) getViewer().getControl());
		}

		@Override
		protected boolean canEdit(Object element) {
			return element instanceof MavenTargetRepository;
		}

		@Override
		protected Object getValue(Object element) {
			return getter.apply((MavenTargetRepository) element);
		}

		@Override
		protected void setValue(Object element, Object value) {
			setter.accept((MavenTargetRepository) element, (String) value);
			getViewer().update(element, null);
		}

	}

}
