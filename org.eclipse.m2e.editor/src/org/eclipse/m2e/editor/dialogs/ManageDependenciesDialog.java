/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.editor.dialogs;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.components.PomHierarchyTreeWrapper;
import org.eclipse.m2e.core.ui.internal.dialogs.AbstractMavenDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.CompoundOperation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;
import org.eclipse.m2e.editor.composites.DependencyLabelProvider;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * This dialog is used to present the user with a list of dialogs that they can move to being managed under
 * "dependencyManagement". It allows them to pick the destination POM where the dependencies will be managed.
 *
 * @author rgould
 */
public class ManageDependenciesDialog extends AbstractMavenDialog {
  private static final Logger LOG = LoggerFactory.getLogger(ManageDependenciesDialog.class);

  private static final String DIALOG_SETTINGS = ManageDependenciesDialog.class.getName();

  private TableViewer dependenciesViewer;

  private final List<ParentHierarchyEntry> projectHierarchy;

  private PomHierarchyTreeWrapper pomHierarchy;

  private IStatus status;

  private final List<Object> originalSelection;

  private final ValueProvider<List<Dependency>> modelVProvider;

  /**
   * Hierarchy is a LinkedList representing the hierarchy relationship between POM represented by model and its parents.
   * The head of the list should be the child, while the tail should be the root parent, with the others in between.
   */
  public ManageDependenciesDialog(Shell parent, ValueProvider<List<Dependency>> modelVProvider,
      List<ParentHierarchyEntry> hierarchy) {
    this(parent, modelVProvider, hierarchy, null);
  }

  public ManageDependenciesDialog(Shell parent, ValueProvider<List<Dependency>> modelVProvider,
      List<ParentHierarchyEntry> hierarchy, List<Object> selection) {
    super(parent, DIALOG_SETTINGS);

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle(Messages.ManageDependenciesDialog_dialogTitle);

    this.projectHierarchy = hierarchy;
    this.originalSelection = selection;
    this.modelVProvider = modelVProvider;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    Label infoLabel = new Label(composite, SWT.WRAP);
    infoLabel.setText(Messages.ManageDependenciesDialog_dialogInfo);

    Label horizontalBar = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);

    SashForm sashForm = new SashForm(composite, SWT.SMOOTH | SWT.HORIZONTAL);
    Composite dependenciesComposite = new Composite(sashForm, SWT.NONE);

    Label selectDependenciesLabel = new Label(dependenciesComposite, SWT.NONE);
    selectDependenciesLabel.setText(Messages.ManageDependenciesDialog_selectDependenciesLabel);

    final Table dependenciesTable = new Table(dependenciesComposite, SWT.FLAT | SWT.MULTI | SWT.BORDER);
    final TableColumn column = new TableColumn(dependenciesTable, SWT.NONE);
    dependenciesTable.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        column.setWidth(dependenciesTable.getClientArea().width);
      }
    });

    Composite pomComposite = new Composite(sashForm, SWT.NONE);

    Label selectPomLabel = new Label(pomComposite, SWT.NONE);
    selectPomLabel.setText(Messages.ManageDependenciesDialog_selectPOMLabel);

    pomHierarchy = new PomHierarchyTreeWrapper(pomComposite, SWT.BORDER);
    pomHierarchy.setHierarchy(getProjectHierarchy());
    // pomsViewer = new TreeViewer(pomComposite, SWT.BORDER);

    /*
     * Configure layouts
     */

    GridLayout layout = new GridLayout(1, false);
    composite.setLayout(layout);

    GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    gridData.widthHint = 300;
    infoLabel.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    horizontalBar.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    sashForm.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    dependenciesComposite.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    dependenciesComposite.setLayout(layout);

    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    selectDependenciesLabel.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    dependenciesTable.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    pomComposite.setLayoutData(gridData);

    layout = new GridLayout(1, false);
    pomComposite.setLayout(layout);

    gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
    selectPomLabel.setLayoutData(gridData);

    gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    pomHierarchy.getTreeComposite().setLayoutData(gridData);
    //pomsViewer.getTree().setLayoutData(gridData);

    /*
     * Set up list/tree viewers
     */

    dependenciesViewer = new TableViewer(dependenciesTable);
    dependenciesViewer.setLabelProvider(new DependencyLabelProvider());
    dependenciesViewer.setContentProvider(new ListEditorContentProvider<>());
    //MNGECLIPSE-2675 only show the dependencies not already managed (decide just by absence of the version element
    List<Dependency> deps = modelVProvider.getValue();
    List<Dependency> nonManaged = new ArrayList<>();
    if(deps != null) {
      for(Dependency d : deps) {
        if(d.getVersion() != null) {
          nonManaged.add(d);
        }
      }
    }
    dependenciesViewer.setInput(nonManaged);
    dependenciesViewer.addSelectionChangedListener(new DependenciesViewerSelectionListener());

    pomHierarchy.addSelectionChangedListener(new PomViewerSelectionChangedListener());
    if(!getProjectHierarchy().isEmpty()) {
      pomHierarchy.setSelection(new StructuredSelection(pomHierarchy.getProject()));
    }

    if(originalSelection != null && !originalSelection.isEmpty()) {
      dependenciesViewer.setSelection(new StructuredSelection(originalSelection));
    }

    return composite;
  }

  @Override
  protected void computeResult() {
    final ParentHierarchyEntry currentPOM = getCurrentPOM();
    final ParentHierarchyEntry targetPOM = getTargetPOM();
    final IFile current = currentPOM.getResource();
    final IFile target = targetPOM.getResource();

    if(target == null || current == null) {
      return;
    }
    final boolean same = targetPOM.equals(currentPOM);

    final LinkedList<Dependency> modelDeps = getDependenciesList();

    /*
     * 1) Remove version values from the dependencies from the current POM
     * 2) Add dependencies to dependencyManagement of targetPOM
     */

    //First we remove the version from the original dependency
    Job perform = new Job("Updating POM file(s)") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          if(same) {
            performOnDOMDocument(new OperationTuple(current,
                new CompoundOperation(createManageOperation(modelDeps), createRemoveVersionOperation(modelDeps))));
          } else {
            performOnDOMDocument(new OperationTuple(target, createManageOperation(modelDeps)),
                new OperationTuple(current, createRemoveVersionOperation(modelDeps)));
          }
        } catch(Exception e) {
          LOG.error("Error updating managed dependencies", e);
          return Status.error("Error updating managed dependencies", e);
        }
        return Status.OK_STATUS;
      }
    };
    perform.setUser(false);
    perform.setSystem(true);
    perform.schedule();
  }

  public static Operation createRemoveVersionOperation(final List<Dependency> modelDeps) {
    return document -> {
      List<Element> deps = findChilds(findChild(document.getDocumentElement(), DEPENDENCIES), DEPENDENCY);
      for(Element dep : deps) {
        String grid = getTextValue(findChild(dep, GROUP_ID));
        String artid = getTextValue(findChild(dep, ARTIFACT_ID));
        for(Dependency modelDep : modelDeps) {
          if(modelDep.getGroupId() != null && modelDep.getGroupId().equals(grid) && modelDep.getArtifactId() != null
              && modelDep.getArtifactId().equals(artid)) {
            removeChild(dep, findChild(dep, VERSION));
          }
        }
      }
    };

  }

  public static Operation createManageOperation(final List<Dependency> modelDeps) {
    return document -> {
      List<Dependency> modelDependencies = new ArrayList<>(modelDeps);
      Element managedDepsElement = getChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT, DEPENDENCIES);
      List<Element> existing = findChilds(managedDepsElement, DEPENDENCY);
      for(Element dep : existing) {
        String artifactId = getTextValue(findChild(dep, ARTIFACT_ID));
        String groupId = getTextValue(findChild(dep, GROUP_ID));
        //cloned list, shall not modify shared resource (used by the remove operation)
        Iterator<Dependency> mdIter = modelDependencies.iterator();
        while(mdIter.hasNext()) {
          //TODO: here we iterate to find existing managed dependencies and decide not to overwrite them.
          // but this could eventually break the current project when the versions are diametrally different
          // we should have shown this information to the user in the UI in the first place (for him to decide what to do)
          Dependency md = mdIter.next();
          if(artifactId.equals(md.getArtifactId()) && groupId.equals(md.getGroupId())) {
            mdIter.remove();
            break;
          }
        }
      }
      //TODO is the version is defined by property expression, we should make sure the property is defined in the current project
      for(Dependency modelDependency : modelDependencies) {
        PomHelper.createDependency(managedDepsElement, modelDependency.getGroupId(), modelDependency.getArtifactId(),
            modelDependency.getVersion());
      }
    };

  }

  protected LinkedList<Dependency> getDependenciesList() {
    IStructuredSelection selection = (IStructuredSelection) dependenciesViewer.getSelection();

    LinkedList<Dependency> dependencies = new LinkedList<>();

    for(Object obj : selection.toArray()) {
      dependencies.add((Dependency) obj);
    }

    return dependencies;
  }

  protected List<ParentHierarchyEntry> getProjectHierarchy() {
    return this.projectHierarchy;
  }

  protected ParentHierarchyEntry getTargetPOM() {
    IStructuredSelection selection = (IStructuredSelection) pomHierarchy.getSelection();
    return (ParentHierarchyEntry) selection.getFirstElement();
  }

  protected ParentHierarchyEntry getCurrentPOM() {
    return pomHierarchy.getProject();
  }

  /**
   * Compare the list of selected dependencies against the selected targetPOM. If one of the dependencies is already
   * under dependencyManagement, but has a different version than the selected dependency, warn the user about this.
   * returns true if the user has been warned (but this method updates the status itself)
   *
   * @param model
   * @param dependencies
   */
  protected boolean checkDependencies(org.apache.maven.model.Model model, LinkedList<Dependency> dependencies) {
    if(this.status != null && this.status.getCode() == IStatus.ERROR) {
      //Don't warn the user if there is already an error
      return false;
    }
    if(model == null || model.getDependencyManagement() == null
        || model.getDependencyManagement().getDependencies() == null
        || model.getDependencyManagement().getDependencies().isEmpty()) {
      return false;
    }

    for(Dependency selectedDep : dependencies) {
      for(org.apache.maven.model.Dependency targetDep : model.getDependencyManagement().getDependencies()) {
        if(selectedDep.getGroupId().equals(targetDep.getGroupId())
            && selectedDep.getArtifactId().equals(targetDep.getArtifactId())
            && !selectedDep.getVersion().equals(targetDep.getVersion())) {
          String modelID = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
          if(targetDep.getLocation("") != null && targetDep.getLocation("").getSource() != null) { //$NON-NLS-1$ //$NON-NLS-2$
            modelID = targetDep.getLocation("").getSource().getModelId(); //$NON-NLS-1$
          }
          Object[] arguments = {selectedDep.getArtifactId() + "-" + selectedDep.getVersion(), //$NON-NLS-1$
              targetDep.getVersion(), modelID};
          String message = NLS.bind(Messages.ManageDependenciesDialog_dependencyExistsWarning, arguments);
          updateStatus(Status.warning(message));
          return true;
        }
      }
    }
    return false;
  }

  protected void checkStatus(ParentHierarchyEntry targetProject, LinkedList<Dependency> selectedDependencies) {
    if(targetProject == null || selectedDependencies.isEmpty()) {
      updateStatus(Status.error(Messages.ManageDependenciesDialog_emptySelectionError));
      return;
    }
    boolean error = false;
    if(targetProject.getFacade() == null) {
      error = true;
      updateStatus(Status.error(Messages.ManageDependenciesDialog_projectNotPresentError));
    } else {
      error = checkDependencies(targetProject.getProject().getModel(), getDependenciesList());
    }

    if(!error) {
      clearStatus();
    }
  }

  protected void clearStatus() {
    updateStatus(Status.OK_STATUS); //$NON-NLS-1$
  }

  protected class DependenciesViewerSelectionListener implements ISelectionChangedListener {
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      checkStatus(getTargetPOM(), getDependenciesList());
    }
  }

  protected class PomViewerSelectionChangedListener implements ISelectionChangedListener {
    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      checkStatus(getTargetPOM(), getDependenciesList());
    }
  }

  @Override
  protected void updateStatus(IStatus status) {
    this.status = status;
    super.updateStatus(status);
  }

  public static class DepLabelProvider extends LabelProvider implements IColorProvider {
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
      MavenProject project = null;
      if(element instanceof MavenProject mavenProject) {
        project = mavenProject;
      } else if(element instanceof Object[] array) {
        project = (MavenProject) array[0];
      } else {
        return ""; //$NON-NLS-1$
      }

      StringBuilder buffer = new StringBuilder();
      buffer.append(project.getGroupId() + " : " + project.getArtifactId() + " : " + project.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
      return buffer.toString();

    }

    @Override
    public Color getForeground(Object element) {
      if(element instanceof MavenProject project) {
        IMavenProjectFacade search = MavenPlugin.getMavenProjectRegistry().getMavenProject(project.getGroupId(),
            project.getArtifactId(), project.getVersion());
        if(search == null) {
          //This project is not in the workspace so we can't really modify it.
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      return null;
    }
  }

  public class ContentProvider implements ITreeContentProvider {
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean hasChildren(Object element) {
      Object[] children = getChildren(element);

      return children.length != 0;
    }

    @Override
    public Object getParent(Object element) {
      if(element instanceof MavenProject project) {
        return project.getParent();
      }
      return null;
    }

    /*
     * Return root element
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {

      if(inputElement instanceof LinkedList) {
        @SuppressWarnings("unchecked")
        LinkedList<MavenProject> projects = (LinkedList<MavenProject>) inputElement;
        if(projects.isEmpty()) {
          return new Object[0];
        }
        return new Object[] {projects.getLast()};
      }

      return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof MavenProject parent) {
        /*
         * Walk the hierarchy list until we find the parentElement and
         * return the previous element, which is the child.
         */
        if(getProjectHierarchy().size() == 1) {
          //No parent exists, only one element in the tree
          return new Object[0];
        }

        if(getProjectHierarchy().get(0).getProject().equals(parent)) {
          //We are the final child
          return new Object[0];
        }

        ListIterator<ParentHierarchyEntry> iter = getProjectHierarchy().listIterator();
        while(iter.hasNext()) {
          ParentHierarchyEntry next = iter.next();
          if(next.getProject().equals(parent)) {
            iter.previous();
            ParentHierarchyEntry previous = iter.previous();
            return new Object[] {previous};
          }
        }
      }
      return new Object[0];
    }
  }
}
