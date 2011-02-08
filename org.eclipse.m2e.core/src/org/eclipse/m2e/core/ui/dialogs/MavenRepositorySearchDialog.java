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

package org.eclipse.m2e.core.ui.dialogs;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.util.M2EUtils;
import org.eclipse.m2e.core.util.ProposalUtil;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.core.wizards.MavenPomSelectionComponent;


/**
 * Maven POM Search dialog
 * 
 * @author Eugene Kuleshov
 */
public class MavenRepositorySearchDialog extends AbstractMavenDialog {
  private static final String DIALOG_SETTINGS = MavenRepositorySearchDialog.class.getName();
  
  public static final String[] SCOPES = new String[] {"compile", "provided", "runtime", "test", "system"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

  /*
   * dependencies under dependencyManagement are permitted to use an the extra "import" scope
   */
  public static final String[] DEP_MANAGEMENT_SCOPES = new String[] {"compile", "provided", "runtime", "test", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      "system", "import"}; //$NON-NLS-1$ //$NON-NLS-2$
  

  /**
   * 
   * @param parent
   * @param title
   * @return
   */
  public static MavenRepositorySearchDialog createOpenPomDialog(Shell parent, String title) {
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_ARTIFACT, Collections.<ArtifactKey>emptySet(), Collections.<ArtifactKey>emptySet(), false, null, null);
  }
  
  /**
   * 
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @param inManagedSection true when the result will be added to the dependencyManagement section of the pom. 
   * @return
   */
  public static MavenRepositorySearchDialog createSearchDependencyDialog(Shell parent, String title, MavenProject mp, IProject p, boolean inManagedSection) {
    Set<ArtifactKey> artifacts = new HashSet<ArtifactKey>(); 
    Set<ArtifactKey> managed = new HashSet<ArtifactKey>(); 
    if (mp != null) {
        Set<ArtifactKey> keys = inManagedSection ? artifacts : managed;
        DependencyManagement dm = mp.getDependencyManagement();
        if (dm != null && dm.getDependencies() != null) {
          for (Dependency dep : dm.getDependencies()) {
            keys.add(new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier()));
          }
        }
        if (!inManagedSection) {
          for (Dependency dep : mp.getModel().getDependencies()) {
            artifacts.add(new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier()));
          }
        }
    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_ARTIFACT,  artifacts, managed, true, mp, p);
  }
  /**
   * 
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @return
   */
  public static MavenRepositorySearchDialog createSearchParentDialog(Shell parent, String title, MavenProject mp, IProject p) {
    Set<ArtifactKey> artifacts = new HashSet<ArtifactKey>(); 
    Set<ArtifactKey> managed = new HashSet<ArtifactKey>(); 
    if (mp != null && mp.getModel().getParent() != null) {
      Parent par = mp.getModel().getParent();
      artifacts.add(new ArtifactKey(par.getGroupId(), par.getArtifactId(), par.getVersion(), null));      
    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_PARENTS, artifacts, managed, false, mp, p);     
  }
  
  /**
   * 
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @param inManagedSection true when the result will be added to the dependencyManagement section of the pom. 
   * @return
   */
  public static MavenRepositorySearchDialog createSearchPluginDialog(Shell parent, String title, MavenProject mp, IProject p, boolean inManagedSection) {
    Set<ArtifactKey> artifacts = new HashSet<ArtifactKey>(); 
    Set<ArtifactKey> managed = new HashSet<ArtifactKey>();
    Set<ArtifactKey> keys = inManagedSection ? artifacts : managed;
    if (mp != null && mp.getBuild() != null) {
        PluginManagement pm = mp.getBuild().getPluginManagement();
        if (pm != null && pm.getPlugins() != null) {
          for (Plugin plug : pm.getPlugins()) {
            keys.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
          }
        }
        if (!inManagedSection && mp.getModel().getBuild() != null) {
          for (Plugin plug : mp.getModel().getBuild().getPlugins()) {
            artifacts.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
          }
        }
        
    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_PLUGIN,  artifacts, managed, false, mp, p);
  }  
  private final boolean showScope;
  
  private final Set<ArtifactKey> artifacts;

  private final Set<ArtifactKey> managed;

  /**
   * One of 
   *   {@link IIndex#SEARCH_ARTIFACT}, 
   *   {@link IIndex#SEARCH_CLASS_NAME}, 
   */
  private final String queryType;

  private String queryText;

  MavenPomSelectionComponent pomSelectionComponent;

  private IndexedArtifact selectedIndexedArtifact;

  private IndexedArtifactFile selectedIndexedArtifactFile;

  private String selectedScope;

  private Combo comScope;

  private Text txtGroupId;

  private Text txtArtifactId;

  private Text txtVersion;
  
  private boolean ignoreTextChange = false;

  private IProject project;

  private MavenProject mavenproject;

  private MavenRepositorySearchDialog(Shell parent, String title, String queryType, 
      Set<ArtifactKey> artifacts, Set<ArtifactKey> managed, boolean showScope, MavenProject mp, IProject p) {
    super(parent, DIALOG_SETTINGS);
    this.artifacts = artifacts;
    this.managed = managed;
    this.queryType = queryType;
    this.showScope = showScope;
    this.project = p;
    this.mavenproject = mp;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setStatusLineAboveButtons(true);
    setTitle(title);
  }

  public void setQuery(String query) {
    this.queryText = query;
  }

  protected Control createDialogArea(Composite parent) {
    readSettings();
    
    Composite composite = (Composite) super.createDialogArea(parent);
    createGAVControls(composite);
    
    pomSelectionComponent = new MavenPomSelectionComponent(composite, SWT.NONE);
    pomSelectionComponent.init(queryText, queryType, artifacts, managed);
    
    pomSelectionComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    pomSelectionComponent.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if (!pomSelectionComponent.getStatus().matches(IStatus.ERROR)) {
          okPressedDelegate();
        }
      }
    });
    pomSelectionComponent.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateStatusDelegate(pomSelectionComponent.getStatus());
        computeResultFromTree();
      }
    });
    pomSelectionComponent.setFocus();
    
    return composite;
  }
  
  /**
   * Sets the up group-artifact-version controls
   */
  private Composite createGAVControls(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    GridLayout gridLayout = new GridLayout(showScope ? 4 : 2, false);
    gridLayout.marginWidth = 0;
    gridLayout.horizontalSpacing = 10;
    composite.setLayout(gridLayout);

    Label groupIDlabel = new Label(composite, SWT.NONE);
    groupIDlabel.setText(Messages.AddDependencyDialog_groupId_label);

    txtGroupId = new Text(composite, SWT.BORDER);
    txtGroupId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(txtGroupId);

    if (showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }
    
    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText(Messages.AddDependencyDialog_artifactId_label);

    txtArtifactId = new Text(composite, SWT.BORDER);
    txtArtifactId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(txtArtifactId);

    if (showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.AddDependencyDialog_version_label);

    txtVersion = new Text(composite, SWT.BORDER);
    txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    if (showScope) {
      Label scopeLabel = new Label(composite, SWT.NONE);
      scopeLabel.setText(Messages.AddDependencyDialog_scope_label);
    
      comScope = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
      comScope.setItems(SCOPES);
      GridData scopeListData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      comScope.setLayoutData(scopeListData);
      comScope.setText(SCOPES[0]);
    }

    if (showScope) {
      /*
       * Fix the tab order (group -> artifact -> version -> scope)
       */
      composite.setTabList(new Control[] {txtGroupId, txtArtifactId, txtVersion, comScope});
    } else {
      composite.setTabList(new Control[] {txtGroupId, txtArtifactId, txtVersion});
    }

    Packaging pack;
    if (queryType.equals(IIndex.SEARCH_PARENTS)) {
      pack = Packaging.POM;
    } else if (queryType.equals(IIndex.SEARCH_PLUGIN)) {
      pack = Packaging.PLUGIN;
    } else {
      pack = Packaging.ALL;
    }
    ProposalUtil.addGroupIdProposal(project, txtGroupId, pack);
    ProposalUtil.addArtifactIdProposal(project, txtGroupId, txtArtifactId, pack);
    ProposalUtil.addVersionProposal(project, txtGroupId, txtArtifactId, txtVersion, pack);

    txtArtifactId.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()), valueOrNull(txtVersion.getText()));
        }
      }
    });

    txtGroupId.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()), valueOrNull(txtVersion.getText()));
        }
      }
    });
    txtVersion.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()), valueOrNull(txtVersion.getText()));
        }
      }
    });

    return composite;
  }  
  
  void okPressedDelegate() {
    okPressed();
  }
  
  void updateStatusDelegate(IStatus status) {
    updateStatus(status);
  }
  
  private String valueOrNull(String text) {
    return text.trim().length() == 0 ? null : text; 
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   */
  protected void computeResult() {
    computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()), valueOrNull(txtVersion.getText()));
  }  
  
  private void computeResultFromField(String groupId, String artifactId, String version) {
    selectedIndexedArtifact = cloneIndexedArtifact(selectedIndexedArtifact, groupId, artifactId);
    selectedIndexedArtifactFile = cloneIndexedArtifactFile(selectedIndexedArtifactFile, groupId, artifactId, version);
    selectedScope = comScope == null ? null : comScope.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
  }

  private void computeResultFromTree() {
    selectedIndexedArtifact = pomSelectionComponent.getIndexedArtifact();
    selectedIndexedArtifactFile = pomSelectionComponent.getIndexedArtifactFile();
    selectedScope = comScope == null ? null : comScope.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
    if (selectedIndexedArtifactFile != null) {
      ignoreTextChange = true;
      try {
        txtGroupId.setText(selectedIndexedArtifactFile.group);
        txtArtifactId.setText(selectedIndexedArtifactFile.artifact);
        if (!managed.contains(new ArtifactKey(selectedIndexedArtifactFile.group, selectedIndexedArtifactFile.artifact, selectedIndexedArtifactFile.version, selectedIndexedArtifactFile.classifier))) {
          txtVersion.setText(selectedIndexedArtifactFile.version);
        } else {
          txtVersion.setText("");
        }
      } finally {
        ignoreTextChange = false;
      }
    }
  }
  
  public IndexedArtifact getSelectedIndexedArtifact() {
    return this.selectedIndexedArtifact;
  }
  
  public IndexedArtifactFile getSelectedIndexedArtifactFile() {
    return this.selectedIndexedArtifactFile;
  }
  
  public String getSelectedScope() {
    return this.selectedScope;
  } 
  
  private IndexedArtifact cloneIndexedArtifact(IndexedArtifact old, String groupId, String artifactId) {
    if (old == null) {
      return new IndexedArtifact(groupId, artifactId, null, null, null);
    }
    return new IndexedArtifact(groupId != null ? groupId : old.getGroupId(), 
        artifactId != null ? artifactId : old.getArtifactId(), 
            old.getPackageName(), old.getClassname(), old.getPackaging());
  }
  
  private IndexedArtifactFile cloneIndexedArtifactFile(IndexedArtifactFile old, String groupId, String artifactId, String version) {
    if (old == null) {
      return new IndexedArtifactFile(null, groupId, artifactId, version, null, null, null, 0L, null, 0, 0, null, null);
    }
    return new IndexedArtifactFile(old.repository,
        groupId, 
        artifactId, 
        version,
        old.type,
        old.classifier, 
        old.fname,
        old.size,
        old.date,
        old.sourcesExists,
        old.javadocExists, 
        old.prefix,
        old.goals);
  }


  
}
