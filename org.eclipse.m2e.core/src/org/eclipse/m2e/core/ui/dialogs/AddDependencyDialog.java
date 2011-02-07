/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.dialogs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.text.DateFormat;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

import org.apache.lucene.search.BooleanQuery;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexManager;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.index.UserInputSearchExpression;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.util.M2EUtils;
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

  protected Combo scopeCombo;

  protected java.util.List<Dependency> dependencies;

  protected WidthGroup widthGroup;

  /*
   * Stores selected files from the results viewer. These are later
   * converted into the above dependencies when OK is pressed.
   */
  protected java.util.List<IndexedArtifactFile> artifactFiles;

  protected SearchJob currentSearch;

  private IProject project;

  private IStatus lastStatus;

  protected SelectionListener resultsListener;

  private boolean updating;

  private MavenProject mavenProject;

  private final boolean isForDependencyManagement;

  private Set<String> managedKeys;

  private Set<String> existingKeys;

  /**
   * The AddDependencyDialog differs slightly in behaviour depending on context. If it is being used to apply a
   * dependency under the "dependencyManagement" context, the extra "import" scope is available. Set @param
   * isForDependencyManagement to true if this is case.
   * 
   * @param parent
   * @param isForDependencyManagement
   * @param project the project which contains this POM. Used for looking up indices
   */
  public AddDependencyDialog(Shell parent, boolean isForDependencyManagement, IProject project, MavenProject mavenProject) {
    super(parent, DIALOG_SETTINGS);
    this.project = project;
    this.mavenProject = mavenProject;
    
    this.isForDependencyManagement = isForDependencyManagement;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle(Messages.AddDependencyDialog_title);
//    setStatusLineAboveButtons(true);

    if(!isForDependencyManagement) {
      this.scopes = SCOPES;
    } else {
      this.scopes = DEP_MANAGEMENT_SCOPES;
    }
  }
  
  public AddDependencyDialog(Shell parent, IFile file) {
    this(parent, false, null, null);
    IProject prj = file.getProject();
    project = prj;
    if (prj != null && IMavenConstants.POM_FILE_NAME.equals(file.getProjectRelativePath().toString())) {
        final IMavenProjectFacade facade = MavenPlugin.getDefault().getMavenProjectManager().getProject(prj);
        if (facade != null) {
          MavenProject mp = facade.getMavenProject();
          if (mp != null) {
            mavenProject = mp;
          } else {
            Job job = new Job("Loading Maven Project") {
              protected IStatus run(IProgressMonitor monitor) {
                try {
                  final MavenProject mp = facade.getMavenProject(monitor);
                  if (mp != null) {
                    Display.getDefault().asyncExec(new Runnable() {
                      public void run() {
                        AddDependencyDialog.this.mavenProject = mp;
                        setResultsLabelProvider();
                      }
                    });
                  }
                } catch(CoreException ex) {
                  MavenLogger.log(ex);
                }
                return Status.OK_STATUS;
              }
            };
            job.schedule();
          }
        } else {
          //what now? nothing I guess..
        }
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

    updateStatus();
    return composite;
  }

  /**
   * Sets the up group-artifact-version controls
   */
  private Composite createGAVControls(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);

    GridLayout gridLayout = new GridLayout(4, false);
    gridLayout.marginWidth = 0;
    gridLayout.horizontalSpacing = 10;
    composite.setLayout(gridLayout);

    Label groupIDlabel = new Label(composite, SWT.NONE);
    groupIDlabel.setText(Messages.AddDependencyDialog_groupId_label);
    widthGroup.addControl(groupIDlabel);

    groupIDtext = new Text(composite, SWT.BORDER);
    groupIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(groupIDtext);

    new Label(composite, SWT.NONE);
    new Label(composite, SWT.NONE);
    

    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText(Messages.AddDependencyDialog_artifactId_label);
    widthGroup.addControl(artifactIDlabel);

    artifactIDtext = new Text(composite, SWT.BORDER);
    artifactIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(artifactIDtext);

    new Label(composite, SWT.NONE);
    new Label(composite, SWT.NONE);

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.AddDependencyDialog_version_label);
    widthGroup.addControl(versionLabel);

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    Label scopeLabel = new Label(composite, SWT.NONE);
    scopeLabel.setText(Messages.AddDependencyDialog_scope_label);

    scopeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
    scopeCombo.setItems(scopes);
    GridData scopeListData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
    scopeCombo.setLayoutData(scopeListData);
    scopeCombo.setText(scopes[0]);

    /*
     * Fix the tab order (group -> artifact -> version -> scope)
     */
    composite.setTabList(new Control[] {groupIDtext, artifactIDtext, versionText, scopeCombo});

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
    if(!updating) {
      resultsViewer.setSelection(StructuredSelection.EMPTY);
      updateStatus();
    }
 // TODO mkleint: for now just ignore.. 
//  this snippet is supposed to tell people that they selected dependency already in the
//  project    
    
//    if(dependencyNode == null) {
//      return;
//    }
//    dependencyNode.accept(new DependencyVisitor() {
//
//      public boolean visitLeave(DependencyNode node) {
//        if(node.getDependency() != null && node.getDependency().getArtifact() != null) {
//          Artifact artifact = node.getDependency().getArtifact();
//          if(artifact.getGroupId().equalsIgnoreCase(groupIDtext.getText().trim())
//              && artifact.getArtifactId().equalsIgnoreCase(artifactIDtext.getText().trim())) {
//            infoTextarea.setText(NLS.bind(Messages.AddDependencyDialog_info_transitive,
//                new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()}));
//          }
//          return false;
//        }
//        return true;
//      }
//
//      public boolean visitEnter(DependencyNode node) {
//        return true;
//      }
//    });

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
    setResultsLabelProvider();

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
   * 
   */
  private void setResultsLabelProvider() {
    //TODO we want to have the artifacts marked for presence and management..
    managedKeys = new HashSet<String>();
    existingKeys = new HashSet<String>();
    if (mavenProject != null && mavenProject.getDependencyManagement() != null) {
      for (org.apache.maven.model.Dependency d : mavenProject.getDependencyManagement().getDependencies()) {
        managedKeys.add(d.getGroupId() + ":" + d.getArtifactId());
        managedKeys.add(d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion());
      }
    }
    if (isForDependencyManagement) {
      existingKeys = managedKeys;
      managedKeys = Collections.<String>emptySet();
    }
    resultsViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
        new MavenPomSelectionComponent.SearchResultLabelProvider(existingKeys, managedKeys,
            IIndex.SEARCH_ARTIFACT)));
    resultsViewer.refresh();
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

// TODO mkleint: for now just ignore.. 
//    this snippet is supposed to tell people that they selected dependency already in the
//    project    
    
//    if(dependencyNode != null) {
//      dependencyNode.accept(new DependencyVisitor() {
//
//        public boolean visitEnter(DependencyNode node) {
//          return true;
//        }
//
//        public boolean visitLeave(DependencyNode node) {
//          if(node.getDependency() == null || node.getDependency().getArtifact() == null) {
//            return true;
//          }
//          Artifact artifact = node.getDependency().getArtifact();
//          if(artifact.getGroupId().equalsIgnoreCase(file.group)
//              && artifact.getArtifactId().equalsIgnoreCase(file.artifact)) {
//            buffer.append(NLS.bind(Messages.AddDependencyDialog_transitive_dependency,
//                new String[] {artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()}));
//            /*
//             * DependencyNodes don't know their parents. Determining which transitive dependency 
//             * is using the selected dependency is non trivial :(
//             */
//            return false;
//          }
//          return true;
//        }
//      });
//    }
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

  protected boolean isGroupAndArtifactPresent() {
    return groupIDtext.getText().trim().length() > 0 && artifactIDtext.getText().trim().length() > 0;
  }

  protected void updateStatus() {
    boolean enableOK = isGroupAndArtifactPresent() || (artifactFiles != null && artifactFiles.size() > 0);

    int severity = enableOK ? IStatus.OK : IStatus.ERROR;
    String message = enableOK ? "" : Messages.AddDependencyDialog_groupAndArtifactRequired; //$NON-NLS-1$

    if(lastStatus == null || lastStatus.getSeverity() != severity) {
      setInfo(severity, message);
    }
  }

  protected void updateStatus(IStatus status) {
    lastStatus = status;
    super.updateStatus(status);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   * This is called when OK is pressed. There's no obligation to do anything.
   */
  protected void computeResult() {
    String scope = scopeCombo.getText(); //$NON-NLS-1$

    if(artifactFiles == null || artifactFiles.size() == 1) {
      String type = "";
      String classifier = "";
      if (artifactFiles != null && artifactFiles.size() == 1) {
        // use the selected artifact props if available..
        IndexedArtifactFile file = artifactFiles.iterator().next();
        classifier = file.classifier;
        type = file.type;
      }
      Dependency dependency = createDependency(groupIDtext.getText().trim(), artifactIDtext.getText().trim(),
          versionText.getText().trim(), scope, type, classifier); //$NON-NLS-1$
      this.dependencies = Collections.singletonList(dependency);
    } else {
      this.dependencies = new LinkedList<Dependency>();
      for(IndexedArtifactFile file : artifactFiles) {
        Dependency dep = createDependency(file.group, file.artifact, managedKeys.contains(MavenPomSelectionComponent.getKey(file)) ? null : file.version, scope, file.type, file.classifier);
        this.dependencies.add(dep);
      }
    }
  }

  private Dependency createDependency(String groupID, String artifactID, String version, String scope, String type, String classifier) {
    Dependency dependency = PomFactory.eINSTANCE.createDependency();
    dependency.setGroupId(groupID);
    dependency.setArtifactId(artifactID);
    if (version != null) {
      dependency.setVersion(version);
    }
    dependency.setClassifier(classifier);

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
        updateStatus();
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
          boolean managed = false;
          if(obj instanceof IndexedArtifact) {
            //the idea here is that if we have a managed version for something, then the IndexedArtifact shall
            //represent that value..
            IndexedArtifact ia = (IndexedArtifact)obj;
            if (managedKeys.contains(MavenPomSelectionComponent.getKey(ia))) {
              for (IndexedArtifactFile f : ia.getFiles()) {
                if (managedKeys.contains(MavenPomSelectionComponent.getKey(f))) {
                  file = f;
                  managed = true;
                  break;
                }
              }
            }
            if (file == null) {
              file = ((IndexedArtifact) obj).getFiles().iterator().next();
            }
          } else {
            file = (IndexedArtifactFile) obj;
            if (managedKeys.contains(MavenPomSelectionComponent.getKey(file))) {
              managed = true;
            }
          }

          appendFileInfo(buffer, file);
          artifactFiles.add(file);

          artifact = chooseWidgetText(artifact, file.artifact);
          group = chooseWidgetText(group, file.group);
          version = chooseWidgetText(version, managed ? "" : file.version);
        }
        setInfo(OK, NLS.bind(artifactFiles.size() == 1 ? Messages.AddDependencyDialog_itemSelected
            : Messages.AddDependencyDialog_itemsSelected, artifactFiles.size()));
        infoTextarea.setText(buffer.toString());

        updating = true;
        artifactIDtext.setText(artifact);
        groupIDtext.setText(group);
        versionText.setText(version);
        updating = false;

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
        Map<String, IndexedArtifact> results = indexManager.getIndex(project).search(
            new UserInputSearchExpression(query), IIndex.SEARCH_ARTIFACT, IIndex.SEARCH_JARS + IIndex.SEARCH_TESTS);
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
          if(status == IStatus.OK) {
            infoTextarea.setText(infoMessage);
          } else {
            setInfo(status, infoMessage);
          }
          if(results != null && resultsViewer != null && resultsViewer.getControl() != null
              && !resultsViewer.getControl().isDisposed()) {
            resultsViewer.setInput(results);
          }
        }
      });
    }

  }
}
