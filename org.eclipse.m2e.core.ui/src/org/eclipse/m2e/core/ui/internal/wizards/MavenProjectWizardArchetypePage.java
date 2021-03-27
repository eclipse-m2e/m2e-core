/*******************************************************************************
 * Copyright (c) 2008-2014 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.archetype.ArchetypeUtil;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.archetype.ArchetypeCatalogFactory;
import org.eclipse.m2e.core.internal.archetype.ArchetypeManager;
import org.eclipse.m2e.core.internal.index.IMutableIndex;
import org.eclipse.m2e.core.internal.index.IndexListener;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.repository.IRepository;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;


/**
 * Maven Archetype selection wizard page presents the user with a list of available Maven Archetypes available for
 * creating new project.
 */
public class MavenProjectWizardArchetypePage extends AbstractMavenWizardPage implements IndexListener {
  private static final Logger log = LoggerFactory.getLogger(MavenProjectWizardArchetypePage.class);

  private static final String KEY_CATALOG = "catalog"; //$NON-NLS-1$

  private static final String ALL_CATALOGS = org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_all;

  public static final Comparator<Archetype> ARCHETYPE_COMPARATOR = (a1, a2) -> {
    String g1 = a1.getGroupId();
    String g2 = a2.getGroupId();
    int res = g1.compareTo(g2);
    if(res != 0) {
      return res;
    }

    String i1 = a1.getArtifactId();
    String i2 = a2.getArtifactId();
    res = i1.compareTo(i2);
    if(res != 0) {
      return res;
    }

    String v1 = a1.getVersion();
    String v2 = a2.getVersion();
    if(v1 == null) {
      return v2 == null ? 0 : -1;
    }
    return v1.compareTo(v2);
  };

  private static final boolean DEFAULT_SHOW_LAST_VERSION = true;

  private boolean includeSnapshots;

  private Map<String, List<Archetype>> archetypesCache = new HashMap<>();

  ComboViewer catalogsComboViewer;

  Text filterText;

  /** the archetype table viewer */
  TableViewer viewer;

  /** the description value label */
  Text descriptionText;

  Button showLastVersionButton;

  Button includeShapshotsButton;

  Button addArchetypeButton;

  /** the list of available archetypes */
  volatile Collection<Archetype> archetypes;

  /**
   * Archetype key to known archetype versions map. Versions are sorted in newest-to-oldest order.
   */
  Map<String, List<ArtifactVersion>> archetypeVersions;

  /** a flag indicating if the archetype selection is actually used in the wizard */
  private boolean isUsed = true;

  ArchetypeCatalogFactory catalogFactory = null;

  private RetrievingArchetypesJob job;

  /**
   * Default constructor. Sets the title and description of this wizard page and marks it as not being complete as user
   * input is required for continuing.
   */
  public MavenProjectWizardArchetypePage(ProjectImportConfiguration projectImportConfiguration) {
    super("MavenProjectWizardArchetypePage", projectImportConfiguration); //$NON-NLS-1$
    setTitle(Messages.wizardProjectPageArchetypeTitle);
    setDescription(Messages.wizardProjectPageArchetypeDescription);
    setPageComplete(false);
  }

  /** Creates the page controls. */
  public void createControl(Composite parent) {
    archetypesCache.clear();

    includeSnapshots = M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getBoolean(MavenPreferenceConstants.P_ENABLE_SNAPSHOT_ARCHETYPES);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));

    createViewer(composite);

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

    MavenPlugin.getIndexManager().addIndexListener(this);
    setControl(composite);
  }

  /** Creates the archetype table viewer. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void createViewer(Composite parent) {
    Label catalogsLabel = new Label(parent, SWT.NONE);
    catalogsLabel.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_lblCatalog);

    Composite catalogsComposite = new Composite(parent, SWT.NONE);
    GridLayout catalogsCompositeLayout = new GridLayout();
    catalogsCompositeLayout.marginWidth = 0;
    catalogsCompositeLayout.marginHeight = 0;
    catalogsCompositeLayout.numColumns = 2;
    catalogsComposite.setLayout(catalogsCompositeLayout);
    catalogsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

    catalogsComboViewer = new ComboViewer(catalogsComposite);
    catalogsComboViewer.getControl().setData("name", "catalogsCombo"); //$NON-NLS-1$ //$NON-NLS-2$
    catalogsComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    catalogsComboViewer.setContentProvider(new IStructuredContentProvider() {
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

    catalogsComboViewer.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof ArchetypeCatalogFactory) {
          return ((ArchetypeCatalogFactory) element).getDescription();
        } else if(element instanceof String) {
          return element.toString();
        }
        return super.getText(element);
      }
    });

    catalogsComboViewer.addSelectionChangedListener(event -> {
      ISelection selection = event.getSelection();
      boolean loadAll = false;
      //hide previous archetypes when switching catalog
      if(selection instanceof IStructuredSelection) {
        Object factory = ((IStructuredSelection) selection).getFirstElement();
        ArchetypeCatalogFactory newCatalogFactory = null;
        if(factory instanceof ArchetypeCatalogFactory) {
          newCatalogFactory = (ArchetypeCatalogFactory) factory;
        }
        if(factory != null && newCatalogFactory == null) {
          loadAll = true;
        } else if(Objects.equals(catalogFactory, newCatalogFactory) && viewer.getInput() != null) {
          return;
        }
        catalogFactory = newCatalogFactory;
        viewer.setInput(null);
        reloadViewer();
      } else {
        loadAll = true;
      }
      if(loadAll) {
        catalogFactory = null;
        viewer.setInput(null);
        loadArchetypes(null, null, null);
      }
      //remember what was switched to here
      if(dialogSettings != null) {
        if(catalogFactory != null)
          dialogSettings.put(KEY_CATALOG, catalogFactory.getId());
        else
          dialogSettings.put(KEY_CATALOG, ALL_CATALOGS);
      }
    });

    final ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
    ArrayList allCatalogs = new ArrayList(archetypeManager.getActiveArchetypeCatalogs());
    allCatalogs.add(0, ALL_CATALOGS);
    catalogsComboViewer.setInput(allCatalogs);

    Button configureButton = new Button(catalogsComposite, SWT.NONE);
    configureButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    configureButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_btnConfigure);
    configureButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {

      Collection<ArchetypeCatalogFactory> oldCatalogs = archetypeManager.getActiveArchetypeCatalogs();

      PreferencesUtil.createPreferenceDialogOn(getShell(),
          "org.eclipse.m2e.core.preferences.MavenArchetypesPreferencePage", null, null).open(); //$NON-NLS-1$

      Collection<ArchetypeCatalogFactory> newCatalogs = archetypeManager.getActiveArchetypeCatalogs();

      //Deselect removed catalog if needed
      if(catalogFactory != null && !newCatalogs.contains(catalogFactory)) {
        catalogFactory = null;
      }

      //Select 1st new catalog
      ArchetypeCatalogFactory selectedCatalog = catalogFactory;
      for(ArchetypeCatalogFactory newCatalog : newCatalogs) {
        if(!oldCatalogs.contains(newCatalog)) {
          selectedCatalog = newCatalog;
          break;
        }
      }

      ArrayList allCatalogs1 = new ArrayList(newCatalogs);
      allCatalogs1.add(0, ALL_CATALOGS);
      catalogsComboViewer.setInput(allCatalogs1);
      catalogsComboViewer
          .setSelection(new StructuredSelection(selectedCatalog == null ? ALL_CATALOGS : selectedCatalog));

    }));

    Label filterLabel = new Label(parent, SWT.NONE);
    filterLabel.setLayoutData(new GridData());
    filterLabel.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_lblFilter);

    QuickViewerFilter quickViewerFilter = new QuickViewerFilter();
    VersionsFilter versionFilter = new VersionsFilter(DEFAULT_SHOW_LAST_VERSION, includeSnapshots);

    filterText = new Text(parent, SWT.BORDER | SWT.SEARCH);
    filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    filterText.addModifyListener(quickViewerFilter);
    filterText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          viewer.getTable().setFocus();
          viewer.getTable().setSelection(0);

          viewer.setSelection(new StructuredSelection(viewer.getElementAt(0)), true);
        }
      }
    });

    ToolBar toolBar = new ToolBar(parent, SWT.FLAT);
    toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    final ToolItem clearToolItem = new ToolItem(toolBar, SWT.PUSH);
    clearToolItem.setEnabled(false);
    clearToolItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE));
    clearToolItem
        .setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
        clearToolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
          filterText.setText(""); //$NON-NLS-1$
        }));

    filterText.addModifyListener(e -> clearToolItem.setEnabled(filterText.getText().length() > 0));

    SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
    GridData gd_sashForm = new GridData(SWT.FILL, SWT.FILL, false, true, 3, 1);
    // gd_sashForm.widthHint = 500;
    gd_sashForm.heightHint = 200;
    sashForm.setLayoutData(gd_sashForm);
    sashForm.setLayout(new GridLayout());

    Composite composite1 = new Composite(sashForm, SWT.NONE);
    GridLayout gridLayout1 = new GridLayout();
    gridLayout1.horizontalSpacing = 0;
    gridLayout1.marginWidth = 0;
    gridLayout1.marginHeight = 0;
    composite1.setLayout(gridLayout1);

    viewer = new TableViewer(composite1, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
    Table table = viewer.getTable();
    table.setData("name", "archetypesTable"); //$NON-NLS-1$ //$NON-NLS-2$
    table.setHeaderVisible(true);

    TableColumn column1 = new TableColumn(table, SWT.LEFT);
    column1.setWidth(150);
    column1.setText(Messages.wizardProjectPageArchetypeColumnGroupId);

    TableColumn column0 = new TableColumn(table, SWT.LEFT);
    column0.setWidth(150);
    column0.setText(Messages.wizardProjectPageArchetypeColumnArtifactId);

    TableColumn column2 = new TableColumn(table, SWT.LEFT);
    column2.setWidth(100);
    column2.setText(Messages.wizardProjectPageArchetypeColumnVersion);

    GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
    tableData.widthHint = 400;
    tableData.heightHint = 200;
    table.setLayoutData(tableData);

    viewer.setLabelProvider(new ArchetypeLabelProvider());

    viewer.setComparator(new ViewerComparator() {
      public int compare(Viewer viewer, Object e1, Object e2) {
        return ARCHETYPE_COMPARATOR.compare((Archetype) e1, (Archetype) e2);
      }
    });

    viewer.setComparer(new IElementComparer() {
      public int hashCode(Object obj) {
        if(obj instanceof Archetype) {
          return ArchetypeUtil.getHashCode((Archetype) obj);
        }
        return obj.hashCode();
      }

      public boolean equals(Object one, Object another) {
        if(one instanceof Archetype && another instanceof Archetype) {
          return ArchetypeUtil.areEqual((Archetype) one, (Archetype) another);
        }
        return one.equals(another);
      }
    });

    viewer.setFilters(versionFilter, quickViewerFilter);

    viewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof Collection) {
          return ((Collection<?>) inputElement).toArray();
        }
        return new Object[0];
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    viewer.addSelectionChangedListener(event -> {
      Archetype archetype = getArchetype();
      if(archetype != null) {
        String repositoryUrl = archetype.getRepository();
        String description = archetype.getDescription();

        String text = description == null ? "" : description; //$NON-NLS-1$
        text = text.replace("\n", "").replaceAll("\\s{2,}", " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        if(repositoryUrl != null) {
          text += text.length() > 0 ? "\n" + repositoryUrl : repositoryUrl; //$NON-NLS-1$
        }

        descriptionText.setText(text);
        setPageComplete(true);
      } else {
        descriptionText.setText(""); //$NON-NLS-1$
        setPageComplete(false);
      }
    });

    viewer.addOpenListener(openevent -> {
      if(canFlipToNextPage()) {
        getContainer().showPage(getNextPage());
      }
    });

    Composite composite2 = new Composite(sashForm, SWT.NONE);
    GridLayout gridLayout2 = new GridLayout();
    gridLayout2.marginHeight = 0;
    gridLayout2.marginWidth = 0;
    gridLayout2.horizontalSpacing = 0;
    composite2.setLayout(gridLayout2);

    descriptionText = new Text(composite2, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.MULTI | SWT.BORDER);

    GridData descriptionTextData = new GridData(SWT.FILL, SWT.FILL, true, true);
    descriptionTextData.heightHint = 40;
    descriptionText.setLayoutData(descriptionTextData);
    //whole dialog resizes badly without the width hint to the desc text
    descriptionTextData.widthHint = 250;
    sashForm.setWeights(80, 20);

    Composite buttonComposite = new Composite(parent, SWT.NONE);
    GridData gd_buttonComposite = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
    buttonComposite.setLayoutData(gd_buttonComposite);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.numColumns = 3;
    buttonComposite.setLayout(gridLayout);

    showLastVersionButton = new Button(buttonComposite, SWT.CHECK);
    showLastVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    showLastVersionButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_btnLast);
    showLastVersionButton.setSelection(DEFAULT_SHOW_LAST_VERSION);
    showLastVersionButton.addSelectionListener(versionFilter);

    includeShapshotsButton = new Button(buttonComposite, SWT.CHECK);
    GridData buttonData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    buttonData.horizontalIndent = 25;
    includeShapshotsButton.setLayoutData(buttonData);
    includeShapshotsButton
        .setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_btnSnapshots);
    includeShapshotsButton.setSelection(includeSnapshots);
    includeShapshotsButton.addSelectionListener(versionFilter);

    addArchetypeButton = new Button(buttonComposite, SWT.NONE);
    addArchetypeButton.setText(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_btnAdd);
    addArchetypeButton.setData("name", "addArchetypeButton"); //$NON-NLS-1$ //$NON-NLS-2$
    buttonData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
    buttonData.horizontalIndent = 35;
    addArchetypeButton.setLayoutData(buttonData);

    addArchetypeButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      CustomArchetypeDialog dialog = new CustomArchetypeDialog(getShell(),
          org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_add_title);
      if(dialog.open() == Window.OK) {
        String archetypeGroupId = dialog.getArchetypeGroupId();
        String archetypeArtifactId = dialog.getArchetypeArtifactId();
        String archetypeVersion = dialog.getArchetypeVersion();
        String repositoryUrl = dialog.getRepositoryUrl();
        downloadArchetype(archetypeGroupId, archetypeArtifactId, archetypeVersion, repositoryUrl);
      }
    }));
  }

  protected IWizardContainer getContainer() {
    return super.getContainer();
  }

  public void addArchetypeSelectionListener(ISelectionChangedListener listener) {
    viewer.addSelectionChangedListener(listener);
  }

  public void dispose() {
    if(job != null) {
      job.cancel();
      job = null;
    }
    MavenPlugin.getIndexManager().removeIndexListener(this);
    archetypesCache.clear();
    super.dispose();
  }

  public List<Archetype> getArchetypesForCatalog() {
    try {
      return getArchetypesForCatalog(catalogFactory, null);
    } catch(CoreException ce) {
      setErrorMessage(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_error_read);
      return null;
    }
  }

  public List<Archetype> getArchetypesForCatalog(ArchetypeCatalogFactory archCatalogFactory, IProgressMonitor monitor)
      throws CoreException {
    if(archCatalogFactory == null) {
      return getAllArchetypes(monitor);
    }
    String catalogId = archCatalogFactory.getId();
    List<Archetype> archs = archetypesCache.get(catalogId);
    if(archs == null) {
      archs = archCatalogFactory.getArchetypeCatalog().getArchetypes();
      if(archs == null) {
        archs = Collections.emptyList();
      }
      archetypesCache.put(catalogId, archs);
    }
    return archs;
  }

  @SuppressWarnings("unchecked")
  private List<Archetype> getAllArchetypes(IProgressMonitor monitor) {
    ArchetypeManager manager = MavenPluginActivator.getDefault().getArchetypeManager();
    Collection<ArchetypeCatalogFactory> archetypeCatalogs = manager.getActiveArchetypeCatalogs();

    ArrayList<Archetype> list = new ArrayList<>();

    if(monitor == null) {
      monitor = new NullProgressMonitor();
    }

    for(ArchetypeCatalogFactory catalog : archetypeCatalogs) {
      if(monitor.isCanceled()) {
        return Collections.emptyList();
      }
      try {
        //temporary hack to get around 'Test Remote Catalog' blowing up on download
        //described in https://issues.sonatype.org/browse/MNGECLIPSE-1792
        if(catalog.getDescription().startsWith("Test")) { //$NON-NLS-1$
          continue;
        }
        @SuppressWarnings("rawtypes")
        List arcs = getArchetypesForCatalog(catalog, monitor);
        if(arcs != null) {
          list.addAll(arcs);
        }
      } catch(Exception ce) {
        log.error("Unable to read archetype catalog: " + catalog.getId(), ce); //$NON-NLS-1$
      }
    }
    return list;
  }

  /** Loads the available archetypes. */
  void loadArchetypes(final String groupId, final String artifactId, final String version) {
    if(job != null) {
      job.cancel();
    }
    job = new RetrievingArchetypesJob(catalogFactory);
    job.addJobChangeListener(new JobChangeAdapter() {
      public void done(IJobChangeEvent event) {

        final RetrievingArchetypesJob thisJob = (RetrievingArchetypesJob) event.getJob();
        if(IStatus.CANCEL == event.getResult().getSeverity() || !isCurrentPage()) {
          return;
        }
        List<Archetype> catalogArchetypes = thisJob.catalogArchetypes;

        final String error;
        if(IStatus.ERROR == event.getResult().getSeverity()) {
          error = event.getResult().getMessage();
        } else if((catalogArchetypes == null || catalogArchetypes.isEmpty())) {
          ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
          Collection<ArchetypeCatalogFactory> catalogs = archetypeManager.getActiveArchetypeCatalogs();
          if(catalogs.isEmpty()) {
            error = Messages.MavenProjectWizardArchetypePage_error_noEnabledCatalogs;
          } else if(catalogFactory != null && "Nexus Indexer".equals(catalogFactory.getDescription())) { //$NON-NLS-1$
            error = Messages.MavenProjectWizardArchetypePage_error_emptyNexusIndexer;
          } else {
            error = Messages.MavenProjectWizardArchetypePage_error_emptyCatalog;
          }
        } else {
          error = null;
        }

        Display.getDefault().asyncExec(() -> setErrorMessage(error));
        TreeSet<Archetype> archs = new TreeSet<>(ARCHETYPE_COMPARATOR);
        if(catalogArchetypes != null) {
          archs.addAll(catalogArchetypes);
        }
        archetypes = archs;

        Display.getDefault().asyncExec(() -> updateViewer(groupId, artifactId, version));
      }
    });
    job.schedule();
  }

  private static String getArchetypeKey(Archetype archetype) {
    return new StringBuilder(archetype.getGroupId()).append(":").append(archetype.getArtifactId()).toString(); //$NON-NLS-1$
  }

  ArchetypeCatalog getArchetypeCatalog() throws CoreException {
    return catalogFactory == null ? null : catalogFactory.getArchetypeCatalog();
  }

  /** Sets the flag that the archetype selection is used in the wizard. */
  public void setUsed(boolean isUsed) {
    this.isUsed = isUsed;
  }

  /** Overrides the default to return "true" if the page is not used. */
  public boolean isPageComplete() {
    return !isUsed || super.isPageComplete();
  }

  /** Sets the focus to the table component. */
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if(visible) {
      ArchetypeManager archetypeManager = MavenPluginActivator.getDefault().getArchetypeManager();
      String catalogId = dialogSettings.get(KEY_CATALOG);
      catalogFactory = null;
      if(catalogId != null && !catalogId.equals(ALL_CATALOGS)) {
        catalogFactory = archetypeManager.getArchetypeCatalogFactory(catalogId);
      }
      if(catalogsComboViewer.getSelection().isEmpty()) {
        catalogsComboViewer
            .setSelection(new StructuredSelection(catalogFactory == null ? ALL_CATALOGS : catalogFactory));
      }

      viewer.getTable().setFocus();
      Archetype selected = getArchetype();
      if(selected != null) {
        viewer.reveal(selected);
      }
    } else {
      if(job != null) {
        job.cancel();
        job = null;
      }
    }
  }

  /** Returns the selected archetype. */
  public Archetype getArchetype() {
    return (Archetype) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
  }

  void updateViewer(String groupId, String artifactId, String version) {
    if(viewer.getControl().isDisposed()) {
      return;
    }

    archetypeVersions = getArchetypeVersions(archetypes);

    viewer.setInput(archetypes);

    if(isCurrentPage()) {
      selectArchetype(groupId, artifactId, version);
    }

    Table table = viewer.getTable();
    int columnCount = table.getColumnCount();
    int width = 0;
    for(int i = 0; i < columnCount; i++ ) {
      TableColumn column = table.getColumn(i);
      column.pack();
      width += column.getWidth();
    }
    GridData tableData = (GridData) table.getLayoutData();
    int oldHint = tableData.widthHint;
    if(width > oldHint) {
      tableData.widthHint = width;
    }
    getShell().pack(true);
    tableData.widthHint = oldHint;
  }

  private static Map<String, List<ArtifactVersion>> getArchetypeVersions(Collection<Archetype> archetypes) {
    HashMap<String, List<ArtifactVersion>> archetypeVersions = new HashMap<>(archetypes.size());

    HashMap<String, ArtifactVersion> versionFactory = new HashMap<>();

    for(Archetype currentArchetype : archetypes) {
      String version = currentArchetype.getVersion();
      if(M2EUIUtils.nullOrEmpty(version)) {
        // we don't know how to handle null/empty versions
        continue;
      }
      String key = getArchetypeKey(currentArchetype);

      List<ArtifactVersion> versions = archetypeVersions.get(key);
      if(versions == null) {
        versions = new ArrayList<>();
        archetypeVersions.put(key, versions);
      }
      ArtifactVersion v = versionFactory.get(version);
      if(v == null) {
        v = new DefaultArtifactVersion(version);
        versionFactory.put(version, v);
      }
      versions.add(v);
    }

    final Comparator<ArtifactVersion> comparator = (v1, v2) -> v2.compareTo(v1);

    for(List<ArtifactVersion> versions : archetypeVersions.values()) {
      Collections.sort(versions, comparator);
    }
    return archetypeVersions;
  }

  protected void selectArchetype(String groupId, String artifactId, String version) {
    Archetype archetype = findArchetype(groupId, artifactId, version);

    Table table = viewer.getTable();
    if(archetype != null) {
      viewer.setSelection(new StructuredSelection(archetype), true);

      int n = table.getSelectionIndex();
      table.setSelection(n);
    }
  }

  /** Locates an archetype with given ids. */
  protected Archetype findArchetype(String groupId, String artifactId, String version) {
    for(Archetype archetype : archetypes) {
      if(archetype.getGroupId().equals(groupId) && archetype.getArtifactId().equals(artifactId)) {
        if(version == null || version.equals(archetype.getVersion())) {
          return archetype;
        }
      }
    }

    return version == null ? null : findArchetype(groupId, artifactId, null);
  }

  protected void downloadArchetype(final String archetypeGroupId, final String archetypeArtifactId,
      final String archetypeVersion, final String repositoryUrl) {
    if(getContainer() == null) {
      return;//page has been disposed
    }
    final String archetypeName = archetypeGroupId + ":" + archetypeArtifactId + ":" + archetypeVersion; //$NON-NLS-1$ //$NON-NLS-2$
    try {
      getContainer().run(true, true, monitor -> {
        monitor.beginTask(
            org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_task_downloading + archetypeName,
            IProgressMonitor.UNKNOWN);

        try {
          final IMaven maven = MavenPlugin.getMaven();

          final List<ArtifactRepository> remoteRepositories;
          if(repositoryUrl.length() == 0) {
            remoteRepositories = maven.getArtifactRepositories(); // XXX should use ArchetypeManager.getArchetypeRepositories()
          } else {
            //Use id = archetypeArtifactId+"-repo" to enable mirror/proxy authentication
            //see http://maven.apache.org/archetype/maven-archetype-plugin/faq.html
            ArtifactRepository repository = maven.createArtifactRepository(archetypeArtifactId + "-repo", //$NON-NLS-1$
                repositoryUrl);

            remoteRepositories = Collections.singletonList(repository);
          }

          monitor.subTask(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_task_resolving);
          Artifact pomArtifact = maven.resolve(archetypeGroupId, archetypeArtifactId, archetypeVersion, "pom", null, //$NON-NLS-1$
              remoteRepositories, monitor);
          monitor.worked(1);
          if(monitor.isCanceled()) {
            throw new InterruptedException();
          }

          File pomFile = pomArtifact.getFile();
          if(pomFile.exists()) {
            monitor.subTask(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_task_resolving2);
            Artifact jarArtifact = maven.resolve(archetypeGroupId, archetypeArtifactId, archetypeVersion, "jar", null, //$NON-NLS-1$
                remoteRepositories, monitor);
            monitor.worked(1);
            if(monitor.isCanceled()) {
              throw new InterruptedException();
            }

            File jarFile = jarArtifact.getFile();

            monitor.subTask(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_task_reading);
            monitor.worked(1);
            if(monitor.isCanceled()) {
              throw new InterruptedException();
            }

            monitor.subTask(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_task_indexing);
            IndexManager indexManager = MavenPlugin.getIndexManager();
            IMutableIndex localIndex = indexManager.getLocalIndex();
            localIndex.addArtifact(jarFile, new ArtifactKey(pomArtifact));

            //save out the archetype
            //TODO move this logic out of UI code!
            Archetype archetype = new Archetype();
            archetype.setGroupId(archetypeGroupId);
            archetype.setArtifactId(archetypeArtifactId);
            archetype.setVersion(archetypeVersion);
            archetype.setRepository(repositoryUrl);
            org.apache.maven.archetype.ArchetypeManager archetyper = MavenPluginActivator.getDefault()
                .getArchetypeManager().getArchetyper();
            archetyper.updateLocalCatalog(archetype);

            archetypesCache.clear();

            loadArchetypes(archetypeGroupId, archetypeArtifactId, archetypeVersion);
          } else {
            final Artifact pom = pomArtifact;
            //the user tried to add an archetype that couldn't be resolved on the server
            getShell().getDisplay()
                .asyncExec(() -> setErrorMessage(
                    NLS.bind(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_error_resolve,
                        pom.toString())));
          }

        } catch(InterruptedException ex1) {
          throw ex1;

        } catch(final Exception ex2) {
          final String msg = NLS.bind(
              org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_error_resolve2, archetypeName);
          log.error(msg, ex2);
          getShell().getDisplay().asyncExec(() -> setErrorMessage(msg + "\n" + ex2.toString()));

        } finally {
          monitor.done();

        }
      });

    } catch(InterruptedException ex) {
      // ignore

    } catch(InvocationTargetException ex) {
      String msg = NLS.bind(org.eclipse.m2e.core.ui.internal.Messages.MavenProjectWizardArchetypePage_error_resolve2,
          archetypeName);
      log.error(msg, ex);
      setErrorMessage(msg + "\n" + ex.toString()); //$NON-NLS-1$

    }
  }

  /**
   * ArchetypeLabelProvider
   */
  protected static class ArchetypeLabelProvider extends LabelProvider implements ITableLabelProvider {
    /** Returns the element text */
    public String getColumnText(Object element, int columnIndex) {
      if(element instanceof Archetype) {
        Archetype archetype = (Archetype) element;
        switch(columnIndex) {
          case 0:
            return archetype.getGroupId();
          case 1:
            return archetype.getArtifactId();
          case 2:
            return archetype.getVersion();
        }
      }
      return super.getText(element);
    }

    /** Returns the element text */
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }
  }

  /**
   * QuickViewerFilter
   */
  protected class QuickViewerFilter extends ViewerFilter implements ModifyListener {

    private String currentFilter;

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(currentFilter == null || currentFilter.length() == 0) {
        return true;
      }
      Archetype archetype = (Archetype) element;
      return archetype.getGroupId().toLowerCase().indexOf(currentFilter) > -1
          || archetype.getArtifactId().toLowerCase().indexOf(currentFilter) > -1;
    }

    public void modifyText(ModifyEvent e) {
      this.currentFilter = filterText.getText().trim().toLowerCase();
      viewer.refresh();
    }
  }

  protected class VersionsFilter extends ViewerFilter implements SelectionListener {

    private boolean showLastVersion;

    private boolean includeSnapshots;

    public VersionsFilter(boolean showLastVersion, boolean includeSnapshots) {
      this.showLastVersion = showLastVersion;
      this.includeSnapshots = includeSnapshots;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(!(element instanceof Archetype)) {
        return false;
      }

      Archetype archetype = (Archetype) element;
      String version = archetype.getVersion();

      if(!includeSnapshots) {
        if(isSnapshotVersion(version)) {
          return false;
        }
      }

      if(!showLastVersion) {
        return true;
      }

      // need to find latest version, skipping snapshots depending on includeSnapshots
      List<ArtifactVersion> versions = archetypeVersions.get(getArchetypeKey(archetype));

      if(versions == null || versions.isEmpty()) {
        return false; // can't really happen
      }

      for(ArtifactVersion otherVersion : versions) {
        if(includeSnapshots || !isSnapshotVersion(otherVersion.toString())) {
          if(otherVersion.toString().equals(version)) {
            return true;
          }
          break;
        }
      }

      return false;
    }

    boolean isSnapshotVersion(String version) {
      return !M2EUIUtils.nullOrEmpty(version) && version.endsWith("SNAPSHOT"); //$NON-NLS-1$
    }

    public void widgetSelected(SelectionEvent e) {
      this.showLastVersion = showLastVersionButton.getSelection();
      this.includeSnapshots = includeShapshotsButton.getSelection();
      viewer.refresh();
      Archetype archetype = getArchetype();
      //can be null in some cases, don't try to reveal
      if(archetype != null) {
        viewer.reveal(archetype);
      }
      viewer.getTable().setSelection(viewer.getTable().getSelectionIndex());
      viewer.getTable().setFocus();
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.index.IndexListener#indexAdded(org.eclipse.m2e.repository.IRepository)
   */
  public void indexAdded(IRepository repository) {

  }

  //reload the table when index updating finishes
  //try to preserve selection in case this is a rebuild
  protected void reloadViewer() {
    Display.getDefault().asyncExec(() -> {
      if(isCurrentPage()) {
        StructuredSelection sel = (StructuredSelection) viewer.getSelection();
        Archetype selArchetype = null;
        if(sel != null && sel.getFirstElement() != null) {
          selArchetype = (Archetype) sel.getFirstElement();
        }
        if(selArchetype != null) {
          loadArchetypes(selArchetype.getGroupId(), selArchetype.getArtifactId(), selArchetype.getVersion());
        } else {
          loadArchetypes("org.apache.maven.archetypes", "maven-archetype-quickstart", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
      }
    });
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.index.IndexListener#indexChanged(org.eclipse.m2e.repository.IRepository)
   */
  public void indexChanged(IRepository repository) {
    reloadViewer();
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.index.IndexListener#indexRemoved(org.eclipse.m2e.repository.IRepository)
   */
  public void indexRemoved(IRepository repository) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.index.IndexListener#indexUpdating(org.eclipse.m2e.repository.IRepository)
   */
  public void indexUpdating(IRepository repository) {
  }

  private class RetrievingArchetypesJob extends Job {

    List<Archetype> catalogArchetypes;

    private ArchetypeCatalogFactory archetypeCatalogFactory;

    public RetrievingArchetypesJob(ArchetypeCatalogFactory catalogFactory) {
      super(Messages.wizardProjectPageArchetypeRetrievingArchetypes);
      this.archetypeCatalogFactory = catalogFactory;
    }

    protected IStatus run(IProgressMonitor monitor) {
      try {
        catalogArchetypes = getArchetypesForCatalog(archetypeCatalogFactory, monitor);
      } catch(Exception e) {
        monitor.done();
        return new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID,
            Messages.MavenProjectWizardArchetypePage_ErrorRetrievingArchetypes, e);
      }
      return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

  }
}
