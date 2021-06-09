/*******************************************************************************
 * Copyright (c) 2008-2011 Sonatype, Inc.
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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CLASSIFIER;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEPENDENCY_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.OPTIONAL;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SCOPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SYSTEM_PATH;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.TYPE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;
import static org.eclipse.m2e.core.ui.internal.util.Util.nvl;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;


public class EditDependencyDialog extends AbstractMavenDialog {
  private static final String[] TYPES = new String[] {"jar", "war", "rar", "ear", "par", "ejb", "ejb-client", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
      "test-jar", "java-source", "javadoc", "maven-plugin", "pom"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

  private final IProject project;

  private String[] scopes;

  protected Text groupIdText;

  protected Text artifactIdText;

  protected Text versionText;

  protected Text classifierText;

  protected Combo typeCombo;

  protected Combo scopeCombo;

  protected Text systemPathText;

  protected Button optionalButton;

  private Dependency dependency;

  private final MavenProject mavenproject;

  private final boolean dependencyManagement;

  private Operation resultOperation;

  /**
   * @param parent
   * @param dependencyManagement
   * @param project can be null, only used for indexer search as scope
   * @param mavenProject
   */
  public EditDependencyDialog(Shell parent, boolean dependencyManagement, IProject project, MavenProject mavenProject) {
    super(parent, EditDependencyDialog.class.getName());
    this.project = project;
    this.mavenproject = mavenProject;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setTitle(Messages.EditDependencyDialog_title);
    this.dependencyManagement = dependencyManagement;
    if(!dependencyManagement) {
      scopes = MavenRepositorySearchDialog.SCOPES;
    } else {
      scopes = MavenRepositorySearchDialog.DEP_MANAGEMENT_SCOPES;
    }
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    readSettings();
    Composite superComposite = (Composite) super.createDialogArea(parent);

    Composite composite = new Composite(superComposite, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Label groupIdLabel = new Label(composite, SWT.NONE);
    groupIdLabel.setText(Messages.EditDependencyDialog_groupId_label);

    groupIdText = new Text(composite, SWT.BORDER);
    GridData gd_groupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_groupIdText.horizontalIndent = 4;
    groupIdText.setLayoutData(gd_groupIdText);
    ProposalUtil.addGroupIdProposal(project, groupIdText, Packaging.ALL);
    M2EUIUtils.addRequiredDecoration(groupIdText);

    Label artifactIdLabel = new Label(composite, SWT.NONE);
    artifactIdLabel.setText(Messages.EditDependencyDialog_artifactId_label);

    artifactIdText = new Text(composite, SWT.BORDER);
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    ProposalUtil.addArtifactIdProposal(project, groupIdText, artifactIdText, Packaging.ALL);
    M2EUIUtils.addRequiredDecoration(artifactIdText);

    Label versionLabel = new Label(composite, SWT.NONE);
    versionLabel.setText(Messages.EditDependencyDialog_version_label);

    versionText = new Text(composite, SWT.BORDER);
    GridData versionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    versionTextData.horizontalIndent = 4;
    versionTextData.widthHint = 200;
    versionText.setLayoutData(versionTextData);
    ProposalUtil.addVersionProposal(project, mavenproject, groupIdText, artifactIdText, versionText, Packaging.ALL);

    Label classifierLabel = new Label(composite, SWT.NONE);
    classifierLabel.setText(Messages.EditDependencyDialog_classifier_label);

    classifierText = new Text(composite, SWT.BORDER);
    GridData gd_classifierText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_classifierText.horizontalIndent = 4;
    gd_classifierText.widthHint = 200;
    classifierText.setLayoutData(gd_classifierText);
    ProposalUtil
        .addClassifierProposal(project, groupIdText, artifactIdText, versionText, classifierText, Packaging.ALL);

    Label typeLabel = new Label(composite, SWT.NONE);
    typeLabel.setText(Messages.EditDependencyDialog_type_label);

    typeCombo = new Combo(composite, SWT.NONE);
    typeCombo.setItems(TYPES);
    GridData gd_typeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_typeText.horizontalIndent = 4;
    gd_typeText.widthHint = 120;
    typeCombo.setLayoutData(gd_typeText);

    Label scopeLabel = new Label(composite, SWT.NONE);
    scopeLabel.setText(Messages.EditDependencyDialog_scope_label);

    scopeCombo = new Combo(composite, SWT.NONE);
    scopeCombo.setItems(scopes);
    GridData gd_scopeText = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    gd_scopeText.horizontalIndent = 4;
    gd_scopeText.widthHint = 120;
    scopeCombo.setLayoutData(gd_scopeText);

    Label systemPathLabel = new Label(composite, SWT.NONE);
    systemPathLabel.setText(Messages.EditDependencyDialog_systemPath_label);

    systemPathText = new Text(composite, SWT.BORDER);
    GridData gd_systemPathText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    gd_systemPathText.horizontalIndent = 4;
    gd_systemPathText.widthHint = 200;
    systemPathText.setLayoutData(gd_systemPathText);

//    selectSystemPathButton = new Button(composite, SWT.NONE);
//    selectSystemPathButton.setText("Select...");

    new Label(composite, SWT.NONE);

    optionalButton = new Button(composite, SWT.CHECK);
    optionalButton.setText(Messages.EditDependencyDialog_optional_checkbox);
    GridData gd_optionalButton = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
    gd_optionalButton.horizontalIndent = 4;
    optionalButton.setLayoutData(gd_optionalButton);

    composite.setTabList(new Control[] {groupIdText, artifactIdText, versionText, classifierText, typeCombo,
        scopeCombo, systemPathText, /*selectSystemPathButton,*/optionalButton});

    setDependency(dependency);

    return superComposite;
  }

  public Operation getEditOperation() {
    return resultOperation;
  }

  @Override
  protected void computeResult() {
    final String oldArtifactId = dependency.getArtifactId();
    final String oldGroupId = dependency.getGroupId();
    final String groupId = valueOrNull(groupIdText.getText());
    final String artifactId = valueOrNull(artifactIdText.getText());
    final String version = valueOrNull(versionText.getText());
    final String type = valueOrNull(typeCombo.getText());
    final String scope = valueOrNull(scopeCombo.getText());
    final String classifier = valueOrNull(classifierText.getText());
    final String system = valueOrNull(systemPathText.getText());
    final boolean optional = optionalButton.getSelection();
    resultOperation = document -> {
      Element depsEl = dependencyManagement ? getChild(document.getDocumentElement(), DEPENDENCY_MANAGEMENT,
          DEPENDENCIES) : getChild(document.getDocumentElement(), DEPENDENCIES);
      Element dep = findChild(depsEl, DEPENDENCY, childEquals(GROUP_ID, oldGroupId),
          childEquals(ARTIFACT_ID, oldArtifactId));
      if(dep != null) {
        if(artifactId != null && !artifactId.equals(oldArtifactId)) {
          setText(getChild(dep, ARTIFACT_ID), artifactId);
        }
        if(groupId != null && !groupId.equals(oldGroupId)) {
          setText(getChild(dep, GROUP_ID), groupId);
        }
        //only set version if already exists
        if(version != null) {
          setText(getChild(dep, VERSION), version);
        } else {
          removeChild(dep, findChild(dep, VERSION));
        }
        if(type != null //
            && !"jar".equals(type) // //$NON-NLS-1$
            && !"null".equals(type)) { // guard against MNGECLIPSE-622 //$NON-NLS-1$

          setText(getChild(dep, TYPE), type);
        } else {
          removeChild(dep, findChild(dep, TYPE));
        }
        if(classifier != null) {
          setText(getChild(dep, CLASSIFIER), classifier);
        } else {
          removeChild(dep, findChild(dep, CLASSIFIER));
        }
        if(scope != null && !"compile".equals(scope)) { //$NON-NLS-1$
          setText(getChild(dep, SCOPE), scope);
        } else {
          removeChild(dep, findChild(dep, SCOPE));
        }
        if(system != null) {
          setText(getChild(dep, SYSTEM_PATH), system);
        } else {
          removeChild(dep, findChild(dep, SYSTEM_PATH));
        }
        if(optional) {
          setText(getChild(dep, OPTIONAL), Boolean.toString(optional));
        } else {
          removeChild(dep, findChild(dep, OPTIONAL));
        }
      }
    };
  }

  private String valueOrNull(String value) {
    if(value != null) {
      value = value.trim();
      if(value.length() == 0) {
        value = null;
      }
    }
    return value;
  }

  public void setDependency(Dependency dependency) {
    this.dependency = dependency;

    if(dependency != null && groupIdText != null && !groupIdText.isDisposed()) {
      groupIdText.setText(nvl(dependency.getGroupId()));
      artifactIdText.setText(nvl(dependency.getArtifactId()));
      versionText.setText(nvl(dependency.getVersion()));
      classifierText.setText(nvl(dependency.getClassifier()));
      typeCombo.setText("".equals(nvl(dependency.getType())) ? "jar" : dependency.getType()); //$NON-NLS-1$ //$NON-NLS-2$
      scopeCombo.setText("".equals(nvl(dependency.getScope())) ? "compile" : dependency.getScope()); //$NON-NLS-1$ //$NON-NLS-2$
      systemPathText.setText(nvl(dependency.getSystemPath()));

      boolean optional = Boolean.parseBoolean(dependency.getOptional());
      if(optionalButton.getSelection() != optional) {
        optionalButton.setSelection(optional);
      }
    }
  }

}
