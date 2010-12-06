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

package org.eclipse.m2e.core.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.Messages;


/**
 * The ProjectsImportPage is the page that allows the user to import projects from a particular location.
 */
public class ProjectsImportPage extends WizardPage implements IOverwriteQuery {

  String location;
  
  CheckboxTreeViewer projectsList;
  
  IProject[] wsProjects;
  
  ProjectRecord[] selectedProjects = new ProjectRecord[0];


  public ProjectsImportPage(String location) {
    super("wizardExternalProjectsPage"); //$NON-NLS-1$
    this.location = location;
    
    setTitle(Messages.ProjectsImportPage_title);
    setDescription(Messages.ProjectsImportPage_desc);
    setPageComplete(false);
  }

  public void createControl(Composite parent) {
    initializeDialogUnits(parent);

    Composite workArea = new Composite(parent, SWT.NONE);
    setControl(workArea);

    workArea.setLayout(new GridLayout());
    workArea.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

    createProjectsList(workArea);
    createOptionsArea(workArea);
    Dialog.applyDialogFont(workArea);
    
    updateProjectsList(location);
  }

  /**
   * Create the area with the extra options.
   * 
   * @param workArea
   */
  private void createOptionsArea(Composite workArea) {
  }

  /**
   * Create the checkbox list for the found projects.
   * 
   * @param workArea
   */
  private void createProjectsList(Composite workArea) {
    Label title = new Label(workArea, SWT.NONE);
    title.setText(Messages.ProjectsImportPage_lstProjects);

    Composite listComposite = new Composite(workArea, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.makeColumnsEqualWidth = false;
    listComposite.setLayout(layout);

    listComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH));

    projectsList = new CheckboxTreeViewer(listComposite, SWT.BORDER);
    GridData listData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
    projectsList.getControl().setLayoutData(listData);

    projectsList.setContentProvider(new ITreeContentProvider() {

      public Object[] getChildren(Object parentElement) {
        return null;
      }

      public Object[] getElements(Object inputElement) {
        return getValidProjects();
      }

      public boolean hasChildren(Object element) {
        return false;
      }

      public Object getParent(Object element) {
        return null;
      }

      public void dispose() {

      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    });

    projectsList.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        ProjectRecord projectRecord = (ProjectRecord) element;
        return projectRecord.getProjectName() + " - " + projectRecord.projectFile.getParentFile().getAbsolutePath(); //$NON-NLS-1$
      }
    });

    projectsList.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        setPageComplete(projectsList.getCheckedElements().length > 0);
      }
    });

    projectsList.setInput(this);
    projectsList.setComparator(new ViewerComparator());
    createSelectionButtons(listComposite);
  }

  /**
   * Create the selection buttons in the listComposite.
   * 
   * @param listComposite
   */
  private void createSelectionButtons(Composite listComposite) {
    Composite buttonsComposite = new Composite(listComposite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    buttonsComposite.setLayout(layout);

    buttonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

    Button selectAll = new Button(buttonsComposite, SWT.PUSH);
    selectAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    selectAll.setText(Messages.ProjectsImportPage_btnSelect);
    selectAll.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        projectsList.setCheckedElements(selectedProjects);
        setPageComplete(projectsList.getCheckedElements().length > 0);
      }
    });
    Dialog.applyDialogFont(selectAll);
    setButtonLayoutData(selectAll);

    Button deselectAll = new Button(buttonsComposite, SWT.PUSH);
    deselectAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    deselectAll.setText(Messages.ProjectsImportPage_btnDeselect);
    deselectAll.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        projectsList.setCheckedElements(new Object[0]);
        setPageComplete(false);
      }
    });
    Dialog.applyDialogFont(deselectAll);
    setButtonLayoutData(deselectAll);

    Button refresh = new Button(buttonsComposite, SWT.PUSH);
    refresh.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    refresh.setText(Messages.ProjectsImportPage_btnRefresh);
    refresh.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateProjectsList(location);
      }
    });
    Dialog.applyDialogFont(refresh);
    setButtonLayoutData(refresh);
  }

  /**
   * Update the list of projects based on path. Method declared public only for test suite.
   * 
   * @param path
   */
  void updateProjectsList(final String path) {
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask(Messages.ProjectsImportPage_task_search, 100);
          File directory = new File(path);
          selectedProjects = new ProjectRecord[0];
          Collection<File> files = new ArrayList<File>();
          monitor.worked(10);

          if(directory.isDirectory()) {
            if(!collectProjectFilesFromDirectory(files, directory, null, monitor)) {
              return;
            }
            selectedProjects = new ProjectRecord[files.size()];
            int index = 0;
            monitor.worked(50);
            monitor.subTask(Messages.ProjectsImportPage_task_processing);
            for(File file : files) {
              selectedProjects[index] = new ProjectRecord(file);
              index++ ;
            }
          } else {
            monitor.worked(60);
          }
          monitor.done();
        }

      });
    } catch(InvocationTargetException e) {
      MavenLogger.log(e.getMessage(), e);
    } catch(InterruptedException e) {
      // Nothing to do if the user interrupts.
    }

    projectsList.refresh(true);
    projectsList.setCheckedElements(getValidProjects());
    if(getValidProjects().length < selectedProjects.length) {
      setMessage(Messages.ProjectsImportPage_message, WARNING);
    } else {
      setMessage(null, WARNING);
    }
    setPageComplete(projectsList.getCheckedElements().length > 0);
  }

  /**
   * Collect the list of .project files that are under directory into files.
   * 
   * @param files
   * @param directory
   * @param directoriesVisited Set of canonical paths of directories, used as recursion guard
   * @param monitor The monitor to report to
   * @return boolean <code>true</code> if the operation was completed.
   */
  boolean collectProjectFilesFromDirectory(Collection<File> files, File directory, Set<String> directoriesVisited,
      IProgressMonitor monitor) {
    if(monitor.isCanceled()) {
      return false;
    }
    
    monitor.subTask(NLS.bind(Messages.ProjectsImportPage_task_checking, directory.getPath()));
    File[] contents = directory.listFiles();
    if(contents == null)
      return false;

    // Initialize recursion guard for recursive symbolic links
    if(directoriesVisited == null) {
      directoriesVisited = new HashSet<String>();
      try {
        directoriesVisited.add(directory.getCanonicalPath());
      } catch(IOException exception) {
        MavenLogger.log(exception.toString(), exception);
      }
    }

    // first look for project description files
    final String dotProject = IProjectDescription.DESCRIPTION_FILE_NAME;
    for(File file : contents) {
      if(file.isFile() && file.getName().equals(dotProject)) {
        files.add(file);
        // don't search sub-directories since we can't have nested
        // projects
        return true;
      }
    }
    
    // no project description found, so recurse into sub-directories
    for(File file : contents) {
      if(file.isDirectory() && !IMavenConstants.METADATA_FOLDER.equals(file.getName())) {
        try {
          String canonicalPath = file.getCanonicalPath();
          if(!directoriesVisited.add(canonicalPath)) {
            // already been here --> do not recurse
            continue;
          }
        } catch(IOException exception) {
          MavenLogger.log(exception.toString(), exception);
        }
        collectProjectFilesFromDirectory(files, file, directoriesVisited, monitor);
      }
    }
    return true;
  }

  /**
   * Create the selected projects
   * 
   * @return boolean <code>true</code> if all project creations were successful.
   */
  public boolean createProjects() {
    final Object[] selected = projectsList.getCheckedElements();
    
    WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
      protected void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
          monitor.beginTask("", selected.length); //$NON-NLS-1$
          if(monitor.isCanceled()) {
            throw new OperationCanceledException();
          }
          
          for(int i = 0; i < selected.length; i++ ) {
            createExistingProject((ProjectRecord) selected[i], new SubProgressMonitor(monitor, 1));
          }
        } finally {
          monitor.done();
        }
      }
    };
    
    // run the new project creation operation
    try {
      getContainer().run(true, true, op);
    } catch(InterruptedException e) {
      return false;
    } catch(InvocationTargetException e) {
      // one of the steps resulted in a core exception
      Throwable t = e.getTargetException();
      String message = Messages.ProjectsImportPage_error_creation;
      IStatus status;
      if(t instanceof CoreException) {
        status = ((CoreException) t).getStatus();
      } else {
        status = new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, 1, message, t);
      }
      ErrorDialog.openError(getShell(), message, null, status);
      return false;
    }
    
    return true;
  }

  /**
   * Performs clean-up if the user cancels the wizard without doing anything
   */
  public void performCancel() {
  }

  /**
   * Create the project described in record. If it is successful return true.
   * 
   * @param record
   * @return boolean <code>true</code> if successful
   * @throws InterruptedException
   */
  boolean createExistingProject(final ProjectRecord record, IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException {
    String projectName = record.getProjectName();
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject project = workspace.getRoot().getProject(projectName);
    if(record.description == null) {
      // error case
      record.description = workspace.newProjectDescription(projectName);
      IPath locationPath = new Path(record.projectFile.getAbsolutePath());

      // If it is under the root use the default location
      if(Platform.getLocation().isPrefixOf(locationPath)) {
        record.description.setLocation(null);
      } else {
        record.description.setLocation(locationPath);
      }
    } else {
      record.description.setName(projectName);
    }

    try {
      monitor.beginTask(Messages.ProjectsImportPage_task_creating, 100);
      
      @SuppressWarnings("deprecation")
      IPath projectPath = record.description.getLocation();
      if(projectPath!=null) {
        MavenConsole console = MavenPlugin.getDefault().getConsole();
        
        IWorkspaceRoot root = workspace.getRoot();
        
        if(projectPath.toFile().equals(root.getLocation().toFile())) {
          console.logError("Can't create project " + projectName + " at Workspace folder");
          return false;
        }
        
        if(projectPath.removeLastSegments(1).toFile().equals(root.getLocation().toFile())) {
          // rename dir in workspace to match expected project name
          if(!projectPath.equals(root.getLocation().append(projectName))) {
            File projectDir = projectPath.toFile();
            File newProject = new File(projectDir.getParent(), projectName);
            if(!projectDir.renameTo(newProject)) {
              MavenLogger.log("Can't rename " + projectDir + " to " + newProject, null);
            }
            record.description.setLocation(null);
          }
        }
      }
      
      project.create(record.description, new SubProgressMonitor(monitor, 30));
      project.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 70));
      
    } catch(CoreException e) {
      throw new InvocationTargetException(e);
    } finally {
      monitor.done();
    }

    return true;
  }

  /**
   * The <code>WizardDataTransfer</code> implementation of this <code>IOverwriteQuery</code> method asks the user
   * whether the existing resource at the given path should be overwritten.
   * 
   * @param pathString
   * @return the user's reply: one of <code>"YES"</code>, <code>"NO"</code>, <code>"ALL"</code>, or
   *         <code>"CANCEL"</code>
   */
  public String queryOverwrite(String pathString) {
    Path path = new Path(pathString);
  
    String messageString;
    // Break the message up if there is a file name and a directory
    // and there are at least 2 segments.
    if(path.getFileExtension() == null || path.segmentCount() < 2) {
      messageString = NLS.bind(Messages.ProjectsImportPage_overwrite, pathString);
    } else {
      messageString = NLS.bind(Messages.ProjectsImportPage_overwrite2,
          path.lastSegment(), path.removeLastSegments(1).toOSString());
    }
  
    final MessageDialog dialog = new MessageDialog(getContainer().getShell(), Messages.ProjectsImportPage_dialog_title, null,
        messageString, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL,
            IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.NO_TO_ALL_LABEL,
            IDialogConstants.CANCEL_LABEL}, 0);
    String[] response = new String[] {YES, ALL, NO, NO_ALL, CANCEL};
    // run in syncExec because callback is from an operation,
    // which is probably not running in the UI thread.
    getControl().getDisplay().syncExec(new Runnable() {
      public void run() {
        dialog.open();
      }
    });
    return dialog.getReturnCode() < 0 ? CANCEL : response[dialog.getReturnCode()];
  }

  /**
   * Retrieve all the projects in the current workspace.
   * 
   * @return IProject[] array of IProject in the current workspace
   */
  private IProject[] getProjectsInWorkspace() {
    if(wsProjects == null) {
      wsProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }
    return wsProjects;
  }

  /**
   * Method used for test suite.
   * 
   * @return CheckboxTreeViewer the viewer containing all the projects found
   */
  public CheckboxTreeViewer getProjectsList() {
    return projectsList;
  }

  /**
   * Get the array of valid project records that can be imported from the source workspace or archive, selected by the
   * user. If a project with the same name exists in both the source workspace and the current workspace, it will not
   * appear in the list of projects to import and thus cannot be selected for import. Method declared public for test
   * suite.
   * 
   * @return ProjectRecord[] array of projects that can be imported into the workspace
   */
  public ProjectRecord[] getValidProjects() {
    List<ProjectRecord> validProjects = new ArrayList<ProjectRecord>();
    for(ProjectRecord projectRecord : selectedProjects) {
      if(!isProjectInWorkspace(projectRecord.getProjectName())) {
        validProjects.add(projectRecord);
      }
    }
    return validProjects.toArray(new ProjectRecord[validProjects.size()]);
  }

  /**
   * Determine if the project with the given name is in the current workspace.
   * 
   * @param projectName String the project name to check
   * @return boolean true if the project with the given name is in this workspace
   */
  private boolean isProjectInWorkspace(String projectName) {
    if(projectName == null) {
      return false;
    }
    IProject[] workspaceProjects = getProjectsInWorkspace();
    for(int i = 0; i < workspaceProjects.length; i++ ) {
      if(projectName.equals(workspaceProjects[i].getName())) {
        return true;
      }
    }
    return false;
  }

  
  
  /**
   * Class declared public only for test suite.
   */
  public static class ProjectRecord {
    File projectFile;
  
    String projectName;
  
    IProjectDescription description;
  
    /**
     * Create a record for a project based on the info in the file.
     * 
     * @param file
     */
    ProjectRecord(File file) {
      projectFile = file;
      setProjectName();
    }
  
    /**
     * Set the name of the project based on the projectFile.
     */
    private void setProjectName() {
      IProjectDescription newDescription = null;
      try {
        IPath path = new Path(projectFile.getPath());
        // if the file is in the default location, use the directory
        // name as the project name
        newDescription = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
        
        if(isDefaultLocation(path)) {
          // projectName = path.segment(path.segmentCount() - 2);
          // newDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
        }
      } catch(CoreException e) {
        // no good couldn't get the name
      }
  
      if(newDescription == null) {
        this.description = null;
        projectName = ""; //$NON-NLS-1$
      } else {
        this.description = newDescription;
        projectName = this.description.getName();
      }
    }
  
    /**
     * Returns whether the given project description file path is in the default location for a project
     * 
     * @param path The path to examine
     * @return Whether the given path is the default location for a project
     */
    private boolean isDefaultLocation(IPath path) {
      // The project description file must at least be within the project, which is within the workspace location
      return path.segmentCount() > 1 && path.removeLastSegments(2).toFile().equals(Platform.getLocation().toFile());
    }
  
    /**
     * Get the name of the project
     * 
     * @return String
     */
    public String getProjectName() {
      return projectName;
    }
  }
  
}
