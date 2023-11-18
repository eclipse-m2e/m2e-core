/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CI_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.CONNECTION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DESCRIPTION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.DEV_CONNECTION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.INCEPTION_YEAR;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ISSUE_MANAGEMENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.MODULE;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.MODULES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.NAME;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ORGANIZATION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PACKAGING;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PARENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PROPERTIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.RELATIVE_PATH;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SCM;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.SYSTEM;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.TAG;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.URL;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childAt;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.createElementWithText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChilds;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.format;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.textEquals;
import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ResourceTransfer;

import org.apache.maven.artifact.handler.ArtifactHandler;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.core.ui.internal.wizards.MavenModuleWizard;
import org.eclipse.m2e.core.ui.internal.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.composites.StringLabelProvider;
import org.eclipse.m2e.editor.dialogs.MavenModuleSelectionDialog;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class OverviewPage extends MavenPomEditorPage {

  static final Logger LOG = LoggerFactory.getLogger(OverviewPage.class);

  //define more as the need arises
  private static final int RELOAD_MODULES = 1;

  private static final int RELOAD_BASE = 2;

  private static final int RELOAD_CI = 4;

  private static final int RELOAD_SCM = 8;

  private static final int RELOAD_IM = 16;

  private static final int RELOAD_PROPERTIES = 32;

  private static final int RELOAD_PARENT = 64;

  private static final int RELOAD_ORG = 128;

  private static final int RELOAD_ALL = RELOAD_MODULES + RELOAD_BASE + RELOAD_CI + RELOAD_SCM + RELOAD_IM
      + RELOAD_PROPERTIES + RELOAD_ORG + RELOAD_PARENT;

  //controls
  Text artifactIdText;

  Text artifactVersionText;

  Text artifactGroupIdText;

  CCombo artifactPackagingCombo;

  Text parentVersionText;

  Text parentArtifactIdText;

  Text parentGroupIdText;

  Text parentRelativePathText;

  Text projectUrlText;

  Text projectNameText;

  Text projectDescriptionText;

  Text inceptionYearText;

  Text organizationUrlText;

  Text organizationNameText;

  Text scmUrlText;

  Text scmDevConnectionText;

  Text scmConnectionText;

  Text scmTagText;

  CCombo issueManagementSystemCombo;

  CCombo issueManagementUrlCombo;

  CCombo ciManagementUrlCombo;

  CCombo ciManagementSystemCombo;

  ListEditorComposite<String> modulesEditor;

  PropertiesSection propertiesSection;

  Section modulesSection;

  Section parentSection;

  Section projectSection;

  Section organizationSection;

  Section scmSection;

  Section issueManagementSection;

  Section ciManagementSection;

//  private Action newModuleProjectAction;

  private Action newModuleElementAction;

  private Action parentSelectAction;

  private Action parentOpenAction;

  private StackLayout modulesStack;

  private Composite noModules;

  private Composite modulesSectionComposite;

  protected GridData projectSectionData;

  public OverviewPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.overview", Messages.OverviewPage_title); //$NON-NLS-1$
  }

  @Override
  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.OverviewPage_form);

    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.horizontalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    Composite leftComposite = toolkit.createComposite(body, SWT.NONE);
    leftComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout leftCompositeLayout = new GridLayout();
    leftCompositeLayout.marginWidth = 0;
    leftCompositeLayout.marginHeight = 0;
    leftComposite.setLayout(leftCompositeLayout);

    WidthGroup leftWidthGroup = new WidthGroup();
    leftComposite.addControlListener(leftWidthGroup);

    createArtifactSection(toolkit, leftComposite, leftWidthGroup);
    createParentsection(toolkit, leftComposite, leftWidthGroup);
    createPropertiesSection(toolkit, leftComposite, leftWidthGroup);
    createModulesSection(toolkit, leftComposite, leftWidthGroup);

    Composite rightComposite = toolkit.createComposite(body, SWT.NONE);
    rightComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout rightCompositeLayout = new GridLayout();
    rightCompositeLayout.marginWidth = 0;
    rightCompositeLayout.marginHeight = 0;
    rightComposite.setLayout(rightCompositeLayout);

    WidthGroup rightWidthGroup = new WidthGroup();
    rightComposite.addControlListener(rightWidthGroup);

    createProjectSection(toolkit, rightComposite, rightWidthGroup);
    createOrganizationSection(toolkit, rightComposite, rightWidthGroup);
    createScmSection(toolkit, rightComposite, rightWidthGroup);
    createIssueManagementSection(toolkit, rightComposite, rightWidthGroup);
    createCiManagementSection(toolkit, rightComposite, rightWidthGroup);

    toolkit.paintBordersFor(leftComposite);
    toolkit.paintBordersFor(rightComposite);

    super.createFormContent(managedForm);
  }

  private void createArtifactSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    Section artifactSection = toolkit.createSection(composite, ExpandableComposite.TITLE_BAR);
    artifactSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    artifactSection.setText(Messages.OverviewPage_section_artifact);

    Composite artifactComposite = toolkit.createComposite(artifactSection, SWT.NONE);
    toolkit.adapt(artifactComposite);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    artifactComposite.setLayout(gridLayout);
    artifactSection.setClient(artifactComposite);

    Label groupIdLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblGroupId, SWT.NONE);

    artifactGroupIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactGroupIdText.setData("name", "groupId"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_artifactGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactGroupIdText.horizontalIndent = 4;
    artifactGroupIdText.setLayoutData(gd_artifactGroupIdText);
    ProposalUtil.addGroupIdProposal(getProject(), artifactGroupIdText, Packaging.ALL);
    createEvaluatorInfo(artifactGroupIdText);
    setElementValueProvider(artifactGroupIdText, new ElementValueProvider(PomEdits.GROUP_ID));
    setModifyListener(artifactGroupIdText);

    Label artifactIdLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblArtifactId, SWT.NONE);

    artifactIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactIdText.setData("name", "artifactId"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    M2EUIUtils.addRequiredDecoration(artifactIdText);
    createEvaluatorInfo(artifactIdText);
    setElementValueProvider(artifactIdText, new ElementValueProvider(PomEdits.ARTIFACT_ID));
    setModifyListener(artifactIdText);

    Label versionLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblVersion, SWT.NONE);

    artifactVersionText = toolkit.createText(artifactComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_versionText.horizontalIndent = 4;
    gd_versionText.widthHint = 200;
    artifactVersionText.setLayoutData(gd_versionText);
    artifactVersionText.setData("name", "version"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(artifactVersionText);
    setElementValueProvider(artifactVersionText, new ElementValueProvider(PomEdits.VERSION));
    setModifyListener(artifactVersionText);

    Label packagingLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblPackaging, SWT.NONE);

    artifactPackagingCombo = new CCombo(artifactComposite, SWT.FLAT);
    Set<String> packagingTypes = new LinkedHashSet<>();

    IMavenProjectFacade projectFacade = Adapters.adapt(getProject(), IMavenProjectFacade.class);
    if(projectFacade != null) {
      try {
        List<String> list = projectFacade.createExecutionContext().execute((context, monitor) -> {
          return context.getComponentLookup().lookupCollection(ArtifactHandler.class).stream()
              .map(ArtifactHandler::getPackaging).filter(Objects::nonNull).distinct().sorted()
              .collect(Collectors.toList());
        }, null);
        packagingTypes.addAll(list);
      } catch(CoreException ex) {
      }
    }

    if(packagingTypes.isEmpty()) {
      //something went wrong, add at least some basic items....
      packagingTypes.add("jar"); //$NON-NLS-1$
      packagingTypes.add("war"); //$NON-NLS-1$
      packagingTypes.add("ejb"); //MNGECLIPSE-688 : add EAR & EJB Support //$NON-NLS-1$
      packagingTypes.add("ear"); //$NON-NLS-1$
      packagingTypes.add("pom"); //$NON-NLS-1$
      packagingTypes.add("maven-plugin"); //$NON-NLS-1$
    }
//    updateAvailablePackagingTypes(packagingTypes); // dynamically load available packaging types from build plugins
    packagingTypes.forEach(type -> artifactPackagingCombo.add(type));

// uncomment this only if you are able to not to break the project
//    artifactPackagingCombo.add("osgi-bundle");
//    artifactPackagingCombo.add("eclipse-feature");

    toolkit.adapt(artifactPackagingCombo, true, true);
    GridData gd_packagingText = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    gd_packagingText.horizontalIndent = 4;
    gd_packagingText.widthHint = 120;
    artifactPackagingCombo.setLayoutData(gd_packagingText);
    artifactPackagingCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    artifactPackagingCombo.setData("name", "packaging"); //$NON-NLS-1$ //$NON-NLS-2$
    toolkit.paintBordersFor(artifactPackagingCombo);
    ElementValueProvider provider = new ElementValueProvider(PomEdits.PACKAGING);
    provider.setDefaultValue("jar");
    setElementValueProvider(artifactPackagingCombo, provider);
    setModifyListener(artifactPackagingCombo);

    widthGroup.addControl(groupIdLabel);
    widthGroup.addControl(artifactIdLabel);
    widthGroup.addControl(versionLabel);
    widthGroup.addControl(packagingLabel);

    toolkit.paintBordersFor(artifactComposite);
  }

  private void createParentsection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    parentSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    parentSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    parentSection.setText(Messages.OverviewPage_section_parent);
    parentSection.setData("name", "parentSection"); //$NON-NLS-1$ //$NON-NLS-2$

    parentSelectAction = new Action(Messages.OverviewPage_action_selectParent, MavenEditorImages.SELECT_ARTIFACT) {
      @Override
      public void run() {
        // calculate current list of artifacts for the project - that's the current parent..
        Set<ArtifactKey> current = new HashSet<>();
        String parentGroup = parentGroupIdText.getText();
        String parentArtifact = parentArtifactIdText.getText();
        String parentVersion = parentVersionText.getText();
        if(parentGroup != null && parentArtifact != null && parentVersion != null) {
          current.add(new ArtifactKey(parentGroup, parentArtifact, parentVersion, null));
        }
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchParentDialog(
            getEditorSite().getShell(), Messages.OverviewPage_searchDialog_selectParent,
            getPomEditor().getMavenProject(), getProject());
        if(parentGroup != null && parentGroup.trim().length() != 0) {
          //chances are we will get good match by adding the groupid here..
          dialog.setQuery(parentGroupIdText.getText());
        } else if(artifactGroupIdText.getText() != null && artifactGroupIdText.getText().trim().length() != 0) {
          //chances are we will get good match by adding the groupid here..
          dialog.setQuery(artifactGroupIdText.getText());
        }
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            String grid = nvl(af.group);
            String ver = nvl(af.version);
            parentGroupIdText.setText(grid);
            parentArtifactIdText.setText(nvl(af.artifact));
            parentVersionText.setText(ver);

            //promote good practices ->
            if(grid.equals(artifactGroupIdText.getText())) {
              //if the groupId is the same, just remove it in child.
              artifactGroupIdText.setText(""); //$NON-NLS-1$
            }
            if(ver.equals(artifactVersionText.getText())) {
              //if the version is the same, just remove it in child.
              artifactVersionText.setText(""); //$NON-NLS-1$
            }
            parentSection.setExpanded(true);
          }
        }
      }
    };
    parentSelectAction.setEnabled(false);

    parentOpenAction = new Action(Messages.OverviewPage_job_open, MavenEditorImages.PARENT_POM) {
      @Override
      public void run() {
        final String groupId = parentGroupIdText.getText();
        final String artifactId = parentArtifactIdText.getText();
        final String version = parentVersionText.getText();
        new Job(NLS.bind(Messages.OverviewPage_job, new Object[] {groupId, artifactId, version})) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            OpenPomAction.openEditor(groupId, artifactId, version, getPomEditor().getMavenProject(), monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    };
    parentOpenAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(parentOpenAction);
    toolBarManager.add(parentSelectAction);

    Composite toolbarComposite = toolkit.createComposite(parentSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);

    toolBarManager.createControl(toolbarComposite);
    parentSection.setTextClient(toolbarComposite);

    Composite parentComposite = toolkit.createComposite(parentSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginBottom = 5;
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 2;
    parentComposite.setLayout(gridLayout);
    parentSection.setClient(parentComposite);

    Label parentGroupIdLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblGroupId2, SWT.NONE);

    parentGroupIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentGroupIdText.horizontalIndent = 4;
    parentGroupIdText.setLayoutData(gd_parentGroupIdText);
    parentGroupIdText.setData("name", "parentGroupId"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addGroupIdProposal(getProject(), parentGroupIdText, Packaging.POM);
    M2EUIUtils.addRequiredDecoration(parentGroupIdText);
    createEvaluatorInfo(parentGroupIdText);
    setElementValueProvider(parentGroupIdText, new ElementValueProvider(PomEdits.PARENT, PomEdits.GROUP_ID));
    setModifyListener(parentGroupIdText);

    final Label parentArtifactIdLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblArtifactId,
        SWT.NONE);

    parentArtifactIdText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentArtifactIdText.horizontalIndent = 4;
    parentArtifactIdText.setLayoutData(gd_parentArtifactIdText);
    parentArtifactIdText.setData("name", "parentArtifactId"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addArtifactIdProposal(getProject(), parentGroupIdText, parentArtifactIdText, Packaging.POM);
    M2EUIUtils.addRequiredDecoration(parentArtifactIdText);
    createEvaluatorInfo(parentArtifactIdText);
    setElementValueProvider(parentArtifactIdText, new ElementValueProvider(PomEdits.PARENT, PomEdits.ARTIFACT_ID));
    setModifyListener(parentArtifactIdText);

    Label parentVersionLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblVersion2, SWT.NONE);
    parentVersionLabel.setLayoutData(new GridData());

    parentVersionText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData parentVersionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    parentVersionTextData.horizontalIndent = 4;
    parentVersionTextData.widthHint = 200;
    parentVersionText.setLayoutData(parentVersionTextData);
    parentVersionText.setData("name", "parentVersion"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addVersionProposal(getProject(), null/** null because we don't want expressions from parent pom here */
        , parentGroupIdText, parentArtifactIdText, parentVersionText, Packaging.POM);
    M2EUIUtils.addRequiredDecoration(parentVersionText);
    createEvaluatorInfo(parentVersionText);
    setElementValueProvider(parentVersionText, new ElementValueProvider(PomEdits.PARENT, PomEdits.VERSION));
    setModifyListener(parentVersionText);

    ModifyListener ml = e -> {
      //apparently the loadParent() method also participates in the enablement logic from time to time..
      String text1 = parentArtifactIdText.getText().trim();
      String text2 = parentGroupIdText.getText().trim();
      String text3 = parentVersionText.getText().trim();
      if(text1.length() > 0 && text2.length() > 0 && text3.length() > 0) {
        parentOpenAction.setEnabled(true);
      } else {
        parentOpenAction.setEnabled(false);
      }
    };
    parentArtifactIdText.addModifyListener(ml);
    parentVersionText.addModifyListener(ml);
    parentGroupIdText.addModifyListener(ml);

    Label parentRealtivePathLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblRelPath, SWT.NONE);

    parentRelativePathText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentRelativePathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentRelativePathText.horizontalIndent = 4;
    parentRelativePathText.setLayoutData(gd_parentRelativePathText);
    parentRelativePathText.setData("name", "parentRelativePath"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(parentRelativePathText);
    setElementValueProvider(parentRelativePathText, new ElementValueProvider(PomEdits.PARENT, PomEdits.RELATIVE_PATH));
    setModifyListener(parentRelativePathText);

    widthGroup.addControl(parentGroupIdLabel);
    widthGroup.addControl(parentArtifactIdLabel);
    widthGroup.addControl(parentVersionLabel);
    widthGroup.addControl(parentRealtivePathLabel);

    toolkit.paintBordersFor(parentComposite);
    parentComposite
        .setTabList(new Control[] {parentGroupIdText, parentArtifactIdText, parentVersionText, parentRelativePathText});
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    propertiesSection = new PropertiesSection(toolkit, composite, this);
  }

  private void createModulesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    // XXX should disable Modules actions based on artifact packaging and only add modules when packaging is "pom"

    modulesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);

    GridData moduleSectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
    modulesSection.setLayoutData(moduleSectionData);
    modulesSection.setText(Messages.OverviewPage_section_modules);
    modulesSection.setData("name", "modulesSection"); //$NON-NLS-1$ //$NON-NLS-2$
    modulesSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        moduleSectionData.grabExcessVerticalSpace = e.getState();
        modulesSection.getParent().layout();
      }
    });

    modulesSectionComposite = toolkit.createComposite(modulesSection);
    modulesStack = new StackLayout();
    modulesSectionComposite.setLayout(modulesStack);
    modulesSection.setClient(modulesSectionComposite);

    noModules = toolkit.createComposite(modulesSectionComposite);
    noModules.setLayout(new GridLayout(1, false));

    Label label = toolkit.createLabel(noModules, Messages.OverviewPage_msg_not_pom_packaging);
    GridData gd_label = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
    gd_label.verticalIndent = 12;
    gd_label.horizontalIndent = 12;
    label.setLayoutData(gd_label);

    modulesEditor = new ListEditorComposite<>(modulesSectionComposite, SWT.NONE, true);
    modulesEditor.getViewer().getTable().setData("name", "modulesEditor"); //$NON-NLS-1$ //$NON-NLS-2$
    toolkit.paintBordersFor(modulesEditor);
    toolkit.adapt(modulesEditor);

    modulesEditor.setContentProvider(new ListEditorContentProvider<>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));

    modulesEditor.setOpenListener(openevent -> {
      final List<String> selection = modulesEditor.getSelection();
      new Job(Messages.OverviewPage_opening_editors) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          for(String module : selection) {
            IMavenProjectFacade projectFacade = findModuleProject(module);
            if(projectFacade != null) {
              ArtifactKey key = projectFacade.getArtifactKey();
              OpenPomAction.openEditor(key.groupId(), key.artifactId(), key.version(), getPomEditor().getMavenProject(),
                  monitor);
            } else {
              IFile modulePom = findModuleFile(module);
              if(modulePom != null && modulePom.isAccessible()) {
                OpenPomAction.openEditor(new FileEditorInput(modulePom), "pom.xml"); //$NON-NLS-1$
              }
            }
          }
          return Status.OK_STATUS;
        }
      }.schedule();
    });

    modulesEditor.setAddButtonListener(SelectionListener.widgetSelectedAdapter(e -> {
      final Set<Object> moduleContainers = new HashSet<>();
      final List<String> modules = new ArrayList<>();
      try {
        performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), document -> {
          Element modsEl = findChild(document.getDocumentElement(), MODULES);
          for(Element el : findChilds(modsEl, MODULE)) {
            String m = getTextValue(el);
            if(m != null) {
              modules.add(m);
            }
          }
        }, true));
      } catch(Exception e1) {
        LOG.error("Cannot load modules", e1);
      }

      for(String module : modules) {
        IMavenProjectFacade facade = findModuleProject(module);
        if(facade != null) {
          moduleContainers.add(facade.getProject().getLocation());
        }
        IFile file = findModuleFile(module);
        if(file != null) {
          moduleContainers.add(file.getParent().getLocation());
        }
      }
      moduleContainers.add(getProject().getLocation());

      MavenModuleSelectionDialog dialog = new MavenModuleSelectionDialog(getSite().getShell(), moduleContainers);

      if(dialog.open() == Window.OK) {
        addSelectedModules(dialog.getResult(), dialog.isPomUpdateRequired());
      }
    }));

    modulesEditor.setCreateButtonListener(SelectionListener.widgetSelectedAdapter(e -> {
      IEditorInput editorInput = OverviewPage.this.pomEditor.getEditorInput();
      if(editorInput instanceof FileEditorInput fileInput) {
        MavenModuleWizard wizard = new MavenModuleWizard(true);
        wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(fileInput.getFile()));
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
        int res = dialog.open();
        if(res == Window.OK) {
          createNewModule(wizard.getModuleName());
        }
      }
    }));

    modulesEditor.setRemoveButtonListener(SelectionListener.widgetSelectedAdapter(e -> {
      try {
        performEditOperation(document -> {
          Element root = document.getDocumentElement();
          Element modules = findChild(root, MODULES);
          if(modules != null) {
            for(String module : modulesEditor.getSelection()) {
              Element modEl = findChild(modules, MODULE, textEquals(module));
              if(modEl != null) {
                modules.removeChild(modEl);
              }
            }
            //now remove the <modules> element itself when there are no more elements left
            removeIfNoChildElement(modules);
          }
        }, LOG, "error removing module entry");
      } finally {
        loadThis(RELOAD_MODULES);
      }
    }));

    modulesEditor.setCellModifier(new ICellModifier() {
      @Override
      public boolean canModify(Object element, String property) {
        return true;
      }

      @Override
      public Object getValue(Object element, String property) {
        return element;
      }

      @Override
      public void modify(Object element, String property, final Object value) {
        final int n = modulesEditor.getViewer().getTable().getSelectionIndex();
        try {
          performEditOperation(document -> {
            Element root = document.getDocumentElement();
            Element module = findChild(findChild(root, MODULES), MODULE, childAt(n));
            if(module != null && !value.equals(getTextValue(module))) {
              setText(module, value.toString());
            }
          }, LOG, "error changing module entry");
        } finally {
          loadThis(RELOAD_MODULES);
        }
      }
    });

    modulesEditor.getViewer().addDropSupport(DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE,
        new Transfer[] {ResourceTransfer.getInstance()}, new DropTargetAdapter() {
          @Override
          public void dragEnter(DropTargetEvent event) {
            event.detail = DND.DROP_LINK;
          }

          @Override
          public void dragOperationChanged(DropTargetEvent event) {
            event.detail = DND.DROP_LINK;
          }

          @Override
          public void drop(DropTargetEvent event) {
            if(event.data instanceof Object[] array) {
              addSelectedModules(array, true);
            }
          }
        });

    newModuleElementAction = new Action(Messages.OverviewPage_action_newModuleElement, MavenImages.NEW_POM) {
      @Override
      public void run() {
        createNewModule("?"); //$NON-NLS-1$
      }
    };

//    newModuleProjectAction = new Action(Messages.OverviewPage_action_new_module_project, MavenEditorImages.ADD_MODULE) {
//      public void run() {
//        IEditorInput editorInput = OverviewPage.this.pomEditor.getEditorInput();
//        if(editorInput instanceof FileEditorInput) {
//          MavenModuleWizard wizard = new MavenModuleWizard(true);
//          wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(((FileEditorInput) editorInput).getFile()));
//          WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
//          int res = dialog.open();
//          if(res == Window.OK) {
//            createNewModule(wizard.getModuleName());
//          }
//        }
//      }
//    };

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
    modulesToolBarManager.add(newModuleElementAction);
//    modulesToolBarManager.add(newModuleProjectAction);

    Composite toolbarComposite = toolkit.createComposite(modulesSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);

    modulesToolBarManager.createControl(toolbarComposite);
    modulesSection.setTextClient(toolbarComposite);

    modulesEditor.setReadOnly(pomEditor.isReadOnly());
    newModuleElementAction.setEnabled(!pomEditor.isReadOnly());
//    newModuleProjectAction.setEnabled(!pomEditor.isReadOnly());
  }

  // right side

  private void createProjectSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    projectSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    projectSectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
    projectSection.setLayoutData(projectSectionData);
    projectSection.setText(Messages.OverviewPage_section_project);
    projectSection.setData("name", "projectSection"); //$NON-NLS-1$ //$NON-NLS-2$
    projectSection.addExpansionListener(new ExpansionAdapter() {
      @Override
      public void expansionStateChanged(ExpansionEvent e) {
        projectSectionData.grabExcessVerticalSpace = e.getState();
        projectSection.getParent().layout();
      }
    });

    Composite projectComposite = toolkit.createComposite(projectSection, SWT.NONE);
    projectComposite.setLayout(new GridLayout(2, false));
    projectSection.setClient(projectComposite);

    Label nameLabel = toolkit.createLabel(projectComposite, Messages.OverviewPage_lblName, SWT.NONE);

    projectNameText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_projectNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectNameText.widthHint = 150;
    projectNameText.setLayoutData(gd_projectNameText);
    projectNameText.setData("name", "projectName"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(projectNameText);
    setElementValueProvider(projectNameText, new ElementValueProvider(PomEdits.NAME));
    setModifyListener(projectNameText);

    Hyperlink urlLabel = toolkit.createHyperlink(projectComposite, Messages.OverviewPage_lblUrl, SWT.NONE);
    urlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectUrlText.getText());
      }
    });

    projectUrlText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_projectUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectUrlText.widthHint = 150;
    projectUrlText.setLayoutData(gd_projectUrlText);
    projectUrlText.setData("name", "projectUrl"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(projectUrlText);
    setElementValueProvider(projectUrlText, new ElementValueProvider(PomEdits.URL));
    setModifyListener(projectUrlText);

    Label descriptionLabel = toolkit.createLabel(projectComposite, Messages.OverviewPage_lblDesc, SWT.NONE);
    descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

    projectDescriptionText = toolkit.createText(projectComposite, null, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
    GridData gd_descriptionText = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd_descriptionText.widthHint = 150;
    gd_descriptionText.heightHint = 100;
    gd_descriptionText.minimumHeight = 100;
    projectDescriptionText.setLayoutData(gd_descriptionText);
    projectDescriptionText.setData("name", "projectDescription"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(projectDescriptionText);
    setElementValueProvider(projectDescriptionText, new ElementValueProvider(PomEdits.DESCRIPTION));
    setModifyListener(projectDescriptionText);

    Label inceptionYearLabel = toolkit.createLabel(projectComposite, Messages.OverviewPage_lblInception, SWT.NONE);

    inceptionYearText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_inceptionYearText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_inceptionYearText.widthHint = 150;
    inceptionYearText.setLayoutData(gd_inceptionYearText);
    inceptionYearText.setData("name", "projectInceptionYear"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(inceptionYearText);
    setElementValueProvider(inceptionYearText, new ElementValueProvider(PomEdits.INCEPTION_YEAR));
    setModifyListener(inceptionYearText);

    widthGroup.addControl(nameLabel);
    widthGroup.addControl(urlLabel);
    widthGroup.addControl(descriptionLabel);
    widthGroup.addControl(inceptionYearLabel);

    toolkit.paintBordersFor(projectComposite);
    projectComposite
        .setTabList(new Control[] {projectNameText, projectUrlText, projectDescriptionText, inceptionYearText});
  }

  private void createOrganizationSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    organizationSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    organizationSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    organizationSection.setText(Messages.OverviewPage_section_org);
    organizationSection.setData("name", "organizationSection"); //$NON-NLS-1$ //$NON-NLS-2$

    Composite organizationComposite = toolkit.createComposite(organizationSection, SWT.NONE);
    organizationComposite.setLayout(new GridLayout(2, false));
    organizationSection.setClient(organizationComposite);

    Label organizationNameLabel = toolkit.createLabel(organizationComposite, Messages.OverviewPage_lblName, SWT.NONE);

    organizationNameText = toolkit.createText(organizationComposite, null, SWT.NONE);
    GridData gd_organizationNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_organizationNameText.widthHint = 150;
    organizationNameText.setLayoutData(gd_organizationNameText);
    organizationNameText.setData("name", "organizationName"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(organizationNameText);
    setElementValueProvider(organizationNameText, new ElementValueProvider(PomEdits.ORGANIZATION, PomEdits.NAME));
    setModifyListener(organizationNameText);

    Hyperlink organizationUrlLabel = toolkit.createHyperlink(organizationComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    organizationUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(organizationUrlText.getText());
      }
    });

    organizationUrlText = toolkit.createText(organizationComposite, null, SWT.NONE);
    GridData gd_organizationUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_organizationUrlText.widthHint = 150;
    organizationUrlText.setLayoutData(gd_organizationUrlText);
    organizationUrlText.setData("name", "organizationUrl"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(organizationUrlText);
    setElementValueProvider(organizationUrlText, new ElementValueProvider(PomEdits.ORGANIZATION, PomEdits.URL));
    setModifyListener(organizationUrlText);

    widthGroup.addControl(organizationNameLabel);
    widthGroup.addControl(organizationUrlLabel);

    toolkit.paintBordersFor(organizationComposite);
    organizationComposite.setTabList(new Control[] {organizationNameText, organizationUrlText});
  }

  private void createScmSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    scmSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    GridData gd_scmSection = new GridData(SWT.FILL, SWT.TOP, false, false);
    scmSection.setLayoutData(gd_scmSection);
    scmSection.setText(Messages.OverviewPage_section_scm);
    scmSection.setData("name", "scmSection"); //$NON-NLS-1$ //$NON-NLS-2$

    Composite scmComposite = toolkit.createComposite(scmSection, SWT.NONE);
    scmComposite.setLayout(new GridLayout(2, false));
    scmSection.setClient(scmComposite);

    Hyperlink scmUrlLabel = toolkit.createHyperlink(scmComposite, Messages.OverviewPage_lblUrl, SWT.NONE);
    scmUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(scmUrlText.getText());
      }
    });

    scmUrlText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmUrlText.widthHint = 150;
    scmUrlText.setLayoutData(gd_scmUrlText);
    scmUrlText.setData("name", "scmUrl"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmUrlText);
    setElementValueProvider(scmUrlText, new ElementValueProvider(PomEdits.SCM, PomEdits.URL));
    setModifyListener(scmUrlText);

    Label scmConnectionLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblConnection, SWT.NONE);

    scmConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmConnectionText.widthHint = 150;
    scmConnectionText.setLayoutData(gd_scmConnectionText);
    scmConnectionText.setData("name", "scmConnection"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmConnectionText);
    setElementValueProvider(scmConnectionText, new ElementValueProvider(PomEdits.SCM, PomEdits.CONNECTION));
    setModifyListener(scmConnectionText);

    Label scmDevConnectionLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblDev, SWT.NONE);

    scmDevConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmDevConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmDevConnectionText.widthHint = 150;
    scmDevConnectionText.setLayoutData(gd_scmDevConnectionText);
    scmDevConnectionText.setData("name", "scmDevConnection"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmDevConnectionText);
    setElementValueProvider(scmDevConnectionText, new ElementValueProvider(PomEdits.SCM, PomEdits.DEV_CONNECTION));
    setModifyListener(scmDevConnectionText);

    Label scmTagLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblTag, SWT.NONE);

    scmTagText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmTagText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmTagText.widthHint = 150;
    scmTagText.setLayoutData(gd_scmTagText);
    scmTagText.setData("name", "scmTag"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmTagText);
    setElementValueProvider(scmTagText, new ElementValueProvider(PomEdits.SCM, PomEdits.TAG));
    setModifyListener(scmTagText);

    widthGroup.addControl(scmUrlLabel);
    widthGroup.addControl(scmConnectionLabel);
    widthGroup.addControl(scmDevConnectionLabel);
    widthGroup.addControl(scmTagLabel);

    toolkit.paintBordersFor(scmComposite);
  }

  private void createIssueManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    issueManagementSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    issueManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    issueManagementSection.setText(Messages.OverviewPage_section_issueMan);
    issueManagementSection.setData("name", "issueManagementSection"); //$NON-NLS-1$ //$NON-NLS-2$

    Composite issueManagementComposite = toolkit.createComposite(issueManagementSection, SWT.NONE);
    issueManagementComposite.setLayout(new GridLayout(2, false));
    issueManagementSection.setClient(issueManagementComposite);

    Label issueManagementSystemLabel = toolkit.createLabel(issueManagementComposite, Messages.OverviewPage_lblSystem,
        SWT.NONE);

    issueManagementSystemCombo = new CCombo(issueManagementComposite, SWT.FLAT);
    GridData gd_issueManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_issueManagementSystemText.widthHint = 150;
    issueManagementSystemCombo.setLayoutData(gd_issueManagementSystemText);
    issueManagementSystemCombo.setData("name", "issueManagementSystem"); //$NON-NLS-1$ //$NON-NLS-2$
    issueManagementSystemCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    addToHistory(issueManagementSystemCombo);
    toolkit.paintBordersFor(issueManagementSystemCombo);
    toolkit.adapt(issueManagementSystemCombo, true, true);
    createEvaluatorInfo(issueManagementSystemCombo);
    setElementValueProvider(issueManagementSystemCombo,
        new ElementValueProvider(PomEdits.ISSUE_MANAGEMENT, PomEdits.SYSTEM));
    setModifyListener(issueManagementSystemCombo);

    Hyperlink issueManagementUrlLabel = toolkit.createHyperlink(issueManagementComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    issueManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(issueManagementUrlCombo.getText());
      }
    });

    issueManagementUrlCombo = new CCombo(issueManagementComposite, SWT.FLAT);
    GridData gd_issueManagementUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_issueManagementUrlText.widthHint = 150;
    issueManagementUrlCombo.setLayoutData(gd_issueManagementUrlText);
    issueManagementUrlCombo.setData("name", "issueManagementUrl"); //$NON-NLS-1$ //$NON-NLS-2$
    issueManagementUrlCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    addToHistory(issueManagementUrlCombo);
    toolkit.paintBordersFor(issueManagementUrlCombo);
    toolkit.adapt(issueManagementUrlCombo, true, true);
    createEvaluatorInfo(issueManagementUrlCombo);
    setElementValueProvider(issueManagementUrlCombo, new ElementValueProvider(PomEdits.ISSUE_MANAGEMENT, PomEdits.URL));
    setModifyListener(issueManagementUrlCombo);

    widthGroup.addControl(issueManagementSystemLabel);
    widthGroup.addControl(issueManagementUrlLabel);

    toolkit.paintBordersFor(issueManagementComposite);
    issueManagementComposite.setTabList(new Control[] {issueManagementSystemCombo, issueManagementUrlCombo});
  }

  private void createCiManagementSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    ciManagementSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    ciManagementSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    ciManagementSection.setText(Messages.OverviewPage_section_ci);
    ciManagementSection.setData("name", "continuousIntegrationSection"); //$NON-NLS-1$ //$NON-NLS-2$

    Composite ciManagementComposite = toolkit.createComposite(ciManagementSection, SWT.NONE);
    ciManagementComposite.setLayout(new GridLayout(2, false));
    ciManagementSection.setClient(ciManagementComposite);

    Label ciManagementSystemLabel = toolkit.createLabel(ciManagementComposite, Messages.OverviewPage_lblSystem,
        SWT.NONE);

    ciManagementSystemCombo = new CCombo(ciManagementComposite, SWT.FLAT);
    GridData gd_ciManagementSystemText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_ciManagementSystemText.widthHint = 150;
    ciManagementSystemCombo.setLayoutData(gd_ciManagementSystemText);
    ciManagementSystemCombo.setData("name", "ciManagementSystem"); //$NON-NLS-1$ //$NON-NLS-2$
    ciManagementSystemCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    addToHistory(ciManagementSystemCombo);
    toolkit.paintBordersFor(ciManagementSystemCombo);
    toolkit.adapt(ciManagementSystemCombo, true, true);
    createEvaluatorInfo(ciManagementSystemCombo);
    setElementValueProvider(ciManagementSystemCombo, new ElementValueProvider(PomEdits.CI_MANAGEMENT, PomEdits.SYSTEM));
    setModifyListener(ciManagementSystemCombo);

    Hyperlink ciManagementUrlLabel = toolkit.createHyperlink(ciManagementComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    ciManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(ciManagementUrlCombo.getText());
      }
    });

    ciManagementUrlCombo = new CCombo(ciManagementComposite, SWT.FLAT);
    GridData gd_ciManagementUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_ciManagementUrlText.widthHint = 150;
    ciManagementUrlCombo.setLayoutData(gd_ciManagementUrlText);
    ciManagementUrlCombo.setData("name", "ciManagementUrl"); //$NON-NLS-1$ //$NON-NLS-2$
    ciManagementUrlCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    addToHistory(ciManagementUrlCombo);
    toolkit.paintBordersFor(ciManagementUrlCombo);
    toolkit.adapt(ciManagementUrlCombo, true, true);
    createEvaluatorInfo(ciManagementUrlCombo);
    setElementValueProvider(ciManagementUrlCombo, new ElementValueProvider(PomEdits.CI_MANAGEMENT, PomEdits.URL));
    setModifyListener(ciManagementUrlCombo);

    widthGroup.addControl(ciManagementSystemLabel);
    widthGroup.addControl(ciManagementUrlLabel);

    toolkit.paintBordersFor(ciManagementComposite);
    ciManagementComposite.setTabList(new Control[] {ciManagementSystemCombo, ciManagementUrlCombo});
  }

  public void updateView(final Notification notification) {
    //noop now
  }

  @Override
  public void loadData() {
    loadThis(RELOAD_ALL);

    // Modules modules = model.getModules();
    // modulesSection.setExpanded(modules !=null && modules.getModule().size()>0);
  }

  private void loadThis(final int mask) {
    Display.getDefault().asyncExec(() -> {

      removeNotifyListener(parentGroupIdText);
      removeNotifyListener(parentArtifactIdText);
      removeNotifyListener(parentVersionText);
      removeNotifyListener(parentRelativePathText);

      removeNotifyListener(artifactGroupIdText);
      removeNotifyListener(artifactIdText);
      removeNotifyListener(artifactVersionText);
      removeNotifyListener(artifactPackagingCombo);
      removeNotifyListener(projectNameText);
      removeNotifyListener(projectDescriptionText);
      removeNotifyListener(projectUrlText);
      removeNotifyListener(inceptionYearText);

      removeNotifyListener(organizationNameText);
      removeNotifyListener(organizationUrlText);

      removeNotifyListener(scmUrlText);
      removeNotifyListener(scmConnectionText);
      removeNotifyListener(scmDevConnectionText);
      removeNotifyListener(scmTagText);

      removeNotifyListener(ciManagementUrlCombo);
      removeNotifyListener(ciManagementSystemCombo);

      removeNotifyListener(issueManagementUrlCombo);
      removeNotifyListener(issueManagementSystemCombo);

      try {
        performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), document -> {
          Element root = document.getDocumentElement();
          String pack = nvl(getTextValue(findChild(root, PACKAGING)));
          pack = "".equals(pack) ? "jar" : pack; //$NON-NLS-1$ //$NON-NLS-2$
          if((mask & RELOAD_BASE) != 0) {
            setText(artifactGroupIdText, nvl(getTextValue(findChild(root, GROUP_ID))));
            setText(artifactIdText, nvl(getTextValue(findChild(root, ARTIFACT_ID))));
            setText(artifactVersionText, nvl(getTextValue(findChild(root, VERSION))));
            setText(artifactPackagingCombo, pack);

            String name = getTextValue(findChild(root, NAME));
            setText(projectNameText, nvl(name));
            String desc = getTextValue(findChild(root, DESCRIPTION));
            setText(projectDescriptionText, nvl(desc));
            String url = getTextValue(findChild(root, URL));
            setText(projectUrlText, nvl(url));
            String incep = getTextValue(findChild(root, INCEPTION_YEAR));
            setText(inceptionYearText, nvl(incep));
            boolean expandProjectSection = name != null || desc != null || url != null || incep != null;
            projectSectionData.grabExcessVerticalSpace = expandProjectSection;
            projectSection.setExpanded(expandProjectSection);
          }
          if((mask & RELOAD_PARENT) != 0) {
            //parent section
            Element parent = findChild(root, PARENT);
            String parentGrId = getTextValue(findChild(parent, GROUP_ID));
            String parentArtId = getTextValue(findChild(parent, ARTIFACT_ID));
            String parentVers = getTextValue(findChild(parent, VERSION));
            setText(parentGroupIdText, nvl(parentGrId));
            setText(parentArtifactIdText, nvl(parentArtId));
            setText(parentVersionText, nvl(parentVers));
            setText(parentRelativePathText, nvl(getTextValue(findChild(parent, RELATIVE_PATH))));

            parentSelectAction.setEnabled(!isReadOnly());
            // only enable when all 3 coordinates are actually present.
            parentOpenAction
                .setEnabled(root != null && parentGrId != null && parentArtId != null && parentVers != null);
            parentSection.setExpanded(parent != null);
          }
          if((mask & RELOAD_ORG) != 0) {
            //organization section
            Element org = findChild(root, ORGANIZATION);
            setText(organizationNameText, nvl(getTextValue(findChild(org, NAME))));
            setText(organizationUrlText, nvl(getTextValue(findChild(org, URL))));
            organizationSection.setExpanded(org != null);
          }
          if((mask & RELOAD_SCM) != 0) {
            //scm section
            Element scm = findChild(root, SCM);
            setText(scmUrlText, nvl(getTextValue(findChild(scm, URL))));
            setText(scmConnectionText, nvl(getTextValue(findChild(scm, CONNECTION))));
            setText(scmDevConnectionText, nvl(getTextValue(findChild(scm, DEV_CONNECTION))));
            setText(scmTagText, nvl(getTextValue(findChild(scm, TAG))));
            scmSection.setExpanded(scm != null);
          }
          if((mask & RELOAD_CI) != 0) {
            //ci section
            Element ci = findChild(root, CI_MANAGEMENT);
            setText(ciManagementSystemCombo, nvl(getTextValue(findChild(ci, SYSTEM))));
            setText(ciManagementUrlCombo, nvl(getTextValue(findChild(ci, URL))));
            ciManagementSection.setExpanded(ci != null);
          }
          if((mask & RELOAD_IM) != 0) {
            // issue management
            Element im = findChild(root, ISSUE_MANAGEMENT);
            setText(issueManagementSystemCombo, nvl(getTextValue(findChild(im, SYSTEM))));
            setText(issueManagementUrlCombo, nvl(getTextValue(findChild(im, URL))));
            issueManagementSection.setExpanded(im != null);
          }

          if((mask & RELOAD_MODULES) != 0) {
            //modules section..
            List<Element> moduleEls = findChilds(findChild(root, MODULES), MODULE);
            List<String> modules = new ArrayList<>();
            for(Element moduleEl : moduleEls) {
              String text = getTextValue(moduleEl);
              if(text != null) {
                modules.add(text);
              }
            }
            loadModules(modules, pack);
            //#335337 no editing of packaging when there are modules, results in error anyway
            artifactPackagingCombo.setEnabled(modules.isEmpty());
          }

          if((mask & RELOAD_PROPERTIES) != 0) {
            propertiesSection.refresh();
            Element props = findChild(root, PROPERTIES);
            propertiesSection.setExpanded(props != null);
            //TODO used to check teh model's empty state as well, not done now..
          }
        }, true));
      } catch(Exception e) {
        LOG.error("Failed to populate overview panel", e);
      }

      addNotifyListener(artifactGroupIdText);
      addNotifyListener(artifactIdText);
      addNotifyListener(artifactVersionText);
      addNotifyListener(artifactPackagingCombo);
      addNotifyListener(projectNameText);
      addNotifyListener(projectDescriptionText);
      addNotifyListener(projectUrlText);
      addNotifyListener(inceptionYearText);

      addNotifyListener(parentGroupIdText);
      addNotifyListener(parentArtifactIdText);
      addNotifyListener(parentVersionText);
      addNotifyListener(parentRelativePathText);

      addNotifyListener(organizationNameText);
      addNotifyListener(organizationUrlText);

      addNotifyListener(scmUrlText);
      addNotifyListener(scmConnectionText);
      addNotifyListener(scmDevConnectionText);
      addNotifyListener(scmTagText);

      addNotifyListener(ciManagementUrlCombo);
      addNotifyListener(ciManagementSystemCombo);

      addNotifyListener(issueManagementUrlCombo);
      addNotifyListener(issueManagementSystemCombo);
    });

  }

  private void loadModules(List<String> modules, String packaging) {
    modulesEditor.setInput(modules);
    modulesEditor.setReadOnly(isReadOnly());
    if("pom".equals(packaging) && modulesStack.topControl != modulesEditor) { //$NON-NLS-1$
      modulesStack.topControl = modulesEditor;
      modulesSection.setExpanded(true);
//      newModuleProjectAction.setEnabled(!isReadOnly());
      newModuleElementAction.setEnabled(!isReadOnly());
    } else if(!"pom".equals(packaging) && modulesStack.topControl != noModules) { //$NON-NLS-1$
//      newModuleProjectAction.setEnabled(false);
      newModuleElementAction.setEnabled(false);
      modulesStack.topControl = noModules;
      modulesSection.setExpanded(false);
    }
    modulesSectionComposite.layout();
  }

  private void createNewModule(final String moduleName) {
    try {
      performEditOperation(document -> {
        Element root = document.getDocumentElement();
        Element modules = getChild(root, MODULES);
        if(findChild(modules, MODULE, textEquals(moduleName)) == null) {
          format(createElementWithText(modules, MODULE, moduleName));
        }
      }, LOG, "error updating modules list for pom file");
    } finally {
      loadThis(RELOAD_MODULES);
    }
  }

  private void addSelectedModules(Object[] result, boolean updateParentSection) {
    final String[] vals = new String[3];
    try {
      performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), document -> {
        Element root = document.getDocumentElement();
        String grid = getTextValue(findChild(root, GROUP_ID));
        Element parent = findChild(root, PARENT);
        if(grid == null) {
          grid = getTextValue(findChild(parent, GROUP_ID));
        }
        String artifactId = getTextValue(findChild(root, ARTIFACT_ID));
        String version = getTextValue(findChild(root, VERSION));
        if(version == null) {
          version = getTextValue(findChild(parent, VERSION));
        }
        vals[0] = grid;
        vals[1] = artifactId;
        vals[2] = version;
      }, true));
    } catch(Exception ex) {
      LOG.error("Error getting values from document", ex);
    }

    final String parentGroupId = vals[0];
    final String parentArtifactId = vals[1];
    final String parentVersion = vals[2];
    final IPath projectPath = getProject().getLocation();

    for(Object selection : result) {
      IContainer container = null;
      IFile pomFile = null;

      if(selection instanceof IFile file) {
        pomFile = file;
        if(!IMavenConstants.POM_FILE_NAME.equals(pomFile.getName())) {
          continue;
        }
        container = pomFile.getParent();
      } else if(selection instanceof IContainer c && !selection.equals(getProject())) {
        container = c;
        pomFile = container.getFile(IPath.fromOSString(IMavenConstants.POM_FILE_NAME));
      }

      if(pomFile == null || !pomFile.exists() || container == null) {
        continue;
      }
      IPath resultPath = container.getLocation();
      String path = resultPath.makeRelativeTo(projectPath).toString();

      if(updateParentSection) {
        final String relativePath = projectPath.makeRelativeTo(resultPath).toString();
        try {
          performOnDOMDocument(new OperationTuple(pomFile, (Operation) document -> {
            Element root = document.getDocumentElement();
            Element parent = getChild(root, PARENT);
            setText(getChild(parent, GROUP_ID), parentGroupId);
            setText(getChild(parent, ARTIFACT_ID), parentArtifactId);
            setText(getChild(parent, VERSION), parentVersion);
            setText(getChild(parent, RELATIVE_PATH), relativePath);
            Element grId = findChild(root, GROUP_ID);
            String grIdText = getTextValue(grId);
            if(grIdText != null && grIdText.equals(parentGroupId)) {
              removeChild(root, grId);
            }
            Element ver = findChild(root, VERSION);
            String verText = getTextValue(ver);
            if(verText != null && verText.equals(parentVersion)) {
              removeChild(root, ver);
            }
          }));
        } catch(Exception e) {
          LOG.error("Error updating parent reference in file:" + pomFile, e);
        }
      }

      createNewModule(path);
    }
  }

  public class ModulesLabelProvider extends StringLabelProvider {

    private final MavenPomEditorPage editorPage;

    public ModulesLabelProvider(MavenPomEditorPage editorPage) {
      super(MavenEditorImages.IMG_JAR);
      this.editorPage = editorPage;
    }

    @Override
    public Image getImage(Object element) {
      if(element instanceof String moduleName) {
        IMavenProjectFacade projectFacade = editorPage.findModuleProject(moduleName);
        if(projectFacade != null) {
          return MavenEditorImages.IMG_PROJECT;
        }

        IFile moduleFile = editorPage.findModuleFile(moduleName);
        if(moduleFile != null && moduleFile.isAccessible()) {
          return MavenEditorImages.IMG_PROJECT;
        }
      }
      return super.getImage(element);
    }
  }

}
