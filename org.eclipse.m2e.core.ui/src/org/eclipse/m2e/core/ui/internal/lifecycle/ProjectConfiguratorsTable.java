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

package org.eclipse.m2e.core.ui.internal.lifecycle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * ProjectConfiguratorsTable
 * Composite that holds a read only table of project configurators for a given lifecycle mapping strategy.
 *
 * @author dyocum
 */
public class ProjectConfiguratorsTable {
  
  private TableViewer configuratorsTable;
  private ProjectConfiguratorsTableContentProvider configuratorsContentProvider;
  private ProjectConfiguratorsTableLabelProvider configuratorsLabelProvider;
  public static final String[] CONFIG_TABLE_COLUMN_PROPERTIES = new String[]{ "name", "id"}; //$NON-NLS-1$ //$NON-NLS-2$
  public static final String[] CONFIG_TABLE_COLUMN_NAMES = new String[]{ Messages.ProjectConfiguratorsTable_column_name, Messages.ProjectConfiguratorsTable_column_id};
  private static final int TABLE_WIDTH = 500;
  
  public ProjectConfiguratorsTable(Composite parent, IProject project){
    createTable(parent);
    updateTable(project);
  }
  
  protected void updateTable(IProject project){
    if(project != null){
      try{
        ILifecycleMapping lifecycleMapping = LifecycleMappingPropertyPageFactory.getLifecycleMapping(project);
        if(lifecycleMapping != null) {
//          List<AbstractProjectConfigurator> projectConfigurators = lifecycleMapping
//              .getProjectConfigurators(new NullProgressMonitor());
//          setProjectConfigurators(projectConfigurators.toArray(new AbstractProjectConfigurator[] {}));
        } else {
          setProjectConfigurators(new AbstractProjectConfigurator[] {});
        }
      } catch(CoreException e){
        setProjectConfigurators(new AbstractProjectConfigurator[]{});
      }
    }
  }
  private void createTable(Composite parent){
    configuratorsTable = new TableViewer(parent, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
    TableViewerColumn nameColumn = new TableViewerColumn(configuratorsTable, SWT.LEFT);
    nameColumn.getColumn().setText(CONFIG_TABLE_COLUMN_NAMES[0]);
    nameColumn.getColumn().setWidth((int)(TABLE_WIDTH*.50));
    
    TableViewerColumn idColumn = new TableViewerColumn(configuratorsTable, SWT.LEFT);
    idColumn.getColumn().setText(CONFIG_TABLE_COLUMN_NAMES[1]);
    idColumn.getColumn().setWidth((int)(TABLE_WIDTH*.50));
    
    configuratorsTable.getTable().setHeaderVisible(true);
    configuratorsTable.getTable().setLinesVisible(true);
    configuratorsContentProvider = new ProjectConfiguratorsTableContentProvider();
    configuratorsLabelProvider = new ProjectConfiguratorsTableLabelProvider();
    configuratorsTable.setContentProvider(configuratorsContentProvider);
    configuratorsTable.setLabelProvider(configuratorsLabelProvider);
    configuratorsTable.setColumnProperties(CONFIG_TABLE_COLUMN_PROPERTIES);
    configuratorsTable.getTable().setData("name", "projectConfiguratorsTable"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    gd.horizontalIndent=6;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    configuratorsTable.getControl().setLayoutData(gd);

    final TableColumn nCol = nameColumn.getColumn();
    final TableColumn iCol = idColumn.getColumn();
    final Table tab = configuratorsTable.getTable();
    configuratorsTable.getTable().addControlListener(new ControlAdapter() {
      public void controlResized(ControlEvent e) {
        nCol.setWidth((int)(tab.getClientArea().width*0.50));
        iCol.setWidth((int)(tab.getClientArea().width*0.50));
      }
    });
  }
  
  public TableViewer getTableViewer(){
    return configuratorsTable;
  }
  
  public void setProjectConfigurators(AbstractProjectConfigurator[] configurators){
    configuratorsTable.setInput(configurators);
  }
  
}
