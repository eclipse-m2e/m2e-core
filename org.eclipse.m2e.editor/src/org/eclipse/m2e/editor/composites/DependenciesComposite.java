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

package org.eclipse.m2e.editor.composites;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;

import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.ui.dialogs.AddDependencyDialog;
import org.eclipse.m2e.core.ui.dialogs.EditDependencyDialog;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.dialogs.ManageDependenciesDialog;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;


/**
 * @author Eugene Kuleshov
 */
public class DependenciesComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

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

  // model

  Model model;

  ValueProvider<DependencyManagement> dependencyManagementProvider;

  final DependencyLabelProvider dependencyLabelProvider = new DependencyLabelProvider(true);

  final DependencyLabelProvider dependencyManagementLabelProvider = new DependencyLabelProvider();

  protected boolean showInheritedDependencies = false;

  final DependencyContentProvider<Object> dependenciesContentProvider = new DependencyContentProvider<Object>();

  DependenciesComparator<Object> dependenciesComparator;

  final DependencyContentProvider<Dependency> dependencyManagementContentProvider = new DependencyContentProvider<Dependency>();

  DependenciesComparator<Dependency> dependencyManagementComparator;

  public DependenciesComposite(Composite composite, MavenPomEditorPage editorPage, int flags, MavenPomEditor pomEditor) {
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
    dependenciesEditor.setLabelProvider(new DelegatingStyledCellLabelProvider( dependencyLabelProvider));
    dependenciesEditor.setContentProvider(dependenciesContentProvider);

    dependenciesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        List<Object> dependencyList = dependenciesEditor.getSelection();
        for(Object obj : dependencyList) {
          if (obj instanceof Dependency) {
            Dependency dependency = (Dependency) obj;
            temporaryRemovedDependencies.add(dependency.getGroupId() + ":" + dependency.getArtifactId());
            Command removeCommand = RemoveCommand.create(editingDomain, model, POM_PACKAGE.getModel_Dependencies(),
                dependency);
            compoundCommand.append(removeCommand);
          } else if (obj instanceof org.apache.maven.model.Dependency) {
            /*
             * TODO: Support a refactoring of removing an inherited/managed dependency.
             */
          }
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        setDependenciesInput();
      }
    });

    dependenciesEditor.setPropertiesListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Object selection = dependenciesEditor.getSelection().get(0);
        if (selection instanceof Dependency) {
          Dependency dependency = (Dependency) selection;
          EditDependencyDialog d = new EditDependencyDialog(getShell(), false, editorPage.getEditingDomain(), editorPage
              .getProject());
          d.setDependency(dependency);
          if(d.open() == Window.OK) {
            setDependenciesInput();
            dependenciesEditor.setSelection(Collections.singletonList((Object) dependency));
          }
        } else if (selection instanceof org.apache.maven.model.Dependency) {
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
          MavenEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Error: ", e1)); //$NON-NLS-1$
        } catch(InterruptedException e1) {
          MavenEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, MavenEditorPlugin.PLUGIN_ID, "Error: ", e1)); //$NON-NLS-1$
        }
      }
    });
    
    dependenciesEditor.setAddButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        final AddDependencyDialog addDepDialog = new AddDependencyDialog(getShell(), false, editorPage.getProject(), editorPage.getPomEditor().getMavenProject());
        if(addDepDialog.open() == Window.OK) {
          List<Dependency> deps = addDepDialog.getDependencies();
          for(Dependency dep : deps) {
            setupDependency(new ValueProvider<Model>() {
              @Override
              public Model getValue() {
                return model;
              }
            }, POM_PACKAGE.getModel_Dependencies(), dep);
          }
          setDependenciesInput();
          dependenciesEditor.setSelection(Collections.singletonList((Object) deps.get(0)));
        }
      }

    });

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
    
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_sortAlphabetically, MavenEditorImages.SORT) {
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
        if (isChecked()) {
          showInheritedDependencies  = true;
        } else {
          showInheritedDependencies  = false;
        }
        ISelection selection = dependenciesEditor.getViewer().getSelection();
        setDependenciesInput();
        dependenciesEditor.getViewer().refresh();
        dependenciesEditor.getViewer().setSelection(selection, true);
      }
    });
    
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_showgroupid,
        MavenEditorImages.SHOW_GROUP) {
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
        TreeViewer viewer = dependenciesEditor.getViewer();
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
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        List<Dependency> dependencyList = dependencyManagementEditor.getSelection();
        for(Dependency dependency : dependencyList) {
          Command removeCommand = RemoveCommand.create(editingDomain, //
              dependencyManagementProvider.getValue(), POM_PACKAGE.getDependencyManagement_Dependencies(), dependency);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    dependencyManagementEditor.setPropertiesListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Dependency dependency = dependencyManagementEditor.getSelection().get(0);
        EditDependencyDialog d = new EditDependencyDialog(getShell(), true, editorPage.getEditingDomain(), editorPage
            .getProject());
        d.setDependency(dependency);
        if(d.open() == Window.OK) {
          dependencyManagementEditor.setInput(dependencyManagementProvider.getValue().getDependencies());
          dependencyManagementEditor.setSelection(Collections.singletonList(dependency));
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
        final AddDependencyDialog addDepDialog = new AddDependencyDialog(getShell(), true, editorPage.getProject(), editorPage.getPomEditor().getMavenProject());
        if(addDepDialog.open() == Window.OK) {
          List<Dependency> deps = addDepDialog.getDependencies();
          for(Dependency dep : deps) {
            setupDependency(dependencyManagementProvider, POM_PACKAGE.getDependencyManagement_Dependencies(), dep);
          }
          setDependenciesInput();
          dependencyManagementEditor.setInput(dependencyManagementProvider.getValue().getDependencies());
          dependencyManagementEditor.setSelection(Collections.singletonList(deps.get(0)));
        }
      }
    });

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);

    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_sortAlphabetically, MavenEditorImages.SORT) {
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
    
    modulesToolBarManager.add(new Action(Messages.DependenciesComposite_action_showgroupid,
        MavenEditorImages.SHOW_GROUP) {
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
        TreeViewer viewer = dependencyManagementEditor.getViewer();
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


  @SuppressWarnings("unchecked")
  public void loadData(Model model, ValueProvider<DependencyManagement> dependencyManagementProvider) {
    this.model = model;
    this.dependencyManagementProvider = dependencyManagementProvider;
    this.dependencyLabelProvider.setPomEditor(editorPage.getPomEditor());
    this.dependencyManagementLabelProvider.setPomEditor(editorPage.getPomEditor());

    setDependenciesInput();

    DependencyManagement dependencyManagement = dependencyManagementProvider.getValue();
    dependencyManagementEditor.setInput(dependencyManagement == null ? null : dependencyManagement.getDependencies());

    dependenciesEditor.setReadOnly(editorPage.isReadOnly());
    dependencyManagementEditor.setReadOnly(editorPage.isReadOnly());

    if(searchControl != null) {
      searchControl.getSearchText().setEditable(true);
    }
  }

  public void updateView(final MavenPomEditorPage editorPage, final Notification notification) {
    Display.getDefault().asyncExec(new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        EObject object = (EObject) notification.getNotifier();
        Object feature = notification.getFeature();

        // XXX event is not received when <dependencies> is deleted in XML
        if(object instanceof Model) {
          Model model2 = (Model) object;
          if((model2.getDependencyManagement() != null && dependencyManagementEditor.getInput() == null) 
              || feature == PomPackage.Literals.DEPENDENCY_MANAGEMENT__DEPENDENCIES) {
            dependencyManagementEditor.setInput(model2.getDependencyManagement().getDependencies());
          } else if(model2.getDependencyManagement() == null) {
            dependencyManagementEditor.setInput(null);
          }

          if((model2.getDependencies() != null && dependenciesEditor.getInput() == null) 
              || feature == PomPackage.Literals.MODEL__DEPENDENCIES) {
            setDependenciesInput();
          } else if(model2.getDependencies() == null) {
            setDependenciesInput();
          }
          dependenciesEditor.refresh();
          dependencyManagementEditor.refresh();
        }

        if(object instanceof DependencyManagement) {
          if(dependencyManagementEditor.getInput() == null || feature == PomPackage.Literals.DEPENDENCY_MANAGEMENT__DEPENDENCIES) {
            dependencyManagementEditor.setInput(((DependencyManagement) object).getDependencies());
          }
          dependencyManagementEditor.refresh();
        }
        if (object instanceof Dependency) {
          dependenciesEditor.refresh();
          dependencyManagementEditor.refresh();
        }
      }
    });
  }

  void setupDependency(ValueProvider<? extends EObject> parentProvider, EReference feature, Dependency dependency) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = editorPage.getEditingDomain();

    EObject parent = parentProvider.getValue();
    if(parent == null) {
      parent = parentProvider.create(editingDomain, compoundCommand);
    }

    //we clone the dependency added in the hope of providing a one-step undo.
    Dependency clone = PomFactory.eINSTANCE.createDependency();
    Command addDependencyCommand = AddCommand.create(editingDomain, parent, feature, clone);
    compoundCommand.append(addDependencyCommand);
    //only the props as defined in AddDependencyDialog.createDependency()
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getGroupId(), PomPackage.eINSTANCE.getDependency_GroupId(), ""));
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getArtifactId(), PomPackage.eINSTANCE.getDependency_ArtifactId(), ""));
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getVersion(), PomPackage.eINSTANCE.getDependency_Version(), ""));
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getClassifier(), PomPackage.eINSTANCE.getDependency_Classifier(), ""));
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getScope(), PomPackage.eINSTANCE.getDependency_Scope(), ""));
    compoundCommand.append(ManageDependenciesDialog.createCommand(editingDomain, clone, dependency.getType(), PomPackage.eINSTANCE.getDependency_Type(), ""));

    editingDomain.getCommandStack().execute(compoundCommand);
  }

//  Dependency createDependency(ValueProvider<? extends EObject> parentProvider, EReference feature, String groupId,
//      String artifactId, String version, String classifier, String type, String scope) {
//    CompoundCommand compoundCommand = new CompoundCommand();
//    EditingDomain editingDomain = editorPage.getEditingDomain();
//
//    EObject parent = parentProvider.getValue();
//    if(parent == null) {
//      parent = parentProvider.create(editingDomain, compoundCommand);
//    }
//
//    Dependency dependency = PomFactory.eINSTANCE.createDependency();
//    dependency.setGroupId(groupId);
//    dependency.setArtifactId(artifactId);
//    dependency.setVersion(version);
//    dependency.setClassifier(classifier);
//    dependency.setType(type);
//    dependency.setScope(scope);
//
//    Command addDependencyCommand = AddCommand.create(editingDomain, parent, feature, dependency);
//    compoundCommand.append(addDependencyCommand);
//
//    editingDomain.getCommandStack().execute(compoundCommand);
//
//    return dependency;
//  }

  public void setSearchControl(SearchControl searchControl) {
    if(this.searchControl != null) {
      return;
    }

    this.searchMatcher = new SearchMatcher(searchControl);
    this.searchFilter = new DependencyFilter(searchMatcher);
    this.searchControl = searchControl;
    this.searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        selectDepenendencies(dependenciesEditor, model, POM_PACKAGE.getModel_Dependencies());
        selectDepenendencies(dependencyManagementEditor, dependencyManagementProvider.getValue(),
            POM_PACKAGE.getDependencyManagement_Dependencies());
      }

      @SuppressWarnings({"unchecked", "rawtypes"})
      private void selectDepenendencies(PropertiesListComposite<?> dependencyManagementEditor, EObject parent,
          EStructuralFeature feature) {
        if(parent != null) {
          dependencyManagementEditor.setSelection((List) parent.eGet(feature));
          dependencyManagementEditor.refresh();
        }
      }
    });
    //we add filter here as the default behaviour is to filter..
    TreeViewer viewer = dependenciesEditor.getViewer();
    viewer.addFilter(searchFilter);
    viewer = dependencyManagementEditor.getViewer();
    viewer.addFilter(searchFilter);

  }

  /** mkleint: apparently this methods shall find the version in resolved pom for the given dependency
   * not sure if getBaseVersion is the way to go..
   * Note: duplicated in DependencyDetailsComposite 
   * @param groupId
   * @param artifactId
   * @param monitor
   * @return
   */
  String getVersion(String groupId, String artifactId, IProgressMonitor monitor) {
    try {
      MavenProject mavenProject = editorPage.getPomEditor().readMavenProject(false, monitor);
      Artifact a = mavenProject.getArtifactMap().get(groupId + ":" + artifactId); //$NON-NLS-1$
      if(a != null) {
        return a.getBaseVersion();
      }
    } catch(CoreException ex) {
      MavenLogger.log(ex);
    }
    return null;
  }

  public static class DependencyFilter extends ViewerFilter {
    private SearchMatcher searchMatcher;

    public DependencyFilter(SearchMatcher searchMatcher) {
      this.searchMatcher = searchMatcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof Dependency) {
        Dependency d = (Dependency) element;
        return searchMatcher.isMatchingArtifact(d.getGroupId(), d.getArtifactId());
      } else if (element instanceof org.apache.maven.model.Dependency) {
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
    final LinkedList<MavenProject> hierarchy = new LinkedList<MavenProject>();
    
    IRunnableWithProgress projectLoader = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
          MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
          IMavenProjectFacade projectFacade = projectManager.create(pomEditor.getPomFile(), true, monitor);
          if (projectFacade != null) {
            hierarchy.addAll(new ParentGatherer(projectFacade.getMavenProject(), projectFacade).getParentHierarchy(monitor));
          }
        } catch(CoreException e) {
          throw new InvocationTargetException(e);
        }
      }
    };

    PlatformUI.getWorkbench().getProgressService().run(false, true, projectLoader);

    if (hierarchy.isEmpty()) {
      //We were unable to read the project metadata above, so there was an error. 
      //User has already been notified to fix the problem.
      return;
    }
    
    final ManageDependenciesDialog manageDepDialog = new ManageDependenciesDialog(getShell(), model, hierarchy,
        pomEditor.getEditingDomain(), dependenciesEditor.getSelection());
    manageDepDialog.open();
  }

  protected void setDependenciesInput() {
    List<Object> deps = new ArrayList<Object>();
    if (model.getDependencies() != null) {
      deps.addAll(model.getDependencies());
    }
    if (showInheritedDependencies) {
      
      /*
       * Add the inherited dependencies into the bunch. But don't we need to
       * filter out the dependencies that are duplicated in the M2E model, so
       * we need to run through each list and only add ones that aren't in both.
       */
      List<org.apache.maven.model.Dependency> allDeps = new LinkedList<org.apache.maven.model.Dependency>();
      MavenProject mp = pomEditor.getMavenProject();
      if (mp != null) {
        allDeps.addAll(mp.getDependencies());
      }
      for (org.apache.maven.model.Dependency mavenDep : allDeps) {
        boolean found = false;
        Iterator<Dependency> iter = model.getDependencies().iterator();
        while (!found && iter.hasNext()) {
          Dependency m2eDep = iter.next();
          if (mavenDep.getGroupId().equals(m2eDep.getGroupId()) 
              && mavenDep.getArtifactId().equals(m2eDep.getArtifactId())) {
            found = true;
          }
        }
        if (!found) {
          //now check the temporary keys
          if (!temporaryRemovedDependencies.contains(mavenDep.getGroupId() + ":" + mavenDep.getArtifactId())) {
            deps.add(mavenDep);
          }
        }
      }
    }
    dependenciesEditor.setInput(deps);
  }

  //no longer extending ListeEditoComposite because we need a Tree, not Table here..
  protected class PropertiesListComposite<T> extends Composite {
    private static final String PROPERTIES_BUTTON_KEY = "PROPERTIES"; //$NON-NLS-1$
    protected Button properties;
    
    TreeViewer viewer;
    
    protected Map<String, Button> buttons = new HashMap<String, Button>(5);
    
    /*
     * Default button keys
     */
    private static final String ADD = "ADD"; //$NON-NLS-1$
    private static final String CREATE = "CREATE"; //$NON-NLS-1$
    private static final String REMOVE = "REMOVE"; //$NON-NLS-1$

    boolean readOnly = false;

    protected FormToolkit toolkit;
    
    public PropertiesListComposite(Composite parent, int style, boolean includeSearch) {
      super(parent, style);
      toolkit = new FormToolkit(parent.getDisplay());

      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 1;
      gridLayout.marginHeight = 1;
      gridLayout.verticalSpacing = 1;
      setLayout(gridLayout);

      final Tree tree = toolkit.createTree(this, SWT.FULL_SELECTION | SWT.MULTI | style);
      tree.setData("name", "list-editor-composite-table"); //$NON-NLS-1$ //$NON-NLS-2$
      viewer = new TreeViewer(tree);
      

      createButtons(includeSearch);
      
      int vSpan = buttons.size();
      GridData viewerData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, vSpan);
      viewerData.widthHint = 100;
      viewerData.heightHint = includeSearch ? 125 : 50;
      viewerData.minimumHeight = includeSearch ? 125 : 50;
      tree.setLayoutData(viewerData);
      viewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);

      viewer.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          viewerSelectionChanged();
        }
      });

      toolkit.paintBordersFor(this);
    }

    public void setLabelProvider(IBaseLabelProvider delegatingStyledCellLabelProvider) {
      viewer.setLabelProvider(delegatingStyledCellLabelProvider);
    }

    public void setContentProvider(DependencyContentProvider<T> contentProvider) {
      viewer.setContentProvider(contentProvider);
    }

    public void setInput(List<T> input) {
      viewer.setInput(input);
      viewer.setSelection(new StructuredSelection());
    }

    public Object getInput() {
      return viewer.getInput();
    }

    public void setOpenListener(IOpenListener listener) {
      viewer.addOpenListener(listener);
    }

    public void addSelectionListener(ISelectionChangedListener listener) {
      viewer.addSelectionChangedListener(listener);
    }

    public void setAddButtonListener(SelectionListener listener) {
      if(getAddButton() != null) {
        getAddButton().addSelectionListener(listener);
        getAddButton().setEnabled(true);
      }
    }
    
    protected Button getCreateButton() {
      return buttons.get(CREATE);
    }
    
    protected Button getRemoveButton() {
      return buttons.get(REMOVE);
    }

    protected Button getAddButton() {
      return buttons.get(ADD);
    }

    public void setCreateButtonListener(SelectionListener listener) {
      getCreateButton().addSelectionListener(listener);
      getCreateButton().setEnabled(true);
    }

    public void setRemoveButtonListener(SelectionListener listener) {
      getRemoveButton().addSelectionListener(listener);
    }

    public TreeViewer getViewer() {
      return viewer;
    }


    @SuppressWarnings("unchecked")
    public List<T> getSelection() {
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      return selection == null ? Collections.emptyList() : selection.toList();
    }

    public void setSelection(List<T> selection) {
      viewer.setSelection(new StructuredSelection(selection), true);
    }

    public void refresh() {
      if(!viewer.getTree().isDisposed()) {
        viewer.refresh(true);
      }
    }

    public void setCellModifier(ICellModifier cellModifier) {
      viewer.setColumnProperties(new String[] {"?"}); //$NON-NLS-1$

      TextCellEditor editor = new TextCellEditor(viewer.getTree());
      viewer.setCellEditors(new CellEditor[] {editor});
      viewer.setCellModifier(cellModifier);
    }

    public void setDoubleClickListener(IDoubleClickListener listener) {
      viewer.addDoubleClickListener(listener);
    }

    
    protected void addButton(String key, Button button) {
      buttons.put(key, button);
    }

    protected void createAddButton() {
      addButton(ADD, createButton(Messages.ListEditorComposite_btnAdd));
    }

    protected void createCreateButton() {
      addButton(CREATE, createButton(Messages.ListEditorComposite_btnCreate));
    }

    protected void createRemoveButton() {
      addButton(REMOVE, createButton(Messages.ListEditorComposite_btnRemove));
    }

    protected Button createButton(String text) {
      Button button = toolkit.createButton(this, text, SWT.FLAT);
      GridData gd = new GridData(SWT.FILL, SWT.TOP, false, false);
      gd.verticalIndent = 0;
      button.setLayoutData(gd);
      button.setEnabled(false);
      return button;
    }    


    /**
     * Create the buttons that populate the column to the right of the ListViewer.
     * Subclasses must call the helper method addButton to add each button to the
     * composite.
     * 
     * @param includeSearch true if the search button should be created
     */

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

    protected void viewerSelectionChanged() {
      updateRemoveButton();
      updatePropertiesButton();
    }

    protected void updatePropertiesButton() {
      boolean enable = !viewer.getSelection().isEmpty() && !isBadSelection();
      properties.setEnabled(!readOnly && enable);
    }
    
    protected void updateRemoveButton() {
      boolean enable = !viewer.getSelection().isEmpty() && !isBadSelection();
      getRemoveButton().setEnabled(!readOnly && enable);
    }
    
    public void setReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
      for (Map.Entry<String, Button> entry : buttons.entrySet()) {
        if (entry.getKey().equals(REMOVE)) {
          //Special case, as it makes no sense to enable if it there's nothing selected.
          updateRemoveButton();
        } else {
          //TODO: mkleint this is fairly dangerous thing to do, each button shall be handled individually based on context.
          entry.getValue().setEnabled(!readOnly);
        }
      }
      updatePropertiesButton();
    }
    
    /**
     * Returns true if the viewer has no input or if there is currently
     * an inherited dependency selected
     * @return
     */
    protected boolean isBadSelection() {
      @SuppressWarnings("unchecked")
      List<Object> deps = (List<Object>) viewer.getInput();
      if (deps == null || deps.isEmpty()) {
        return true;
      }
      boolean bad = false;
      IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
      @SuppressWarnings("unchecked")
      Iterator<Object> iter = selection.iterator();
      while (iter.hasNext()) {
        Object obj = iter.next();
        if (obj instanceof org.apache.maven.model.Dependency) {
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
      if (model.getDependencies() != null) {
        for (Dependency d : model.getDependencies()) {
          if (d.getVersion() != null) {
            hasNonManaged = true;
            break;
          }
        }
      }
      manage.setEnabled(!readOnly && hasNonManaged);
    }
    
    public void setManageButtonListener(SelectionListener listener) {
      manage.addSelectionListener(listener);
    }
  }

  public void mavenProjectHasChanged() {
    temporaryRemovedDependencies.clear();
    //MNGECLIPSE-2673 when maven project changes and we show the inherited items, update now..
    if (showInheritedDependencies) {
      setDependenciesInput();
    }
  }
  
  private static class DependencyContentProvider<T> implements ITreeContentProvider {
    private final Object[] EMPTY = new Object[0];
    private boolean shouldSort;
    private Comparator<T> comparator;

    public void dispose() {
      // TODO Auto-generated method stub
      
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // TODO Auto-generated method stub
      
    }

    @SuppressWarnings("unchecked")
    public Object[] getElements(Object input) {
      if(input instanceof List) {
        List<T> list = (List<T>) input;
        if (shouldSort) {
          T[] array = (T[]) list.toArray();
          Arrays.<T>sort(array, comparator);
          return array;
        }
        return list.toArray();
      }
      return EMPTY;
    }

    public Object[] getChildren(Object parentElement) {
      return null;
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return false;
    }
    public void setShouldSort(boolean shouldSort) {
      this.shouldSort = shouldSort;
    }
    
    public void setComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }    
    
  }
  
}
