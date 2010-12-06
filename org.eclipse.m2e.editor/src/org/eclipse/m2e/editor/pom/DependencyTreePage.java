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

package org.eclipse.m2e.editor.pom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;
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
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;


/**
 * @author Eugene Kuleshov
 * @author Benjamin Bentmann
 */
public class DependencyTreePage extends FormPage implements IMavenProjectChangedListener, IPomFileChangedListener {

  protected static final Object[] EMPTY = new Object[0];

  final MavenPomEditor pomEditor;

  TreeViewer treeViewer;

  TableViewer listViewer;

  SearchControl searchControl;

  SearchMatcher searchMatcher;

  DependencyFilter searchFilter;

  ListSelectionFilter listSelectionFilter;

  ViewerFilter currentFilter;

  ArrayList<DependencyNode> dependencyNodes = new ArrayList<DependencyNode>();

  Color searchHighlightColor;

  MavenProject mavenProject;

  boolean isSettingSelection = false;

  Action hierarchyFilterAction;

  private Job dataLoadingJob;

  String currentClasspath = Artifact.SCOPE_TEST;

  public DependencyTreePage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.dependencyTree", Messages.DependencyTreePage_title); //$NON-NLS-1$
    this.pomEditor = pomEditor;
  }

  protected void createFormContent(IManagedForm managedForm) {
    MavenPlugin.getDefault().getMavenProjectManager().addMavenProjectChangedListener(this);

    FormToolkit formToolkit = managedForm.getToolkit();

    searchHighlightColor = new Color(Display.getDefault(), 242, 218, 170);

    ScrolledForm form = managedForm.getForm();
    form.setText(formatFormTitle());
    form.setExpandHorizontal(true);
    form.setExpandVertical(true);

    Composite body = form.getBody();
    body.setLayout(new FillLayout());

    SashForm sashForm = new SashForm(body, SWT.NONE);
    formToolkit.adapt(sashForm);
    formToolkit.adapt(sashForm, true, true);

    createHierarchySection(sashForm, formToolkit);

    createListSection(sashForm, formToolkit);

    sashForm.setWeights(new int[] {1, 1});

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
    treeViewer.setInput(null);
    listViewer.setInput(null);
    FormUtils.setMessage(getManagedForm().getForm(), Messages.DependencyTreePage_message_resolving, IMessageProvider.WARNING);

    dataLoadingJob = new Job(Messages.DependencyTreePage_job_loading) {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          mavenProject = pomEditor.readMavenProject(force, monitor);
          if(mavenProject == null){
            MavenLogger.log("Unable to read maven project. Dependencies not updated.", null); //$NON-NLS-1$
            return Status.CANCEL_STATUS;
          }

          final DependencyNode dependencyNode = pomEditor.readDependencyTree(force, currentClasspath, monitor);
          if(dependencyNode == null) {
            return Status.CANCEL_STATUS;
          }
          dependencyNode.accept(new DependencyVisitor() {
            public boolean visitEnter(DependencyNode node) {
              if(node.getDependency() != null) {
                dependencyNodes.add(node);
              }
              return true;
            }

            public boolean visitLeave(DependencyNode dependencynode) {
              return true;
            }
          });

          getPartControl().getDisplay().syncExec(new Runnable() {
            public void run() {
              FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.NONE);
              treeViewer.setInput(dependencyNode);
              treeViewer.expandAll();
              listViewer.setInput(mavenProject);
            }
          });
        } catch(final CoreException ex) {
          MavenLogger.log(ex);
          getPartControl().getDisplay().asyncExec(new Runnable() {
            public void run() {
              FormUtils.setMessage(getManagedForm().getForm(), ex.getMessage(), IMessageProvider.ERROR);
            }
          });
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

    treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if(!isSettingSelection) {
          isSettingSelection = true;
          IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
          selectListElements(new DependencyNodeMatcher(selection));
          isSettingSelection = false;
        }
      }
    });

    treeViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
        for(Iterator<?> it = selection.iterator(); it.hasNext();) {
          Object o = it.next();
          if(o instanceof DependencyNode) {
            org.sonatype.aether.artifact.Artifact a = ((DependencyNode) o).getDependency().getArtifact();
            OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion(), null);
          }
        }
      }
    });

    createHierarchyToolbar(hierarchySection, treeLabelProvider, formToolkit);
  }

  private void createHierarchyToolbar(Section hierarchySection, final DependencyTreeLabelProvider treeLabelProvider,
      FormToolkit formToolkit) {
    ToolBarManager hiearchyToolBarManager = new ToolBarManager(SWT.FLAT);

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_collapseAll, MavenEditorImages.COLLAPSE_ALL) {
      public void run() {
        treeViewer.collapseAll();
      }
    });

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_expandAll, MavenEditorImages.EXPAND_ALL) {
      public void run() {
        treeViewer.expandAll();
      }
    });

    hiearchyToolBarManager.add(new Separator());

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_sort, MavenEditorImages.SORT) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(treeViewer.getComparator() == null) {
          treeViewer.setComparator(new ViewerComparator());
        } else {
          treeViewer.setComparator(null);
        }
      }
    });

    hiearchyToolBarManager.add(new Action(Messages.DependencyTreePage_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        treeLabelProvider.setShowGroupId(isChecked());
        treeViewer.refresh();
      }
    });

    hierarchyFilterAction = new Action(Messages.DependencyTreePage_action_filterSearch, MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(isChecked()) {
          setTreeFilter(currentFilter, true);
//          treeViewer.setFilters(new ViewerFilter[] {searchFilter, listSelectionFilter});
        } else {
          treeViewer.removeFilter(searchFilter);
        }
        treeViewer.refresh();
        treeViewer.expandAll();
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

    listViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        IStructuredSelection selection = (IStructuredSelection) listViewer.getSelection();
        for(Iterator<?> it = selection.iterator(); it.hasNext();) {
          Object o = it.next();
          if(o instanceof Artifact) {
            Artifact a = (Artifact) o;
            OpenPomAction.openEditor(a.getGroupId(), a.getArtifactId(), a.getVersion(), null);
          }
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

      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        if(listViewer.getComparator() == null) {
          listViewer.setComparator(new ViewerComparator());
        } else {
          listViewer.setComparator(null);
        }
      }
    });

    listToolBarManager.add(new Action(Messages.DependencyTreePage_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

      public void run() {
        listLabelProvider.setShowGroupId(isChecked());
        listViewer.refresh();
      }
    });

    listToolBarManager.add(new Action(Messages.DependencyTreePage_action_filter, MavenEditorImages.FILTER) {
      public int getStyle() {
        return AS_CHECK_BOX;
      }

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
      
      public Menu getMenu(Menu parent) {
        return null;
      }

      public Menu getMenu(Control parent) {
        if (menu != null) {
          menu.dispose();
        }
        
        menu = new Menu(parent);
        addToMenu(menu, Messages.DependencyTreePage_scope_all, Artifact.SCOPE_TEST, currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_comp_runtime, Artifact.SCOPE_COMPILE_PLUS_RUNTIME, currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_compile, Artifact.SCOPE_COMPILE, currentClasspath);
        addToMenu(menu, Messages.DependencyTreePage_scope_runtime, Artifact.SCOPE_RUNTIME, currentClasspath);
        return menu;
      }
      
      protected void addToMenu(Menu parent, String text, String scope, String currentScope) {
        ClasspathAction action = new ClasspathAction(text, scope);
        action.setChecked(scope.equals(currentScope));
        new ActionContributionItem(action).fill(parent, -1);
      }
      
      public void dispose() {
        if (menu != null)  {
          menu.dispose();
          menu = null;
        }
      }
    }
    toolBarManager.add(new ClasspathDropdown());
    
    toolBarManager.add(new Separator());
    toolBarManager.add(new Action(Messages.DependencyTreePage_action_refresh, MavenEditorImages.REFRESH) {
      public void run() {
        loadData(true);
      }
    });

    form.updateToolBar();

    searchControl.getSearchText().addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        isSettingSelection = true;
        selectListElements(searchMatcher);
        selectTreeElements(searchMatcher);
        setTreeFilter(searchFilter, false);
        isSettingSelection = false;
      }
    });

    searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        isSettingSelection = true;
        selectListElements(searchMatcher);
        selectTreeElements(searchMatcher);
        setTreeFilter(searchFilter, false);
        isSettingSelection = false;
      }
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
    treeViewer.refresh();
    treeViewer.expandAll();

    if(!matcher.isEmpty()) {
      for(DependencyNode node : dependencyNodes) {
        org.sonatype.aether.artifact.Artifact a = node.getDependency().getArtifact();
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

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(matcher != null && !matcher.isEmpty()) {
        // matcher = new TextMatcher(searchControl.getSearchText().getText());
        if(element instanceof Artifact) {
          Artifact a = (Artifact) element;
          return matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId());

        } else if(element instanceof DependencyNode) {
          DependencyNode node = (DependencyNode) element;
          org.sonatype.aether.artifact.Artifact a = node.getDependency().getArtifact();
          if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
            return true;
          }

          class ChildMatcher implements DependencyVisitor {
            protected boolean foundMatch = false;

            public boolean visitEnter(DependencyNode node) {
              org.sonatype.aether.artifact.Artifact a = node.getDependency().getArtifact();
              if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
                foundMatch = true;
                return false;
              }
              return true;
            }

            public boolean visitLeave(DependencyNode node) {
              return true;
            }
          }
          ;

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

    public void focusGained(FocusEvent e) {
      if(hierarchyFilterAction.isChecked()) {
        setTreeFilter(this, false);
//        treeViewer.addFilter(this);
      }
    }

    public void focusLost(FocusEvent e) {
//      treeViewer.removeFilter(this);
      matcher = null;
    }
  }

  final class DependencyTreeContentProvider implements ITreeContentProvider {

    public Object[] getElements(Object input) {
      return getChildren(input);
    }

    public Object[] getChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        List<DependencyNode> children = node.getChildren();
        return children.toArray(new DependencyNode[children.size()]);
      }
      return new Object[0];
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        return !node.getChildren().isEmpty();
      }
      return false;
    }

    public void dispose() {
    }

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

    public Color getForeground(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        String scope = node.getDependency().getScope();
        if(scope != null && !"compile".equals(scope)) { //$NON-NLS-1$
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      if(matcher != null && !matcher.isEmpty() && element instanceof DependencyNode) {
        org.sonatype.aether.artifact.Artifact a = ((DependencyNode) element).getDependency().getArtifact();
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          return searchHighlightColor;
        }
      }
      return null;
    }

    // LabelProvider

    @Override
    public String getText(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;

        org.sonatype.aether.artifact.Artifact a = node.getDependency().getArtifact();

        org.sonatype.aether.artifact.Artifact c = null;
        if(!node.getAliases().isEmpty()) {
          c = node.getAliases().iterator().next();
        }
        
        StringBuilder label = new StringBuilder(128);

        if(showGroupId) {
          label.append(a.getGroupId()).append(" : ");
        }

        label.append(a.getArtifactId()).append(" : ");

        label.append(a.getBaseVersion());

        if(node.getPremanagedVersion() != null && !node.getPremanagedVersion().equals(a.getBaseVersion())) {
          label.append(" (managed from ").append(node.getPremanagedVersion()).append(")");
        }

        if(c != null) {
          String version = c.getBaseVersion();
          if(!a.getBaseVersion().equals(version)) {
            label.append(" (omitted for conflict with ").append(version).append(")");
          }
        }

        if(a.getClassifier().length() > 0) {
          label.append(Messages.DependencyTreePage_0).append(a.getClassifier());
        }

        label.append(" [").append(node.getDependency().getScope()).append("]");

        if(node.getPremanagedScope() != null) {
          label.append(" (from ").append(node.getPremanagedScope()).append(")");
        }

        return label.toString();
      }
      return element.toString();
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof DependencyNode) {
        DependencyNode node = (DependencyNode) element;
        org.sonatype.aether.artifact.Artifact a = node.getDependency().getArtifact();
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
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

    public Color getForeground(Object element) {
      if(element instanceof Artifact) {
        Artifact a = (Artifact) element;
        String scope = a.getScope();
        if(scope != null && !"compile".equals(scope)) { //$NON-NLS-1$
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      if(matcher != null && !matcher.isEmpty() && element instanceof Artifact) {
        Artifact a = (Artifact) element;
        if(matcher.isMatchingArtifact(a.getGroupId(), a.getArtifactId())) {
          return searchHighlightColor;
        }
      }
      return null;
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
        MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
        IMavenProjectFacade projectFacade = projectManager.getMavenProject(a.getGroupId(), //
            a.getArtifactId(), //
            a.getBaseVersion() == null ? a.getVersion() : a.getBaseVersion());
        return projectFacade == null ? MavenEditorImages.IMG_JAR : MavenEditorImages.IMG_PROJECT;
      }
      return null;
    }

  }

  public class DependencyListContentProvider implements IStructuredContentProvider {

    public Object[] getElements(Object input) {
      if(input instanceof MavenProject) {
        MavenProject project = (MavenProject) input;
        List<Artifact> artifacts = new ArrayList<Artifact>();
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

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }

  }

  public static class ArtifactMatcher extends Matcher {

    protected final HashSet<String> artifactKeys = new HashSet<String>();

    public ArtifactMatcher(IStructuredSelection selection) {
      for(Iterator<?> it = selection.iterator(); it.hasNext();) {
        addArtifactKey(it.next());
      }
    }

    public boolean isEmpty() {
      return artifactKeys.isEmpty();
    }

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
        org.sonatype.aether.artifact.Artifact a = ((DependencyNode) o).getDependency().getArtifact();
        artifactKeys.add(getKey(a.getGroupId(), a.getArtifactId()));
      }
    }

  }

  @Override
  public void dispose() {
    MavenPlugin.getDefault().getMavenProjectManager().removeMavenProjectChangedListener(this);

    if(searchHighlightColor != null) {
      searchHighlightColor.dispose();
    }
    super.dispose();
  }
  
  public void selectDepedency(ArtifactKey artifactKey) {
    if(dataLoadingJob!=null && dataLoadingJob.getState()==Job.RUNNING) {
      try {
        dataLoadingJob.join();
      } catch(InterruptedException ex) {
        // ignore
      }
    }

    if(mavenProject!=null) {
      Artifact artifact = getArtifact(artifactKey);
      if(artifact!=null) {
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

  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    if (getManagedForm() == null || getManagedForm().getForm() == null)
      return;
    
    for (int i=0; i<events.length; i++) {
      if (events[i].getSource().equals(((MavenPomEditor) getEditor()).getPomFile())) {
        // file has been changed. need to update graph  
        new UIJob(Messages.DependencyTreePage_job_reloading) {
          public IStatus runInUIThread(IProgressMonitor monitor) {
            loadData();
            FormUtils.setMessage(getManagedForm().getForm(), null, IMessageProvider.WARNING);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    }
  }

  public void fileChanged() {
    if (getManagedForm() == null || getManagedForm().getForm() == null)
      return;
    
    new UIJob(Messages.DependencyTreePage_job_reloading) {
      public IStatus runInUIThread(IProgressMonitor monitor) {
        FormUtils.setMessage(getManagedForm().getForm(), Messages.DependencyTreePage_message_updating, IMessageProvider.WARNING);
        return Status.OK_STATUS;
      }
    }.schedule();
  }
}
