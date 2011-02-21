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

import static org.eclipse.m2e.editor.pom.FormUtils.isEmpty;
import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.core.ui.internal.wizards.MavenModuleWizard;
import org.eclipse.m2e.core.ui.internal.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.dialogs.MavenModuleSelectionDialog;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.CiManagement;
import org.eclipse.m2e.model.edit.pom.IssueManagement;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Organization;
import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.m2e.model.edit.pom.Scm;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;


/**
 * @author Eugene Kuleshov
 */
public class OverviewPage extends MavenPomEditorPage {

  static final Logger LOG = LoggerFactory.getLogger(OverviewPage.class);
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

    Label artifactIdLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblArtifactId, SWT.NONE);

    artifactIdText = toolkit.createText(artifactComposite, null, SWT.NONE);
    artifactIdText.setData("name", "artifactId"); //$NON-NLS-1$ //$NON-NLS-2$
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    M2EUIUtils.addRequiredDecoration(artifactIdText);
    createEvaluatorInfo(artifactIdText);

    Label versionLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblVersion, SWT.NONE);

    artifactVersionText = toolkit.createText(artifactComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_versionText.horizontalIndent = 4;
    gd_versionText.widthHint = 200;
    artifactVersionText.setLayoutData(gd_versionText);
    artifactVersionText.setData("name", "version"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(artifactVersionText);

    Label packagingLabel = toolkit.createLabel(artifactComposite, Messages.OverviewPage_lblPackaging, SWT.NONE);

    artifactPackagingCombo = new CCombo(artifactComposite, SWT.FLAT);

    artifactPackagingCombo.add("jar"); //$NON-NLS-1$
    artifactPackagingCombo.add("war"); //$NON-NLS-1$
    artifactPackagingCombo.add("ejb"); //MNGECLIPSE-688 : add EAR & EJB Support //$NON-NLS-1$
    artifactPackagingCombo.add("ear"); //$NON-NLS-1$
    artifactPackagingCombo.add("pom"); //$NON-NLS-1$
    artifactPackagingCombo.add("maven-plugin"); //$NON-NLS-1$
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
      public void run() {
        // calculate current list of artifacts for the project - that's the current parent..
        Set<ArtifactKey> current = new HashSet<ArtifactKey>();
        String parentGroup = parentGroupIdText.getText();
        String parentArtifact = parentArtifactIdText.getText();
        String parentVersion = parentVersionText.getText();
        if (parentGroup != null && parentArtifact != null && parentVersion != null) {
          current.add(new ArtifactKey(parentGroup, parentArtifact, parentVersion, null));
        }
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchParentDialog(
            getEditorSite().getShell(), Messages.OverviewPage_searchDialog_selectParent, getPomEditor().getMavenProject(), getProject());
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
      public void run() {
        final String groupId = parentGroupIdText.getText();
        final String artifactId = parentArtifactIdText.getText();
        final String version = parentVersionText.getText();
        new Job(NLS.bind(Messages.OverviewPage_job, new Object[] {groupId, artifactId, version})) {
          protected IStatus run(IProgressMonitor monitor) {
            OpenPomAction.openEditor(groupId, artifactId, version, monitor);
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

    Label parentVersionLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblVersion2, SWT.NONE);
    parentVersionLabel.setLayoutData(new GridData());

    parentVersionText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData parentVersionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
    parentVersionTextData.horizontalIndent = 4;
    parentVersionTextData.widthHint = 200;
    parentVersionText.setLayoutData(parentVersionTextData);
    parentVersionText.setData("name", "parentVersion"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addVersionProposal(getProject(), null/** null because we don't want expressions from parent pom here */,
        parentGroupIdText, parentArtifactIdText, parentVersionText,
        Packaging.POM);
    M2EUIUtils.addRequiredDecoration(parentVersionText);
    createEvaluatorInfo(parentVersionText);
    

    ModifyListener ml = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        //apparently the loadParent() method also participates in the enablement logic from time to time..
        String text1 = parentArtifactIdText.getText().trim();
        String text2 = parentGroupIdText.getText().trim();
        String text3 = parentVersionText.getText().trim();
        if(text1.length() > 0 && text2.length() > 0 && text3.length() > 0) {
          parentOpenAction.setEnabled(true);
        } else {
          parentOpenAction.setEnabled(false);
        }
      }
    };
    parentArtifactIdText.addModifyListener(ml);
    parentVersionText.addModifyListener(ml);
    parentGroupIdText.addModifyListener(ml);

//    Button parentSelectButton = toolkit.createButton(parentComposite, "Select...", SWT.NONE);
//    parentSelectButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        // TODO calculate current list of artifacts for the project
//        Set<Artifact> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(),
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            parentGroupIdText.setText(nvl(af.group));
//            parentArtifactIdText.setText(nvl(af.artifact));
//            parentVersionText.setText(nvl(af.version));
//          }
//        }
//      }
//    });

    Label parentRealtivePathLabel = toolkit.createLabel(parentComposite, Messages.OverviewPage_lblRelPath, SWT.NONE);

    parentRelativePathText = toolkit.createText(parentComposite, null, SWT.NONE);
    GridData gd_parentRelativePathText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_parentRelativePathText.horizontalIndent = 4;
    parentRelativePathText.setLayoutData(gd_parentRelativePathText);
    parentRelativePathText.setData("name", "parentRelativePath"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(parentRelativePathText);

    widthGroup.addControl(parentGroupIdLabel);
    widthGroup.addControl(parentArtifactIdLabel);
    widthGroup.addControl(parentVersionLabel);
    widthGroup.addControl(parentRealtivePathLabel);

    toolkit.paintBordersFor(parentComposite);
    parentComposite.setTabList(new Control[] {parentGroupIdText, parentArtifactIdText, parentVersionText,
        parentRelativePathText});
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    propertiesSection = new PropertiesSection(toolkit, composite, getEditingDomain());
  }

  private void createModulesSection(FormToolkit toolkit, Composite composite, WidthGroup widthGroup) {
    // XXX should disable Modules actions based on artifact packaging and only add modules when packaging is "pom"

    modulesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    modulesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    modulesSection.setText(Messages.OverviewPage_section_modules);
    modulesSection.setData("name", "modulesSection"); //$NON-NLS-1$ //$NON-NLS-2$

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

    modulesEditor = new ListEditorComposite<String>(modulesSectionComposite, SWT.NONE, true);
    modulesEditor.getViewer().getTable().setData("name", "modulesEditor"); //$NON-NLS-1$ //$NON-NLS-2$
    toolkit.paintBordersFor(modulesEditor);
    toolkit.adapt(modulesEditor);

    modulesEditor.setContentProvider(new ListEditorContentProvider<String>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));

    modulesEditor.setOpenListener(new IOpenListener() {
      public void open(OpenEvent openevent) {
        final List<String> selection = modulesEditor.getSelection();
        new Job(Messages.OverviewPage_opening_editors) {
          protected IStatus run(IProgressMonitor monitor) {
            for(String module : selection) {
              IMavenProjectFacade projectFacade = findModuleProject(module);
              if(projectFacade != null) {
                ArtifactKey key = projectFacade.getArtifactKey();
                OpenPomAction.openEditor(key.getGroupId(), key.getArtifactId(), key.getVersion(), monitor);
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
      }
    });

    modulesEditor.setAddButtonListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final Set<Object> moduleContainers = new HashSet<Object>();
        for(String module : model.getModules()) {
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
      }
    });

    modulesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IEditorInput editorInput = OverviewPage.this.pomEditor.getEditorInput();
        if(editorInput instanceof FileEditorInput) {
          MavenModuleWizard wizard = new MavenModuleWizard(true);
          wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(((FileEditorInput) editorInput).getFile()));
          WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
          int res = dialog.open();
          if(res == Window.OK) {
            createNewModule(wizard.getModuleName());
          }
        }
      }
    });

    modulesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IDocument document = getPomEditor().getDocument();
        try {
          performOnDOMDocument(new OperationTuple(document, new Operation() {
            public void process(Document document) {
              Element root = document.getDocumentElement();
              Element modules = findChild(root, "modules");
              if (modules != null) {
                for (String module : modulesEditor.getSelection()) {
                  Element modEl = findChild(modules, "module", textEquals(module));
                  if (modEl != null) {
                    modules.removeChild(modEl);
                  }
                }
                //now remove the <modules> element itself when there are no more elements left
                removeIfNoChildElement(modules);
              }
            }
          }));
        } catch(Exception ex) {
          LOG.error("error removing module entry", ex);
        }
      }
    });

    modulesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        return element;
      }

      public void modify(Object element, String property, final Object value) {
        int n = modulesEditor.getViewer().getTable().getSelectionIndex();
        //TODO: eventually we might want to get rid of the EMF reference.
        EList<String> modules = model.getModules();
        final String oldValue = modules.get(n);
        if(!value.equals(modules.get(n))) {
          try {
            performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), new Operation() {
              public void process(Document document) {
                Element root = document.getDocumentElement();
                Element module = findChild(findChild(root, "modules"), "module", textEquals(oldValue));
                if (module != null) {
                  setText(module, value.toString());
                }
              }
            }));
          } catch(Exception ex) {
            LOG.error("error changing module entry", ex);
          }
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
            if(event.data instanceof Object[]) {
              addSelectedModules((Object[]) event.data, true);
            }
          }
        });

    newModuleElementAction = new Action(Messages.OverviewPage_action_newModuleElement, MavenImages.NEW_POM) {
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

    Hyperlink urlLabel = toolkit.createHyperlink(projectComposite, Messages.OverviewPage_lblUrl, SWT.NONE);
    urlLabel.addHyperlinkListener(new HyperlinkAdapter() {
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

    Label inceptionYearLabel = toolkit.createLabel(projectComposite, Messages.OverviewPage_lblInception, SWT.NONE);

    inceptionYearText = toolkit.createText(projectComposite, null, SWT.NONE);
    GridData gd_inceptionYearText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_inceptionYearText.widthHint = 150;
    inceptionYearText.setLayoutData(gd_inceptionYearText);
    inceptionYearText.setData("name", "projectInceptionYear"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(inceptionYearText);

    widthGroup.addControl(nameLabel);
    widthGroup.addControl(urlLabel);
    widthGroup.addControl(descriptionLabel);
    widthGroup.addControl(inceptionYearLabel);

    toolkit.paintBordersFor(projectComposite);
    projectComposite.setTabList(new Control[] {projectNameText, projectUrlText, projectDescriptionText,
        inceptionYearText});
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

    Hyperlink organizationUrlLabel = toolkit.createHyperlink(organizationComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    organizationUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
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

    Label scmConnectionLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblConnection, SWT.NONE);

    scmConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmConnectionText.widthHint = 150;
    scmConnectionText.setLayoutData(gd_scmConnectionText);
    scmConnectionText.setData("name", "scmConnection"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmConnectionText);

    Label scmDevConnectionLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblDev, SWT.NONE);

    scmDevConnectionText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmDevConnectionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmDevConnectionText.widthHint = 150;
    scmDevConnectionText.setLayoutData(gd_scmDevConnectionText);
    scmDevConnectionText.setData("name", "scmDevConnection"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmDevConnectionText);

    Label scmTagLabel = toolkit.createLabel(scmComposite, Messages.OverviewPage_lblTag, SWT.NONE);

    scmTagText = toolkit.createText(scmComposite, null, SWT.NONE);
    GridData gd_scmTagText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_scmTagText.widthHint = 150;
    scmTagText.setLayoutData(gd_scmTagText);
    scmTagText.setData("name", "scmTag"); //$NON-NLS-1$ //$NON-NLS-2$
    createEvaluatorInfo(scmTagText);
    
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

    Hyperlink issueManagementUrlLabel = toolkit.createHyperlink(issueManagementComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    issueManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
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

    Hyperlink ciManagementUrlLabel = toolkit.createHyperlink(ciManagementComposite, Messages.OverviewPage_lblUrl,
        SWT.NONE);
    ciManagementUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
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
    

    widthGroup.addControl(ciManagementSystemLabel);
    widthGroup.addControl(ciManagementUrlLabel);

    toolkit.paintBordersFor(ciManagementComposite);
    ciManagementComposite.setTabList(new Control[] {ciManagementSystemCombo, ciManagementUrlCombo});
  }

  protected void doUpdate(Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    Object feature = notification.getFeature();

    if(object instanceof Model) {
      loadThis();
    }

    if(object instanceof PropertyElement) {
      propertiesSection.refresh();
    }

    Object notificationObject = getFromNotification(notification);

    if(feature == PomPackage.Literals.MODEL__PARENT
        || (object instanceof Parent && (notificationObject == null || notificationObject instanceof Parent))) {
      loadParent((Parent) notificationObject);
    }

    if(feature == PomPackage.Literals.MODEL__ORGANIZATION
        || (object instanceof Organization && (notificationObject == null || notificationObject instanceof Organization))) {
      loadOrganization((Organization) notificationObject);
    }

    if(feature == PomPackage.Literals.MODEL__SCM
        || (object instanceof Scm && (notificationObject == null || notificationObject instanceof Scm))) {
      loadScm((Scm) notificationObject);
    }

    if(object instanceof CiManagement && (notificationObject == null || notificationObject instanceof CiManagement)) {
      loadCiManagement((CiManagement) notificationObject);
    }

    if(object instanceof IssueManagement
        && (notificationObject == null || notificationObject instanceof IssueManagement)) {
      loadIssueManagement((IssueManagement) notificationObject);
    }

    if(feature == PomPackage.Literals.MODEL__MODULES) {
      modulesEditor.refresh();
    }

    if(feature == PomPackage.Literals.MODEL__PROPERTIES) {
      propertiesSection.setModel(model, POM_PACKAGE.getModel_Properties());
    }
  }

  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        doUpdate(notification);
      }
    });
  }

  public void loadData() {
    Parent parent = model.getParent();
    Organization organization = model.getOrganization();
    Scm scm = model.getScm();
    IssueManagement issueManagement = model.getIssueManagement();
    CiManagement ciManagement = model.getCiManagement();

    loadThis();
    loadParent(parent);
    loadOrganization(organization);
    loadScm(scm);
    loadIssueManagement(issueManagement);
    loadCiManagement(ciManagement);
    loadModules(model.getModules());
    propertiesSection.setModel(model, POM_PACKAGE.getModel_Properties());

    boolean expandProjectSection = !isEmpty(model.getName()) || !isEmpty(model.getDescription())
        || !isEmpty(model.getUrl()) || !isEmpty(model.getInceptionYear());
    projectSectionData.grabExcessVerticalSpace = expandProjectSection;
    projectSection.setExpanded(expandProjectSection);

    parentSection.setExpanded(parent != null //
        && (!isEmpty(parent.getGroupId()) || !isEmpty(parent.getArtifactId()) //
        || !isEmpty(parent.getVersion())));

    organizationSection.setExpanded(organization != null
        && (!isEmpty(organization.getName()) || !isEmpty(organization.getUrl())));

    scmSection.setExpanded(scm != null
        && (!isEmpty(scm.getUrl()) || !isEmpty(scm.getConnection()) || !isEmpty(scm.getDeveloperConnection())));

    ciManagementSection.setExpanded(ciManagement != null
        && (!isEmpty(ciManagement.getSystem()) || !isEmpty(ciManagement.getUrl())));

    issueManagementSection.setExpanded(issueManagement != null
        && (!isEmpty(issueManagement.getSystem()) || !isEmpty(issueManagement.getUrl())));

    propertiesSection.getSection().setExpanded(model.getProperties() != null && !model.getProperties().isEmpty());

    // Modules modules = model.getModules();
    // modulesSection.setExpanded(modules !=null && modules.getModule().size()>0);
  }

  private void loadThis() {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        removeNotifyListener(artifactGroupIdText);
        removeNotifyListener(artifactIdText);
        removeNotifyListener(artifactVersionText);
        removeNotifyListener(artifactPackagingCombo);

        removeNotifyListener(projectNameText);
        removeNotifyListener(projectDescriptionText);
        removeNotifyListener(projectUrlText);
        removeNotifyListener(inceptionYearText);

        setText(artifactGroupIdText, model.getGroupId());
        setText(artifactIdText, model.getArtifactId());
        setText(artifactVersionText, model.getVersion());
        setText(artifactPackagingCombo, "".equals(nvl(model.getPackaging())) ? "jar" : nvl(model.getPackaging())); //$NON-NLS-1$ //$NON-NLS-2$
        //show/hide modules section when packaging changes..
        loadModules(model.getModules());
        //#335337 no editing of packaging when there are modules, results in error anyway
        if (model.getModules() != null && model.getModules().size() > 0) {
          artifactPackagingCombo.setEnabled(false);
        } else {
          artifactPackagingCombo.setEnabled(true);
        }

        setText(projectNameText, model.getName());
        setText(projectDescriptionText, model.getDescription());
        setText(projectUrlText, model.getUrl());
        setText(inceptionYearText, model.getInceptionYear());

        ValueProvider<Model> modelProvider = new ValueProvider.DefaultValueProvider<Model>(model);
        setModifyListener(artifactGroupIdText, modelProvider, POM_PACKAGE.getModel_GroupId(), ""); //$NON-NLS-1$
        setModifyListener(artifactIdText, modelProvider, POM_PACKAGE.getModel_ArtifactId(), ""); //$NON-NLS-1$
        setModifyListener(artifactVersionText, modelProvider, POM_PACKAGE.getModel_Version(), ""); //$NON-NLS-1$
        setModifyListener(artifactPackagingCombo, modelProvider, POM_PACKAGE.getModel_Packaging(), "jar"); //$NON-NLS-1$

        setModifyListener(projectNameText, modelProvider, POM_PACKAGE.getModel_Name(), ""); //$NON-NLS-1$
        setModifyListener(projectDescriptionText, modelProvider, POM_PACKAGE.getModel_Description(), ""); //$NON-NLS-1$
        setModifyListener(projectUrlText, modelProvider, POM_PACKAGE.getModel_Url(), ""); //$NON-NLS-1$
        setModifyListener(inceptionYearText, modelProvider, POM_PACKAGE.getModel_InceptionYear(), ""); //$NON-NLS-1$
      }
    });

  }

  private void loadParent(Parent parent) {
    removeNotifyListener(parentGroupIdText);
    removeNotifyListener(parentArtifactIdText);
    removeNotifyListener(parentVersionText);
    removeNotifyListener(parentRelativePathText);

    if(parent != null) {
      setText(parentGroupIdText, parent.getGroupId());
      setText(parentArtifactIdText, parent.getArtifactId());
      setText(parentVersionText, parent.getVersion());
      setText(parentRelativePathText, parent.getRelativePath());
    } else {
      setText(parentGroupIdText, ""); //$NON-NLS-1$
      setText(parentArtifactIdText, ""); //$NON-NLS-1$
      setText(parentVersionText, ""); //$NON-NLS-1$
      setText(parentRelativePathText, ""); //$NON-NLS-1$
    }

//    parentGroupIdText.setEditable(!isReadOnly());
//    parentArtifactIdText.setEditable(!isReadOnly());
//    parentVersionText.setEditable(!isReadOnly());
//    parentRelativePathText.setEditable(!isReadOnly());
    parentSelectAction.setEnabled(!isReadOnly());
    // only enable when all 3 coordinates are actually present.
    parentOpenAction.setEnabled(parent != null && parent.getGroupId() != null && parent.getArtifactId() != null && parent.getVersion() != null);

    ValueProvider<Parent> parentProvider = new ValueProvider.ParentValueProvider<Parent>(parentGroupIdText,
        parentArtifactIdText, parentVersionText, parentRelativePathText) {
      public Parent getValue() {
        return model.getParent();
      }

      public Parent create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Parent parent = PomFactory.eINSTANCE.createParent();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Parent(), parent));
        return parent;
      }
    };
    setModifyListener(parentGroupIdText, parentProvider, POM_PACKAGE.getParent_GroupId(), ""); //$NON-NLS-1$
    setModifyListener(parentArtifactIdText, parentProvider, POM_PACKAGE.getParent_ArtifactId(), ""); //$NON-NLS-1$
    setModifyListener(parentVersionText, parentProvider, POM_PACKAGE.getParent_Version(), ""); //$NON-NLS-1$
    setModifyListener(parentRelativePathText, parentProvider, POM_PACKAGE.getParent_RelativePath(), ""); //$NON-NLS-1$
  }

  private void loadModules(EList<String> modules) {
    modulesEditor.setInput(modules);
    modulesEditor.setReadOnly(isReadOnly());
    if("pom".equals(model.getPackaging()) && modulesStack.topControl != modulesEditor) { //$NON-NLS-1$
      modulesStack.topControl = modulesEditor;
      modulesSection.setExpanded(true);
//      newModuleProjectAction.setEnabled(!isReadOnly());
      newModuleElementAction.setEnabled(!isReadOnly());
    } else if(!"pom".equals(model.getPackaging()) && modulesStack.topControl != noModules) { //$NON-NLS-1$
//      newModuleProjectAction.setEnabled(false);
      newModuleElementAction.setEnabled(false);
      modulesStack.topControl = noModules;
      modulesSection.setExpanded(false);
    }
    modulesSectionComposite.layout();
  }

  private void loadOrganization(Organization organization) {
    removeNotifyListener(organizationNameText);
    removeNotifyListener(organizationUrlText);

    if(organization == null) {
      setText(organizationNameText, ""); //$NON-NLS-1$
      setText(organizationUrlText, ""); //$NON-NLS-1$
    } else {
      setText(organizationNameText, organization.getName());
      setText(organizationUrlText, organization.getUrl());
    }

    ValueProvider<Organization> organizationProvider = new ValueProvider.ParentValueProvider<Organization>(
        organizationNameText, organizationUrlText) {
      public Organization getValue() {
        return model.getOrganization();
      }

      public Organization create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Organization organization = PomFactory.eINSTANCE.createOrganization();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Organization(), //
            organization));
        return organization;
      }
    };
    setModifyListener(organizationNameText, organizationProvider, POM_PACKAGE.getOrganization_Name(), ""); //$NON-NLS-1$
    setModifyListener(organizationUrlText, organizationProvider, POM_PACKAGE.getOrganization_Url(), ""); //$NON-NLS-1$
  }

  private void loadScm(Scm scm) {
    removeNotifyListener(scmUrlText);
    removeNotifyListener(scmConnectionText);
    removeNotifyListener(scmDevConnectionText);
    removeNotifyListener(scmTagText);
    if(scm == null) {
      setText(scmUrlText, ""); //$NON-NLS-1$
      setText(scmConnectionText, ""); //$NON-NLS-1$
      setText(scmDevConnectionText, ""); //$NON-NLS-1$
      setText(scmTagText, ""); //$NON-NLS-1$
    } else {
      setText(scmUrlText, scm.getUrl());
      setText(scmConnectionText, scm.getConnection());
      setText(scmDevConnectionText, scm.getDeveloperConnection());
      setText(scmTagText, scm.getTag());
    }

    ValueProvider<Scm> scmProvider = new ValueProvider.ParentValueProvider<Scm>(scmUrlText, scmConnectionText,
        scmDevConnectionText, scmTagText) {
      public Scm getValue() {
        return model.getScm();
      }

      public Scm create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Scm scm = PomFactory.eINSTANCE.createScm();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Scm(), scm));
        return scm;
      }
    };
    setModifyListener(scmUrlText, scmProvider, POM_PACKAGE.getScm_Url(), ""); //$NON-NLS-1$
    setModifyListener(scmConnectionText, scmProvider, POM_PACKAGE.getScm_Connection(), ""); //$NON-NLS-1$
    setModifyListener(scmDevConnectionText, scmProvider, POM_PACKAGE.getScm_DeveloperConnection(), ""); //$NON-NLS-1$
    setModifyListener(scmTagText, scmProvider, POM_PACKAGE.getScm_Tag(), ""); //$NON-NLS-1$
  }

  private void loadCiManagement(CiManagement ciManagement) {
    removeNotifyListener(ciManagementUrlCombo);
    removeNotifyListener(ciManagementSystemCombo);

    if(ciManagement == null) {
      setText(ciManagementSystemCombo, ""); //$NON-NLS-1$
      setText(ciManagementUrlCombo, ""); //$NON-NLS-1$
    } else {
      setText(ciManagementSystemCombo, ciManagement.getSystem());
      setText(ciManagementUrlCombo, ciManagement.getUrl());
    }

    ValueProvider<CiManagement> ciManagementProvider = new ValueProvider.ParentValueProvider<CiManagement>(
        ciManagementUrlCombo, ciManagementSystemCombo) {
      public CiManagement getValue() {
        return model.getCiManagement();
      }

      public CiManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        CiManagement ciManagement = PomFactory.eINSTANCE.createCiManagement();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_CiManagement(), //
            ciManagement));
        return ciManagement;
      }
    };
    setModifyListener(ciManagementUrlCombo, ciManagementProvider, POM_PACKAGE.getCiManagement_Url(), ""); //$NON-NLS-1$
    setModifyListener(ciManagementSystemCombo, ciManagementProvider, POM_PACKAGE.getCiManagement_System(), ""); //$NON-NLS-1$
  }

  private void loadIssueManagement(IssueManagement issueManagement) {
    removeNotifyListener(issueManagementUrlCombo);
    removeNotifyListener(issueManagementSystemCombo);

    if(issueManagement == null) {
      setText(issueManagementSystemCombo, ""); //$NON-NLS-1$
      setText(issueManagementUrlCombo, ""); //$NON-NLS-1$
    } else {
      setText(issueManagementSystemCombo, issueManagement.getSystem());
      setText(issueManagementUrlCombo, issueManagement.getUrl());
    }

    ValueProvider<IssueManagement> issueManagementProvider = new ValueProvider.ParentValueProvider<IssueManagement>(
        issueManagementUrlCombo, issueManagementSystemCombo) {
      public IssueManagement getValue() {
        return model.getIssueManagement();
      }

      public IssueManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        IssueManagement issueManagement = PomFactory.eINSTANCE.createIssueManagement();
        compoundCommand.append(SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_IssueManagement(), //
            issueManagement));
        return issueManagement;
      }
    };
    setModifyListener(issueManagementUrlCombo, issueManagementProvider, POM_PACKAGE.getIssueManagement_Url(), ""); //$NON-NLS-1$
    setModifyListener(issueManagementSystemCombo, issueManagementProvider, POM_PACKAGE.getIssueManagement_System(), ""); //$NON-NLS-1$
  }

  protected void createNewModule(final String moduleName) {
    try {
      performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), new Operation() {
        //same with MavenModuleWizard's module adding operation..
        public void process(Document document) {
          Element root = document.getDocumentElement();
          Element modules = getChild(root, "modules");
          if (findChild(modules, "module", textEquals(moduleName)) == null) {
            format(createElementWithText(modules, "module", moduleName));
          }
        }
      }));
    } catch(Exception e) {
      LOG.error("error updating modules list for pom file", e);
    }
    modulesEditor.setInput(model.getModules());
  }

  protected void addSelectedModules(Object[] result, boolean updateParentSection) {
    String groupId = model.getGroupId();
    if(groupId == null) {
      Parent parent = model.getParent();
      if(parent != null) {
        groupId = parent.getGroupId();
      }
    }

    String version = model.getVersion();
    if(version == null) {
      Parent parent = model.getParent();
      if(parent != null) {
        version = parent.getVersion();
      }
    }

    final String parentGroupId = groupId;
    final String parentArtifactId = model.getArtifactId();
    final String parentVersion = version;
    final IPath projectPath = getProject().getLocation();

    for(Object selection : result) {
      IContainer container = null;
      IFile pomFile = null;

      if(selection instanceof IFile) {
        pomFile = (IFile) selection;
        if(!IMavenConstants.POM_FILE_NAME.equals(pomFile.getName())) {
          continue;
        }
        container = pomFile.getParent();
      } else if(selection instanceof IContainer && !selection.equals(getProject())) {
        container = (IContainer) selection;
        pomFile = container.getFile(new Path(IMavenConstants.POM_FILE_NAME));
      }

      if(pomFile == null || !pomFile.exists() || container == null) {
        continue;
      }

      IPath resultPath = container.getLocation();
      String path = resultPath.makeRelativeTo(projectPath).toString();
      if(!model.getModules().contains(path)) {
        if(updateParentSection) {
          final String relativePath = projectPath.makeRelativeTo(resultPath).toString();
          try {
            performOnDOMDocument(new OperationTuple(pomFile, new Operation() {
              public void process(Document document) {
                Element root = document.getDocumentElement();
                Element parent = getChild(root, PARENT);
                setText(getChild(parent, GROUP_ID), parentGroupId);
                setText(getChild(parent, ARTIFACT_ID), parentArtifactId);
                setText(getChild(parent, VERSION), parentVersion);
                setText(getChild(parent, RELATIVE_PATH), relativePath);
                Element grId = findChild(root, GROUP_ID);
                String grIdText = getTextValue(grId);
                if (grIdText != null && grIdText.equals(parentGroupId)) {
                  removeChild(root, grId);
                }
                Element ver = findChild(root, VERSION);
                String verText = getTextValue(ver);
                if (verText != null && verText.equals(parentVersion)) {
                  removeChild(root, ver);
                }
              }
            }));
          } catch(Exception e) {
            LOG.error("Error updating parent reference in file:" + pomFile, e);
          }
        }

        createNewModule(path);
      }
    }
  }

  private boolean checkDrop() {
    return true;
  }
}
