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

package org.eclipse.m2e.core.ui.internal.components;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 * A TextComboBoxCellEditor to overcome the limitation of the standard ComboBoxCellEditor, which does not allow to edit
 * plain text values.
 * 
 * @author Dmitry Platonoff
 */
public class TextComboBoxCellEditor extends CellEditor {

  protected String[] items;

  
  protected CCombo combo;

  public TextComboBoxCellEditor(Composite parent, int style) {
    super(parent, style);
  }

  protected Control createControl(Composite parent) {
    combo = new CCombo(parent, getStyle());
    combo.setFont(parent.getFont());

    combo.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        keyReleaseOccured(e);
      }
    });
    combo.addTraverseListener(new TraverseListener() {
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_ESCAPE
            || e.detail == SWT.TRAVERSE_RETURN) {
          e.doit = false;
        }
      }
    });
    
    loadItems();

    return combo;
  }

  protected Object doGetValue() {
    Assert.isNotNull(combo);
    return combo.getText();
  }

  protected void doSetFocus() {
    Assert.isNotNull(combo);
    combo.setFocus();
  }

  protected void doSetValue(Object value) {
    Assert.isNotNull(combo);
    combo.setText(String.valueOf(value));
  }

  public String[] getItems() {
    return items;
  }

  public void setItems(String[] items) {
    this.items = items;
    loadItems();
  }

  protected void loadItems() {
    if(combo != null && items != null) {
      combo.setItems(items);
    }
  }

  protected void keyReleaseOccured(KeyEvent keyEvent) {
    if(keyEvent.character == SWT.ESC) {
      fireCancelEditor();
    } else if(keyEvent.character == SWT.TAB || keyEvent.character == SWT.CR) {
      focusLost();
    }
  }
}
