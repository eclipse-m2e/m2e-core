/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.composites;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.eclipse.m2e.editor.internal.Messages;


/**
 * List editor composite
 * 
 * @author Eugene Kuleshov
 */
public class ListEditorComposite<T> extends Composite {

  TableViewer viewer;

  protected Map<String, Button> buttons = new HashMap<String, Button>(5);

  /*
   * Default button keys
   */
  private static final String ADD = "ADD"; //$NON-NLS-1$

  private static final String CREATE = "CREATE"; //$NON-NLS-1$

  private static final String REMOVE = "REMOVE"; //$NON-NLS-1$

  boolean readOnly = false;

  protected FormToolkit toolkit;

  private TableViewerColumn column;

  public ListEditorComposite(Composite parent, int style, boolean includeSearch) {
    super(parent, style);
    toolkit = new FormToolkit(parent.getDisplay());

    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 1;
    gridLayout.verticalSpacing = 1;
    setLayout(gridLayout);

    final Table table = toolkit.createTable(this, SWT.FLAT | SWT.MULTI);
    table.setData("name", "list-editor-composite-table"); //$NON-NLS-1$ //$NON-NLS-2$

    viewer = new TableViewer(table);
    column = new TableViewerColumn(viewer, SWT.LEFT);
    //mkleint: TODO this is sort of suboptimal, as the horizontal scrollbar gets never shown and we hide information
    // if the viewable are is not enough. No idea what to replace it with just yet.
    table.addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        column.getColumn().setWidth(table.getClientArea().width);
      }
    });

    createButtons(includeSearch);

    int vSpan = buttons.size();
    GridData viewerData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, vSpan);
    viewerData.widthHint = 100;
    viewerData.heightHint = includeSearch ? 125 : 50;
    viewerData.minimumHeight = includeSearch ? 125 : 50;
    table.setLayoutData(viewerData);
    viewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);

    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        viewerSelectionChanged();
      }
    });

    toolkit.paintBordersFor(this);
  }

  public ListEditorComposite(Composite parent, int style) {
    this(parent, style, false);
  }

  public void setLabelProvider(ILabelProvider labelProvider) {
    viewer.setLabelProvider(labelProvider);
  }

  public void setCellLabelProvider(CellLabelProvider cell) {
    column.setLabelProvider(cell);
  }

  public void setContentProvider(ListEditorContentProvider<T> contentProvider) {
    viewer.setContentProvider(contentProvider);
  }

  public void setInput(List<T> input) {
    viewer.setInput(input);
    viewer.setSelection(new StructuredSelection());
  }

  public Object getInput() {
    return viewer.getInput();
  }

  public void setOpenListener(IOpenListener listener) {
    viewer.addOpenListener(listener);
  }

  public void addSelectionListener(ISelectionChangedListener listener) {
    viewer.addSelectionChangedListener(listener);
  }

  public void setAddButtonListener(SelectionListener listener) {
    if(getAddButton() != null) {
      getAddButton().addSelectionListener(listener);
      getAddButton().setEnabled(true);
    }
  }

  protected Button getCreateButton() {
    return buttons.get(CREATE);
  }

  protected Button getRemoveButton() {
    return buttons.get(REMOVE);
  }

  protected Button getAddButton() {
    return buttons.get(ADD);
  }

  public void setCreateButtonListener(SelectionListener listener) {
    getCreateButton().addSelectionListener(listener);
    getCreateButton().setEnabled(true);
  }

  public void setRemoveButtonListener(SelectionListener listener) {
    getRemoveButton().addSelectionListener(listener);
  }

  public TableViewer getViewer() {
    return viewer;
  }

  public int getSelectionIndex() {
    return viewer.getTable().getSelectionIndex();
  }

  public void setSelectionIndex(int n) {
    viewer.getTable().setSelection(n);
  }

  @SuppressWarnings("unchecked")
  public List<T> getSelection() {
    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
    return selection == null ? Collections.emptyList() : selection.toList();
  }

  public void setSelection(List<T> selection) {
    viewer.setSelection(new StructuredSelection(selection), true);
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
    for(Map.Entry<String, Button> entry : buttons.entrySet()) {
      if(entry.getKey().equals(REMOVE)) {
        //Special case, as it makes no sense to enable if it there's nothing selected.
        updateRemoveButton();
      } else {
        //TODO: mkleint this is fairly dangerous thing to do, each button shall be handled individually based on context.
        entry.getValue().setEnabled(!readOnly);
      }
    }
  }

  protected void viewerSelectionChanged() {
    updateRemoveButton();
  }

  protected void updateRemoveButton() {
    getRemoveButton().setEnabled(!readOnly && !viewer.getSelection().isEmpty());
  }

  public void refresh() {
    if(!viewer.getTable().isDisposed()) {
      viewer.refresh(true);
      column.getColumn().setWidth(viewer.getTable().getClientArea().width);
    }
  }

  public void setCellModifier(ICellModifier cellModifier) {

    // trigger editing only on second click to prevent losing viewer selection on defocus
    ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(viewer) {
      Object prevSelection;

      protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {

        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if(selection.size() != 1)
          return false;

        Object selElement = selection.getFirstElement();
        if(prevSelection != (prevSelection = selElement))
          return false;

        return super.isEditorActivationEvent(event);
      }
    };

    TableViewerEditor.create(viewer, activationSupport, ColumnViewerEditor.DEFAULT);

    viewer.setColumnProperties(new String[] {"?"}); //$NON-NLS-1$

    TextCellEditor editor = new TextCellEditor(viewer.getTable());
    viewer.setCellEditors(new CellEditor[] {editor});
    viewer.setCellModifier(cellModifier);
  }

  public void setDoubleClickListener(IDoubleClickListener listener) {
    viewer.addDoubleClickListener(listener);
  }

  /**
   * Create the buttons that populate the column to the right of the ListViewer. Subclasses must call the helper method
   * addButton to add each button to the composite.
   * 
   * @param includeSearch true if the search button should be created
   */
  protected void createButtons(boolean includeSearch) {
    if(includeSearch) {
      createAddButton();
    }
    createCreateButton();
    createRemoveButton();
  }

  protected void addButton(String key, Button button) {
    buttons.put(key, button);
  }

  protected void createAddButton() {
    addButton(ADD, createButton(Messages.ListEditorComposite_btnAdd));
  }

  protected void createCreateButton() {
    addButton(CREATE, createButton(Messages.ListEditorComposite_btnCreate));
  }

  protected void createRemoveButton() {
    addButton(REMOVE, createButton(Messages.ListEditorComposite_btnRemove));
  }

  protected Button createButton(String text) {
    Button button = toolkit.createButton(this, text, SWT.FLAT);
    GridData gd = new GridData(SWT.FILL, SWT.TOP, false, false);
    gd.verticalIndent = 0;
    button.setLayoutData(gd);
    button.setEnabled(false);
    return button;
  }
}
