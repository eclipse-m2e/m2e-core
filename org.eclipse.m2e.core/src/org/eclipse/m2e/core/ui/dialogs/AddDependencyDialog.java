/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.dialogs;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.icu.text.DateFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.apache.lucene.search.BooleanQuery;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.index.UserInputSearchExpression;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.util.ProposalUtil;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.core.wizards.MavenPomSelectionComponent;
import org.eclipse.m2e.core.wizards.WidthGroup;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.PomFactory;


/**
 * A Dialog whose primary goal is to allow the user to select a dependency, either by entering the GAV coordinates
 * manually, or by search through a repository index.
 * 
 * @author rgould
 */
public class AddDependencyDialog extends AbstractMavenDialog {

  public static final String[] SCOPES = new String[] {"compile", "provided", "runtime", "test", "system"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

  /*
   * dependencies under dependencyManagement are permitted to use an the extra "import" scope
   */
  public static final String[] DEP_MANAGEMENT_SCOPES = new String[] {"compile", "provided", "runtime", "test", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      "system", "import"}; //$NON-NLS-1$ //$NON-NLS-2$

  protected static final String DIALOG_SETTINGS = AddDependencyDialog.class.getName();

  protected static final long SEARCH_DELAY = 500L; //in milliseconds

  protected String[] scopes;

  protected TreeViewer resultsViewer;

  protected Text queryText;

  protected Text groupIDtext;

  protected Text artifactIDtext;

  protected Text versionText;

  protected Text infoTextarea;

  protected List scopeList;

  protected java.util.List<Dependency> dependencies;

  protected WidthGroup widthGroup;

  /*
   * Stores selected files from the results viewer. These are later
   * converted into the above dependencies when OK is pressed.
   */
  protected java.util.List<IndexedArtifactFile> artifactFiles;

  protected SearchJob currentSearch;

  protected IProject project;

  protected DependencyNode dependencyNode;

  /*
   * This is to be run when the dialog is done creating its controls, but
   * before open() is called
   */
  protected Runnable onLoad;

  protected SelectionListener resultsListener;

  /**
   * The AddDependencyDialog differs slightly in behaviour depending on context. If it is being used to apply a
   * dependency under the "dependencyManagement" context, the extra "import" scope is available. Set @param
   * isForDependencyManagement to true if this is case.
   * 
   * @param parent
   * @param isForDependencyManagement
   * @param project the project which contains this POM. Used for looking up indices
   */
  public AddDependencyDialog(Shell parent, boolean isForDependencyManagement, IProject project) {
    super(parent, DIALOG_SETTINGS);
    this.project = project;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle(Messages.AddDependencyDialog_title);
//    setStatusLineAboveButtons(true);

    if(!isForDependencyManagement) {
      this.scopes = SCOPES;
    } else {
      this.scopes = DEP_MANAGEMENT_SCOPES;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea()
   */
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);

    widthGroup = new WidthGroup();
    composite.addControlListener(widthGroup);

    Composite gavControls = createGAVControls(composite);
    gavControls.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Composite searchControls = createSearchControls(composite);
    searchControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Display.getDefault().asyncExec(this.onLoad);

    return composite;
  }

  
  /**
   * Sets the up group-artifact-version controls
   */
  private Composite createGAVControls(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    Label groupIDlabel = new Label(composite, SWT.NONE);
    groupIDlabel.setText(Messages.AddDependencyDialog_groupId_label);
    widthGroup.addControl(groupIDlabel);

    groupIDtext = new Text(composite, SWT.BORDER);
    groupIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label scopeLabel = new Label(composite, SWT.NONE);
    scopeLabel.setText(Messages.AddDependencyDialog_scope_label);

    scopeList = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
    scopeList.setItems(scopes);
    GridData scopeListData = new GridData(SWT.LEFT, SWT.FILL, false, true, 1, 3);
    scopeListData.heightHint = 20;
    scopeListData.widthHint = 100;
    scopeList.setLayoutData(scopeListData);
    scopeList.setSelection(0);

    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText(Messages.AddDependencyDialog_artifactId_label);
    widthGroup.addControl(artifactIDlabel);

    artifactIDtext = new Text(composite, SWT.BORDER);
    artifactIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label filler = new Label(composite, SWT.NONE);
    filler.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 2));

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.AddDependencyDialog_version_label);
    widthGroup.addControl(versionLabel);

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    /*
     * Fix the tab order (group -> artifact -> version -> scope)
     */
    composite.setTabList(new Control[] { groupIDtext, artifactIDtext, versionText, scopeList});

    ProposalUtil.addGroupIdProposal(project, groupIDtext, Packaging.ALL);
    ProposalUtil.addArtifactIdProposal(project, groupIDtext, artifactIDtext, Packaging.ALL);
    ProposalUtil.addVersionProposal(project, groupIDtext, artifactIDtext, versionText, Packaging.ALL);

    artifactIDtext.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        updateInfo();
      }
    });

    groupIDtext.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        updateInfo();
      }
    });

    return composite;
  }

  void updateInfo() {
//    infoTextarea.setText(""); //$NON-NLS-1$
    if(dependencyNode == null) {
      return;
    }
    dependencyNode.accept(new DependencyVisitor() {

      public boolean visitLeave(DependencyNode node) {
        if(node.getDependency() != null && node.getDependency().getArtifact() != null) {
          Artifact artifact = node.getDependency().getArtifact();
          if(artifact.getGroupId().equalsIgnoreCase(groupIDtext.getText().trim())
              && artifact.getArtifactId().equalsIgnoreCase(artifactIDtext.getText().trim())) {
            infoTextarea.setText(NLS.bind(Messages.AddDependencyDialog_info_transitive,
                new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()}));
          }
          return false;
        }
        return true;
      }

      public boolean visitEnter(DependencyNode node) {
        return true;
      }
    });

  }

  private Composite createSearchControls(Composite parent) {
    SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    sashForm.setLayout(new FillLayout());

    Composite resultsComposite = new Composite(sashForm, SWT.NONE);
    GridLayout resultsLayout = new GridLayout(1, false);
    resultsLayout.marginWidth = 0;
    resultsComposite.setLayout(resultsLayout);

    Label queryLabel = new Label(resultsComposite, SWT.NONE);
    queryLabel.setText(Messages.AddDependencyDialog_search_label);
//    widthGroup.addControl(queryLabel);

    queryText = new Text(resultsComposite, SWT.BORDER | SWT.SEARCH);
    queryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    queryText.setFocus();
    
//    queryText.setMessage(Messages.AddDependencyDialog_search_message);

    Label resultsLabel = new Label(resultsComposite, SWT.NONE);
    resultsLabel.setText(Messages.AddDependencyDialog_results_label);
    resultsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
//    widthGroup.addControl(resultsLabel);

    Tree resultsTree = new Tree(resultsComposite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
    GridData treeData = new GridData(SWT.FILL, SWT.FILL, true, true);
    treeData.heightHint = 140;
    treeData.widthHint = 100;
    resultsTree.setLayoutData(treeData);

    Composite infoComposite = new Composite(sashForm, SWT.NONE);
    GridLayout infoLayout = new GridLayout(1, false);
    infoLayout.marginWidth = 0;
    infoComposite.setLayout(infoLayout);

    Label infoLabel = new Label(infoComposite, SWT.NONE);
    infoLabel.setText(Messages.AddDependencyDialog_info_label);
    infoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
//    widthGroup.addControl(infoLabel);

    infoTextarea = new Text(infoComposite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData infoData = new GridData(SWT.FILL, SWT.FILL, true, true);
    infoData.heightHint = 60;
    infoData.widthHint = 100;
    infoTextarea.setLayoutData(infoData);

    sashForm.setWeights(new int[] {70, 30});

    /*
     * Set up TreeViewer for search results
     */

    resultsViewer = new TreeViewer(resultsTree);
    resultsViewer.setContentProvider(new MavenPomSelectionComponent.SearchResultContentProvider());
    //TODO we want to have the artifacts marked for presence and management..
    resultsViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(new MavenPomSelectionComponent.SearchResultLabelProvider(Collections.EMPTY_SET, Collections.EMPTY_SET,
        IIndex.SEARCH_ARTIFACT)));

    /*
     * Hook up events
     */

    resultsListener = new SelectionListener();
    resultsViewer.addSelectionChangedListener(resultsListener);

    queryText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if(e.keyCode == SWT.ARROW_DOWN) {
          resultsViewer.getTree().setFocus();
        }
      }
    });

    queryText.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        search(queryText.getText());
      }
    });

    return sashForm;
  }

  /**
   * Just a short helper method to determine what to display in the text widgets when the user selects multiple objects
   * in the tree viewer. If the objects have the same value, then we should show that to them, otherwise we show
   * something like "(multiple selected)"
   * 
   * @param current
   * @param newValue
   * @return
   */
  String chooseWidgetText(String current, String newValue) {
    if(current == null) {
      return newValue;
    } else if(!current.equals(newValue)) {
      return Messages.AddDependencyDialog_multipleValuesSelected;
    }
    return current;
  }

  void appendFileInfo(final StringBuffer buffer, final IndexedArtifactFile file) {
    buffer.append(" * " + file.fname);
    if(file.size != -1) {
      buffer.append(", size: ");
      if((file.size / 1024 / 1024) > 0) {
        buffer.append((file.size / 1024 / 1024) + "MB");
      } else {
        buffer.append(Math.max(1, file.size / 1024) + "KB");
      }
    }
    buffer.append(", date: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(file.date));
    buffer.append("\n");

    if(dependencyNode != null) {
      dependencyNode.accept(new DependencyVisitor() {

        public boolean visitEnter(DependencyNode node) {
          return true;
        }

        public boolean visitLeave(DependencyNode node) {
          if(node.getDependency() == null || node.getDependency().getArtifact() == null) {
            return true;
          }
          Artifact artifact = node.getDependency().getArtifact();
          if(artifact.getGroupId().equalsIgnoreCase(file.group)
              && artifact.getArtifactId().equalsIgnoreCase(file.artifact)) {
            buffer.append(NLS.bind(Messages.AddDependencyDialog_transitive_dependency,
                new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()}));
            /*
             * DependencyNodes don't know their parents. Determining which transitive dependency 
             * is using the selected dependency is non trivial :(
             */
            return false;
          }
          return true;
        }
      });
    }
  }

  protected void search(String query) {
    if(query == null || query.length() <= 2) {
      if(this.currentSearch != null) {
        this.currentSearch.cancel();
      }
    } else {
      IndexManager indexManager = MavenPlugin.getDefault().getIndexManager();

      if(this.currentSearch != null) {
        this.currentSearch.cancel();
      }

      this.currentSearch = new SearchJob(query.toLowerCase(), indexManager);
      this.currentSearch.schedule(SEARCH_DELAY);
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   * This is called when OK is pressed. There's no obligation to do anything.
   */
  protected void computeResult() {
    String scope = ""; //$NON-NLS-1$
    if(scopeList.getSelection().length != 0) {
      scope = scopeList.getSelection()[0];
    }

    if(artifactFiles == null || artifactFiles.size() == 1) {
      Dependency dependency = createDependency(groupIDtext.getText().trim(), artifactIDtext.getText().trim(),
          versionText.getText().trim(), scope, ""); //$NON-NLS-1$
      this.dependencies = Collections.singletonList(dependency);
    } else {
      this.dependencies = new LinkedList<Dependency>();
      for(IndexedArtifactFile file : artifactFiles) {
        Dependency dep = createDependency(file.group, file.artifact, file.version, scope, file.type);
        this.dependencies.add(dep);
      }
    }
  }

  private Dependency createDependency(String groupID, String artifactID, String version, String scope, String type) {
    Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId(groupID);
    dependency.setArtifactId(artifactID);
    dependency.setVersion(version);

    /*
     * For scope and type, if the values are the default, don't save them.
     * This reduces clutter in the XML file (although forces people who don't
     * know what the defaults are to look them up).
     */
    dependency.setScope("compile".equals(scope) ? "" : scope); //$NON-NLS-1$ //$NON-NLS-2$
    dependency.setType("jar".equals(type) ? "" : type); //$NON-NLS-1$ //$NON-NLS-2$

    return dependency;
  }

  public java.util.List<Dependency> getDependencies() {
    return this.dependencies;
  }

  void setInfo(int status, String message) {
    updateStatus(new Status(status, IMavenConstants.PLUGIN_ID, message));
  }

  public final class SelectionListener implements ISelectionChangedListener {
    public void selectionChanged(SelectionChangedEvent event) {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      if(selection.isEmpty()) {
        infoTextarea.setText(""); //$NON-NLS-1$
        artifactFiles = null;
      } else {
        String artifact = null;
        String group = null;
        String version = null;

        artifactFiles = new LinkedList<IndexedArtifactFile>();
        StringBuffer buffer = new StringBuffer();
        Iterator iter = selection.iterator();
        while(iter.hasNext()) {
          Object obj = iter.next();
          IndexedArtifactFile file = null;

          if(obj instanceof IndexedArtifact) {
            file = ((IndexedArtifact) obj).getFiles().iterator().next();
          } else {
            file = (IndexedArtifactFile) obj;
          }

          appendFileInfo(buffer, file);
          artifactFiles.add(file);

          artifact = chooseWidgetText(artifact, file.artifact);
          group = chooseWidgetText(group, file.group);
          version = chooseWidgetText(version, file.version);
        }
        setInfo(OK, NLS.bind(artifactFiles.size() == 1 ? Messages.AddDependencyDialog_itemSelected : Messages.AddDependencyDialog_itemsSelected, artifactFiles.size()));
        infoTextarea.setText(buffer.toString());
        artifactIDtext.setText(artifact);
        groupIDtext.setText(group);
        versionText.setText(version);

        boolean enabled = !(artifactFiles.size() > 1);
        artifactIDtext.setEnabled(enabled);
        groupIDtext.setEnabled(enabled);
        versionText.setEnabled(enabled);
      }
    }
  }

  private class SearchJob extends Job {

    private String query;

    private IndexManager indexManager;

    private boolean cancelled = false;

    public SearchJob(String query, IndexManager indexManager) {
      super(NLS.bind(Messages.AddDependencyDialog_searchingFor, query));
      this.query = query;
      this.indexManager = indexManager;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IStatus run(IProgressMonitor monitor) {
      if(this.cancelled || resultsViewer == null || resultsViewer.getControl() == null
          || resultsViewer.getControl().isDisposed()) {
        return Status.CANCEL_STATUS;
      }

      try {
        setResults(IStatus.OK, Messages.AddDependencyDialog_searching, Collections.<String, IndexedArtifact> emptyMap());
        // TODO: before it was searching all indexes, but it should current project? (cstamas)
        // If not, the change getIndex(project) to getAllIndexes() and done
        // TODO: cstamas identified this as "user input", true?
        Map<String, IndexedArtifact> results = indexManager.getIndex(project).search(new UserInputSearchExpression(query), IIndex.SEARCH_ARTIFACT,
            IIndex.SEARCH_ALL);
        setResults(IStatus.OK, NLS.bind(Messages.AddDependencyDialog_searchDone, results.size()), results);
      } catch(BooleanQuery.TooManyClauses exception) {
        setResults(IStatus.ERROR, Messages.AddDependencyDialog_tooManyResults,
            Collections.<String, IndexedArtifact> emptyMap());
      } catch(RuntimeException exception) {
        setResults(IStatus.ERROR, NLS.bind(Messages.AddDependencyDialog_searchError, exception.toString()),
            Collections.<String, IndexedArtifact> emptyMap());
      } catch(CoreException ex) {
        setResults(IStatus.ERROR, NLS.bind(Messages.AddDependencyDialog_searchError, ex.getMessage()),
            Collections.<String, IndexedArtifact> emptyMap());
        MavenLogger.log(ex);
      }

      return Status.OK_STATUS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#canceling()
     */
    protected void canceling() {
      this.cancelled = true;
      super.canceling();
    }

    private void setResults(final int status, final String infoMessage, final Map<String, IndexedArtifact> results) {
      if(cancelled) {
        return;
      }

      Display.getDefault().syncExec(new Runnable() {

        public void run() {
          setInfo(status, infoMessage);
          if(results != null && resultsViewer != null && resultsViewer.getControl() != null
              && !resultsViewer.getControl().isDisposed()) {
            resultsViewer.setInput(results);
          }
        }
      });
    }

  }

  public void setDepdencyNode(DependencyNode node) {
    this.dependencyNode = node;
  }

  /**
   * The provided runnable will be called after createDialogArea is done, but before it returns. This provides a way for
   * long running operations to be executed in such a way as to not block the UI. This is primarily intended to allow
   * the loading of the dependencyTree. The runnable should load the tree and then call setDependencyNode()
   * 
   * @param runnable
   */
  public void onLoad(Runnable runnable) {
    this.onLoad = runnable;
  }
}
