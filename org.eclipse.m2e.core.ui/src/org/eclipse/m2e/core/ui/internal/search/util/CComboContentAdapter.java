/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.search.util;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;


/**
 * An {@link IControlContentAdapter} for SWT CCombo controls. This is a convenience class for easily creating a
 * {@link ContentProposalAdapter} for combo fields.
 */
public class CComboContentAdapter implements IControlContentAdapter /*, IControlContentAdapter2 */{

  /*
   * Set to <code>true</code> if we should compute the text
   * vertical bounds rather than just use the field size.
   * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=164748
   * The corresponding SWT bug is
   * https://bugs.eclipse.org/bugs/show_bug.cgi?id=44072
   */
  private static final boolean COMPUTE_TEXT_USING_CLIENTAREA = !"carbon".equals(SWT.getPlatform()); //$NON-NLS-1$

  @Override
  public String getControlContents(Control control) {
    return ((CCombo) control).getText();
  }

  @Override
  public void setControlContents(Control control, String text, int cursorPosition) {
    ((CCombo) control).setText(text);
    ((CCombo) control).setSelection(new Point(cursorPosition, cursorPosition));
  }

  @Override
  public void insertControlContents(Control control, String text, int cursorPosition) {
    CCombo combo = (CCombo) control;
    String contents = combo.getText();
    Point selection = combo.getSelection();
    StringBuilder sb = new StringBuilder();
    sb.append(contents.substring(0, selection.x));
    sb.append(text);
    if(selection.y < contents.length()) {
      sb.append(contents.substring(selection.y, contents.length()));
    }
    combo.setText(sb.toString());
    selection.x = selection.x + cursorPosition;
    selection.y = selection.x;
    combo.setSelection(selection);
  }

  @Override
  public int getCursorPosition(Control control) {
    return ((CCombo) control).getSelection().x;
  }

  @Override
  public Rectangle getInsertionBounds(Control control) {
    // This doesn't take horizontal scrolling into affect.
    // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=204599
    CCombo combo = (CCombo) control;
    int position = combo.getSelection().y;
    String contents = combo.getText();
    GC gc = new GC(combo);
    gc.setFont(combo.getFont());
    Point extent = gc.textExtent(contents.substring(0, Math.min(position, contents.length())));
    gc.dispose();
    if(COMPUTE_TEXT_USING_CLIENTAREA) {
      return new Rectangle(combo.getClientArea().x + extent.x, combo.getClientArea().y, 1, combo.getClientArea().height);
    }
    return new Rectangle(extent.x, 0, 1, combo.getSize().y);
  }

  @Override
  public void setCursorPosition(Control control, int index) {
    ((CCombo) control).setSelection(new Point(index, index));
  }

  public Point getSelection(Control control) {
    return ((CCombo) control).getSelection();
  }

  public void setSelection(Control control, Point range) {
    ((CCombo) control).setSelection(range);
  }

}
