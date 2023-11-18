/*******************************************************************************
 * Copyright (c) 2008, 2021 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored lifecycle mapping discovery
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkingSet;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.AbstractProjectScanner;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.WorkingSets;


/**
 * Maven Import Wizard Page
 *
 * @author Eugene Kuleshov
 */
public class MavenImportWizardPage extends AbstractMavenWizardPage {
  private static final Logger log = LoggerFactory.getLogger(MavenImportWizardPage.class);

  static final Object[] EMPTY = new Object[0];

  protected Combo rootDirectoryCombo;

  protected CheckboxTreeViewer projectTreeViewer;

  private List<String> locations;

  private final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

  private boolean showLocation = true;

  private boolean basedirRemameRequired = false;

  private String rootDirectory;

  private String loadingErrorMessage;

  private Button btnSelectTree;

  private Button btnDeselectTree;

  private Button createWorkingSet;

  private Combo workingSetName;

  private String preselectedWorkingSetName;

  public MavenImportWizardPage(ProjectImportConfiguration importConfiguration) {
    super("MavenProjectImportWizardPage", importConfiguration); //$NON-NLS-1$
    setTitle(org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_title);
    setDescription(org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_desc);
    setPageComplete(false);
  }

  public void setShowLocation(boolean showLocation) {
    this.showLocation = showLocation;
  }

  public void setLocations(List<String> locations) {
    this.locations = locations;
  }

  public void setBasedirRemameRequired(boolean basedirRemameRequired) {
    this.basedirRemameRequired = basedirRemameRequired;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    setControl(composite);

    if(showLocation || locations == null || locations.isEmpty()) {
      final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
      selectRootDirectoryLabel.setLayoutData(new GridData());
      selectRootDirectoryLabel.setText(Messages.wizardImportPageRoot);

      rootDirectoryCombo = new Combo(composite, SWT.NONE);
      rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      rootDirectoryCombo.setFocus();
      addFieldWithHistory("rootDirectory", rootDirectoryCombo); //$NON-NLS-1$

      if(locations != null && locations.size() == 1) {
        rootDirectoryCombo.setText(locations.get(0));
        rootDirectory = locations.get(0);
      }

      final Button browseButton = new Button(composite, SWT.NONE);
      browseButton.setText(Messages.wizardImportPageBrowse);
      browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
        DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
        dialog.setText(Messages.wizardImportPageSelectRootFolder);
        String path = rootDirectoryCombo.getText();
        if(path.length() == 0) {
          path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
        }
        dialog.setFilterPath(path);

        String result = dialog.open();
        if(result != null) {
          rootDirectoryCombo.setText(result);
          if(rootDirectoryChanged()) {
            scanProjects();
          }
        }
      }));

      rootDirectoryCombo.addListener(SWT.Traverse, e -> {
        if(e.keyCode == SWT.CR && rootDirectoryChanged()) {
          //New location entered : don't finish the wizard
          if(e.detail == SWT.TRAVERSE_RETURN) {
            e.doit = false;
          }
          scanProjects();
        }
      });

      rootDirectoryCombo.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          if(rootDirectoryChanged()) {
            scanProjects();
          }
        }
      });
      rootDirectoryCombo.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
          if(rootDirectoryChanged()) {
            scanProjects();
          }
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
          if(rootDirectoryChanged()) {
            //in runnable to have the combo popup collapse before disabling controls.
            Display.getDefault().asyncExec(() -> scanProjects());
          }
        }
      });
    }

    final Label projectsLabel = new Label(composite, SWT.NONE);
    projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    projectsLabel.setText(Messages.wizardImportPageProjects);

    projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);

    projectTreeViewer.addCheckStateListener(event -> {
      updateCheckedState();
      setPageComplete();
    });

    projectTreeViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      btnSelectTree.setEnabled(!selection.isEmpty());
      btnDeselectTree.setEnabled(!selection.isEmpty());
      if(selection.getFirstElement() != null) {
        String errorMsg = validateProjectInfo((MavenProjectInfo) selection.getFirstElement());
        if(errorMsg != null) {
          setMessage(errorMsg, IMessageProvider.WARNING);
        } else {
          //TODO if no error on current, shall show any existing general errors if found..
          setMessage(loadingErrorMessage, IMessageProvider.WARNING);
        }
      } else {
        //TODO if on current selection, shall show any existing general errors if existing..
        setMessage(loadingErrorMessage, IMessageProvider.WARNING);
      }
    });

    projectTreeViewer.setContentProvider(new ITreeContentProvider() {

      @Override
      public Object[] getElements(Object element) {
        if(element instanceof List) {
          @SuppressWarnings("unchecked")
          List<MavenProjectInfo> projects = (List<MavenProjectInfo>) element;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        }
        return EMPTY;
      }

      @Override
      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof List) {
          @SuppressWarnings("unchecked")
          List<MavenProjectInfo> projects = (List<MavenProjectInfo>) parentElement;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        } else if(parentElement instanceof MavenProjectInfo mavenProjectInfo) {
          Collection<MavenProjectInfo> projects = mavenProjectInfo.getProjects();
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        }
        return EMPTY;
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public boolean hasChildren(Object parentElement) {
        if(parentElement instanceof List<?> projects) {
          return !projects.isEmpty();
        } else if(parentElement instanceof MavenProjectInfo mavenProjectInfo) {
          return !mavenProjectInfo.getProjects().isEmpty();
        }
        return false;
      }

    });

    projectTreeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new ProjectLabelProvider()));

    final Tree projectTree = projectTreeViewer.getTree();
    GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 5);
    projectTreeData.heightHint = 250;
    projectTreeData.widthHint = 500;
    projectTree.setLayoutData(projectTreeData);

    Menu menu = new Menu(projectTree);
    projectTree.setMenu(menu);

    MenuItem mntmSelectTree = new MenuItem(menu, SWT.NONE);
    mntmSelectTree.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setProjectSubtreeChecked(true)));
    mntmSelectTree.setText(Messages.MavenImportWizardPage_mntmSelectTree_text);

    MenuItem mntmDeselectTree = new MenuItem(menu, SWT.NONE);
    mntmDeselectTree
        .addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setProjectSubtreeChecked(false)));
    mntmDeselectTree.setText(Messages.MavenImportWizardPage_mntmDeselectTree_text);

    final Button selectAllButton = new Button(composite, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    selectAllButton.setText(Messages.wizardImportPageSelectAll);
    selectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      projectTreeViewer.expandAll();
      setAllChecked(true);
      // projectTreeViewer.setSubtreeChecked(projectTreeViewer.getInput(), true);
      validate();
    }));

    final Button deselectAllButton = new Button(composite, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    deselectAllButton.setText(Messages.wizardImportPageDeselectAll);
    deselectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      setAllChecked(false);
      // projectTreeViewer.setSubtreeChecked(projectTreeViewer.getInput(), false);
      setPageComplete(false);
    }));

    btnSelectTree = new Button(composite, SWT.NONE);
    btnSelectTree.setEnabled(false);
    btnSelectTree.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setProjectSubtreeChecked(true)));
    btnSelectTree.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnSelectTree.setText(Messages.MavenImportWizardPage_btnSelectTree_text);

    btnDeselectTree = new Button(composite, SWT.NONE);
    btnDeselectTree.setEnabled(false);
    btnDeselectTree.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> setProjectSubtreeChecked(false)));
    btnDeselectTree.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnDeselectTree.setText(Messages.MavenImportWizardPage_btnDeselectTree_text);

    final Button refreshButton = new Button(composite, SWT.NONE);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    refreshButton.setText(Messages.wizardImportPageRefresh);
    refreshButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> scanProjects()));

    createWorkingSet = new Button(composite, SWT.CHECK);
    createWorkingSet.setText(Messages.MavenImportWizardPage_createWorkingSet);
    createWorkingSet.setSelection(true);
    createWorkingSet.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    createWorkingSet.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      boolean enabled = createWorkingSet.getSelection();
      workingSetName.setEnabled(enabled);
      if(enabled) {
        workingSetName.setFocus();
      }
    }));

    workingSetName = new Combo(composite, SWT.BORDER);
    GridData gd_workingSet = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
    gd_workingSet.horizontalIndent = 20;
    workingSetName.setLayoutData(gd_workingSet);

    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    resolverConfigurationComponent.template.addModifyListener(arg0 -> Display.getDefault().asyncExec(() -> validate()));

    if(locations != null && !locations.isEmpty()) {
      scanProjects();
    }

  }

  protected boolean rootDirectoryChanged() {
    String _rootDirectory = rootDirectory;
    rootDirectory = rootDirectoryCombo.getText().trim();
    IPath p = IPath.fromOSString(rootDirectory);
    if(p.isRoot()) {
      setErrorMessage(Messages.MavenImportWizardPage_forbiddenImportFromRoot);
      return false;
    }
    setErrorMessage(null);
    return _rootDirectory == null || !_rootDirectory.equals(rootDirectory);
  }

  public void scanProjects() {
    final AbstractProjectScanner<MavenProjectInfo> projectScanner = getProjectScanner();
    try {
      getWizard().getContainer().run(true, true, monitor -> projectScanner.run(monitor));

      List<MavenProjectInfo> projects = projectScanner.getProjects();
      projectTreeViewer.setInput(projects);
      projectTreeViewer.expandAll();
      // projectTreeViewer.setAllChecked(true);
      setAllChecked(true);
      setPageComplete();
      setErrorMessage(null);
      setMessage(null);
      loadingErrorMessage = null;

      updateWorkingSet(projects);

      //mkleint: XXX this sort of error handling is rather unfortunate

      List<Throwable> errors = new ArrayList<>(projectScanner.getErrors());
      if(!errors.isEmpty()) {
        StringBuilder sb = new StringBuilder(NLS.bind(Messages.wizardImportPageScanningErrors, errors.size()));
        int n = 1;
        for(Throwable ex : errors) {
          if(ex instanceof CoreException coreEx) {
            String msg = coreEx.getStatus().getMessage();
            sb.append("\n  ").append(n).append(" ").append(msg.trim()); //$NON-NLS-1$ //$NON-NLS-2$
          } else {
            String msg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            sb.append("\n  ").append(n).append(" ").append(msg.trim()); //$NON-NLS-1$ //$NON-NLS-2$
          }
          n++ ;
        }
        loadingErrorMessage = sb.toString();
        setMessage(sb.toString(), IMessageProvider.WARNING);
      }

    } catch(InterruptedException ex) {
      // canceled
    } catch(InvocationTargetException ex) {
      Throwable e = ex.getCause() == null ? ex : ex.getCause();
      String msg;
      if(e instanceof CoreException) {
        msg = e.getMessage();
        log.error(msg, e);
      } else {
        msg = "Scanning error " + projectScanner.getDescription() + "; " + e.toString(); //$NON-NLS-1$//$NON-NLS-2$
        log.error(msg, e);
      }
      projectTreeViewer.setInput(null);
      setPageComplete(false);
      setErrorMessage(msg);
    }
  }

  private void updateWorkingSet(List<MavenProjectInfo> projects) {
    MavenProjectInfo rootProject = null;
    if(projects != null && projects.size() == 1) {
      rootProject = projects.get(0);
    }

    // check if working set name was preselected
    if(preselectedWorkingSetName != null) {
      updateWorkingSet(preselectedWorkingSetName, true);
      return;
    }

    // check if imported project(s) are nested inside existing workspace project
    String rootDirectory = rootDirectoryCombo != null ? rootDirectoryCombo.getText().trim() : null;
    if(rootDirectory != null && rootDirectory.length() > 0) {
      Set<IWorkingSet> workingSets = new HashSet<>();
      for(IContainer container : workspaceRoot.findContainersForLocationURI(new File(rootDirectory).toURI())) {
        workingSets.addAll(WorkingSets.getAssignedWorkingSets(container.getProject()));
      }
      if(workingSets.size() == 1) {
        updateWorkingSet(workingSets.iterator().next().getName(), true);
        return;
      }
    }

    // derive working set name from project name
    if(rootProject != null) {
      updateWorkingSet(ProjectConfigurationManager.getProjectName(getImportConfiguration(), rootProject.getModel()), //
          !rootProject.getProjects().isEmpty());
    } else {
      updateWorkingSet(null, false);
    }
  }

  private void updateWorkingSet(String name, boolean enabled) {
    Set<String> workingSetNames = new LinkedHashSet<>();
    if(name == null) {
      name = ""; //$NON-NLS-1$
    } else {
      workingSetNames.add(name);
    }
    workingSetNames.addAll(Arrays.asList(WorkingSets.getWorkingSets()));
    workingSetName.setItems(workingSetNames.toArray(new String[workingSetNames.size()]));
    workingSetName.setText(name);
    createWorkingSet.setSelection(enabled);
    workingSetName.setEnabled(enabled);
  }

  private void setSubtreeChecked(Object obj, boolean checked) {
    // CheckBoxTreeViewer#setSubtreeChecked is severely inefficient
    projectTreeViewer.setChecked(obj, checked);
    Object[] children = ((ITreeContentProvider) projectTreeViewer.getContentProvider()).getChildren(obj);
    if(children != null) {
      for(Object child : children) {
        setSubtreeChecked(child, checked);
      }
    }
  }

  void setAllChecked(boolean state) {
    @SuppressWarnings("unchecked")
    List<MavenProjectInfo> input = (List<MavenProjectInfo>) projectTreeViewer.getInput();
    if(input != null) {
      for(MavenProjectInfo mavenProjectInfo : input) {
        setSubtreeChecked(mavenProjectInfo, state);
      }
      updateCheckedState();
    }
  }

  void updateCheckedState() {
    Object[] elements = projectTreeViewer.getCheckedElements();
    for(Object element : elements) {
      if(element instanceof MavenProjectInfo info) {
        if(isWorkspaceFolder(info) || isAlreadyExists(info)) {
          projectTreeViewer.setChecked(info, false);
        }
      }
    }
  }

  boolean isWorkspaceFolder(MavenProjectInfo info) {
    if(info != null) {
      File pomFile = info.getPomFile();
      if(pomFile != null) {
        File parentFile = pomFile.getParentFile();
        if(parentFile.getAbsolutePath().equals(workspaceRoot.getLocation().toFile().getAbsolutePath())) {
          return true;
        }
      }
    }
    return false;
  }

  boolean isAlreadyExists(MavenProjectInfo info) {
    if(info != null) {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      String name = ProjectConfigurationManager.getProjectName(getImportConfiguration(), info.getModel());
      if(name != null && name.length() > 0) {
        IProject project = workspace.getRoot().getProject(name);
        return project.exists();
      }
    }
    return false;
  }

  /**
   * this will iterate all existing projects and return true if the absolute location URI of the old (imported) and new
   * (to-be-imported) projects match
   *
   * @param info
   * @return
   */
  boolean isAlreadyImported(MavenProjectInfo info) {
    if(info != null) {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      for(IProject project : workspace.getRoot().getProjects()) {
        URI mavenuri = info.getPomFile().getParentFile().toURI();
        //mkleint: this is sort of heuristic blah blah code. unfortunately for some reason the
        // URI returned by the eclipse code in project.getLocationURI() differs by the ending / character from the
        // java.io.File code. That results in failing match of the URIs. I've blah it by removing the ending slash.
        // please tell me there is a more sane solution!
        if(mavenuri.toString().endsWith("/")) { //$NON-NLS-1$
          try {
            mavenuri = new URI(mavenuri.toString().substring(0, mavenuri.toString().length() - 1));
          } catch(URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
          }
        }
        boolean ok = project.exists() && project.getLocationURI().equals(mavenuri);
        if(ok) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean shouldCreateWorkingSet() {
    return createWorkingSet.getSelection();
  }

  public String getWorkingSetName() {
    return workingSetName.getText();
  }

  protected AbstractProjectScanner<MavenProjectInfo> getProjectScanner() {
    MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
    if(showLocation) {
      String location = rootDirectoryCombo.getText().trim();
      if(!location.isEmpty()) {
        return new LocalProjectScanner(List.of(location), basedirRemameRequired, modelManager);
      }
    } else if(locations != null && !locations.isEmpty()) {
      return new LocalProjectScanner(locations, basedirRemameRequired, modelManager);
    }

    // empty scanner
    return new AbstractProjectScanner<>() {
      @Override
      public String getDescription() {
        return ""; //$NON-NLS-1$
      }

      @Override
      public void run(IProgressMonitor monitor) {
        // do nothing
      }
    };
  }

  /**
   * @return collection of <code>MavenProjectInfo</code>
   */
  public Collection<MavenProjectInfo> getProjects() {
    Collection<MavenProjectInfo> checkedProjects = new ArrayList<>();
    for(Object o : projectTreeViewer.getCheckedElements()) {
      checkedProjects.add((MavenProjectInfo) o);
    }

    return checkedProjects;
  }

  public MavenProjectInfo getRootProject() {
    Object[] elements = projectTreeViewer.getExpandedElements();
    return elements == null || elements.length == 0 ? null : (MavenProjectInfo) elements[0];
  }

  /**
   * @param info
   * @return
   */
  protected String validateProjectInfo(MavenProjectInfo info) {
    if(info != null) {
      if(isWorkspaceFolder(info)) {
        String projectName = ProjectConfigurationManager.getProjectName(getImportConfiguration(), info.getModel());
        return NLS.bind(Messages.wizardImportValidatorWorkspaceFolder, projectName);
      } else if(isAlreadyImported(info)) {
        String projectName = ProjectConfigurationManager.getProjectName(getImportConfiguration(), info.getModel());
        return NLS.bind(Messages.wizardImportValidatorProjectImported, projectName);
      } else if(isAlreadyExists(info)) {
        String projectName = ProjectConfigurationManager.getProjectName(getImportConfiguration(), info.getModel());
        return NLS.bind(Messages.wizardImportValidatorProjectExists, projectName);
      }
    }
    return null;
  }

  protected void validate() {
    if(projectTreeViewer.getControl().isDisposed()) {
      return;
    }
    Object[] elements = projectTreeViewer.getCheckedElements();
    for(Object element : elements) {
      if(element instanceof MavenProjectInfo info) {
        String errorMsg = validateProjectInfo(info);
        if(errorMsg != null) {
          setPageComplete(false);
          return;
        }
      }
    }
    setMessage(null);
    setPageComplete();
    projectTreeViewer.refresh();
  }

  void setPageComplete() {
    Object[] checkedElements = projectTreeViewer.getCheckedElements();
    setPageComplete(checkedElements != null && checkedElements.length > 0);
  }

  void setProjectSubtreeChecked(boolean checked) {
    ITreeSelection selection = (ITreeSelection) projectTreeViewer.getSelection();
    setSubtreeChecked(selection.getFirstElement(), checked);
    updateCheckedState();
    setPageComplete();
  }

  /**
   * ProjectLabelProvider
   */
  class ProjectLabelProvider extends LabelProvider implements IColorProvider,
      DelegatingStyledCellLabelProvider.IStyledLabelProvider {

    @Override
    public String getText(Object element) {
      if(element instanceof MavenProjectInfo info) {

        if(info.getProfiles().isEmpty()) {
          return info.getLabel() + " - " + getId(info); //$NON-NLS-1$
        }

        return info.getLabel() + " - " + getId(info) + "  " + info.getProfiles(); //$NON-NLS-1$ //$NON-NLS-2$
      }
      return super.getText(element);
    }

    private String getId(MavenProjectInfo info) {
      Model model = info.getModel();

      String groupId = model.getGroupId();
      String artifactId = model.getArtifactId();
      String version = model.getVersion();
      String packaging = model.getPackaging();

      Parent parent = model.getParent();

      if(groupId == null && parent != null) {
        groupId = parent.getGroupId();
      }
      if(groupId == null) {
        groupId = org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_inherited;
      }

      if(version == null && parent != null) {
        version = parent.getVersion();
      }
      if(version == null) {
        version = org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_inherited;
      }

      return groupId + ":" + artifactId + ":" + version + ":" + packaging; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public Color getForeground(Object element) {
      if(element instanceof MavenProjectInfo info) {
        if(isWorkspaceFolder(info)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_RED);
        } else if(isAlreadyExists(info)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        }
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }

    @Override
    public StyledString getStyledText(Object element) {
      if(element instanceof MavenProjectInfo info) {
        StyledString ss = new StyledString();
        ss.append(info.getLabel() + "  "); //$NON-NLS-1$
        ss.append(getId(info), StyledString.DECORATIONS_STYLER);
        if(!info.getProfiles().isEmpty()) {
          ss.append(" - " + info.getProfiles(), StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
        }
        return ss;
      }
      return null;
    }

  }

  /**
   * Preselected default working set name.
   *
   * @since 1.5
   */
  public void setWorkingSetName(String workingSetName) {
    this.preselectedWorkingSetName = workingSetName;
  }
}
