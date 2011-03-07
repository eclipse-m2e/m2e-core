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

package org.eclipse.m2e.core.ui.internal.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.AbstractProjectScanner;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.LifecycleMappingConfiguration;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

  private IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

  private WorkingSetGroup workingSetGroup;

  private boolean showLocation = true;

  private final List<IWorkingSet> workingSets;
  
  private boolean comboCharged = false;

  private String loadingErrorMessage;

  protected MavenImportWizardPage(ProjectImportConfiguration importConfiguration, List<IWorkingSet> workingSets) {
    super("MavenProjectImportWizardPage", importConfiguration);
    this.workingSets = workingSets;
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
  
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    setControl(composite);

    if(showLocation || locations==null || locations.isEmpty()) {
      final Label selectRootDirectoryLabel = new Label(composite, SWT.NONE);
      selectRootDirectoryLabel.setLayoutData(new GridData());
      selectRootDirectoryLabel.setText(Messages.wizardImportPageRoot);

      rootDirectoryCombo = new Combo(composite, SWT.NONE);
      rootDirectoryCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      rootDirectoryCombo.addSelectionListener(new SelectionAdapter() {
        public void widgetDefaultSelected(SelectionEvent e) {
          comboCharged = false;
          if(rootDirectoryCombo.getText().trim().length() > 0) {
            scanProjects();
          }
        }
        
        public void widgetSelected(SelectionEvent e) {
          comboCharged = false;
          if(rootDirectoryCombo.getText().trim().length() > 0) {
            //in runnable to have the combo popup collapse before disabling controls.
            Display.getDefault().asyncExec(new Runnable() {
              public void run() {
                scanProjects();
              }
            });
          }
        }
      });
      rootDirectoryCombo.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          //this relies on having the modify event arrive before the selection event.
          comboCharged = true;
        }
      });
      rootDirectoryCombo.addFocusListener(new FocusAdapter() {
        public void focusLost(FocusEvent e) {
          if (comboCharged && rootDirectoryCombo.getText().trim().length() > 0) {
            scanProjects();
          }
          comboCharged = false;
        }
      });
      rootDirectoryCombo.setFocus();
      addFieldWithHistory("rootDirectory", rootDirectoryCombo); //$NON-NLS-1$
      
      if(locations!=null && locations.size()==1) {
        rootDirectoryCombo.setText(locations.get(0));
        comboCharged = false;
      }

      final Button browseButton = new Button(composite, SWT.NONE);
      browseButton.setText(Messages.wizardImportPageBrowse);
      browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
      browseButton.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
          dialog.setText(Messages.wizardImportPageSelectRootFolder);
          String path = rootDirectoryCombo.getText();
          if(path.length()==0) {
            path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
          }
          dialog.setFilterPath(path);

          String result = dialog.open();
          if(result != null) {
            rootDirectoryCombo.setText(result);
            comboCharged = false;
            scanProjects();
          }
        }
      });
    }

    final Label projectsLabel = new Label(composite, SWT.NONE);
    projectsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    projectsLabel.setText(Messages.wizardImportPageProjects);

    projectTreeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);

    projectTreeViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        projectTreeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
        updateCheckedState();
        Object[] checkedElements = projectTreeViewer.getCheckedElements();
        setPageComplete(checkedElements != null && checkedElements.length > 0);
      }
    });
    
    projectTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        if (selection.getFirstElement() != null) {
          String  errorMsg = validateProjectInfo((MavenProjectInfo) selection.getFirstElement());
          if (errorMsg != null) {
            setMessage(errorMsg, IMessageProvider.WARNING);
          } else {
          //TODO if no error on current, shall show any existing general errors if found..
            setMessage(loadingErrorMessage, IMessageProvider.WARNING);
          }
        } else {
          //TODO if on current selection, shall show any existing general errors if existing..
          setMessage(loadingErrorMessage, IMessageProvider.WARNING);
        }
      }});

    projectTreeViewer.setContentProvider(new ITreeContentProvider() {

      public Object[] getElements(Object element) {
        if(element instanceof List) {
          @SuppressWarnings("unchecked")
          List<MavenProjectInfo> projects = (List<MavenProjectInfo>) element;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        }
        return EMPTY;
      }

      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof List) {
          @SuppressWarnings("unchecked")
          List<MavenProjectInfo> projects = (List<MavenProjectInfo>) parentElement;
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        } else if(parentElement instanceof MavenProjectInfo) {
          MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
          Collection<MavenProjectInfo> projects = mavenProjectInfo.getProjects();
          return projects.toArray(new MavenProjectInfo[projects.size()]);
        }
        return EMPTY;
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object parentElement) {
        if(parentElement instanceof List) {
          List<?> projects = (List<?>) parentElement;
          return !projects.isEmpty();
        } else if(parentElement instanceof MavenProjectInfo) {
          MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
          return !mavenProjectInfo.getProjects().isEmpty();
        }
        return false;
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    projectTreeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new ProjectLabelProvider()));

    final Tree projectTree = projectTreeViewer.getTree();
    GridData projectTreeData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 3);
    projectTreeData.heightHint = 250;
    projectTreeData.widthHint = 500;
    projectTree.setLayoutData(projectTreeData);

    final Button selectAllButton = new Button(composite, SWT.NONE);
    selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    selectAllButton.setText(Messages.wizardImportPageSelectAll);
    selectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        projectTreeViewer.expandAll();
        setAllChecked(true);
        // projectTreeViewer.setSubtreeChecked(projectTreeViewer.getInput(), true);
        validate();
      }
    });

    final Button deselectAllButton = new Button(composite, SWT.NONE);
    deselectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    deselectAllButton.setText(Messages.wizardImportPageDeselectAll);
    deselectAllButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        setAllChecked(false);
        // projectTreeViewer.setSubtreeChecked(projectTreeViewer.getInput(), false);
        setPageComplete(false);
      }
    });

    final Button refreshButton = new Button(composite, SWT.NONE);
    refreshButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    refreshButton.setText(Messages.wizardImportPageRefresh);
    refreshButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        scanProjects();
      }
    });

    this.workingSetGroup = new WorkingSetGroup(composite, workingSets, getShell());
    
    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));
    resolverConfigurationComponent.template.addModifyListener(new ModifyListener(){
      public void modifyText(ModifyEvent arg0) {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            validate();
          }
        });
      }
    });
    
    if(locations!=null && !locations.isEmpty()) {
      scanProjects();
    }
  }

  public void dispose() {
    super.dispose();
    workingSetGroup.dispose();
  }
  
  protected void scanProjects() {
    final AbstractProjectScanner<MavenProjectInfo> projectScanner = getProjectScanner();
    final CoreException[] analyzingExc = new CoreException[1]; 
    try {
      getWizard().getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          projectScanner.run(monitor);
          try {
            ((MavenImportWizard) getWizard()).scanProjects(getProjects(projectScanner.getProjects()), monitor);
          } catch (CoreException x ) {
            analyzingExc[0] = x; 
          }
        }
        
        //this collects all projects for analyzing..
        List<MavenProjectInfo> getProjects(Collection<MavenProjectInfo> input) {
          List<MavenProjectInfo> toRet = new ArrayList<MavenProjectInfo>();
          for (MavenProjectInfo info : input) {
            toRet.add(info);
            toRet.addAll(getProjects(info.getProjects()));
          }
          return toRet;
        }
        
      });

      projectTreeViewer.setInput(projectScanner.getProjects());
      projectTreeViewer.expandAll();
      // projectTreeViewer.setAllChecked(true);
      setAllChecked(true);
      Object[] checkedElements = projectTreeViewer.getCheckedElements();
      setPageComplete(checkedElements != null && checkedElements.length > 0);
      setErrorMessage(null);
      setMessage(null);
      loadingErrorMessage = null;
      //mkleint: XXX this sort of error handling is rather unfortunate
      List<Throwable> errors = projectScanner.getErrors();
      if(!errors.isEmpty() || analyzingExc[0] != null) {
        StringBuffer sb = new StringBuffer(NLS.bind(Messages.wizardImportPageScanningErrors, errors.size() + (analyzingExc[0] != null ? 1 : 0)));
        int n = 1;
        for(Throwable ex : errors) {
          if(ex instanceof CoreException) {
            String msg = ((CoreException) ex).getStatus().getMessage();
            sb.append("\n  ").append(n).append(" ").append(msg.trim()); //$NON-NLS-1$ //$NON-NLS-2$
            
          } else {
            String msg = ex.getMessage()==null ? ex.toString() : ex.getMessage();
            sb.append("\n  ").append(n).append(" ").append(msg.trim()); //$NON-NLS-1$ //$NON-NLS-2$
          }
          n++;
        }
        if (analyzingExc[0] != null) {
          sb.append("\n  ").append(n).append(analyzingExc[0].getStatus().getMessage());
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
        msg = "Scanning error " + projectScanner.getDescription() + "; " + e.toString(); //$NON-NLS-2$
        log.error(msg, e);
      }
      projectTreeViewer.setInput(null);
      setPageComplete(false);
      setErrorMessage(msg);
    }
  }

  void setAllChecked(boolean state) {
    @SuppressWarnings("unchecked")
    List<MavenProjectInfo> input = (List<MavenProjectInfo>) projectTreeViewer.getInput();
    if(input!=null) {
      for(MavenProjectInfo mavenProjectInfo : input) {
        projectTreeViewer.setSubtreeChecked(mavenProjectInfo, state);
      }
      updateCheckedState();
    }
  }
  
  void updateCheckedState() {
    Object[] elements = projectTreeViewer.getCheckedElements();
    for(int i = 0; i < elements.length; i++ ) {
      Object element = elements[i];
      if(element instanceof MavenProjectInfo) {
        MavenProjectInfo info = (MavenProjectInfo) element;
        if(isWorkspaceFolder(info) || isAlreadyExists(info)) {
          projectTreeViewer.setChecked(info, false);
        }
      }
    }
  }

  boolean isWorkspaceFolder(MavenProjectInfo info) {
    if(info!=null) {
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
    if(info!=null) {
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      String name = getImportConfiguration().getProjectName(info.getModel());
      if (name != null && name.length() > 0) {
        IProject project = workspace.getRoot().getProject(name);
        return project.exists();
      }
    }
    return false;
  }

  protected AbstractProjectScanner<MavenProjectInfo> getProjectScanner() {
    File root = workspaceRoot.getLocation().toFile();
    MavenPlugin mavenPlugin = MavenPlugin.getDefault();
    MavenModelManager modelManager = mavenPlugin.getMavenModelManager();
    if(showLocation || locations == null || locations.isEmpty()) {
      return new LocalProjectScanner(root, rootDirectoryCombo.getText(), false, modelManager);
    }
    return new LocalProjectScanner(root, locations, true, modelManager);
  }

  /**
   * @return collection of <code>MavenProjectInfo</code>
   */
  public Collection<MavenProjectInfo> getProjects() {
    Collection<MavenProjectInfo> checkedProjects = new ArrayList<MavenProjectInfo>();
    for(Object o : projectTreeViewer.getCheckedElements()) {
      checkedProjects.add((MavenProjectInfo) o);
    }

    return checkedProjects;
  }

  /**
   * 
   * @param info
   * @return
   */
  protected String validateProjectInfo(MavenProjectInfo info) {
    if(info!=null) {
      if(isWorkspaceFolder(info)) {
        String projectName = getImportConfiguration().getProjectName(info.getModel());
        return NLS.bind(Messages.wizardImportValidatorWorkspaceFolder, projectName); //$NON-NLS-1$
      } else if(isAlreadyExists(info)) {
        String projectName = getImportConfiguration().getProjectName(info.getModel());
        return NLS.bind(Messages.wizardImportValidatorProjectExists, projectName); //$NON-NLS-1$
      }
    }
    return null;
  }

  protected void validate() {
    Object[] elements = projectTreeViewer.getCheckedElements();
    for(int i = 0; i < elements.length; i++ ) {
      Object element = elements[i];
      if(element instanceof MavenProjectInfo) {
        String errorMsg = validateProjectInfo((MavenProjectInfo) element); 
        if (errorMsg != null) {
          setPageComplete(false);
          return;
        }
      }
    }
    setMessage(null);
    setPageComplete(projectTreeViewer.getCheckedElements().length > 0);
    projectTreeViewer.refresh();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
   */
  @Override
  public IWizardPage getNextPage() {
    IWizardPage next = super.getNextPage();
    MavenImportWizard wizard = (MavenImportWizard)getWizard();
    LifecycleMappingConfiguration config = wizard.getMappingConfiguration();
    if (config == null || config.isMappingComplete()) {
      next = null;
    }
    return next;
  }

  /**
   * ProjectLabelProvider
   */
  class ProjectLabelProvider extends LabelProvider implements IColorProvider, DelegatingStyledCellLabelProvider.IStyledLabelProvider {

    public String getText(Object element) {
      if(element instanceof MavenProjectInfo) {
        MavenProjectInfo info = (MavenProjectInfo) element;
        
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

      if(groupId==null && parent!=null) {
        groupId = parent.getGroupId();
      }
      if(groupId==null) {
        groupId = org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_inherited;
      }
      
      if(version==null && parent!=null) {
        version = parent.getVersion();
      }
      if(version==null) {
        version = org.eclipse.m2e.core.ui.internal.Messages.MavenImportWizardPage_inherited;
      }

      return groupId + ":" + artifactId + ":" + version + ":" + packaging; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
     */
    public Color getForeground(Object element) {
      if(element instanceof MavenProjectInfo) {
        MavenProjectInfo info = (MavenProjectInfo) element;
        if(isWorkspaceFolder(info)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_RED);
        } else if(isAlreadyExists(info)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        }
      }
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
     */
    public Color getBackground(Object element) {
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
     */
    public StyledString getStyledText(Object element) {
      if(element instanceof MavenProjectInfo) {
        MavenProjectInfo info = (MavenProjectInfo) element;
        StyledString ss = new StyledString();
        ss.append(info.getLabel() + "  ");
        ss.append(getId(info), StyledString.DECORATIONS_STYLER);
        if (! info.getProfiles().isEmpty()) {
          ss.append(" - " + info.getProfiles(), StyledString.QUALIFIER_STYLER );
        }
        return ss;
      }
      return null;
    }

  }

}
