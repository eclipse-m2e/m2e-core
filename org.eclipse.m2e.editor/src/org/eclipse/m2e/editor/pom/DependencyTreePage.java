/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.editor.pom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.services.IDisposable;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * @author Eugene Kuleshov
 * @author Benjamin Bentmann
 */
public class DependencyTreePage extends FormPage implements IMavenProjectChangedListener, IPomFileChangedListener {
  private static final Logger log = LoggerFactory.getLogger(DependencyTreePage.class);

  protected static final Object[] EMPTY = new Object[0];

  final MavenPomEditor pomEditor;

  TreeViewer treeViewer;

  TableViewer listViewer;

  SearchControl searchControl;

  SearchMatcher searchMatcher;

  DependencyFilter searchFilter;

  ListSelectionFilter listSelectionFilter;

  ViewerFilter currentFilter;

  ArrayList<DependencyNode> dependencyNodes = new ArrayList<>();

  Highlighter highlighter;

  MavenProject mavenProject;

  boolean isSettingSelection = false;

  Action hierarchyFilterAction;

  private Job dataLoadingJob;

  String currentClasspath = Artifact.SCOPE_TEST;

  public DependencyTreePage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.dependencyTree", Messages.DependencyTreePage_title); //$NON-NLS-1$
    this.pomEditor = pomEditor;
  }

  @Override
  protected void createFormContent(IManagedForm managedForm) {
    MavenPluginActivator.getDefault().getMavenProjectManager().addMavenProjectChangedListener(this);

    FormToolkit formToolkit = managedForm.getToolkit();

    ScrolledForm form = managedForm.getForm();
    form.setText(formatFormTitle());
    form.setExpandHorizontal(true);
    form.setExpandVertical(true);

    Composite body = form.getBody();
    body.setLayout(new FillLayout());

    SashForm sashForm = new SashForm(body, SWT.NONE);
    formToolkit.adapt(sashForm);
    formToolkit.adapt(sashForm, true, true);

    highlighter = new Highlighter();

    createHierarchySection(sashForm, formToolkit);

    createListSection(sashForm, formToolkit);

    sashForm.setWeights(1, 1);

    createSearchBar(managedForm);

    // compatibility proxy to support Eclipse 3.2
    FormUtils.decorateHeader(managedForm.getToolkit(), form.getForm());

    initPopupMenu(treeViewer, ".tree"); //$NON-NLS-1$
    initPopupMenu(listViewer, ".list"); //$NON-NLS-1$

    loadData(false);
  }

  private void initPopupMenu(Viewer viewer, String id) {
    MenuManager menuMgr = new MenuManager("#PopupMenu-" + id); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);

    Menu menu = menuMgr.createContextMenu(viewer.getControl());

    viewer.getControl().setMenu(menu);

    getEditorSite().registerContextMenu(MavenPomEditor.EDITOR_ID + id, menuMgr, viewer, false);
  }

  String formatFormTitle() {
    return NLS.bind(Messages.DependencyTreePage_form_title, currentClasspath);
  }

  void loadData(final boolean force) {
    // form.setMessage() forces the panel layout, which messes up the viewers
    // (e.g. long entries in the tree cause it to expand horizontally so much
    // doesn't fit into the editor anymore). Clearing the input in the viewers
    // helps to ensure they won't change the size when the message is set.
    if(treeViewer.getTree().isDisposed()) {
      return;
    }
    treeViewer.setInput(null);
    if(listViewer.getTable().isDisposed()) {
      return;
    }
    listViewer.setInput(null);
    FormUtils.setMessage(getManagedForm().getForm(), Messages.DependencyTreePage_message_resolving,
        IMessageProvider.WARNING);

    dataLoadingJob = new Job(Messages.DependencyTreePage_job_loading) {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          mavenProject = pomEditor.readMavenProject(force, monitor);
          if(mavenProject == null) {
            log.error("Unable to read maven project. Dependencies not updated."); //$NON-NLS-1$
            return Status.CANCEL_STATUS;
          }

          final DependencyNode dependencyNode = pomEditor.readDependencyTree(force, currentClasspath, monitor);
          if(dependencyNode == null) {
            return Status.CANCEL_STATUS;
          }
          dependencyNode.accept(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
              if(node.getDependency() != null) {
                dependencyNodes.add(node);
              }
              return true;
            }

            @Override
            public boolean visitLeave(DependencyNode dependencynode) {
              return true;
            }
          });

          getPartControl().getDisplay().syncExec(() -> {
            FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.NONE);
            if(treeViewer.getTree().isDisposed()) {
              return;
            }

            treeViewer.setInput(dependencyNode);
            treeViewer.getTree().setRedraw(false);
            try {
              treeViewer.expandAll();
            } finally {
              treeViewer.getTree().setRedraw(true);
            }
            if(listViewer.getTable().isDisposed()) {
              return;
            }
            listViewer.setInput(mavenProject);
          });
        } catch(final CoreException ex) {
          log.error(ex.getMessage(), ex);
          getPartControl().getDisplay().asyncExec(
              () -> FormUtils.setMessage(getManagedForm().getForm(), ex.getMessage(), IMessageProvider.ERROR));
        }

        return Status.OK_STATUS;
      }
    };
    dataLoadingJob.schedule();
  }

  private void createHierarchySection(Composite sashForm, FormToolkit formToolkit) {
    Composite hierarchyComposite = formToolkit.createComposite(sashForm, SWT.NONE);
    hierarchyComposite.setLayout(new GridLayout());

    Section hierarchySection = formToolkit.createSection(hierarchyComposite, ExpandableComposite.TITLE_BAR);
    hierarchySection.marginHeight = 1;
    GridData gd_hierarchySection = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd_hierarchySection.widthHint = 100;
    gd_hierarchySection.minimumWidth = 100;
    hierarchySection.setLayoutData(gd_hierarchySection);
    hierarchySection.setText(Messages.DependencyTreePage_section_hierarchy);
    formToolkit.paintBordersFor(hierarchySection);

    Tree tree = formToolkit.createTree(hierarchySection, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI);
    hierarchySection.setClient(tree);

    treeViewer = new TreeViewer(tree);
    treeViewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);

    DependencyTreeLabelProvider treeLabelProvider = new DependencyTreeLabelProvider();
    treeViewer.setContentProvider(new DependencyTreeContentProvider());
    treeViewer.setLabelProvider(treeLabelProvider);

    treeViewer.addSelectionChangedListener(event -> {
      if(!isSettingSelection) {
        isSettingSelection = true;
        IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
        selectListElements(new DependencyNodeMatcher(selection));
        isSettingSelection = false;
      }
    });

    treeViewer.addOpenListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
      for(Object o : selection) {
        if(o instanceof DependencyNode) {
          org.eclipse.aether.artifact.Artifact a = ((DependencyNode) o).getDependency().getArtifact();
          OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion(), mavenProject, null);
        }
      }
    });

    createHierarchyToolbar(hierarchySection, treeLabelProvider, formToolkit);
  }

  private void createHierarchyToolbar(Section hierarchySection, final DependencyTreeLabelProvider treeLabelProvider,
      FormToolkit formToolkit) {
    ToolBarManager hiearchyToolBarManager = new ToolBarManager(SWT.FLAT);

    hiearchyToolBarManager
        .add(new Action(Messages.DependencyTreePage_action_collapseAll, MavenEditorImages.COLLAPSE_ALL) {
          @Override
          public void run() {
            treeViewer.getTree().setRedraw(false);
            try {
              treeViewer.collapseAll();
            } finally {
              treeViewer.getTree().setRedraw(true);
            }
          }
        });

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_expandAll, MavenEditorImages.EXPAND_ALL) {
      @Override
      public void run() {
        treeViewer.getTree().setRedraw(false);
        try {
          treeViewer.expandAll();
        } finally {
          treeViewer.getTree().setRedraw(true);
        }
      }
    });

    hiearchyToolBarManager.add(new Separator());

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_sort, MavenEditorImages.SORT) {
      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        if(treeViewer.getComparator() == null) {
          treeViewer.setComparator(new ViewerComparator());
        } else {
          treeViewer.setComparator(null);
        }
      }
    });

    hiearchyToolBarManager
        .add(new Action(Messages.DependencyTreePage_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
          @Override
          public int getStyle() {
            return AS_CHECK_BOX;
          }

          @Override
          public void run() {
            treeLabelProvider.setShowGroupId(isChecked());
            treeViewer.refresh();
          }
        });

    hierarchyFilterAction = new Action(Messages.DependencyTreePage_action_filterSearch, MavenEditorImages.FILTER) {
      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        if(isChecked()) {
          setTreeFilter(currentFilter, true);
//          treeViewer.setFilters(new ViewerFilter[] {searchFilter, listSelectionFilter});
        } else {
          treeViewer.removeFilter(searchFilter);
        }
        treeViewer.getTree().setRedraw(false);
        try {
          treeViewer.refresh();
          treeViewer.expandAll();
        } finally {
          treeViewer.getTree().setRedraw(true);
        }
      }
    };
    hierarchyFilterAction.setChecked(true);
    hiearchyToolBarManager.add(hierarchyFilterAction);

    Composite toolbarComposite = formToolkit.createComposite(hierarchySection);
    toolbarComposite.setBackground(null);
    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = false;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    toolbarComposite.setLayout(rowLayout);

    hiearchyToolBarManager.createControl(toolbarComposite);
    hierarchySection.setTextClient(toolbarComposite);
  }

  private void createListSection(SashForm sashForm, FormToolkit formToolkit) {
    Composite listComposite = formToolkit.createComposite(sashForm, SWT.NONE);
    listComposite.setLayout(new GridLayout());

    Section listSection = formToolkit.createSection(listComposite, ExpandableComposite.TITLE_BAR);
    listSection.marginHeight = 1;
    GridData gd_listSection = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd_listSection.widthHint = 100;
    gd_listSection.minimumWidth = 100;
    listSection.setLayoutData(gd_listSection);
    listSection.setText(Messages.DependencyTreePage_section_resolvedDeps);
    formToolkit.paintBordersFor(listSection);

    final DependencyListLabelProvider listLabelProvider = new DependencyListLabelProvider();

    Table table = formToolkit.createTable(listSection, SWT.FLAT | SWT.MULTI);
    listSection.setClient(table);

    // listViewer = new TableViewer(listSection, SWT.FLAT | SWT.MULTI);
    listViewer = new TableViewer(table);
    listViewer.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.TRUE);
    listViewer.setContentProvider(new DependencyListContentProvider());
    listViewer.setLabelProvider(listLabelProvider);
    listViewer.setComparator(new ViewerComparator()); // by default is sorted

    listSelectionFilter = new ListSelectionFilter();
    listViewer.addSelectionChangedListener(listSelectionFilter);
    listViewer.getTable().addFocusListener(listSelectionFilter);

    listViewer.addOpenListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
      for(Object o : selection) {
        if(o instanceof Artifact) {
          Artifact a = (Artifact) o;
          OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion(), mavenProject, null);
        }
      }
    });

    createListToolbar(listSection, listLabelProvider, formToolkit);

  }

  private void createListToolbar(Section listSection, final DependencyListLabelProvider listLabelProvider,
      FormToolkit formToolkit) {
    ToolBarManager listToolBarManager = new ToolBarManager(SWT.FLAT);

    listToolBarManager.add(new Action(Messages.DependencyTreePage_action_sort, MavenEditorImages.SORT) {
      {
        setChecked(true);
      }

      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        if(listViewer.getComparator() == null) {
          listViewer.setComparator(new ViewerComparator());
        } else {
          listViewer.setComparator(null);
        }
      }
    });

    listToolBarManager.add(new Action(Messages.DependencyTreePage_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        listLabelProvider.setShowGroupId(isChecked());
        listViewer.refresh();
      }
    });

    listToolBarManager.add(new Action(Messages.DependencyTreePage_action_filter, MavenEditorImages.FILTER) {
      @Override
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      @Override
      public void run() {
        if(listViewer.getFilters() == null || listViewer.getFilters().length == 0) {
          listViewer.addFilter(searchFilter);
        } else {
          listViewer.removeFilter(searchFilter);
        }
      }
    });

    Composite toolbarComposite = formToolkit.createComposite(listSection);
    toolbarComposite.setBackground(null);
    RowLayout rowLayout = new RowLayout();
    rowLayout.wrap = false;
    rowLayout.marginRight = 0;
    rowLayout.marginLeft = 0;
    rowLayout.marginTop = 0;
    rowLayout.marginBottom = 0;
    toolbarComposite.setLayout(rowLayout);

    listToolBarManager.createControl(toolbarComposite);
    listSection.setTextClient(toolbarComposite);
  }

  private void createSearchBar(IManagedForm managedForm) {
    searchControl = new SearchControl(Messages.DependencyTreePage_find, managedForm);
    searchMatcher = new SearchMatcher(searchControl);
    searchFilter = new DependencyFilter(new SearchMatcher(searchControl));
    treeViewer.addFilter(searchFilter); // by default is filtered

    ScrolledForm form = managedForm.getForm();

    IToolBarManager toolBarManager = form.getForm().getToolBarManager();
    toolBarManager.add(searchControl);

    class ClasspathDropdown extends Action implements IMenuCreator {
      private Menu menu;

      public ClasspathDropdown() {
        setText(Messages.DependencyTreePage_classpath);
        setImageDescriptor(MavenEditorImages.SCOPE);
        setMenuCreator(this);
      }

      @Override
      public Menu getMenu(Menu parent) {
        return null;
      }

      @Override
      public Menu getMenu(Control parent) {
        if(menu != null) {
          menu.dispose();
        }

        menu = new Menu(parent);
        addToMenu(menu, Messages.DependencyTreePage_scope_all, Artifact.SCOPE_TEST, currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_comp_runtime, Artifact.SCOPE_COMPILE_PLUS_RUNTIME,
            currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_compile, Artifact.SCOPE_COMPILE, currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_runtime, Artifact.SCOPE_RUNTIME, currentClasspath);
        return menu;
      }

      protected void addToMenu(Menu parent, String text, String scope, String currentScope) {
        ClasspathAction action = new ClasspathAction(text, scope);
        action.setChecked(scope.equals(currentScope));
        new ActionContributionItem(action).fill(parent, -1);
      }

      @Override
      public void dispose() {
        if(menu != null) {
          menu.dispose();
          menu = null;
        }
      }
    }
    toolBarManager.add(new ClasspathDropdown());

    toolBarManager.add(new Separator());
    toolBarManager.add(new Action(Messages.DependencyTreePage_action_refresh, MavenEditorImages.REFRESH) {
      @Override
      public void run() {
        loadData(true);
      }
    });

    form.updateToolBar();

    // Create a job to update the contents of the viewers when the
    // filter text is modified. Using a job is in this way lets us
    // defer updating the field while the user is typing.
    final Job updateJob = new WorkbenchJob("Update Maven Dependency Viewers") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if(!listViewer.getTable().isDisposed()) {
          isSettingSelection = true;
          selectListElements(searchMatcher);
          selectTreeElements(searchMatcher);
          setTreeFilter(searchFilter, false);
          isSettingSelection = false;
        }
        return Status.OK_STATUS;
      }
    };

    searchControl.getSearchText().addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        // The net effect here is that the field will update 200 ms after
        // the user stops typing.
        updateJob.cancel();
        updateJob.schedule(200);
      }
    });

    searchControl.getSearchText().addModifyListener(e -> {
      updateJob.cancel();
      updateJob.schedule(200);
    });
  }

  protected void setTreeFilter(ViewerFilter filter, boolean force) {
    currentFilter = filter;
    if(filter != null && (force || (treeViewer.getFilters().length > 0 && treeViewer.getFilters()[0] != filter))) {
      treeViewer.addFilter(filter);
    }
  }

  protected void selectListElements(Matcher matcher) {
    DependencyListLabelProvider listLabelProvider = (DependencyListLabelProvider) listViewer.getLabelProvider();
    listLabelProvider.setMatcher(matcher);
    listViewer.refresh();

    if(!matcher.isEmpty() && mavenProject != null) {
      Set<Artifact> projectArtifacts = mavenProject.getArtifacts();
      for(Artifact a : projectArtifacts) {
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          listViewer.reveal(a);
          break;
        }
      }
    }
  }

  void selectTreeElements(Matcher matcher) {
    DependencyTreeLabelProvider treeLabelProvider = (DependencyTreeLabelProvider) treeViewer.getLabelProvider();
    treeLabelProvider.setMatcher(matcher);
    treeViewer.getTree().setRedraw(false);
    try {
      treeViewer.refresh();
      treeViewer.expandAll();
    } finally {
      treeViewer.getTree().setRedraw(true);
    }

    if(!matcher.isEmpty()) {
      for(DependencyNode node : dependencyNodes) {
        org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getGroupId())) {
          treeViewer.reveal(node);
          break;
        }
      }
    }
  }

  static class DependencyFilter extends ViewerFilter {
    protected Matcher matcher;

    public DependencyFilter(Matcher matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(matcher != null && !matcher.isEmpty()) {
        // matcher = new TextMatcher(searchControl.getSearchText().getText());
        if(element instanceof Artifact) {
          Artifact a = (Artifact) element;
          return matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId());

        } else if(element instanceof DependencyNode) {
          DependencyNode node = (DependencyNode) element;
          org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
          if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
            return true;
          }

          class ChildMatcher implements DependencyVisitor {
            protected boolean foundMatch = false;

            @Override
            public boolean visitEnter(DependencyNode node) {
              org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
              if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
                foundMatch = true;
                return false;
              }
              return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
              return true;
            }
          }

          ChildMatcher childMatcher = new ChildMatcher();
          node.accept(childMatcher);
          return childMatcher.foundMatch;
        }
      }
      return true;
    }

  }

  class ListSelectionFilter extends DependencyFilter implements ISelectionChangedListener, FocusListener {

    public ListSelectionFilter() {
      super(null); // no filter by default
    }

    // ISelectionChangedListener

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
      if(!isSettingSelection) {
        isSettingSelection = true;
        IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
        matcher = new ArtifactMatcher(selection);
        selectTreeElements(matcher);
        setTreeFilter(this, false);
        isSettingSelection = false;
      }
    }

    // FocusListener

    @Override
    public void focusGained(FocusEvent e) {
      if(hierarchyFilterAction.isChecked()) {
        setTreeFilter(this, false);
//        treeViewer.addFilter(this);
      }
    }

    @Override
    public void focusLost(FocusEvent e) {
//      treeViewer.removeFilter(this);
      matcher = null;
    }
  }

  final class DependencyTreeContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object input) {
      return getChildren(input);
    }

    @Override
    public Object[] getChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        List<DependencyNode> children = node.getChildren();
        return children.toArray(new DependencyNode[children.size()]);
      }
      return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        return !node.getChildren().isEmpty();
      }
      return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  final class DependencyTreeLabelProvider extends LabelProvider implements IColorProvider {

    private boolean showGroupId = false;

    private Matcher matcher = null;

    public void setMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }

    // IColorProvider

    @Override
    public Color getForeground(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        String scope = node.getDependency().getScope();
        if(scope != null && !"compile".equals(scope) && !isMatching(node)) { //$NON-NLS-1$
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      if(isMatching(element)) {
        return highlighter.getBackgroundColor();
      }
      return null;
    }

    private boolean isMatching(Object element) {
      if(element instanceof DependencyNode) {
        return isMatching(((DependencyNode) element));
      }
      return false;
    }

    private boolean isMatching(DependencyNode node) {
      if(matcher != null && !matcher.isEmpty()) {
        org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
        return matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId());
      }
      return false;
    }

    // LabelProvider

    @Override
    public String getText(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;

        org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();

        StringBuilder label = new StringBuilder(128);

        if(showGroupId) {
          label.append(a.getGroupId()).append(" : ");
        }

        label.append(a.getArtifactId()).append(" : ");

        String nodeVersion = a.getBaseVersion();
        label.append(nodeVersion);

        String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(node);

        if(premanagedVersion != null && !premanagedVersion.equals(nodeVersion)) {
          label.append(" (managed from ").append(premanagedVersion).append(")");
        }

        DependencyNode winner = (DependencyNode) node.getData().get(ConflictResolver.NODE_DATA_WINNER);
        if(winner != null) {
          String winnerVersion = winner.getArtifact().getVersion();
          if(!nodeVersion.equals(winnerVersion)) {
            label.append(" (omitted for conflict with ").append(winnerVersion).append(")");
          }
        }

        if(a.getClassifier().length() > 0) {
          label.append(Messages.DependencyTreePage_0).append(a.getClassifier());
        }

        label.append(" [").append(node.getDependency().getScope()).append("]");

        String premanagedScope = DependencyManagerUtils.getPremanagedScope(node);

        if(premanagedScope != null) {
          label.append(" (from ").append(premanagedScope).append(")");
        }

        return label.toString();
      }
      return element.toString();
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(a.getGroupId(), //
            a.getArtifactId(), //
            a.getBaseVersion() == null ? a.getVersion() : a.getBaseVersion());
        return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
      }
      return null;
    }
  }

  public class DependencyListLabelProvider extends LabelProvider implements IColorProvider {

    private boolean showGroupId = false;

    private Matcher matcher = null;

    public void setMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }

    // IColorProvider

    @Override
    public Color getForeground(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        String scope = a.getScope();
        if(scope != null && !"compile".equals(scope) && !isMatching(a)) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    @Override
    public Color getBackground(Object element) {
      if(element instanceof Artifact && isMatching((Artifact) element)) {
        return highlighter.getBackgroundColor();
      }
      return null;
    }

    private boolean isMatching(Artifact a) {
      if(matcher != null && !matcher.isEmpty()) {
        return matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId());
      }
      return false;
    }
    // LabelProvider

    @Override
    public String getText(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        StringBuilder label = new StringBuilder(64);

        if(showGroupId) {
          label.append(a.getGroupId()).append(" : ");
        }

        label.append(a.getArtifactId()).append(" : ").append(a.getVersion());

        if(a.hasClassifier()) {
          label.append(" - ").append(a.getClassifier());
        }

        if(a.getScope() != null) {
          label.append(" [").append(a.getScope()).append("]");
        }

        return label.toString();
      }
      return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(a.getGroupId(), //
            a.getArtifactId(), //
            a.getBaseVersion() == null ? a.getVersion() : a.getBaseVersion());
        return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
      }
      return null;
    }

  }

  public class DependencyListContentProvider implements IStructuredContentProvider {

    @Override
    public Object[] getElements(Object input) {
      if(input instanceof MavenProject) {
        MavenProject project = (MavenProject) input;
        List<Artifact> artifacts = new ArrayList<>();
        ArtifactFilter filter = new org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter(currentClasspath);
        for(Artifact artifact : project.getArtifacts()) {
          if(filter.include(artifact)) {
            artifacts.add(artifact);
          }
        }
        return artifacts.toArray(new Artifact[artifacts.size()]);
      }
      return null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public void dispose() {
    }

  }

  public static class ArtifactMatcher extends Matcher {

    protected final HashSet<String> artifactKeys = new HashSet<>();

    public ArtifactMatcher(IStructuredSelection selection) {
      for(Object name : selection) {
        addArtifactKey(name);
      }
    }

    @Override
    public boolean isEmpty() {
      return artifactKeys.isEmpty();
    }

    @Override
    public boolean isMatchingArtifact(String groupId, String artifactId) {
      return artifactKeys.contains(getKey(groupId, artifactId));
    }

    protected void addArtifactKey(Object o) {
      if(o instanceof Artifact) {
        Artifact a = (Artifact) o;
        artifactKeys.add(getKey(a.getGroupId(), a.getArtifactId()));
      }
    }

    protected String getKey(String groupId, String artifactId) {
      return groupId + ":" + artifactId;
    }

  }

  public static class DependencyNodeMatcher extends ArtifactMatcher {

    public DependencyNodeMatcher(IStructuredSelection selection) {
      super(selection);
    }

    @Override
    protected void addArtifactKey(Object o) {
      if(o instanceof DependencyNode) {
        org.eclipse.aether.artifact.Artifact a = ((DependencyNode) o).getDependency().getArtifact();
        artifactKeys.add(getKey(a.getGroupId(), a.getArtifactId()));
      }
    }

  }

  @Override
  public void dispose() {
    MavenPluginActivator.getDefault().getMavenProjectManager().removeMavenProjectChangedListener(this);
    if(highlighter != null) {
      highlighter.dispose();
    }
    super.dispose();
  }

  public void selectDepedency(ArtifactKey artifactKey) {
    if(dataLoadingJob != null && dataLoadingJob.getState() == Job.RUNNING) {
      try {
        dataLoadingJob.join();
      } catch(InterruptedException ex) {
        // ignore
      }
    }

    if(mavenProject != null) {
      Artifact artifact = getArtifact(artifactKey);
      if(artifact != null) {
        listViewer.getTable().setFocus();
        listViewer.setSelection(new StructuredSelection(artifact), true);
      }
    }
  }

  private Artifact getArtifact(ArtifactKey artifactKey) {
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      if(artifactKey.equals(new ArtifactKey(artifact))) {
        return artifact;
      }
    }
    return null;
  }

  public class ClasspathAction extends Action {

    private final String classpath;

    public ClasspathAction(String text, String classpath) {
      super(text, IAction.AS_RADIO_BUTTON);
      this.classpath = classpath;
    }

    @Override
    public void run() {
      if(isChecked()) {
        currentClasspath = classpath;
        IManagedForm managedForm = DependencyTreePage.this.getManagedForm();
        managedForm.getForm().setText(formatFormTitle());
        loadData(false);
      }
    }
  }

  public void loadData() {
    loadData(true);
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    for(MavenProjectChangedEvent event : events) {
      if(event.getSource().equals(((MavenPomEditor) getEditor()).getPomFile())) {
        // file has been changed. need to update graph
        new UIJob(Messages.DependencyTreePage_job_reloading) {
          @Override
          public IStatus runInUIThread(IProgressMonitor monitor) {
            loadData();
            FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.WARNING);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    }
  }

  @Override
  public void fileChanged() {
    if(getManagedForm() == null || getManagedForm().getForm() == null)
      return;

    new UIJob(Messages.DependencyTreePage_job_reloading) {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        FormUtils.setMessage(getManagedForm().getForm(), Messages.DependencyTreePage_message_updating,
            IMessageProvider.WARNING);
        return Status.OK_STATUS;
      }
    }.schedule();
  }

  /**
   * Holds highlight color, bound to "org.eclipse.search.ui.match.highlight" preference. Updates when preference
   * changes.
   */
  private static class Highlighter implements IPropertyChangeListener, IDisposable {

    private static final String HIGHLIGHT_BG_COLOR_NAME = "org.eclipse.search.ui.match.highlight";

    private Color backgroundColor;

    public Highlighter() {
      initialize();
    }

    public void initialize() {
      dispose();
      JFaceResources.getColorRegistry().addListener(this);
      setColors();
    }

    private void setColors() {
      backgroundColor = JFaceResources.getColorRegistry().get(HIGHLIGHT_BG_COLOR_NAME);
    }

    @Override
    public void dispose() {
      JFaceResources.getColorRegistry().removeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      String property = event.getProperty();
      if(HIGHLIGHT_BG_COLOR_NAME.equals(property)) {
        Display.getDefault().asyncExec(() -> setColors());
      }
    }

    Color getBackgroundColor() {
      return backgroundColor;
    }

  }

}
