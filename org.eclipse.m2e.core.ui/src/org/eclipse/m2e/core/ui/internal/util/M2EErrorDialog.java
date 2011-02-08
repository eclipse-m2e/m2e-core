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

package org.eclipse.m2e.core.ui.internal.util;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

/**
 * M2EErrorDialog
 * Error dialog for displaying a list/table of error values. 
 *
 * @author dyocum
 */
public class M2EErrorDialog extends MessageDialog {

  
  private TableViewer errorTable;
  private static final int PROJECT_COL = 0;
  protected static final int TABLE_WIDTH = 700;
  protected String[] COL_NAMES = {Messages.M2EErrorDialog_column_name, Messages.M2EErrorDialog_column_error};
  protected int[] COL_STYLES = {SWT.LEFT, SWT.LEFT};
  protected Map<String, Throwable> errorMap;
  
  /**
   * @param parentShell
   * @param dialogTitle
   * @param dialogTitleImage
   * @param dialogMessage
   * @param dialogImageType
   * @param dialogButtonLabels
   * @param defaultIndex
   */
  public M2EErrorDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
      int dialogImageType, String[] dialogButtonLabels, int defaultIndex, Map<String, Throwable> errorMap) {
    super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
    this.errorMap = errorMap;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(1, true);
    comp.setLayout(layout);
    
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.widthHint = TABLE_WIDTH+50;
    gd.grabExcessHorizontalSpace=true;
    gd.grabExcessVerticalSpace=true;
    comp.setLayoutData(gd);
    
    gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.widthHint = TABLE_WIDTH;
    gd.heightHint = 200;
    errorTable = new TableViewer(comp, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.FULL_SELECTION);
    errorTable.getTable().setHeaderVisible(true);
    errorTable.getTable().setLinesVisible(true);
    
    errorTable.setContentProvider(new ErrorTableContentProvider());
    errorTable.setLabelProvider(new ErrorTableLabelProvider());
    errorTable.getControl().setLayoutData(gd);

    setupTableColumns();
    errorTable.setInput(errorMap);
    return comp;
  }

  /**
   * Create the table columns and set up their widths
   */
  protected void setupTableColumns() {
    GC gc = new GC(errorTable.getControl());
    gc.setFont(errorTable.getControl().getFont());
    for(int i=0;i<COL_NAMES.length;i++){
      TableColumn col = new TableColumn(errorTable.getTable(), COL_STYLES[i]);
      col.setText(COL_NAMES[i]);
      int width = calcStringWidth(gc, i);
      col.setWidth(width);
    }
    gc.dispose();
  }
  
  /**
   * Find out how wide the strings are so the columns can be set correctly. 
   * @param gc
   * @param column
   * @return
   */
  private int calcStringWidth(GC gc, int column){
    int maxWidth = 100;
    if(column == PROJECT_COL){
      Set<String> keySet = errorMap.keySet();
      for(String projectName : keySet){
        int width = gc.stringExtent(projectName).x+10;
        maxWidth = Math.max(maxWidth, width);
      }
      return maxWidth;
    }
    Collection<Throwable> values = errorMap.values();
    for(Throwable t : values){
      String msg = M2EUtils.getRootCauseMessage(t);
      if(msg == null){
        msg = ""; //$NON-NLS-1$
      }
      int width = gc.stringExtent(msg).x+10;
      maxWidth = Math.max(maxWidth, width);
    }
    return maxWidth;
  }
  
  /**
   * ErrorTableContentProvider
   *
   * @author dyocum
   */
  class ErrorTableContentProvider implements IStructuredContentProvider {

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
      if(inputElement instanceof Map){
        return ((Map)inputElement).keySet().toArray();
      }
      return new Object[0];
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  class ErrorTableLabelProvider implements ITableLabelProvider{

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    public String getColumnText(Object element, int columnIndex) {
      if(columnIndex == PROJECT_COL){
        return element.toString();
      }
      String msg = M2EUtils.getRootCauseMessage(errorMap.get(element));
      return msg == null ? "" : msg; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void addListener(ILabelProviderListener listener) {
      
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
     */
    public void dispose() {

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
     */
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    public void removeListener(ILabelProviderListener listener) {
    }
  }


}
