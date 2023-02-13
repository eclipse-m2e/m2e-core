/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc.
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

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
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

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypePlugin;


/**
 * Maven Archetype catalogs preference page
 *
 * @author Eugene Kuleshov
 */
public class MavenArchetypesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private static final Logger log = LoggerFactory.getLogger(MavenArchetypesPreferencePage.class);

  ArchetypePlugin archetypeManager;

  CheckboxTableViewer archetypesViewer;

  List<ArchetypeCatalogFactory> archetypeCatalogs;

  private Button snapshotsBtn;

  public MavenArchetypesPreferencePage() {
    setTitle(Messages.MavenArchetypesPreferencePage_title);
    setPreferenceStore(M2EUIPluginActivator.getDefault().getPreferenceStore());
    this.archetypeManager = M2EUIPluginActivator.getDefault().getArchetypePlugin();
  }

  @Override
  protected void performDefaults() {
    for(Iterator<ArchetypeCatalogFactory> it = archetypeCatalogs.iterator(); it.hasNext();) {
      ArchetypeCatalogFactory factory = it.next();
      if(factory.isEditable()) {
        it.remove();
      }
    }
    archetypesViewer.setAllChecked(true);
    archetypesViewer.setInput(archetypeCatalogs);
    archetypesViewer.setSelection(null, true);

    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getArchetypeCatalogs();
    for(ArchetypeCatalogFactory factory : catalogs) {
      if(factory.isEditable()) {
        archetypeManager.removeArchetypeCatalogFactory(factory.getId());
      }
    }
    for(ArchetypeCatalogFactory factory : archetypeCatalogs) {
      factory.setEnabled(archetypesViewer.getChecked(factory));
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

    getPreferenceStore().setValue(MavenPreferenceConstants.P_ENABLE_SNAPSHOT_ARCHETYPES, snapshotsBtn.getSelection());
    return super.performOk();
  }

  @Override
  public void init(IWorkbench workbench) {
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new BorderLayout());

    Link addRemoveOrLink = new Link(composite, SWT.NONE);
    addRemoveOrLink.setLayoutData(new BorderData(SWT.TOP));
    addRemoveOrLink.setText(Messages.MavenArchetypesPreferencePage_link);
    addRemoveOrLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      try {
        URL url = new URL(
            "http://maven.apache.org/plugins/maven-archetype-plugin/specification/archetype-catalog.html"); //$NON-NLS-1$
        IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
        browser.openURL(url);
      } catch(MalformedURLException ex) {
        log.error("Malformed URL", ex); //$NON-NLS-1$
      } catch(PartInitException ex) {
        log.error(ex.getMessage(), ex);
      }
    }));

    archetypesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);

    archetypesViewer.setLabelProvider(new CatalogsLabelProvider());
    archetypesViewer.setContentProvider(new ArrayContentProvider());

    Table table = archetypesViewer.getTable();
    table.setLayoutData(new BorderData(SWT.CENTER));
    table.setLinesVisible(false);
    table.setHeaderVisible(false);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 6));

    TableColumn typeColumn = new TableColumn(table, SWT.NONE);
    typeColumn.setWidth(250);
    typeColumn.setText(""); //$NON-NLS-1$

    Composite buttons = new Composite(composite, SWT.NONE);
    buttons.setLayoutData(new BorderData(SWT.RIGHT));
    buttons.setLayout(new GridLayout(1, true));
    Button enableAllBtn = new Button(buttons, SWT.NONE);
    enableAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    enableAllBtn.setText(Messages.MavenArchetypesPreferencePage_btnEnableAll);
    enableAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> toggleRepositories(true)));

    Button disableAllBtn = new Button(buttons, SWT.NONE);
    disableAllBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    disableAllBtn.setText(Messages.MavenArchetypesPreferencePage_btnDisableAll);
    disableAllBtn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> toggleRepositories(false)));

    Button addLocalButton = new Button(buttons, SWT.NONE);
    addLocalButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addLocalButton.setText(Messages.MavenArchetypesPreferencePage_btnAddLocal);
    addLocalButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      LocalArchetypeCatalogDialog dialog = new LocalArchetypeCatalogDialog(getShell(), null);
      if(dialog.open() == Window.OK) {
        addCatalogFactory(dialog.getArchetypeCatalogFactory());
      }
    }));

    Button addRemoteButton = new Button(buttons, SWT.NONE);
    addRemoteButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addRemoteButton.setText(Messages.MavenArchetypesPreferencePage_btnAddRemote);
    addRemoteButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      RemoteArchetypeCatalogDialog dialog = new RemoteArchetypeCatalogDialog(getShell(), null);
      if(dialog.open() == Window.OK) {
        addCatalogFactory(dialog.getArchetypeCatalogFactory());
      }
    }));

    final Button editButton = new Button(buttons, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setEnabled(false);
    editButton.setText(Messages.MavenArchetypesPreferencePage_btnEdit);
    editButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
      ArchetypeCatalogFactory newFactory = null;
      if(factory instanceof LocalCatalogFactory) {
        LocalArchetypeCatalogDialog dialog = new LocalArchetypeCatalogDialog(getShell(), factory);
        if(dialog.open() == Window.OK) {
          newFactory = dialog.getArchetypeCatalogFactory();
        }
      } else if(factory instanceof RemoteCatalogFactory) {
        RemoteArchetypeCatalogDialog dialog = new RemoteArchetypeCatalogDialog(getShell(), factory);
        if(dialog.open() == Window.OK) {
          newFactory = dialog.getArchetypeCatalogFactory();
        }
      }
      if(newFactory != null) {
        int n = archetypeCatalogs.indexOf(factory);
        if(n > -1) {
          archetypeCatalogs.set(n, newFactory);
          archetypesViewer.setInput(archetypeCatalogs);
          archetypesViewer.setSelection(new StructuredSelection(newFactory), true);
        }
      }
    }));

    final Button removeButton = new Button(buttons, SWT.NONE);
    removeButton.setEnabled(false);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeButton.setText(Messages.MavenArchetypesPreferencePage_btnRemove);
    removeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
      archetypeCatalogs.remove(factory);
      archetypesViewer.setInput(archetypeCatalogs);
      archetypesViewer.setSelection(null, true);
    }));

    archetypesViewer.addSelectionChangedListener(event -> {
      if(archetypesViewer.getSelection() instanceof IStructuredSelection) {
        ArchetypeCatalogFactory factory = getSelectedArchetypeCatalogFactory();
        boolean isEnabled = factory != null && factory.isEditable();
        removeButton.setEnabled(isEnabled);
        editButton.setEnabled(isEnabled);
      }
    });

    archetypesViewer.addCheckStateListener((event) -> {
      archetypesViewer.refresh(event.getElement(), true);
    });

    archetypeCatalogs = new ArrayList<>(archetypeManager.getArchetypeCatalogs());
    archetypesViewer.setInput(archetypeCatalogs);
    archetypeCatalogs.forEach(a -> archetypesViewer.setChecked(a, a.isEnabled()));
    archetypesViewer.refresh(); // should listen on property changes instead?

    this.snapshotsBtn = new Button(composite, SWT.CHECK);
    this.snapshotsBtn.setLayoutData(new BorderData(SWT.BOTTOM));
    this.snapshotsBtn.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_btnSnapshots);
    return composite;
  }

  protected void toggleRepositories(boolean toggle) {
    archetypeCatalogs.forEach(a -> archetypesViewer.setChecked(a, toggle));
    archetypesViewer.refresh();
  }

  protected ArchetypeCatalogFactory getSelectedArchetypeCatalogFactory() {
    IStructuredSelection selection = (IStructuredSelection) archetypesViewer.getSelection();
    return (ArchetypeCatalogFactory) selection.getFirstElement();
  }

  private void addCatalogFactory(ArchetypeCatalogFactory factory) {
    if(factory == null) {
      return;
    }
    archetypeCatalogs.add(factory);
    if(!archetypesViewer.getControl().isDisposed()) {
      archetypesViewer.setInput(archetypeCatalogs);
      archetypesViewer.setChecked(factory, true);
      archetypesViewer.setSelection(new StructuredSelection(factory), true);
    }
  }

  class CatalogsLabelProvider implements ITableLabelProvider, IColorProvider {

    private final Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

    @Override
    public String getColumnText(Object element, int columnIndex) {
      ArchetypeCatalogFactory factory = (ArchetypeCatalogFactory) element;
      String description = factory.getDescription();
      String text;
      if(factory instanceof LocalCatalogFactory) {
        text = NLS.bind(Messages.MavenArchetypesPreferencePage_local, description);
      } else if(factory instanceof RemoteCatalogFactory) {
        if(factory.isEditable()) {
          text = NLS.bind(Messages.MavenArchetypesPreferencePage_remote, description);
        } else {
          text = NLS.bind(Messages.MavenArchetypesPreferencePage_packaged, description);
        }
      } else {
        text = description;
      }

      return factory.isEditable() ? text : NLS.bind(Messages.MavenArchetypesPreferencePage_SystemLabel, text);
    }

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }

    @Override
    public Color getForeground(Object element) {
      return archetypesViewer.getChecked(element) ? null : disabledColor;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

  }


}
