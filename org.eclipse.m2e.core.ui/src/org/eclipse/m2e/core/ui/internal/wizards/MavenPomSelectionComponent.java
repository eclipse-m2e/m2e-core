/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import org.apache.lucene.search.BooleanQuery;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.index.UserInputSearchExpression;
import org.eclipse.m2e.core.internal.index.filter.ArtifactFilterManager;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * MavenPomSelectionComposite
 * 
 * @author Eugene Kuleshov
 */
@SuppressWarnings("restriction")
public class MavenPomSelectionComponent extends Composite {

  public static final String PROP_DECORATION_CONTEXT_PROJECT = M2EUIPluginActivator.PLUGIN_ID
      + ".decorationContextProject"; //$NON-NLS-1$

  /* (non-Javadoc)
   * @see org.eclipse.swt.widgets.Widget#dispose()
   */
  public void dispose() {
    if(searchJob != null) {
      searchJob.cancel();
    }
    super.dispose();
  }

  Text searchText = null;

  TreeViewer searchResultViewer = null;

  /**
   * One of {@link IIndex#SEARCH_ARTIFACT}, {@link IIndex#SEARCH_CLASS_NAME},
   */
  String queryType;

  SearchJob searchJob;

  private IStatus status;

  private ISelectionChangedListener selectionListener;

  /**
   * @deprecated
   */
  @Deprecated
  public static final String P_SEARCH_INCLUDE_JAVADOC = "searchIncludesJavadoc"; //$NON-NLS-1$

  /**
   * @deprecated
   */
  @Deprecated
  public static final String P_SEARCH_INCLUDE_SOURCES = "searchIncludesSources"; //$NON-NLS-1$

  /**
   * @deprecated
   */
  @Deprecated
  public static final String P_SEARCH_INCLUDE_TESTS = "searchIncludesTests"; //$NON-NLS-1$

  private static final long SHORT_DELAY = 150L;

  private static final long LONG_DELAY = 500L;

  final HashSet<String> artifactKeys = new HashSet<>();

  final HashSet<String> managedKeys = new HashSet<>();

  private IProject project;

  public MavenPomSelectionComponent(Composite parent, int style) {
    super(parent, style);
    createSearchComposite();
  }

  private void createSearchComposite() {
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);

    Label searchTextlabel = new Label(this, SWT.NONE);
    searchTextlabel.setText(Messages.MavenPomSelectionComponent_search_title);
    searchTextlabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

    searchText = new Text(this, SWT.BORDER | SWT.SEARCH);
    searchText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    searchText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          searchResultViewer.getTree().setFocus();
          selectFirstElementInTheArtifactTreeIfNoSelectionHasBeenMade();
        }
      }
    });

    searchText.addModifyListener(e -> scheduleSearch(searchText.getText(), true));

    if(!MavenPlugin.getMavenConfiguration().isUpdateIndexesOnStartup()) {
      createWarningArea(this);
    }

    Label searchResultsLabel = new Label(this, SWT.NONE);
    searchResultsLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    searchResultsLabel.setText(Messages.MavenPomSelectionComponent_lblResults);

    Tree tree = new Tree(this, SWT.BORDER | SWT.SINGLE);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    tree.setData("name", "searchResultTree"); //$NON-NLS-1$ //$NON-NLS-2$
    tree.addFocusListener(new FocusListener() {

      public void focusGained(FocusEvent e) {
        selectFirstElementInTheArtifactTreeIfNoSelectionHasBeenMade();
      }

      public void focusLost(FocusEvent e) {

      }
    });

    searchResultViewer = new TreeViewer(tree);
  }

  private void createWarningArea(Composite composite) {

    Composite warningArea = new Composite(composite, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).span(2, 1).hint(100, SWT.DEFAULT)
        .applyTo(warningArea);
    warningArea.setLayout(new GridLayout(2, false));

    Label warningImg = new Label(warningArea, SWT.NONE);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(warningImg);
    warningImg.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING));

    Text warningLabel = new Text(warningArea, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
    warningLabel.setBackground(composite.getBackground());
    GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL).grab(true, false).applyTo(warningLabel);
    warningLabel.setText(Messages.MavenPomSelectionComponent_UnavailableRemoteRepositoriesIndexes);
  }

  /* (non-Javadoc)
   * @see org.eclipse.swt.widgets.Composite#setFocus()
   */
  public boolean setFocus() {
    return searchText.setFocus();
  }

  void selectFirstElementInTheArtifactTreeIfNoSelectionHasBeenMade() {
    //
    // If we have started a new search when focus is passed to the tree viewer we will automatically select
    // the first element if no element has been selected from a previous expedition into the tree viewer.
    //
    if(searchResultViewer.getTree().getItemCount() > 0 && searchResultViewer.getSelection().isEmpty()) {
      Object artifact = searchResultViewer.getTree().getTopItem().getData();
      searchResultViewer.setSelection(new StructuredSelection(artifact), true);
    }
  }

  protected boolean showClassifiers() {
    return (IIndex.SEARCH_ARTIFACT.equals(queryType));
  }

  public void init(String queryText, String queryType, IProject project, Set<ArtifactKey> artifacts,
      Set<ArtifactKey> managed) {
    this.queryType = queryType;
    this.project = project;

    if(queryText != null) {
      searchText.setText(queryText);
    }

    if(artifacts != null) {
      for(ArtifactKey a : artifacts) {
        artifactKeys.add(a.getGroupId() + ":" + a.getArtifactId()); //$NON-NLS-1$
        artifactKeys.add(a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    if(managed != null) {
      for(ArtifactKey a : managed) {
        managedKeys.add(a.getGroupId() + ":" + a.getArtifactId()); //$NON-NLS-1$
        managedKeys.add(a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion()); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

    searchResultViewer.setContentProvider(new SearchResultContentProvider());

    SearchResultLabelProvider labelProvider = new SearchResultLabelProvider(artifactKeys, managedKeys);
    DecoratingStyledCellLabelProvider decoratingLabelProvider = new DecoratingStyledCellLabelProvider(labelProvider,
        PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator(), null);
    DecorationContext decorationContext = new DecorationContext();
    if(project != null) {
      decorationContext.putProperty(PROP_DECORATION_CONTEXT_PROJECT, project);
    }
    decoratingLabelProvider.setDecorationContext(decorationContext);
    searchResultViewer.setLabelProvider(decoratingLabelProvider);

    searchResultViewer.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      if(!selection.isEmpty()) {
        List<IndexedArtifactFile> files = getSelectedIndexedArtifactFiles(selection);

        ArtifactFilterManager filterManager = MavenPluginActivator.getDefault().getArifactFilterManager();

        for(IndexedArtifactFile file : files) {
          ArtifactKey key = file.getAdapter(ArtifactKey.class);
          IStatus status = filterManager.filter(MavenPomSelectionComponent.this.project, key);
          if(!status.isOK()) {
            setStatus(IStatus.ERROR, status.getMessage());
            return; // TODO not nice to exit method like this
          }
        }

        if(files.size() == 1) {
          IndexedArtifactFile f = files.get(0);
          // int severity = artifactKeys.contains(f.group + ":" + f.artifact) ? IStatus.ERROR : IStatus.OK;
          int severity = IStatus.OK;
          String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(f.date);
          setStatus(severity, NLS.bind(Messages.MavenPomSelectionComponent_detail1, f.fname,
              (f.size != -1 ? NLS.bind(Messages.MavenPomSelectionComponent_details2, date, f.size) : date)));
        } else {
          setStatus(IStatus.OK, NLS.bind(Messages.MavenPomSelectionComponent_selected, selection.size()));
        }
      } else {
        setStatus(IStatus.ERROR, Messages.MavenPomSelectionComponent_nosel);
      }
    });
    setupClassifiers();
    setStatus(IStatus.ERROR, ""); //$NON-NLS-1$
    scheduleSearch(queryText, false);
  }

  protected void setupClassifiers() {
  }

  public IStatus getStatus() {
    return this.status;
  }

  public void addDoubleClickListener(IDoubleClickListener listener) {
    searchResultViewer.addDoubleClickListener(listener);
  }

  public void addSelectionChangedListener(ISelectionChangedListener listener) {
    this.selectionListener = listener;
  }

  void setStatus(int severity, String message) {
    this.status = new Status(severity, IMavenConstants.PLUGIN_ID, 0, message, null);
    if(selectionListener != null) {
      selectionListener.selectionChanged(new SelectionChangedEvent(searchResultViewer, searchResultViewer
          .getSelection()));
    }
  }

  public IndexedArtifact getIndexedArtifact() {
    IStructuredSelection selection = (IStructuredSelection) searchResultViewer.getSelection();
    Object element = selection.getFirstElement();
    if(element instanceof IndexedArtifact) {
      return (IndexedArtifact) element;
    }
    TreeItem[] treeItems = searchResultViewer.getTree().getSelection();
    if(treeItems.length == 0) {
      return null;
    }
    return (IndexedArtifact) treeItems[0].getParentItem().getData();
  }

  public IndexedArtifactFile getIndexedArtifactFile() {
    List<IndexedArtifactFile> files = getSelectedIndexedArtifactFiles((IStructuredSelection) searchResultViewer
        .getSelection());
    return !files.isEmpty() ? files.get(0) : null;
  }

  List<IndexedArtifactFile> getSelectedIndexedArtifactFiles(IStructuredSelection selection) {
    ArrayList<IndexedArtifactFile> result = new ArrayList<>();
    for(Object element : selection.toList()) {
      if(element instanceof IndexedArtifact) {
        //the idea here is that if we have a managed version for something, then the IndexedArtifact shall
        //represent that value..
        IndexedArtifact ia = (IndexedArtifact) element;
        if(managedKeys.contains(getKey(ia))) {
          for(IndexedArtifactFile file : ia.getFiles()) {
            if(managedKeys.contains(getKey(file))) {
              result.add(file);
            }
          }
        } else {
          //335383 find first non-snasphot version in case none is managed
          boolean added = false;
          for(IndexedArtifactFile file : ia.getFiles()) {
            //what better means of recognizing snapshots?
            if(file.version != null && !file.version.endsWith("-SNAPSHOT")) { //$NON-NLS-1$
              added = true;
              result.add(file);
              break;
            }
          }
          if(!added) {//just in case we deal with all snapshots..
            result.add(ia.getFiles().iterator().next());
          }
        }
      } else if(element instanceof IndexedArtifactFile) {
        result.add((IndexedArtifactFile) element);
      }
    }

    return result;
  }

  void scheduleSearch(String query, boolean delay) {
    if(query != null && query.length() > 2) {
      if(searchJob == null) {
        IndexManager indexManager = MavenPlugin.getIndexManager();
        searchJob = new SearchJob(queryType, indexManager);
      } else {
        if(!searchJob.cancel()) {
          //for already running ones, just create new instance so that the previous one can peacefully die
          //without preventing the new one from completing first
          IndexManager indexManager = MavenPlugin.getIndexManager();
          searchJob = new SearchJob(queryType, indexManager);
        }
      }
      searchJob.setQuery(query.toLowerCase());
      searchJob.schedule(delay ? LONG_DELAY : SHORT_DELAY);
    } else {
      if(searchJob != null) {
        searchJob.cancel();
      }
    }
  }

  public static String getKey(IndexedArtifactFile file) {
    return file.group + ":" + file.artifact + ":" + file.version; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public static String getKey(IndexedArtifact art) {
    return art.getGroupId() + ":" + art.getArtifactId(); //$NON-NLS-1$
  }

  /**
   * Search Job
   */
  private class SearchJob extends Job {

    private IndexManager indexManager;

    private String query;

    private String field;

    private volatile boolean stop = false;

    public SearchJob(String field, IndexManager indexManager) {
      super(Messages.MavenPomSelectionComponent_searchJob);
      this.field = field;
      this.indexManager = indexManager;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public boolean shouldRun() {
      stop = false;
      return super.shouldRun();
    }

    public int getClassifier() {
      // mkleint: no more allowing people to opt in/out displaying javadoc and sources..
      // allow tests and every other classifier..
      return IIndex.SEARCH_JARS + IIndex.SEARCH_TESTS;
    }

    protected IStatus run(IProgressMonitor monitor) {
      int classifier = showClassifiers() ? getClassifier() : IIndex.SEARCH_ALL;
      if(searchResultViewer == null || searchResultViewer.getControl() == null
          || searchResultViewer.getControl().isDisposed()) {
        return Status.CANCEL_STATUS;
      }
      if(query != null) {
        String activeQuery = query;
        try {
          setResult(IStatus.OK, NLS.bind(Messages.MavenPomSelectionComponent_searching, activeQuery.toLowerCase()),
              null);

          Map<String, IndexedArtifact> res = indexManager.getAllIndexes().search(
              new UserInputSearchExpression(activeQuery), field, classifier);

          //335139 have the managed entries always come up as first results
          LinkedHashMap<String, IndexedArtifact> managed = new LinkedHashMap<>();
          LinkedHashMap<String, IndexedArtifact> nonManaged = new LinkedHashMap<>();
          for(Map.Entry<String, IndexedArtifact> art : res.entrySet()) {
            String key = art.getValue().getGroupId() + ":" + art.getValue().getArtifactId(); //$NON-NLS-1$
            if(managedKeys.contains(key)) {
              managed.put(art.getKey(), art.getValue());
            } else {
              nonManaged.put(art.getKey(), art.getValue());
            }
          }
          managed.putAll(nonManaged);
          setResult(IStatus.OK, NLS.bind(Messages.MavenPomSelectionComponent_results, activeQuery, res.size()), managed);
        } catch(BooleanQuery.TooManyClauses ex) {
          setResult(IStatus.ERROR, Messages.MavenPomSelectionComponent_toomany,
              Collections.<String, IndexedArtifact> emptyMap());
        } catch(final RuntimeException ex) {
          setResult(IStatus.ERROR, NLS.bind(Messages.MavenPomSelectionComponent_error, ex.toString()),
              Collections.<String, IndexedArtifact> emptyMap());
        } catch(final Exception ex) {
          setResult(IStatus.ERROR, NLS.bind(Messages.MavenPomSelectionComponent_error, ex.getMessage()),
              Collections.<String, IndexedArtifact> emptyMap());
        }
      }
      return Status.OK_STATUS;
    }

    protected void canceling() {
      stop = true;
    }

    private void setResult(final int severity, final String message, final Map<String, IndexedArtifact> result) {
      if(stop)
        return;
      Display.getDefault().syncExec(() -> {
        setStatus(severity, message);
        if(result != null) {
          if(!searchResultViewer.getControl().isDisposed()) {
            searchResultViewer.setInput(result);
          }
        }
      });
    }
  }

  public static class SearchResultLabelProvider extends LabelProvider implements IColorProvider,
      DelegatingStyledCellLabelProvider.IStyledLabelProvider {
    private final Set<String> artifactKeys;

    private final Set<String> managedKeys;

    /**
     * both managedkeys and artifctkeys are supposed to hold both gr:art:ver combos and gr:art combos
     * 
     * @param artifactKeys
     * @param managedKeys
     */
    public SearchResultLabelProvider(Set<String> artifactKeys, Set<String> managedKeys) {
      this.artifactKeys = artifactKeys;
      this.managedKeys = managedKeys;
    }

    public String getText(Object element) {
      return super.getText(element);
    }

    protected String getRepoDisplayName(String repo) {
      return repo;
    }

    public Color getForeground(Object element) {
      if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        if(artifactKeys.contains(getKey(f))) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      } else if(element instanceof IndexedArtifact) {
        IndexedArtifact i = (IndexedArtifact) element;
        if(artifactKeys.contains(getKey(i))) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Image getImage(Object element) {
      if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        if(managedKeys.contains(getKey(f))) {
          return MavenImages.getOverlayImage(f.sourcesExists == IIndex.PRESENT ? MavenImages.PATH_VERSION_SRC
              : MavenImages.PATH_VERSION, MavenImages.PATH_LOCK, IDecoration.BOTTOM_LEFT);
        }

        if(f.sourcesExists == IIndex.PRESENT) {
          return MavenImages.IMG_VERSION_SRC;
        }
        return MavenImages.IMG_VERSION;
      } else if(element instanceof IndexedArtifact) {
        IndexedArtifact i = (IndexedArtifact) element;
        if(managedKeys.contains(getKey(i))) {
          return MavenImages.getOverlayImage(MavenImages.PATH_JAR, MavenImages.PATH_LOCK, IDecoration.BOTTOM_LEFT);
        }
        return MavenImages.IMG_JAR;
      }
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider#getStyledText(java.lang.Object)
     */
    public StyledString getStyledText(Object element) {
      if(element instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) element;
        String name = (a.getClassname() == null ? "" : a.getClassname() + "   " + a.getPackageName() + "   ") + a.getGroupId() + "   " + a.getArtifactId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        StyledString ss = new StyledString();
        ss.append(name);
        if(managedKeys.contains(getKey(a))) {
          ss.append(Messages.MavenPomSelectionComponent_managed_decoration, StyledString.DECORATIONS_STYLER);
        }
        return ss;
      } else if(element instanceof IndexedArtifactFile) {
        IndexedArtifactFile f = (IndexedArtifactFile) element;
        StyledString ss = new StyledString();
        String name = f.version
            + " [" + (f.type == null ? "jar" : f.type) + (f.classifier != null ? ", " + f.classifier : "") + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        ss.append(name);
        if(managedKeys.contains(getKey(f))) {
          ss.append(Messages.MavenPomSelectionComponent_managed_decoration, StyledString.DECORATIONS_STYLER);
        }
        return ss;
      }
      return new StyledString();
    }

  }

  public static class SearchResultContentProvider implements ITreeContentProvider {
    private static Object[] EMPTY = new Object[0];

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object inputElement) {
      if(inputElement instanceof Map) {
        return ((Map<?, ?>) inputElement).values().toArray();
      }
      return EMPTY;
    }

    public Object[] getChildren(Object parentElement) {
      if(parentElement instanceof IndexedArtifact) {
        IndexedArtifact a = (IndexedArtifact) parentElement;
        return a.getFiles().toArray();
      }
      return EMPTY;
    }

    public boolean hasChildren(Object element) {
      return element instanceof IndexedArtifact;
    }

    public Object getParent(Object element) {
      return null;
    }

    public void dispose() {

    }

  }
}
