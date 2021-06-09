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

package org.eclipse.m2e.core.ui.internal.dialogs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
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
import org.eclipse.m2e.core.internal.index.IIndex;
import org.eclipse.m2e.core.internal.index.IndexedArtifact;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.core.ui.internal.wizards.MavenPomSelectionComponent;


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
   * @param parent
   * @param title
   * @return
   */
  public static MavenRepositorySearchDialog createOpenPomDialog(Shell parent, String title) {
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_ARTIFACT, Collections.<ArtifactKey> emptySet(),
        Collections.<ArtifactKey, String> emptyMap(), false, null, null, false);
  }

  /**
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @param inManagedSection true when the result will be added to the dependencyManagement section of the pom.
   * @return
   */
  public static MavenRepositorySearchDialog createSearchDependencyDialog(Shell parent, String title, MavenProject mp,
      IProject p, boolean inManagedSection) {
    Set<ArtifactKey> artifacts = new HashSet<>();
    Map<ArtifactKey, String> managed = new HashMap<>();
    if(mp != null) {
      DependencyManagement dm = mp.getDependencyManagement();
      if(dm != null && dm.getDependencies() != null) {
        for(Dependency dep : dm.getDependencies()) {
          ArtifactKey artifactKey = new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
              dep.getClassifier());
          if(inManagedSection) {
            artifacts.add(artifactKey);
          } else {
            managed.put(artifactKey, dep.getType());
          }
        }
      }
      if(!inManagedSection) {
        for(Dependency dep : mp.getModel().getDependencies()) {
          artifacts.add(new ArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getClassifier()));
        }
      }
    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_ARTIFACT, artifacts, managed, true, mp, p, true);
  }

  /**
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @return
   */
  public static MavenRepositorySearchDialog createSearchParentDialog(Shell parent, String title, MavenProject mp,
      IProject p) {
    Set<ArtifactKey> artifacts = new HashSet<>();
    Map<ArtifactKey, String> managed = new HashMap<>();
    if(mp != null && mp.getModel().getParent() != null) {
      Parent par = mp.getModel().getParent();
      artifacts.add(new ArtifactKey(par.getGroupId(), par.getArtifactId(), par.getVersion(), null));
    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_PARENTS, artifacts, managed, false, mp, p, true);
  }

  /**
   * @param parent
   * @param title
   * @param mp
   * @param p
   * @param inManagedSection true when the result will be added to the dependencyManagement section of the pom.
   * @return
   */
  public static MavenRepositorySearchDialog createSearchPluginDialog(Shell parent, String title, MavenProject mp,
      IProject p, boolean inManagedSection) {
    Set<ArtifactKey> artifacts = new HashSet<>();
    Map<ArtifactKey, String> managed = new HashMap<>();
    if(mp != null && mp.getBuild() != null) {
      PluginManagement pm = mp.getBuild().getPluginManagement();
      if(pm != null && pm.getPlugins() != null) {
        for(Plugin plug : pm.getPlugins()) {
          ArtifactKey artifactKey = new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null);
          if(inManagedSection) {
            artifacts.add(artifactKey);
          } else {
            managed.put(artifactKey, ""); //$NON-NLS-1$
          }
        }
      }
      if(!inManagedSection && mp.getModel().getBuild() != null) {
        for(Plugin plug : mp.getModel().getBuild().getPlugins()) {
          artifacts.add(new ArtifactKey(plug.getGroupId(), plug.getArtifactId(), plug.getVersion(), null));
        }
      }

    }
    return new MavenRepositorySearchDialog(parent, title, IIndex.SEARCH_PLUGIN, artifacts, managed, false, mp, p, true);
  }

  private final boolean showScope;

  private final Set<ArtifactKey> artifacts;

  /**
   * keys = artifact keys, values = type
   */
  private final Map<ArtifactKey, String> managed;

  /**
   * One of {@link IIndex#SEARCH_ARTIFACT}, {@link IIndex#SEARCH_CLASS_NAME},
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

  private final IProject project;

  private final MavenProject mavenproject;

  private final boolean showCoords;

  private MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts,
      Map<ArtifactKey, String> managed, boolean showScope, MavenProject mp, IProject p, boolean showCoordinates) {
    super(parent, DIALOG_SETTINGS);
    this.artifacts = artifacts;
    this.managed = managed;
    this.queryType = queryType;
    this.showScope = showScope;
    this.project = p;
    this.mavenproject = mp;
    this.showCoords = showCoordinates;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setStatusLineAboveButtons(true);
    setTitle(title);
  }

  public void setQuery(String query) {
    this.queryText = query;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    readSettings();

    Composite composite = (Composite) super.createDialogArea(parent);
    if(showCoords) {
      createGAVControls(composite);
      Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
      separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    pomSelectionComponent = new MavenPomSelectionComponent(composite, SWT.NONE);
    pomSelectionComponent.init(queryText, queryType, project, artifacts, managed.keySet());

    pomSelectionComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    pomSelectionComponent.addDoubleClickListener(event -> {
      if(!pomSelectionComponent.getStatus().matches(IStatus.ERROR)) {
        okPressedDelegate();
      }
    });
    pomSelectionComponent.addSelectionChangedListener(event -> {
      updateStatusDelegate(pomSelectionComponent.getStatus());
      computeResultFromTree();
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
    M2EUIUtils.addRequiredDecoration(txtGroupId);

    if(showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }

    Label artifactIDlabel = new Label(composite, SWT.NONE);
    artifactIDlabel.setText(Messages.AddDependencyDialog_artifactId_label);

    txtArtifactId = new Text(composite, SWT.BORDER);
    txtArtifactId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    M2EUIUtils.addRequiredDecoration(txtArtifactId);

    if(showScope) {
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
    }

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.AddDependencyDialog_version_label);

    txtVersion = new Text(composite, SWT.BORDER);
    txtVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    if(showScope) {
      Label scopeLabel = new Label(composite, SWT.NONE);
      scopeLabel.setText(Messages.AddDependencyDialog_scope_label);

      comScope = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
      comScope.setItems(SCOPES);
      GridData scopeListData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      comScope.setLayoutData(scopeListData);
      comScope.setText(SCOPES[0]);
    }

    if(showScope) {
      /*
       * Fix the tab order (group -> artifact -> version -> scope)
       */
      composite.setTabList(new Control[] {txtGroupId, txtArtifactId, txtVersion, comScope});
    } else {
      composite.setTabList(new Control[] {txtGroupId, txtArtifactId, txtVersion});
    }

    Packaging pack;
    if(IIndex.SEARCH_PARENTS.equals(queryType)) {
      pack = Packaging.POM;
    } else if(IIndex.SEARCH_PLUGIN.equals(queryType)) {
      pack = Packaging.PLUGIN;
    } else {
      pack = Packaging.ALL;
    }
    ProposalUtil.addGroupIdProposal(project, txtGroupId, pack);
    ProposalUtil.addArtifactIdProposal(project, txtGroupId, txtArtifactId, pack);
    ProposalUtil.addVersionProposal(project, mavenproject, txtGroupId, txtArtifactId, txtVersion, pack);

    txtArtifactId.addModifyListener(e -> {
      updateStatus(validateArtifactEntries());
      if(!ignoreTextChange && !hasDisposedTextField()) {
        computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()),
            valueOrNull(txtVersion.getText()));
      }
    });

    txtGroupId.addModifyListener(e -> {
      updateStatus(validateArtifactEntries());
      if(!ignoreTextChange && !hasDisposedTextField()) {
        computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()),
            valueOrNull(txtVersion.getText()));
      }
    });
    txtVersion.addModifyListener(e -> {
      if(!ignoreTextChange && !hasDisposedTextField()) {
        computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()),
            valueOrNull(txtVersion.getText()));
      }
    });

    updateStatus(validateArtifactEntries());
    return composite;
  }

  protected boolean hasDisposedTextField() {
    return txtGroupId.isDisposed() || txtArtifactId.isDisposed() || txtVersion.isDisposed();
  }

  IStatus validateArtifactEntries() {
    if(!txtArtifactId.isDisposed() && txtArtifactId.getText().isEmpty())
      return new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, Messages.AddDependencyDialog_artifactId_error);

    if(!txtGroupId.isDisposed() && txtGroupId.getText().isEmpty())
      return new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID, Messages.AddDependencyDialog_groupId_error);

    return new Status(IStatus.OK, M2EUIPluginActivator.PLUGIN_ID, "");//$NON-NLS-1$;
  }

  void okPressedDelegate() {
    okPressed();
  }

  void updateStatusDelegate(IStatus status) {
    IStatus validationStatus = validateArtifactEntries();
    if(validationStatus.isOK())
      updateStatus(status);
    else
      updateStatus(validationStatus);
  }

  private String valueOrNull(String text) {
    return text.trim().length() == 0 ? null : text;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
   */
  @Override
  protected void computeResult() {
    if(showCoords) {
      computeResultFromField(valueOrNull(txtGroupId.getText()), valueOrNull(txtArtifactId.getText()),
          valueOrNull(txtVersion.getText()));
    } else {
      computeResultFromTree();
    }
  }

  private void computeResultFromField(String groupId, String artifactId, String version) {
    selectedIndexedArtifact = cloneIndexedArtifact(selectedIndexedArtifact, groupId, artifactId);
    selectedIndexedArtifactFile = cloneIndexedArtifactFile(selectedIndexedArtifactFile, groupId, artifactId, version);
    selectedScope = comScope == null ? null : comScope.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
  }

  private void computeResultFromTree() {
    if(pomSelectionComponent.isDisposed()) {
      return;
    }
    selectedIndexedArtifact = pomSelectionComponent.getIndexedArtifact();
    selectedIndexedArtifactFile = pomSelectionComponent.getIndexedArtifactFile();
    selectedScope = comScope == null ? null : comScope.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
    if(selectedIndexedArtifactFile != null && showCoords) {
      ignoreTextChange = true;
      try {
        txtGroupId.setText(selectedIndexedArtifactFile.group);
        txtArtifactId.setText(selectedIndexedArtifactFile.artifact);

        String type = managed.get(new ArtifactKey(selectedIndexedArtifactFile.group, selectedIndexedArtifactFile.artifact,
                selectedIndexedArtifactFile.version, selectedIndexedArtifactFile.classifier));
        if(type != null) {
          txtVersion.setText(""); //$NON-NLS-1$
          // fix type (take from depMgmt instead of from pom), compare with https://bugs.eclipse.org/bugs/show_bug.cgi?id=473998
          if(type.length() > 0) {
            selectedIndexedArtifactFile = cloneIndexedArtifactFile(selectedIndexedArtifactFile, type);
          }
        } else {
          txtVersion.setText(selectedIndexedArtifactFile.version);
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
    if(old == null) {
      return new IndexedArtifact(groupId, artifactId, null, null, null);
    }
    return new IndexedArtifact(groupId != null ? groupId : old.getGroupId(), artifactId != null ? artifactId
        : old.getArtifactId(), old.getPackageName(), old.getClassname(), old.getPackaging());
  }

  private IndexedArtifactFile cloneIndexedArtifactFile(IndexedArtifactFile old, String groupId, String artifactId,
      String version) {
    if(old == null) {
      return new IndexedArtifactFile(null, groupId, artifactId, version, null, null, null, 0L, null, 0, 0, null, null);
    }
    return new IndexedArtifactFile(old.repository, groupId, artifactId, version, old.type, old.classifier, old.fname,
        old.size, old.date, old.sourcesExists, old.javadocExists, old.prefix, old.goals);
  }

  private IndexedArtifactFile cloneIndexedArtifactFile(IndexedArtifactFile old, String type) {
    if(old == null) {
      throw new IllegalArgumentException("Must call with argument type != null"); //$NON-NLS-1$
    }
    return new IndexedArtifactFile(old.repository, old.group, old.artifact, old.version, type, old.classifier,
        old.fname, old.size, old.date, old.sourcesExists, old.javadocExists, old.prefix, old.goals);
  }
}
