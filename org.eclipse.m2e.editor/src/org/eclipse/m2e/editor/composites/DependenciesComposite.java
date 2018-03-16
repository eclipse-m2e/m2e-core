/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.composites;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CLASSIFIER;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.OPTIONAL;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SCOPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SYSTEM_PATH;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.TYPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.WorkbenchJob;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.ui.internal.dialogs.EditDependencyDialog;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;
import org.eclipse.m2e.core.ui.internal.util.ParentGatherer;
import org.eclipse.m2e.core.ui.internal.util.ParentHierarchyEntry;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.dialogs.ManageDependenciesDialog;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;


/**
 * @author Eugene Kuleshov
 */
@SuppressWarnings("synthetic-access")
public class DependenciesComposite extends Composite {
  private static final Logger log = LoggerFactory.getLogger(DependenciesComposite.class);

  protected MavenPomEditorPage editorPage;

  MavenPomEditor pomEditor;

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  // controls

  PropertiesListComposite<Dependency> dependencyManagementEditor;

  //This ListComposite takes both m2e and maven Dependencies
  DependenciesListComposite<Object> dependenciesEditor;

  private final List<String> temporaryRemovedDependencies = new ArrayList<String>();

  Button dependencySelectButton;

  Action dependencySelectAction;

  SearchControl searchControl;

  SearchMatcher searchMatcher;

  DependencyFilter searchFilter;

  Action openWebPageAction;

  final DependencyLabelProvider dependencyLabelProvider = new DependencyLabelProvider(true);

  final DependencyLabelProvider dependencyManagementLabelProvider = new DependencyLabelProvider();

  protected boolean showInheritedDependencies = false;

  final ListEditorContentProvider<Object> dependenciesContentProvider = new ListEditorContentProvider<Object>();

  DependenciesComparator<Object> dependenciesComparator;

  final ListEditorContentProvider<Dependency> dependencyManagementContentProvider = new ListEditorContentProvider<Dependency>();

  DependenciesComparator<Dependency> dependencyManagementComparator;

  private List<DependenciesComposite.Dependency> dependencies;

  private List<DependenciesComposite.Dependency> manageddependencies;

  public DependenciesComposite(Composite composite, MavenPomEditorPage editorPage, int flags,
      MavenPomEditor pomEditor) {
    super(composite, flags);
    this.editorPage = editorPage;
    this.pomEditor = pomEditor;
    createComposite();
    editorPage.initPopupMenu(dependenciesEditor.getViewer(), ".dependencies"); //$NON-NLS-1$
    editorPage.initPopupMenu(dependencyManagementEditor.getViewer(), ".dependencyManagement"); //$NON-NLS-1$
  }

  private void createComposite() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.makeColumnsEqualWidth = true;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    GridData horizontalCompositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    horizontalCompositeGridData.heightHint = 200;
    horizontalSash.setLayoutData(horizontalCompositeGridData);
    toolkit.adapt(horizontalSash, true, true);

    createDependenciesSection(horizontalSash);
    createDependencyManagementSection(horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createDependenciesSection(SashForm verticalSash) {
    Section dependenciesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    dependenciesSection.marginWidth = 3;
    dependenciesSection.setText(Messages.DependenciesComposite_sectionDependencies);

    dependenciesComparator = new DependenciesComparator<Object>();
    dependenciesContentProvider.setComparator(dependenciesComparator);

    dependenciesEditor = new DependenciesListComposite<Object>(dependenciesSection, SWT.NONE, true);
    dependenciesEditor.setCellLabelProvider(new DelegatingStyledCellLabelProvider(dependencyLabelProvider));
    dependenciesEditor.setContentProvider(dependenciesContentProvider);

    dependenciesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        final List<Object> dependencyList = dependenciesEditor.getSelection();
        try {
          editorPage.performEditOperation(new Operation() {
            public void process(Document document) {
              Element deps = findChild(document.getDocumentElement(), DEPENDENCIES);
              if(deps == null) {
                //TODO log
                return;
              }
              for(Object dependency : dependencyList) {
                if(dependency instanceof Dependency) {
                  Element dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, ((Dependency) dependency).groupId),
                      childEquals(ARTIFACT_ID, ((Dependency) dependency).artifactId));
                  removeChild(deps, dep);
                }
              }
              removeIfNoChildElement(deps);
            }
          }, log, "error removing dependencies");
        } finally {
          setDependenciesInput();
        }
      }
    });

    dependenciesEditor.setPropertiesListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Object selection = dependenciesEditor.getSelection().get(0);
        if(selection instanceof Dependency) {
          Dependency dependency = (Dependency) selection;
          EditDependencyDialog d = new EditDependencyDialog(getShell(), false, editorPage.getProject(),
              editorPage.getPomEditor().getMavenProject());
          d.setDependency(toApacheDependency(dependency));
          if(d.open() == Window.OK) {
            try {
              editorPage.performEditOperation(d.getEditOperation(), log, "Error updating dependency");
            } finally {
              setDependenciesInput();
              dependenciesEditor.setSelection(Collections.singletonList((Object) dependency));
            }
          }
        } else if(selection instanceof org.apache.maven.model.Dependency) {
          /*
           * TODO: Support editing or displaying of inherited/managed dependencies.
           */
        }
      }

    });

    dependenciesSection.setClient(dependenciesEditor);
    toolkit.adapt(dependenciesEditor);
    toolkit.paintBordersFor(dependenciesEditor);

    dependenciesEditor.setManageButtonListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          openManageDependenciesDialog();
        } catch(InvocationTargetException e1) {
          MavenEditorPlugin.getDefault().getLog()
              .log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Error: ", e1)); //$NON-NLS-1$
        } catch(InterruptedException e1) {
          MavenEditorPlugin.getDefault().getLog()
              .log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Error: ", e1)); //$NON-NLS-1$
        }
      }
    });

    dependenciesEditor.setAddButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        final MavenRepositorySearchDialog addDepDialog = MavenRepositorySearchDialog.createSearchDependencyDialog(
            getShell(), Messages.DependenciesComposite_action_selectDependency,
            editorPage.getPomEditor().getMavenProject(), editorPage.getProject(), false);

        if(addDepDialog.open() == Window.OK) {
          final IndexedArtifactFile dep = (IndexedArtifactFile) addDepDialog.getFirstResult();
          final String selectedScope = addDepDialog.getSelectedScope();
          try {
            editorPage.performEditOperation(new Operation() {
              public void process(Document document) {
                Element depsEl = getChild(document.getDocumentElement(), DEPENDENCIES);
                PomHelper.addOrUpdateDependency(depsEl, dep.group, dep.artifact,
                    isManaged(dep.group, dep.artifact, dep.version) ? null : dep.version, dep.type, selectedScope,
                    dep.classifier);
              }
            }, log, "errror adding dependency");
          } finally {
            setDependenciesInput();
            List<Dependency> deps = getDependencies();
            if(deps.size() > 0) {
              dependenciesEditor.setSelection(Collections.<Object> singletonList(deps.get(deps.size() - 1)));
            }
          }
        }
      }

    });

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);

    modulesToolBarManager
        .add(new Action(Messages.DependenciesComposite_action_sortAlphabetically, MavenEditorImages.SORT) {
          {
            setChecked(false);
          }

          @Override
          public int getStyle() {
            return AS_CHECK_BOX;
          }

          @Override
          public void run() {
            dependenciesContentProvider.setShouldSort(isChecked());
            dependenciesEditor.getViewer().refresh();
          }
        });

    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_showInheritedDependencies,
        MavenEditorImages.SHOW_INHERITED_DEPENDENCIES) {
      {
        setChecked(false);
      }

      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        if(isChecked()) {
          showInheritedDependencies = true;
        } else {
          showInheritedDependencies = false;
        }
        ISelection selection = dependenciesEditor.getViewer().getSelection();
        setDependenciesInput();
        dependenciesEditor.getViewer().refresh();
        dependenciesEditor.getViewer().setSelection(selection, true);
      }
    });

    modulesToolBarManager
        .add(new Action(Messages.DependenciesComposite_action_showgroupid, MavenEditorImages.SHOW_GROUP) {
          {
            setChecked(false);
            dependenciesComparator.setSortByGroups(false);
          }

          public int getStyle() {
            return AS_CHECK_BOX;
          }

          public void run() {
            dependencyLabelProvider.setShowGroupId(isChecked());
            dependenciesComparator.setSortByGroups(isChecked());
            dependenciesEditor.getViewer().refresh();
          }
        });

    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_filter, MavenEditorImages.FILTER) {
      {
        setChecked(true);
      }

      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        TableViewer viewer = dependenciesEditor.getViewer();
        if(isChecked()) {
          viewer.addFilter(searchFilter);
        } else {
          viewer.removeFilter(searchFilter);
        }
        viewer.refresh();
        if(isChecked()) {
          searchControl.getSearchText().setFocus();
        }
      }
    });

    Composite toolbarComposite = toolkit.createComposite(dependenciesSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);

    modulesToolBarManager.createControl(toolbarComposite);
    dependenciesSection.setTextClient(toolbarComposite);
  }

  private void createDependencyManagementSection(SashForm verticalSash) {
    Section dependencyManagementSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    dependencyManagementSection.marginWidth = 3;
    dependencyManagementSection.setText(Messages.DependenciesComposite_sectionDependencyManagement);
    dependencyManagementComparator = new DependenciesComparator<Dependency>();
    dependencyManagementContentProvider.setComparator(dependencyManagementComparator);

    dependencyManagementEditor = new PropertiesListComposite<Dependency>(dependencyManagementSection, SWT.NONE, true);
    dependencyManagementEditor.setContentProvider(dependencyManagementContentProvider);
    dependencyManagementEditor.setLabelProvider(dependencyManagementLabelProvider);
    dependencyManagementSection.setClient(dependencyManagementEditor);

    dependencyManagementEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        final List<Dependency> dependencyList = dependencyManagementEditor.getSelection();
        try {
          editorPage.performEditOperation(new Operation() {
            public void process(Document document) {
              Element deps = findChild(findChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT), DEPENDENCIES);
              if(deps == null) {
                //TODO log
                return;
              }
              for(Dependency dependency : dependencyList) {
                Element dep = findChild(deps, DEPENDENCY, childEquals(GROUP_ID, dependency.groupId),
                    childEquals(ARTIFACT_ID, dependency.artifactId));
                removeChild(deps, dep);
              }
              removeIfNoChildElement(deps);
            }
          }, log, "error removing managed dependencies");
        } finally {
          setDependencyManagementInput();
          dependenciesEditor.refresh();
        }
      }
    });

    dependencyManagementEditor.setPropertiesListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Dependency dependency = dependencyManagementEditor.getSelection().get(0);
        EditDependencyDialog d = new EditDependencyDialog(getShell(), true, editorPage.getProject(),
            editorPage.getPomEditor().getMavenProject());
        d.setDependency(toApacheDependency(dependency));
        if(d.open() == Window.OK) {
          try {
            editorPage.performEditOperation(d.getEditOperation(), log, "Error updating dependency");
          } finally {
            setDependencyManagementInput();
            dependencyManagementEditor.setSelection(Collections.singletonList(dependency));
            //refresh this one to update decorations..
            dependenciesEditor.refresh();
          }
        }
      }
    });

    dependencyManagementEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Dependency> selection = dependencyManagementEditor.getSelection();

        if(!selection.isEmpty()) {
          dependenciesEditor.setSelection(Collections.<Object> emptyList());
        }
      }
    });

    toolkit.adapt(dependencyManagementEditor);
    toolkit.paintBordersFor(dependencyManagementEditor);

    dependencyManagementEditor.setAddButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        final MavenRepositorySearchDialog addDepDialog = MavenRepositorySearchDialog.createSearchDependencyDialog(
            getShell(), Messages.DependenciesComposite_action_selectDependency,
            editorPage.getPomEditor().getMavenProject(), editorPage.getProject(), true);
        if(addDepDialog.open() == Window.OK) {
          final IndexedArtifactFile dep = (IndexedArtifactFile) addDepDialog.getFirstResult();
          final String selectedScope = addDepDialog.getSelectedScope();
          try {
            editorPage.performEditOperation(new Operation() {
              public void process(Document document) {
                Element depsEl = getChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT, DEPENDENCIES);
                PomHelper.addOrUpdateDependency(depsEl, dep.group, dep.artifact, dep.version, dep.type, selectedScope,
                    dep.classifier);
              }
            }, log, "errror adding dependency");
          } finally {
            setDependencyManagementInput();
            List<Dependency> dlist = getManagedDependencies();
            if(dlist.size() > 0) {
              dependencyManagementEditor
                  .setSelection(Collections.<Dependency> singletonList(dlist.get(dlist.size() - 1)));
            }
            //refresh this one to update decorations..
            dependenciesEditor.refresh();
          }

        }
      }
    });

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);

    modulesToolBarManager
        .add(new Action(Messages.DependenciesComposite_action_sortAlphabetically, MavenEditorImages.SORT) {
          {
            setChecked(false);
            dependencyManagementContentProvider.setShouldSort(false);
          }

          @Override
          public int getStyle() {
            return AS_CHECK_BOX;
          }

          @Override
          public void run() {
            dependencyManagementContentProvider.setShouldSort(isChecked());
            dependencyManagementEditor.getViewer().refresh();
          }
        });

    modulesToolBarManager
        .add(new Action(Messages.DependenciesComposite_action_showgroupid, MavenEditorImages.SHOW_GROUP) {
          {
            setChecked(false);
            dependencyManagementComparator.setSortByGroups(false);
          }

          public int getStyle() {
            return AS_CHECK_BOX;
          }

          public void run() {
            dependencyManagementLabelProvider.setShowGroupId(isChecked());
            dependencyManagementComparator.setSortByGroups(isChecked());
            dependencyManagementEditor.getViewer().refresh();
          }
        });

    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_filter, MavenEditorImages.FILTER) {
      {
        setChecked(true);
      }

      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        TableViewer viewer = dependencyManagementEditor.getViewer();
        if(isChecked()) {
          viewer.addFilter(searchFilter);
        } else {
          viewer.removeFilter(searchFilter);
        }
        viewer.refresh();
        if(isChecked()) {
          searchControl.getSearchText().setFocus();
        }
      }
    });

    Composite toolbarComposite = toolkit.createComposite(dependencyManagementSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);

    modulesToolBarManager.createControl(toolbarComposite);
    dependencyManagementSection.setTextClient(toolbarComposite);
  }

  public void loadData() {
    resetDependencies();
    resetManagedDependencies();
    ValueProvider<List<org.apache.maven.model.Dependency>> dmValueProvider = new ValueProvider<List<org.apache.maven.model.Dependency>>() {
      @Override
      public List<org.apache.maven.model.Dependency> getValue() {
        List<org.apache.maven.model.Dependency> toRet = new ArrayList<org.apache.maven.model.Dependency>();
        for(DependenciesComposite.Dependency d : getManagedDependencies()) {
          toRet.add(toApacheDependency(d));
        }
        return toRet;
      }
    };
    this.dependencyLabelProvider.setPomEditor(editorPage.getPomEditor(), dmValueProvider);
    this.dependencyManagementLabelProvider.setPomEditor(editorPage.getPomEditor(), dmValueProvider);

    setDependenciesInput();
    setDependencyManagementInput();

    dependenciesEditor.setReadOnly(editorPage.isReadOnly());
    dependencyManagementEditor.setReadOnly(editorPage.isReadOnly());

    if(searchControl != null) {
      searchControl.getSearchText().setEditable(true);
    }
  }

  public void setSearchControl(SearchControl searchControl) {
    if(this.searchControl != null) {
      return;
    }

    this.searchMatcher = new SearchMatcher(searchControl);
    this.searchFilter = new DependencyFilter(searchMatcher);
    this.searchControl = searchControl;

    //we add filter here as the default behaviour is to filter..
    final TableViewer dependenciesViewer = dependenciesEditor.getViewer();
    dependenciesViewer.addFilter(searchFilter);

    final TableViewer dependencyManagementViewer = dependencyManagementEditor.getViewer();
    dependencyManagementViewer.addFilter(searchFilter);

    // Create a job to update the contents of the viewers when the
    // filter text is modified. Using a job is in this way lets us
    // defer updating the field while the user is typing.
    final Job updateJob = new WorkbenchJob("Update Maven Dependency Viewers") {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        dependenciesViewer.refresh();
        dependencyManagementViewer.refresh();

        return Status.OK_STATUS;
      }
    };

    // Run the update job when the user modifies the filter text.
    this.searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        // The net effect here is that the field will update 200 ms after
        // the user stops typing.
        updateJob.cancel();
        updateJob.schedule(200);
      }
    });
  }

  public static class DependencyFilter extends ViewerFilter {
    private SearchMatcher searchMatcher;

    public DependencyFilter(SearchMatcher searchMatcher) {
      this.searchMatcher = searchMatcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof Dependency) {
        Dependency d = (Dependency) element;
        return searchMatcher.isMatchingArtifact(d.groupId, d.artifactId);
      } else if(element instanceof org.apache.maven.model.Dependency) {
        org.apache.maven.model.Dependency dependency = (org.apache.maven.model.Dependency) element;
        return searchMatcher.isMatchingArtifact(dependency.getGroupId(), dependency.getArtifactId());
      }
      return false;
    }
  }

  void openManageDependenciesDialog() throws InvocationTargetException, InterruptedException {
    /*
     * A linked list representing the path from child to root parent pom.
     * The head is the child, the tail is the root pom
     */
    final List<ParentHierarchyEntry> hierarchy = new ArrayList<ParentHierarchyEntry>();

    IRunnableWithProgress projectLoader = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          IMavenProjectFacade projectFacade = projectManager.create(pomEditor.getPomFile(), true, monitor);
          if(projectFacade != null) {
            hierarchy.addAll(new ParentGatherer(projectFacade).getParentHierarchy(monitor));
          }
        } catch(CoreException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    PlatformUI.getWorkbench().getProgressService().run(false, true, projectLoader);

    if(hierarchy.isEmpty()) {
      //We were unable to read the project metadata above, so there was an error. 
      //User has already been notified to fix the problem.
      return;
    }

    final ManageDependenciesDialog manageDepDialog = new ManageDependenciesDialog(getShell(),
        new ValueProvider<List<org.apache.maven.model.Dependency>>() {
          @Override
          public List<org.apache.maven.model.Dependency> getValue() {
            List<org.apache.maven.model.Dependency> toRet = new ArrayList<org.apache.maven.model.Dependency>();
            for(DependenciesComposite.Dependency d : getDependencies()) {
              toRet.add(toApacheDependency(d));
            }
            return toRet;
          }
        }, hierarchy, dependenciesEditor.getSelection());
    manageDepDialog.open();
  }

  protected void setDependencyManagementInput() {
    resetManagedDependencies();
    final List<Dependency> managed = getManagedDependencies();
    dependencyManagementEditor.setInput(managed);
  }

  /**
   * only to be called within the perform* methods..
   * 
   * @param depEl
   * @return
   */
  private Dependency toDependency(Element depEl) {
    Dependency dep = new Dependency();
    dep.groupId = getTextValue(findChild(depEl, GROUP_ID));
    dep.artifactId = getTextValue(findChild(depEl, ARTIFACT_ID));
    dep.version = getTextValue(findChild(depEl, VERSION));
    dep.type = getTextValue(findChild(depEl, TYPE));
    dep.scope = getTextValue(findChild(depEl, SCOPE));
    dep.classifier = getTextValue(findChild(depEl, CLASSIFIER));
    dep.systemPath = getTextValue(findChild(depEl, SYSTEM_PATH));
    dep.optional = Boolean.parseBoolean(getTextValue(findChild(depEl, OPTIONAL)));
    return dep;
  }

  private final Object MAN_DEP_LOCK = new Object();

  private List<Dependency> getManagedDependencies() {
    synchronized(MAN_DEP_LOCK) {
      if(manageddependencies == null) {
        manageddependencies = new ArrayList<Dependency>();
        try {
          performOnDOMDocument(new OperationTuple(pomEditor.getDocument(), new Operation() {
            public void process(Document document) {
              Element dms = findChild(findChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT), DEPENDENCIES);
              for(Element depEl : findChilds(dms, DEPENDENCY)) {
                Dependency dep = toDependency(depEl);
                if(dep != null) {
                  manageddependencies.add(dep);
                }
              }
            }
          }, true));
        } catch(Exception ex) {
          log.error("Error loading managed dependencies", ex);
        }
      }
      return manageddependencies;
    }
  }

  private void resetManagedDependencies() {
    synchronized(MAN_DEP_LOCK) {
      manageddependencies = null;
    }
  }

  private final Object DEP_LOCK = new Object();

  private List<Dependency> getDependencies() {
    synchronized(DEP_LOCK) {
      if(dependencies == null) {
        dependencies = new ArrayList<Dependency>();
        try {
          performOnDOMDocument(new OperationTuple(pomEditor.getDocument(), new Operation() {
            public void process(Document document) {
              Element dms = findChild(document.getDocumentElement(), DEPENDENCIES);
              for(Element depEl : findChilds(dms, DEPENDENCY)) {
                Dependency dep = toDependency(depEl);
                if(dep != null) {
                  dependencies.add(dep);
                }
              }
            }
          }, true));
        } catch(Exception ex) {
          log.error("Error loading dependencies", ex);
        }
      }
      return dependencies;
    }
  }

  private void resetDependencies() {
    synchronized(DEP_LOCK) {
      dependencies = null;
    }
  }

  protected void setDependenciesInput() {
    resetDependencies();
    List<Object> deps = new ArrayList<Object>();
    deps.addAll(getDependencies());

    if(showInheritedDependencies) {

      /*
       * Add the inherited dependencies into the bunch. But don't we need to
       * filter out the dependencies that are duplicated in the M2E model, so
       * we need to run through each list and only add ones that aren't in both.
       */
      List<org.apache.maven.model.Dependency> allDeps = new LinkedList<org.apache.maven.model.Dependency>();
      MavenProject mp = pomEditor.getMavenProject();
      if(mp != null) {
        allDeps.addAll(mp.getDependencies());
      }
      for(org.apache.maven.model.Dependency mavenDep : allDeps) {
        boolean found = false;
        Iterator<Dependency> iter = getDependencies().iterator();
        while(!found && iter.hasNext()) {
          Dependency m2eDep = iter.next();
          if(mavenDep.getGroupId().equals(m2eDep.groupId) && mavenDep.getArtifactId().equals(m2eDep.artifactId)) {
            found = true;
          }
        }
        if(!found) {
          //now check the temporary keys
          if(!temporaryRemovedDependencies.contains(mavenDep.getGroupId() + ":" + mavenDep.getArtifactId())) {
            deps.add(mavenDep);
          }
        }
      }
    }
    dependenciesEditor.setInput(deps);
  }

  protected class PropertiesListComposite<T> extends ListEditorComposite<T> {
    private static final String PROPERTIES_BUTTON_KEY = "PROPERTIES"; //$NON-NLS-1$

    protected Button properties;

    public PropertiesListComposite(Composite parent, int style, boolean includeSearch) {
      super(parent, style, includeSearch);
    }

    @Override
    protected void createButtons(boolean includeSearch) {
      if(includeSearch) {
        createAddButton();
      }
      createRemoveButton();
      properties = createButton(Messages.ListEditorComposite_btnProperties);
      addButton(PROPERTIES_BUTTON_KEY, properties);
    }

    public void setPropertiesListener(SelectionListener listener) {
      properties.addSelectionListener(listener);
    }

    @Override
    protected void viewerSelectionChanged() {
      super.viewerSelectionChanged();
      updatePropertiesButton();
    }

    protected void updatePropertiesButton() {
      boolean enable = !viewer.getSelection().isEmpty() && !isBadSelection();
      properties.setEnabled(!readOnly && enable);
    }

    @Override
    protected void updateRemoveButton() {
      boolean enable = !viewer.getSelection().isEmpty() && !isBadSelection();
      getRemoveButton().setEnabled(!readOnly && enable);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      super.setReadOnly(readOnly);
      updatePropertiesButton();
    }

    /**
     * Returns true if the viewer has no input or if there is currently an inherited dependency selected
     * 
     * @return
     */
    protected boolean isBadSelection() {
      @SuppressWarnings("unchecked")
      List<Object> deps = (List<Object>) viewer.getInput();
      if(deps == null || deps.isEmpty()) {
        return true;
      }
      boolean bad = false;
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      @SuppressWarnings("unchecked")
      Iterator<Object> iter = selection.iterator();
      while(iter.hasNext()) {
        Object obj = iter.next();
        if(obj instanceof org.apache.maven.model.Dependency) {
          bad = true;
          break;
        }
      }
      return bad;
    }
  }

  protected class DependenciesListComposite<T> extends PropertiesListComposite<T> {

    private static final String MANAGE = "MANAGE"; //$NON-NLS-1$

    protected Button manage;

    public DependenciesListComposite(Composite parent, int style, boolean includeSearch) {
      super(parent, style, includeSearch);
    }

    @Override
    protected void createButtons(boolean includeSearch) {
      super.createButtons(includeSearch);
      manage = createButton(Messages.DependenciesComposite_manageButton);
      addButton(MANAGE, manage);
    }

    @Override
    protected void viewerSelectionChanged() {
      super.viewerSelectionChanged();
      updateManageButton();
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      super.setReadOnly(readOnly);
      updateManageButton();
    }

    @Override
    public void refresh() {
      super.refresh();
      updateManageButton();
    }

    protected void updateManageButton() {
      boolean hasNonManaged = false;
      //MNGECLIPSE-2675 only enable when there are unmanaged dependencies
      for(Dependency d : getDependencies()) {
        if(d.version != null) {
          hasNonManaged = true;
          break;
        }
      }
      if(!manage.isDisposed()) {
        manage.setEnabled(!readOnly && hasNonManaged);
      }
    }

    public void setManageButtonListener(SelectionListener listener) {
      manage.addSelectionListener(listener);
    }
  }

  public void mavenProjectHasChanged() {
    temporaryRemovedDependencies.clear();
    //MNGECLIPSE-2673 when maven project changes and we show the inherited items, update now..
    if(showInheritedDependencies) {
      setDependenciesInput();
    }
    dependenciesEditor.refresh();
  }

  private org.apache.maven.model.Dependency toApacheDependency(Dependency dependency) {
    org.apache.maven.model.Dependency toRet = new org.apache.maven.model.Dependency();
    toRet.setArtifactId(dependency.artifactId);
    toRet.setGroupId(dependency.groupId);
    toRet.setClassifier(dependency.classifier);
    toRet.setScope(dependency.scope);
    toRet.setOptional(dependency.optional);
    toRet.setSystemPath(dependency.systemPath);
    toRet.setType(dependency.type);
    toRet.setVersion(dependency.version);
    return toRet;
  }

  private boolean isManaged(String groupId, String artifactId, String version) {
    if(version == null) {
      return true;
    }
    DependencyManagement depManagement = editorPage.getPomEditor().getMavenProject().getDependencyManagement();
    if(depManagement != null && groupId != null && artifactId != null) {
      List<org.apache.maven.model.Dependency> managedDep = depManagement.getDependencies();
      if(managedDep != null) {
        for(org.apache.maven.model.Dependency dependency : managedDep) {
          if(version.equals(dependency.getVersion()) && artifactId.equals(dependency.getArtifactId())
              && groupId.equals(dependency.getGroupId())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  class Dependency implements IAdaptable {
    String artifactId;

    String groupId;

    String version;

    String type;

    String classifier;

    String scope;

    String systemPath;

    boolean optional;

    public Dependency() {
    }

    public <T> T getAdapter(Class<T> adapter) {
      if(ArtifactKey.class.equals(adapter)) {
        return adapter.cast(new ArtifactKey(groupId, artifactId, version, classifier));
      }
      return null;
    }
  }
}
