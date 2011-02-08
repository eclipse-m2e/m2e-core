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

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.archetype.ArchetypeManager;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;


/**
 * Maven Archetype catalogs preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenArchetypesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  ArchetypeManager archetypeManager;
  TableViewer archetypesViewer;

  List<ArchetypeCatalogFactory> archetypeCatalogs;
  
  public MavenArchetypesPreferencePage() {
    setTitle(Messages.MavenArchetypesPreferencePage_title);

    this.archetypeManager = MavenPlugin.getDefault().getArchetypeManager();
  }
  
  protected void performDefaults() {
    for(Iterator<ArchetypeCatalogFactory> it = archetypeCatalogs.iterator(); it.hasNext();) {
      ArchetypeCatalogFactory factory = it.next();
      if(factory.isEditable()) {
        it.remove();
      }
    }
    
    archetypesViewer.setInput(archetypeCatalogs);
    archetypesViewer.setSelection(null, true);
    
    super.performDefaults();
  }

  public boolean performOk() {
    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    for(ArchetypeCatalogFactory factory : catalogs) {
      if(factory.isEditable()) {
        archetypeManager.removeArchetypeCatalogFactory(factory.getId());
      }
    }
    for(ArchetypeCatalogFactory factory : archetypeCatalogs) {
      if(factory.isEditable()) {
        archetypeManager.addArchetypeCatalogFactory(factory);
      }
    }
    
    try {
      archetypeManager.saveCatalogs();
    } catch(IOException ex) {
      setErrorMessage(NLS.bind(Messages.MavenArchetypesPreferencePage_error, ex.getMessage()));
      return false;
    }
    
    return super.performOk();
  }
  
  public void init(IWorkbench workbench) {
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    Link addRemoveOrLink = new Link(composite, SWT.NONE);
    GridData gd_addRemoveOrLink = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
    addRemoveOrLink.setLayoutData(gd_addRemoveOrLink);
    addRemoveOrLink.setText(Messages.MavenArchetypesPreferencePage_link);
    addRemoveOrLink.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        try {
          URL url = new URL("http://maven.apache.org/plugins/maven-archetype-plugin/specification/archetype-catalog.html"); //$NON-NLS-1$
          IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
          browser.openURL(url);
        } catch(MalformedURLException ex) {
          MavenLogger.log("Malformed URL", ex);
        } catch(PartInitException ex) {
          MavenLogger.log(ex);
        }
      }      
    });

    // archetypesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
    archetypesViewer = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);

    archetypesViewer.setLabelProvider(new CatalogsLabelProvider());

    archetypesViewer.setContentProvider(new IStructuredContentProvider() {

      public Object[] getElements(Object input) {
        if(input instanceof Collection) {
          return ((Collection<?>) input).toArray();
        }
        return new Object[0];
      }
      
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }
      
    });

    Table table = archetypesViewer.getTable();
    table.setLinesVisible(false);
    table.setHeaderVisible(false);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 4));

    TableColumn typeColumn = new TableColumn(table, SWT.NONE);
    typeColumn.setWidth(250);
    typeColumn.setText(""); //$NON-NLS-1$

    Button addLocalButton = new Button(composite, SWT.NONE);
    addLocalButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addLocalButton.setText(Messages.MavenArchetypesPreferencePage_btnAddLocal);
    addLocalButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        LocalArchetypeCatalogDialog dialog = new LocalArchetypeCatalogDialog(getShell(), null);
        if (dialog.open()==Window.OK) {
          ArchetypeCatalogFactory factory = dialog.getArchetypeCatalogFactory();
          archetypeCatalogs.add(factory);
          archetypesViewer.setInput(archetypeCatalogs);
          archetypesViewer.setSelection(new StructuredSelection(factory), true);
        }
      }
    });

    Button addRemoteButton = new Button(composite, SWT.NONE);
    addRemoteButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addRemoteButton.setText(Messages.MavenArchetypesPreferencePage_btnAddRemote);
    addRemoteButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        RemoteArchetypeCatalogDialog dialog = new RemoteArchetypeCatalogDialog(getShell(), null);
        if (dialog.open()==Window.OK) {
          ArchetypeCatalogFactory factory = dialog.getArchetypeCatalogFactory();
          archetypeCatalogs.add(factory);
          archetypesViewer.setInput(archetypeCatalogs);
          archetypesViewer.setSelection(new StructuredSelection(factory), true);
        }
      }
    });
    
    final Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setEnabled(false);
    editButton.setText(Messages.MavenArchetypesPreferencePage_btnEdit);
    editButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
        ArchetypeCatalogFactory newFactory = null;
        if(factory instanceof LocalCatalogFactory) {
          LocalArchetypeCatalogDialog dialog = new LocalArchetypeCatalogDialog(getShell(), factory);
          if (dialog.open()==Window.OK) {
            newFactory = dialog.getArchetypeCatalogFactory();
          }
        } else if(factory instanceof RemoteCatalogFactory) {
          RemoteArchetypeCatalogDialog dialog = new RemoteArchetypeCatalogDialog(getShell(), factory);
          if (dialog.open()==Window.OK) {
            newFactory = dialog.getArchetypeCatalogFactory();
          }
        }
        if(newFactory!=null) {
          int n = archetypeCatalogs.indexOf(factory);
          if(n>-1) {
            archetypeCatalogs.set(n, newFactory);
            archetypesViewer.setInput(archetypeCatalogs);
            archetypesViewer.setSelection(new StructuredSelection(newFactory), true);
          }
        }
      }
    });

    final Button removeButton = new Button(composite, SWT.NONE);
    removeButton.setEnabled(false);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeButton.setText(Messages.MavenArchetypesPreferencePage_btnRemove);
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
        archetypeCatalogs.remove(factory);
        archetypesViewer.setInput(archetypeCatalogs);
        archetypesViewer.setSelection(null, true);
      }
    });
    
    archetypesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if(archetypesViewer.getSelection() instanceof IStructuredSelection) {
          ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
          boolean isEnabled = factory != null && factory.isEditable();
          removeButton.setEnabled(isEnabled);
          editButton.setEnabled(isEnabled);
        }
      }
    });
    
    archetypeCatalogs = new ArrayList<ArchetypeCatalogFactory>(archetypeManager.getArchetypeCatalogs());
    archetypesViewer.setInput(archetypeCatalogs);
    archetypesViewer.refresh();  // should listen on property changes instead?
    
    return composite;
  }

  protected ArchetypeCatalogFactory getSelectedArchetypeCatalogFactory() {
    IStructuredSelection selection = (IStructuredSelection) archetypesViewer.getSelection();
    return (ArchetypeCatalogFactory) selection.getFirstElement();
  }


  static class CatalogsLabelProvider implements ITableLabelProvider, IColorProvider {
    
    private Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

    public String getColumnText(Object element, int columnIndex) {
      ArchetypeCatalogFactory factory = (ArchetypeCatalogFactory) element;
      if(factory instanceof LocalCatalogFactory) {
        return NLS.bind(Messages.MavenArchetypesPreferencePage_local, factory.getDescription());
      } else if(factory instanceof RemoteCatalogFactory) {
        if(factory.isEditable()) {
          return NLS.bind(Messages.MavenArchetypesPreferencePage_remote,factory.getDescription());
        }
        return NLS.bind(Messages.MavenArchetypesPreferencePage_packaged, factory.getDescription());
      }
      return factory.getDescription();
    }
    
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Color getForeground(Object element) {
      ArchetypeCatalogFactory factory = (ArchetypeCatalogFactory) element;
      return !factory.isEditable() ? disabledColor : null;
    }
    
    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void addListener(ILabelProviderListener listener) {
    }
    
    public void removeListener(ILabelProviderListener listener) {
    }

  }
  
}
