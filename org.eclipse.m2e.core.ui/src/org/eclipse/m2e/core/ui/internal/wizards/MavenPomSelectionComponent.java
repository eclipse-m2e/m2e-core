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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
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
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.filter.ArtifactFilterManager;
import org.eclipse.m2e.core.search.ISearchProvider;
import org.eclipse.m2e.core.search.ISearchProvider.SearchType;
import org.eclipse.m2e.core.search.ISearchResultGA;
import org.eclipse.m2e.core.search.ISearchResultGAVEC;
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
  SearchType queryType;

  SearchJob searchJob;

  private IStatus status;

  private ISelectionChangedListener selectionListener;

  private static final long SHORT_DELAY = 150L;

  private static final long LONG_DELAY = 500L;

  final HashSet<String> artifactKeys = new HashSet<String>();

  final HashSet<String> managedKeys = new HashSet<String>();

  private IProject project;

  private ComboViewer searchProviderCombo;

  private WarningComposite warningArea;

  private ISearchProvider searchProvider;

  public MavenPomSelectionComponent(Composite parent, int style) {
    super(parent, style);
    createSearchComposite();
  }

  private void createSearchComposite() {
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    setLayout(gridLayout);

    createSearchProviderCombo();

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

    warningArea = new WarningComposite(this, SWT.NONE);

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

  private void createSearchProviderCombo() {
    Label searchProviderLabel = new Label(this, SWT.NONE);
    searchProviderLabel.setText("Search Provider:");
    searchProviderLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

    IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();

    searchProviderCombo = new ComboViewer(this, SWT.BORDER | SWT.READ_ONLY);
    searchProviderCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
    searchProviderCombo.setContentProvider(ArrayContentProvider.getInstance());

    searchProviderCombo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object o) {
        if(o instanceof IConfigurationElement) {
          IConfigurationElement element = (IConfigurationElement) o;
          return element.getAttribute("name"); //$NON-NLS-1$
        }
        return super.getText(o);
      }
    });

    searchProviderCombo.addPostSelectionChangedListener(event -> {
      IStructuredSelection selection = event.getStructuredSelection();

      try {
        if(selection != null && !selection.isEmpty()) {
          IConfigurationElement extension = (IConfigurationElement) selection.getFirstElement();
          searchProvider = (ISearchProvider) extension.createExecutableExtension("class"); //$NON-NLS-1$

          ((DeferredSearchResultTreeContentProvider) searchResultViewer.getContentProvider())
              .setSearchProvider(searchProvider);

          if(searchText != null) {
            dialogSettings.put(this.getClass().getSimpleName() + ".searchProvider", extension.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
            scheduleSearch(searchText.getText(), true);
          }

          warningArea.setStatus(searchProvider.getStatus());
        }
      } catch(CoreException ex) {
        StatusManager.getManager().handle(ex, M2EUIPluginActivator.PLUGIN_ID);
      }
    });
  }

  private void loadSettings() {
    IDialogSettings dialogSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
    IConfigurationElement[] elements = getSearchProviders().toArray(new IConfigurationElement[0]);
    searchProviderCombo.setInput(elements);

    String searchProvider = dialogSettings.get(this.getClass().getSimpleName() + ".searchProvider"); //$NON-NLS-1$
    if(searchProvider == null) {
      searchProviderCombo.setSelection(new StructuredSelection(elements[0]));
    } else {
      for(IConfigurationElement candidate : elements) {
        if(searchProvider.equals(candidate.getAttribute("class"))) { //$NON-NLS-1$
          searchProviderCombo.setSelection(new StructuredSelection(candidate));
          break;
        }
      }
    }

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
    return (queryType != null && !SearchType.className.equals(queryType));
  }

  public void init(String queryText, SearchType queryType, IProject project, Set<ArtifactKey> artifacts,
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

    searchResultViewer.setContentProvider(new DeferredSearchResultTreeContentProvider());

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
        List<ISearchResultGAVEC> files = getSelectedSearchResultFiles(selection);

        ArtifactFilterManager filterManager = MavenPluginActivator.getDefault().getArifactFilterManager();

        for(ISearchResultGAVEC file : files) {
          ArtifactKey key = file.getAdapter(ArtifactKey.class);
          IStatus status = filterManager.filter(MavenPomSelectionComponent.this.project, key);
          if(!status.isOK()) {
            setStatus(IStatus.ERROR, status.getMessage());
            return; // TODO not nice to exit method like this
          }
        }

        if(files.size() == 1) {
          ISearchResultGAVEC f = files.get(0);
          // int severity = artifactKeys.contains(f.group + ":" + f.artifact) ? IStatus.ERROR : IStatus.OK;
          int severity = IStatus.OK;
          String date = f.getDate() != null
              ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(f.getDate())
              : null;
          setStatus(severity, NLS.bind(Messages.MavenPomSelectionComponent_detail1, f.getFilename(),
              (f.getSize() != -1 ? NLS.bind(Messages.MavenPomSelectionComponent_details2, date, f.getSize()) : date)));
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

    loadSettings();
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

  public ISearchResultGA getSearchResult() {
    IStructuredSelection selection = (IStructuredSelection) searchResultViewer.getSelection();
    Object element = selection.getFirstElement();
    if(element instanceof ISearchResultGA) {
      return (ISearchResultGA) element;
    }
    TreeItem[] treeItems = searchResultViewer.getTree().getSelection();
    if(treeItems.length == 0) {
      return null;
    }
    return (ISearchResultGA) treeItems[0].getParentItem().getData();
  }

  public ISearchResultGAVEC getSearchResultFile() {
    List<ISearchResultGAVEC> files = getSelectedSearchResultFiles((IStructuredSelection) searchResultViewer
        .getSelection());
    return !files.isEmpty() ? files.get(0) : null;
  }

  List<ISearchResultGAVEC> getSelectedSearchResultFiles(IStructuredSelection selection) {
    List<ISearchResultGAVEC> result = new ArrayList<>();
    for(Object element : selection.toList()) {
      if(element instanceof ISearchResultGA) {
        //the idea here is that if we have a managed version for something, then the ISearchResult shall
        //represent that value..
        ISearchResultGA ia = (ISearchResultGA) element;
        if(managedKeys.contains(getKey(ia))) {
          for(ISearchResultGAVEC file : ia.getComponents()) {
            if(managedKeys.contains(getKey(file))) {
              result.add(file);
            }
          }
        } else {
          //335383 find first non-snasphot version in case none is managed
          boolean added = false;
          for(ISearchResultGAVEC file : ia.getComponents()) {
            //what better means of recognizing snapshots?
            if(file.getVersion() != null && !file.getVersion().endsWith("-SNAPSHOT")) { //$NON-NLS-1$
              added = true;
              result.add(file);
              break;
            }
          }
          if(!added) {//just in case we deal with all snapshots..
            result.add(ia.getComponents().iterator().next());
          }
        }
      } else if(element instanceof ISearchResultGAVEC) {
        result.add((ISearchResultGAVEC) element);
      }
    }

    return result;
  }

  void scheduleSearch(String query, boolean delay) {
    // TODO performance enhancement, re-use job if the provider is the same
    if(searchJob != null) {
      searchJob.cancel();
    }
    if(query != null && query.length() > 2 && !searchProviderCombo.getSelection().isEmpty()) {
      searchJob = new SearchJob(searchProvider);
      searchJob.setQuery(query.toLowerCase());
      searchJob.schedule(delay ? LONG_DELAY : SHORT_DELAY);
    }
  }

  private Collection<IConfigurationElement> getSearchProviders() {
    Stream<IConfigurationElement> elements = Arrays.stream(
        Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.m2e.core.m2eComponentSearch")); //$NON-NLS-1$

    return elements.filter(element -> element.getName().equals("search_provider")) //$NON-NLS-1$
        .filter(element -> Boolean.valueOf(element.getAttribute(queryType.toString()))).collect(Collectors.toList());
  }

  public static String getKey(ISearchResultGAVEC file) {
    return file.getGroupId() + ":" + file.getArtifactId() + ":" + file.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public static String getKey(ISearchResultGA art) {
    return art.getGroupId() + ":" + art.getArtifactId(); //$NON-NLS-1$
  }

  /**
   * Search Job
   */
  private class SearchJob extends Job {

    private String query;

    private volatile boolean stop = false;

    private ISearchProvider searchProvider;

    public SearchJob(ISearchProvider searchProvider) {
      super(Messages.MavenPomSelectionComponent_searchJob);
      this.searchProvider = searchProvider;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public boolean shouldRun() {
      stop = false;
      return super.shouldRun();
    }

    protected IStatus run(IProgressMonitor monitor) {
      try {
        List<ISearchResultGA> results = searchProvider.find(monitor, queryType, query);

        //335139 have the managed entries always come up as first results
        LinkedHashMap<String, ISearchResultGA> managed = new LinkedHashMap<String, ISearchResultGA>();
        LinkedHashMap<String, ISearchResultGA> nonManaged = new LinkedHashMap<String, ISearchResultGA>();
        for(ISearchResultGA entry : results) {
          String key = entry.getGroupId() + ":" + entry.getArtifactId(); //$NON-NLS-1$
          if(managedKeys.contains(key)) {
            managed.put(key, entry);
          } else {
            nonManaged.put(key, entry);
          }
        }
        managed.putAll(nonManaged);
        setResult(IStatus.OK, NLS.bind(Messages.MavenPomSelectionComponent_results, query, results.size()), managed);
      } catch(CoreException e) {
        setResult(e.getStatus().getCode(), e.getMessage(), Collections.<String, ISearchResultGA> emptyMap());
      }
      return Status.OK_STATUS;
    }

    protected void canceling() {
      stop = true;
    }

    private void setResult(final int severity, final String message, final Map<String, ISearchResultGA> result) {
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
      if(element instanceof ISearchResultGAVEC) {
        ISearchResultGAVEC f = (ISearchResultGAVEC) element;
        if(artifactKeys.contains(getKey(f))) {
          return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
        }
      } else if(element instanceof ISearchResultGA) {
        ISearchResultGA i = (ISearchResultGA) element;
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
      if(element instanceof ISearchResultGAVEC) {
        ISearchResultGAVEC f = (ISearchResultGAVEC) element;
        if(managedKeys.contains(getKey(f))) {
          return MavenImages.getOverlayImage(f.hasSources() ? MavenImages.PATH_VERSION_SRC
              : MavenImages.PATH_VERSION, MavenImages.PATH_LOCK, IDecoration.BOTTOM_LEFT);
        }

        if(f.hasSources()) {
          return MavenImages.IMG_VERSION_SRC;
        }
        return MavenImages.IMG_VERSION;
      } else if(element instanceof ISearchResultGA) {
        ISearchResultGA i = (ISearchResultGA) element;
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
      if(element instanceof ISearchResultGA) {
        ISearchResultGA a = (ISearchResultGA) element;
        String name = (a.getClassname() == null ? "" : a.getClassname() + "   " + a.getPackageName() + "   ") + a.getGroupId() + "   " + a.getArtifactId(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        StyledString ss = new StyledString();
        ss.append(name);
        if(managedKeys.contains(getKey(a))) {
          ss.append(Messages.MavenPomSelectionComponent_managed_decoration, StyledString.DECORATIONS_STYLER);
        }
        return ss;
      } else if(element instanceof ISearchResultGAVEC) {
        ISearchResultGAVEC f = (ISearchResultGAVEC) element;
        StyledString ss = new StyledString();
        String name = f.getVersion() + " [" + (f.getExtension() == null ? "jar" : f.getExtension()) //$NON-NLS-1$//$NON-NLS-2$
            + (f.getClassifier() != null ? ", " + f.getClassifier() : "") + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
      if(parentElement instanceof ISearchResultGA) {
        ISearchResultGA a = (ISearchResultGA) parentElement;
        return a.getComponents().toArray();
      }
      return EMPTY;
    }

    public boolean hasChildren(Object element) {
      return element instanceof ISearchResultGA;
    }

    public Object getParent(Object element) {
      return null;
    }

    public void dispose() {

    }

  }
}
