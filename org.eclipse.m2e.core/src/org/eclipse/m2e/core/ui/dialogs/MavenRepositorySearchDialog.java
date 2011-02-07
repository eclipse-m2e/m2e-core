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

  private Combo scopeCombo;

  private Text groupIDtext;

  private Text artifactIDtext;

  private Text versionText;
  
  private boolean ignoreTextChange = false;


  /**
   * Create repository search dialog
   * 
   * @param parent parent shell
   * @param title dialog title
   * @param queryType one of 
   *   {@link IIndex#SEARCH_ARTIFACT}, 
   *   {@link IIndex#SEARCH_CLASS_NAME}, 
   * @param artifacts Set&lt;Artifact&gt;
   * @deprecated
   */
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts) {
    this(parent, title, queryType, artifacts, Collections.<ArtifactKey>emptySet(), false);
  }
  
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, Set<ArtifactKey> managed) {
    this(parent, title, queryType, artifacts, managed, false);
  }
  /**
   * @deprecated
   */
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, boolean showScope) {
    this(parent, title, queryType, artifacts, Collections.<ArtifactKey>emptySet(), showScope);
  }

  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, Set<ArtifactKey> managed, boolean showScope) {
    super(parent, DIALOG_SETTINGS);
    this.artifacts = artifacts;
    this.managed = managed;
    this.queryType = queryType;
    this.showScope = showScope;

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
//    widthGroup.addControl(groupIDlabel);

    groupIDtext = new Text(composite, SWT.BORDER);
    groupIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(groupIDtext);

    if (showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }
    

    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText(Messages.AddDependencyDialog_artifactId_label);
//    widthGroup.addControl(artifactIDlabel);

    artifactIDtext = new Text(composite, SWT.BORDER);
    artifactIDtext.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUtils.addRequiredDecoration(artifactIDtext);

    if (showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.AddDependencyDialog_version_label);
//    widthGroup.addControl(versionLabel);

    versionText = new Text(composite, SWT.BORDER);
    versionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    if (showScope) {
      Label scopeLabel = new Label(composite, SWT.NONE);
      scopeLabel.setText(Messages.AddDependencyDialog_scope_label);
    
      scopeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
      scopeCombo.setItems(SCOPES);
      GridData scopeListData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      scopeCombo.setLayoutData(scopeListData);
      scopeCombo.setText(SCOPES[0]);
    }

    if (showScope) {
      /*
       * Fix the tab order (group -> artifact -> version -> scope)
       */
      composite.setTabList(new Control[] {groupIDtext, artifactIDtext, versionText, scopeCombo});
    } else {
      composite.setTabList(new Control[] {groupIDtext, artifactIDtext, versionText});
    }

    Packaging pack;
    if (queryType.equals(IIndex.SEARCH_PARENTS)) {
      pack = Packaging.POM;
    } else if (queryType.equals(IIndex.SEARCH_PLUGIN)) {
      pack = Packaging.PLUGIN;
    } else {
      pack = Packaging.ALL;
    }
    ProposalUtil.addGroupIdProposal((IProject)null, groupIDtext, pack);
    ProposalUtil.addArtifactIdProposal((IProject)null, groupIDtext, artifactIDtext, pack);
    ProposalUtil.addVersionProposal((IProject)null, groupIDtext, artifactIDtext, versionText, pack);

    artifactIDtext.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(null, artifactIDtext.getText(), null);
        }
      }
    });

    groupIDtext.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(groupIDtext.getText(), null, null);
        }
      }
    });
    versionText.addModifyListener(new ModifyListener() {

      public void modifyText(ModifyEvent e) {
        if (!ignoreTextChange) {
          computeResultFromField(null, null, versionText.getText());
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
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   */
  protected void computeResult() {
    computeResultFromField(groupIDtext.getText(), artifactIDtext.getText(), versionText.getText().trim().length() == 0 ? null : versionText.getText());
  }  
  
  private void computeResultFromField(String groupId, String artifactId, String version) {
    selectedIndexedArtifact = cloneIndexedArtifact(selectedIndexedArtifact, groupId, artifactId);
    selectedIndexedArtifactFile = cloneIndexedArtifactFile(selectedIndexedArtifactFile, groupId, artifactId, version);
    selectedScope = scopeCombo == null ? null : scopeCombo.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
  }

  private void computeResultFromTree() {
    selectedIndexedArtifact = pomSelectionComponent.getIndexedArtifact();
    selectedIndexedArtifactFile = pomSelectionComponent.getIndexedArtifactFile();
    selectedScope = scopeCombo == null ? null : scopeCombo.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
    if (selectedIndexedArtifactFile != null) {
      ignoreTextChange = true;
      try {
        groupIDtext.setText(selectedIndexedArtifactFile.group);
        artifactIDtext.setText(selectedIndexedArtifactFile.artifact);
        if (!managed.contains(new ArtifactKey(selectedIndexedArtifactFile.group, selectedIndexedArtifactFile.artifact, selectedIndexedArtifactFile.version, selectedIndexedArtifactFile.classifier))) {
          versionText.setText(selectedIndexedArtifactFile.version);
        } else {
          versionText.setText("");
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
        groupId != null ? groupId : old.group, 
        artifactId != null ? artifactId : old.artifact, 
        version != null ? version : old.version,
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
