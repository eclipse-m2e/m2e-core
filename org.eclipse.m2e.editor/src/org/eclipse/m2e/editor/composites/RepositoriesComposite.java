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

package org.eclipse.m2e.editor.composites;

import static org.eclipse.m2e.editor.pom.FormUtils.isEmpty;
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;
import static org.eclipse.m2e.editor.pom.FormUtils.nvl;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.DeploymentRepository;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Relocation;
import org.eclipse.m2e.model.edit.pom.Repository;
import org.eclipse.m2e.model.edit.pom.RepositoryPolicy;
import org.eclipse.m2e.model.edit.pom.Site;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;


/**
 * @author Eugene Kuleshov
 */
public class RepositoriesComposite extends Composite {

  static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  MavenPomEditorPage parent;

  FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  // controls

  ListEditorComposite<Repository> repositoriesEditor;

  ListEditorComposite<Repository> pluginRepositoriesEditor;

  Section repositoryDetailsSection;

  Section releaseRepositorySection;

  Section snapshotRepositorySection;

  Section projectSiteSection;

  Section relocationSection;

  Text repositoryIdText;

  Text repositoryNameText;

  Text repositoryUrlText;

  CCombo repositoryLayoutCombo;

  Button releasesEnabledButton;

  CCombo releasesUpdatePolicyCombo;

  CCombo releasesChecksumPolicyCombo;

  Label releasesChecksumPolicyLabel;

  Label releasesUpdatePolicyLabel;

  Button snapshotsEnabledButton;

  CCombo snapshotsUpdatePolicyCombo;

  CCombo snapshotsChecksumPolicyCombo;

  Label snapshotsChecksumPolicyLabel;

  Label snapshotsUpdatePolicyLabel;

  Text projectSiteIdText;

  Text projectSiteNameText;

  Text projectSiteUrlText;

  Text projectDownloadUrlText;

  Text relocationGroupIdText;

  Text relocationArtifactIdText;

  Text relocationVersionText;

  Text relocationMessageText;

  Text snapshotRepositoryIdText;

  Text snapshotRepositoryNameText;

  Text snapshotRepositoryUrlText;

  CCombo snapshotRepositoryLayoutCombo;

  Button snapshotRepositoryUniqueVersionButton;

  Text releaseRepositoryIdText;

  Text releaseRepositoryNameText;

  Text releaseRepositoryUrlText;

  CCombo releaseRepositoryLayoutCombo;

  Button releaseRepositoryUniqueVersionButton;

  WidthGroup leftWidthGroup = new WidthGroup();

  WidthGroup rightWidthGroup = new WidthGroup();

  Composite projectSiteComposite;

  Composite releaseDistributionRepositoryComposite;

  Composite relocationComposite;

  Composite snapshotRepositoryComposite;

  boolean changingSelection = false;

  // model

  // Model model;
  Repository currentRepository;

  Model model;
  
  ValueProvider<DistributionManagement> distributionManagementProvider;

  public RepositoriesComposite(Composite parent, int flags) {
    super(parent, flags);

    toolkit.adapt(this);

    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createRepositoriesSection(verticalSash);
    createPluginRepositoriesSection(verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    createRepositoryDetailsSection(horizontalSash);

    toolkit.adapt(horizontalSash, true, true);
    horizontalSash.setWeights(new int[] {1, 1});

    SashForm repositoriesSash = new SashForm(this, SWT.NONE);
    repositoriesSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(repositoriesSash, true, true);

    createReleaseRepositorySection(repositoriesSash);
    createSnapshotRepositorySection(repositoriesSash);

    repositoriesSash.setWeights(new int[] {1, 1});

    SashForm projectSiteSash = new SashForm(this, SWT.NONE);
    projectSiteSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(projectSiteSash, true, true);

    createProjectSiteSection(projectSiteSash);
    createRelocationSection(projectSiteSash);

    projectSiteSash.setWeights(new int[] {1, 1});
  }

  public void dispose() {
    // projectSiteComposite.removeControlListener(leftWidthGroup);
    // releaseDistributionRepositoryComposite.removeControlListener(leftWidthGroup);

    // snapshotRepositoryComposite.removeControlListener(rightWidthGroup);
    // relocationComposite.removeControlListener(rightWidthGroup);

    super.dispose();
  }

  private void createRepositoriesSection(SashForm verticalSash) {
    Section repositoriesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR | ExpandableComposite.COMPACT);
    repositoriesSection.setText(Messages.RepositoriesComposite_section_repositories);

    repositoriesEditor = new ListEditorComposite<Repository>(repositoriesSection, SWT.NONE);

    repositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    repositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());

    repositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Repository> selection = repositoriesEditor.getSelection();
        updateRepositoryDetailsSection(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginRepositoriesEditor.setSelection(Collections.<Repository> emptyList());
          changingSelection = false;
        }
      }
    });

    repositoriesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Repository repository = PomFactory.eINSTANCE.createRepository();
        Command addCommand = AddCommand.create(editingDomain, model, POM_PACKAGE.getModel_Repositories(),
            repository);
        compoundCommand.append(addCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        repositoriesEditor.setSelection(Collections.singletonList(repository));
        updateRepositoryDetailsSection(repository);
        repositoryIdText.setFocus();
      }
    });

    repositoriesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Repository> list = repositoriesEditor.getSelection();
        for(Repository repository : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model, POM_PACKAGE
              .getModel_Repositories(), repository);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateRepositoryDetailsSection(null);
      }
    });

    toolkit.paintBordersFor(repositoriesEditor);
    toolkit.adapt(repositoriesEditor);
    repositoriesSection.setClient(repositoriesEditor);
  }

  private void createPluginRepositoriesSection(SashForm verticalSash) {
    Section pluginRepositoriesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR | ExpandableComposite.COMPACT);
    pluginRepositoriesSection.setText(Messages.RepositoriesComposite_section_pluginRepositories);

    pluginRepositoriesEditor = new ListEditorComposite<Repository>(pluginRepositoriesSection, SWT.NONE);

    pluginRepositoriesEditor.setLabelProvider(new RepositoryLabelProvider());
    pluginRepositoriesEditor.setContentProvider(new ListEditorContentProvider<Repository>());

    pluginRepositoriesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Repository> selection = pluginRepositoriesEditor.getSelection();
        updateRepositoryDetailsSection(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          repositoriesEditor.setSelection(Collections.<Repository> emptyList());
          changingSelection = false;
        }
      }
    });

    pluginRepositoriesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Repository pluginRepository = PomFactory.eINSTANCE.createRepository();
        Command addCommand = AddCommand.create(editingDomain, model, POM_PACKAGE
            .getModel_PluginRepositories(), pluginRepository);
        compoundCommand.append(addCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        pluginRepositoriesEditor.setSelection(Collections.singletonList(pluginRepository));
        updateRepositoryDetailsSection(pluginRepository);
        repositoryIdText.setFocus();
      }
    });

    pluginRepositoriesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Repository> list = pluginRepositoriesEditor.getSelection();
        for(Repository repository : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model,
              POM_PACKAGE.getModel_PluginRepositories(), repository);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateRepositoryDetailsSection(null);
      }
    });

    toolkit.paintBordersFor(pluginRepositoriesEditor);
    toolkit.adapt(pluginRepositoriesEditor);
    pluginRepositoriesSection.setClient(pluginRepositoriesEditor);
  }

  private void createRepositoryDetailsSection(Composite parent) {
    repositoryDetailsSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
    repositoryDetailsSection.setText(Messages.RepositoriesComposite_section_repositoryDetails);

    Composite repositoryDetailsComposite = toolkit.createComposite(repositoryDetailsSection);
    repositoryDetailsComposite.setLayout(new GridLayout(2, false));
    repositoryDetailsSection.setClient(repositoryDetailsComposite);
    toolkit.paintBordersFor(repositoryDetailsComposite);

    Label idLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    idLabel.setText(Messages.RepositoriesComposite_lblId);

    repositoryIdText = toolkit.createText(repositoryDetailsComposite, ""); //$NON-NLS-1$
    GridData gd_repositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_repositoryIdText.widthHint = 100;
    repositoryIdText.setLayoutData(gd_repositoryIdText);

    Label nameLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    nameLabel.setText(Messages.RepositoriesComposite_lblName);

    repositoryNameText = toolkit.createText(repositoryDetailsComposite, ""); //$NON-NLS-1$
    repositoryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink repositoryUrlHyperlink = toolkit.createHyperlink(repositoryDetailsComposite, Messages.RepositoriesComposite_lblUrl, SWT.NONE);
    repositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(repositoryUrlText.getText());
      }
    });

    repositoryUrlText = toolkit.createText(repositoryDetailsComposite, ""); //$NON-NLS-1$
    GridData gd_repositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_repositoryUrlText.widthHint = 100;
    repositoryUrlText.setLayoutData(gd_repositoryUrlText);

    Label layoutLabel = new Label(repositoryDetailsComposite, SWT.NONE);
    layoutLabel.setText(Messages.RepositoriesComposite_lblLayout);

    repositoryLayoutCombo = new CCombo(repositoryDetailsComposite, SWT.FLAT);
    repositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    repositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    repositoryLayoutCombo.setItems(new String[] {"default", "legacy"});

    Composite composite = new Composite(repositoryDetailsComposite, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
    toolkit.adapt(composite, true, true);
    toolkit.paintBordersFor(composite);
    GridLayout compositeLayout = new GridLayout();
    compositeLayout.marginBottom = 2;
    compositeLayout.marginWidth = 2;
    compositeLayout.marginHeight = 0;
    compositeLayout.numColumns = 2;
    composite.setLayout(compositeLayout);

    releasesEnabledButton = toolkit.createButton(composite, Messages.RepositoriesComposite_btnEnableRelease, SWT.CHECK | SWT.FLAT);
    GridData releasesEnabledButtonData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    releasesEnabledButtonData.verticalIndent = 5;
    releasesEnabledButton.setLayoutData(releasesEnabledButtonData);
    releasesEnabledButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean isEnabled = releasesEnabledButton.getSelection();
        releasesUpdatePolicyLabel.setEnabled(isEnabled);
        releasesUpdatePolicyCombo.setEnabled(isEnabled);
        releasesChecksumPolicyLabel.setEnabled(isEnabled);
        releasesChecksumPolicyCombo.setEnabled(isEnabled);
      }
    });

    releasesUpdatePolicyLabel = new Label(composite, SWT.NONE);
    releasesUpdatePolicyLabel.setText(Messages.RepositoriesComposite_lblUpdatePolicy);
    GridData releasesUpdatePolicyLabelData = new GridData();
    releasesUpdatePolicyLabelData.horizontalIndent = 15;
    releasesUpdatePolicyLabel.setLayoutData(releasesUpdatePolicyLabelData);

    releasesUpdatePolicyCombo = new CCombo(composite, SWT.FLAT);
    releasesUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    releasesUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    toolkit.adapt(releasesUpdatePolicyCombo, true, true);

    releasesChecksumPolicyLabel = new Label(composite, SWT.NONE);
    releasesChecksumPolicyLabel.setText(Messages.RepositoriesComposite_lblChecksumPolicy);
    GridData releasesChecksumPolicyLabelData = new GridData();
    releasesChecksumPolicyLabelData.horizontalIndent = 15;
    releasesChecksumPolicyLabel.setLayoutData(releasesChecksumPolicyLabelData);

    releasesChecksumPolicyCombo = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    toolkit.adapt(releasesChecksumPolicyCombo, true, true);
    releasesChecksumPolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    releasesChecksumPolicyCombo.setItems(new String[] {"ignore", "fail", "warn"});
    releasesChecksumPolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    snapshotsEnabledButton = toolkit.createButton(composite, Messages.RepositoriesComposite_btnEnableSnapshots, SWT.CHECK | SWT.FLAT);
    GridData snapshotsEnabledButtonData = new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1);
    snapshotsEnabledButtonData.verticalIndent = 5;
    snapshotsEnabledButton.setLayoutData(snapshotsEnabledButtonData);
    snapshotsEnabledButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean isEnabled = releasesEnabledButton.getSelection();
        snapshotsUpdatePolicyLabel.setEnabled(isEnabled);
        snapshotsUpdatePolicyCombo.setEnabled(isEnabled);
        snapshotsChecksumPolicyLabel.setEnabled(isEnabled);
        snapshotsChecksumPolicyCombo.setEnabled(isEnabled);
      }
    });

    snapshotsUpdatePolicyLabel = new Label(composite, SWT.NONE);
    snapshotsUpdatePolicyLabel.setText(Messages.RepositoriesComposite_lblUpdatePolicy);
    GridData snapshotsUpdatePolicyLabelData = new GridData();
    snapshotsUpdatePolicyLabelData.horizontalIndent = 15;
    snapshotsUpdatePolicyLabel.setLayoutData(snapshotsUpdatePolicyLabelData);

    snapshotsUpdatePolicyCombo = new CCombo(composite, SWT.FLAT);
    snapshotsUpdatePolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    snapshotsUpdatePolicyCombo.setItems(new String[] {"daily", "always", "interval:30", "never"});
    snapshotsUpdatePolicyCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    toolkit.adapt(snapshotsUpdatePolicyCombo, true, true);
    toolkit.paintBordersFor(snapshotsUpdatePolicyCombo);

    snapshotsChecksumPolicyLabel = new Label(composite, SWT.NONE);
    snapshotsChecksumPolicyLabel.setText(Messages.RepositoriesComposite_lblChecksumPolicy);
    GridData checksumPolicyLabelData = new GridData();
    checksumPolicyLabelData.horizontalIndent = 15;
    snapshotsChecksumPolicyLabel.setLayoutData(checksumPolicyLabelData);
    toolkit.adapt(snapshotsChecksumPolicyLabel, true, true);

    snapshotsChecksumPolicyCombo = new CCombo(composite, SWT.READ_ONLY | SWT.FLAT);
    snapshotsChecksumPolicyCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    snapshotsChecksumPolicyCombo.setItems(new String[] {"ignore", "fail", "warn"});
    snapshotsChecksumPolicyCombo.setLayoutData(new GridData());
    toolkit.adapt(snapshotsChecksumPolicyCombo, true, true);
    toolkit.paintBordersFor(snapshotsChecksumPolicyCombo);
    repositoryDetailsComposite.setTabList(new Control[] {repositoryIdText, repositoryNameText, repositoryUrlText, repositoryLayoutCombo, composite});

    updateRepositoryDetailsSection(null);
  }

  private void createRelocationSection(SashForm sashForm) {
    relocationSection = toolkit.createSection(sashForm, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    relocationSection.setText(Messages.RepositoriesComposite_sectionRelocation);

    relocationComposite = toolkit.createComposite(relocationSection, SWT.NONE);
    relocationComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(relocationComposite);
    relocationSection.setClient(relocationComposite);
    relocationComposite.addControlListener(rightWidthGroup);

    Label relocationGroupIdLabel = toolkit.createLabel(relocationComposite, Messages.RepositoriesComposite_lblGroupId, SWT.NONE);
    rightWidthGroup.addControl(relocationGroupIdLabel);

    relocationGroupIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationGroupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationGroupIdText.widthHint = 100;
    relocationGroupIdText.setLayoutData(gd_relocationGroupIdText);

    Hyperlink relocationArtifactIdHyperlink = toolkit.createHyperlink(relocationComposite, Messages.RepositoriesComposite_lblArtifactid, SWT.NONE);
    relocationArtifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = relocationGroupIdText.getText();
        final String artifactId = relocationArtifactIdText.getText();
        final String version = relocationVersionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version, null);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });

    rightWidthGroup.addControl(relocationArtifactIdHyperlink);

    relocationArtifactIdText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationArtifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationArtifactIdText.widthHint = 100;
    relocationArtifactIdText.setLayoutData(gd_relocationArtifactIdText);

    Label relocationVersionLabel = toolkit.createLabel(relocationComposite, Messages.RepositoriesComposite_lblVersion, SWT.NONE);
    rightWidthGroup.addControl(relocationVersionLabel);

    relocationVersionText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationVersionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationVersionText.widthHint = 100;
    relocationVersionText.setLayoutData(gd_relocationVersionText);

    Label relocationMessageLabel = toolkit.createLabel(relocationComposite, Messages.RepositoriesComposite_lblMessage, SWT.NONE);
    rightWidthGroup.addControl(relocationMessageLabel);

    relocationMessageText = toolkit.createText(relocationComposite, null, SWT.NONE);
    GridData gd_relocationMessageText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_relocationMessageText.widthHint = 100;
    relocationMessageText.setLayoutData(gd_relocationMessageText);
    relocationComposite.setTabList(new Control[] {relocationGroupIdText, relocationArtifactIdText, relocationVersionText, relocationMessageText});
  }

  private void createProjectSiteSection(SashForm sashForm) {
    projectSiteSection = toolkit.createSection(sashForm, ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    projectSiteSection.setText(Messages.RepositoriesComposite_section_projectSite);

    projectSiteComposite = toolkit.createComposite(projectSiteSection, SWT.NONE);
    projectSiteComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(projectSiteComposite);
    projectSiteSection.setClient(projectSiteComposite);
    projectSiteComposite.addControlListener(leftWidthGroup);

    Label siteIdLabel = toolkit.createLabel(projectSiteComposite, Messages.RepositoriesComposite_lblSiteId, SWT.NONE);
    leftWidthGroup.addControl(siteIdLabel);

    projectSiteIdText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteIdText.widthHint = 100;
    projectSiteIdText.setLayoutData(gd_projectSiteIdText);

    Label siteNameLabel = toolkit.createLabel(projectSiteComposite, Messages.RepositoriesComposite_lblName, SWT.NONE);
    leftWidthGroup.addControl(siteNameLabel);

    projectSiteNameText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteNameText.widthHint = 100;
    projectSiteNameText.setLayoutData(gd_projectSiteNameText);

    Hyperlink projectSiteUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, Messages.RepositoriesComposite_lblUrl2, SWT.NONE);
    projectSiteUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectSiteUrlText.getText());
      }
    });
    leftWidthGroup.addControl(projectSiteUrlHyperlink);

    projectSiteUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectSiteUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectSiteUrlText.widthHint = 100;
    projectSiteUrlText.setLayoutData(gd_projectSiteUrlText);
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    toolkit.adapt(sashForm, true, true);

    Hyperlink projectDownloadUrlHyperlink = toolkit.createHyperlink(projectSiteComposite, Messages.RepositoriesComposite_lblDownload, SWT.NONE);
    projectDownloadUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(projectDownloadUrlText.getText());
      }
    });
    leftWidthGroup.addControl(projectDownloadUrlHyperlink);

    projectDownloadUrlText = toolkit.createText(projectSiteComposite, null, SWT.NONE);
    GridData gd_projectDownloadUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_projectDownloadUrlText.widthHint = 100;
    projectDownloadUrlText.setLayoutData(gd_projectDownloadUrlText);
    projectSiteComposite.setTabList(new Control[] {projectSiteIdText, projectSiteNameText, projectSiteUrlText, projectDownloadUrlText});
  }

  private void createSnapshotRepositorySection(SashForm distributionManagementSash) {
    snapshotRepositorySection = toolkit.createSection(distributionManagementSash, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    snapshotRepositorySection.setText(Messages.RepositoriesComposite_section_snapshotDistRepo);

    snapshotRepositoryComposite = toolkit.createComposite(snapshotRepositorySection, SWT.NONE);
    snapshotRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(snapshotRepositoryComposite);
    snapshotRepositorySection.setClient(snapshotRepositoryComposite);
    snapshotRepositoryComposite.addControlListener(rightWidthGroup);

    Label snapshotRepositoryIdLabel = toolkit.createLabel(snapshotRepositoryComposite, Messages.RepositoriesComposite_lblRepoId, SWT.NONE);
    rightWidthGroup.addControl(snapshotRepositoryIdLabel);

    snapshotRepositoryIdText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryIdText.widthHint = 100;
    snapshotRepositoryIdText.setLayoutData(gd_snapshotRepositoryIdText);

    Label snapshotRepositoryNameLabel = toolkit.createLabel(snapshotRepositoryComposite, Messages.RepositoriesComposite_lblName, SWT.NONE);
    rightWidthGroup.addControl(snapshotRepositoryNameLabel);

    snapshotRepositoryNameText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryNameText.widthHint = 100;
    snapshotRepositoryNameText.setLayoutData(gd_snapshotRepositoryNameText);

    Hyperlink snapshotRepositoryUrlHyperlink = toolkit.createHyperlink(snapshotRepositoryComposite, Messages.RepositoriesComposite_lblUrl2, SWT.NONE);
    snapshotRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(snapshotRepositoryUrlText.getText());
      }
    });
    rightWidthGroup.addControl(snapshotRepositoryUrlHyperlink);

    snapshotRepositoryUrlText = toolkit.createText(snapshotRepositoryComposite, null, SWT.NONE);
    GridData gd_snapshotRepositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_snapshotRepositoryUrlText.widthHint = 100;
    snapshotRepositoryUrlText.setLayoutData(gd_snapshotRepositoryUrlText);

    Label snapshotRepositoryLayoutLabel = toolkit.createLabel(snapshotRepositoryComposite, Messages.RepositoriesComposite_lblLayout, SWT.NONE);
    snapshotRepositoryLayoutLabel.setLayoutData(new GridData());
    rightWidthGroup.addControl(snapshotRepositoryLayoutLabel);

    snapshotRepositoryLayoutCombo = new CCombo(snapshotRepositoryComposite, SWT.FLAT);
    snapshotRepositoryLayoutCombo.setItems(new String[] {"default", "legacy"});
    snapshotRepositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    snapshotRepositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(snapshotRepositoryLayoutCombo, true, true);
    new Label(snapshotRepositoryComposite, SWT.NONE);

    snapshotRepositoryUniqueVersionButton = toolkit.createButton(snapshotRepositoryComposite, //
        Messages.RepositoriesComposite_btnUniqueVersion, SWT.CHECK);
    snapshotRepositoryUniqueVersionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    snapshotRepositoryComposite.setTabList(new Control[] {snapshotRepositoryIdText, snapshotRepositoryNameText, snapshotRepositoryUrlText, snapshotRepositoryLayoutCombo, snapshotRepositoryUniqueVersionButton});
  }

  private void createReleaseRepositorySection(SashForm distributionManagementSash) {
    releaseRepositorySection = toolkit.createSection(distributionManagementSash, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    releaseRepositorySection.setText(Messages.RepositoriesComposite_section_releaseDistRepo);

    releaseDistributionRepositoryComposite = toolkit.createComposite(releaseRepositorySection, SWT.NONE);
    releaseDistributionRepositoryComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(releaseDistributionRepositoryComposite);
    releaseRepositorySection.setClient(releaseDistributionRepositoryComposite);
    releaseDistributionRepositoryComposite.addControlListener(leftWidthGroup);

    Label releaseRepositoryIdLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, Messages.RepositoriesComposite_lblRepoId, SWT.NONE);
    leftWidthGroup.addControl(releaseRepositoryIdLabel);

    releaseRepositoryIdText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryIdText.widthHint = 100;
    releaseRepositoryIdText.setLayoutData(gd_releaseRepositoryIdText);

    Label releaseRepositoryNameLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, Messages.RepositoriesComposite_lblName, SWT.NONE);
    leftWidthGroup.addControl(releaseRepositoryNameLabel);

    releaseRepositoryNameText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryNameText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryNameText.widthHint = 100;
    releaseRepositoryNameText.setLayoutData(gd_releaseRepositoryNameText);

    Hyperlink releaseRepositoryUrlHyperlink = toolkit.createHyperlink(releaseDistributionRepositoryComposite, Messages.RepositoriesComposite_lblUrl2,
        SWT.NONE);
    releaseRepositoryUrlHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(releaseRepositoryUrlText.getText());
      }
    });
    leftWidthGroup.addControl(releaseRepositoryUrlHyperlink);

    releaseRepositoryUrlText = toolkit.createText(releaseDistributionRepositoryComposite, null, SWT.NONE);
    GridData gd_releaseRepositoryUrlText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_releaseRepositoryUrlText.widthHint = 100;
    releaseRepositoryUrlText.setLayoutData(gd_releaseRepositoryUrlText);

    Label releaseRepositoryLayoutLabel = toolkit.createLabel(releaseDistributionRepositoryComposite, Messages.RepositoriesComposite_lblLayout,
        SWT.NONE);
    releaseRepositoryLayoutLabel.setLayoutData(new GridData());
    leftWidthGroup.addControl(releaseRepositoryLayoutLabel);

    releaseRepositoryLayoutCombo = new CCombo(releaseDistributionRepositoryComposite, SWT.FLAT);
    releaseRepositoryLayoutCombo.setItems(new String[] {"default", "legacy"});
    releaseRepositoryLayoutCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
    releaseRepositoryLayoutCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
    toolkit.adapt(releaseRepositoryLayoutCombo, true, true);
    new Label(releaseDistributionRepositoryComposite, SWT.NONE);

    releaseRepositoryUniqueVersionButton = toolkit.createButton(releaseDistributionRepositoryComposite,
        Messages.RepositoriesComposite_btnUniqueVersion, SWT.CHECK);
    releaseRepositoryUniqueVersionButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    releaseDistributionRepositoryComposite.setTabList(new Control[] {releaseRepositoryIdText, releaseRepositoryNameText, releaseRepositoryUrlText, releaseRepositoryLayoutCombo, releaseRepositoryUniqueVersionButton});
  }

  public void loadData(MavenPomEditorPage editorPage, Model model,
      ValueProvider<DistributionManagement> distributionManagementProvider) {
    this.parent = editorPage;
    this.model = model;
    this.distributionManagementProvider = distributionManagementProvider;

    loadRepositories();
    loadPluginRepositories();

    loadReleaseDistributionRepository();
    loadSnapshotDistributionRepository();
    loadProjectSite();
    loadRelocation();

    registerReleaseRepositoryListeners();
    registerSnapshotRepositoryListeners();
    registerProjectListeners();
    registerRelocationListeners();

    repositoriesEditor.setReadOnly(parent.isReadOnly());
    pluginRepositoriesEditor.setReadOnly(parent.isReadOnly());

    expandSections();
  }

  private void expandSections() {
    DistributionManagement dm = distributionManagementProvider.getValue();
    if(dm != null) {
      boolean isRepositoriesExpanded = false;

      if(dm.getRepository() != null) {
        DeploymentRepository r = dm.getRepository();
        isRepositoriesExpanded |= !isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
            || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion());
      }

      if(dm.getSnapshotRepository() != null) {
        DeploymentRepository r = dm.getSnapshotRepository();
        isRepositoriesExpanded |= !isEmpty(r.getId()) || !isEmpty(r.getName()) || !isEmpty(r.getUrl())
            || !isEmpty(r.getLayout()) || !isEmpty(r.getUniqueVersion());
      }

      releaseRepositorySection.setExpanded(isRepositoriesExpanded);
      snapshotRepositorySection.setExpanded(isRepositoriesExpanded);

      boolean isSiteExpanded = false;

      Site s = dm.getSite();
      if(s != null) {
        isSiteExpanded |= !isEmpty(s.getId()) || !isEmpty(s.getName()) || !isEmpty(s.getUrl())
            || !isEmpty(dm.getDownloadUrl());
      } else {
        isSiteExpanded |= !isEmpty(dm.getDownloadUrl());
      }

      if(dm.getRelocation() != null) {
        Relocation r = dm.getRelocation();
        isSiteExpanded |= !isEmpty(r.getGroupId()) || !isEmpty(r.getArtifactId()) || !isEmpty(r.getVersion())
            || !isEmpty(r.getMessage());
      }

      projectSiteSection.setExpanded(isSiteExpanded);
      relocationSection.setExpanded(isSiteExpanded);

    } else {
      releaseRepositorySection.setExpanded(false);
      snapshotRepositorySection.setExpanded(false);
      projectSiteSection.setExpanded(false);
      relocationSection.setExpanded(false);
    }

    relocationSection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;

      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          projectSiteSection.setExpanded(relocationSection.isExpanded());
          isExpanding = false;
        }
      }
    });
    projectSiteSection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;

      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          relocationSection.setExpanded(projectSiteSection.isExpanded());
          isExpanding = false;
        }
      }
    });

    releaseRepositorySection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;

      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          snapshotRepositorySection.setExpanded(releaseRepositorySection.isExpanded());
          isExpanding = false;
        }
      }
    });
    snapshotRepositorySection.addExpansionListener(new ExpansionAdapter() {
      boolean isExpanding = false;

      public void expansionStateChanged(ExpansionEvent e) {
        if(!isExpanding) {
          isExpanding = true;
          releaseRepositorySection.setExpanded(snapshotRepositorySection.isExpanded());
          isExpanding = false;
        }
      }
    });
  }

  private void registerReleaseRepositoryListeners() {
    ValueProvider<DeploymentRepository> repositoryProvider = new ValueProvider.ParentValueProvider<DeploymentRepository>(
        releaseRepositoryIdText, releaseRepositoryNameText, releaseRepositoryUrlText) {
      public DeploymentRepository getValue() {
        DistributionManagement dm = distributionManagementProvider.getValue();
        return dm == null ? null : dm.getRepository();
      }

      public DeploymentRepository create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = createDistributionManagement(editingDomain, compoundCommand);
        DeploymentRepository r = dm.getRepository();
        if(r == null) {
          r = PomFactory.eINSTANCE.createDeploymentRepository();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Repository(), r);
          compoundCommand.append(command);
        }
        return r;
      }
    };
    parent.setModifyListener(releaseRepositoryIdText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Id(), ""); //$NON-NLS-1$
    parent.setModifyListener(releaseRepositoryNameText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Name(),
        ""); //$NON-NLS-1$
    parent.setModifyListener(releaseRepositoryUrlText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Url(),
        ""); //$NON-NLS-1$
    parent.setModifyListener(releaseRepositoryLayoutCombo, repositoryProvider, POM_PACKAGE
        .getDeploymentRepository_Layout(), "default");
    parent.setModifyListener(releaseRepositoryUniqueVersionButton, repositoryProvider, POM_PACKAGE
        .getDeploymentRepository_UniqueVersion(), "true");
  }

  private void registerSnapshotRepositoryListeners() {
    ValueProvider<DeploymentRepository> repositoryProvider = new ValueProvider.ParentValueProvider<DeploymentRepository>(
        snapshotRepositoryIdText, snapshotRepositoryNameText, snapshotRepositoryUrlText) {
      public DeploymentRepository getValue() {
        DistributionManagement dm = distributionManagementProvider.getValue();
        return dm == null ? null : dm.getSnapshotRepository();
      }

      public DeploymentRepository create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = createDistributionManagement(editingDomain, compoundCommand);
        DeploymentRepository r = dm.getSnapshotRepository();
        if(r == null) {
          r = PomFactory.eINSTANCE.createDeploymentRepository();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE
              .getDistributionManagement_SnapshotRepository(), r);
          compoundCommand.append(command);
        }
        return r;
      }
    };
    parent
        .setModifyListener(snapshotRepositoryIdText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Id(), ""); //$NON-NLS-1$
    parent.setModifyListener(snapshotRepositoryNameText, repositoryProvider,
        POM_PACKAGE.getDeploymentRepository_Name(), ""); //$NON-NLS-1$
    parent.setModifyListener(snapshotRepositoryUrlText, repositoryProvider, POM_PACKAGE.getDeploymentRepository_Url(),
        ""); //$NON-NLS-1$
    parent.setModifyListener(snapshotRepositoryLayoutCombo, repositoryProvider, POM_PACKAGE
        .getDeploymentRepository_Layout(), "default");
    parent.setModifyListener(snapshotRepositoryUniqueVersionButton, repositoryProvider, POM_PACKAGE
        .getDeploymentRepository_UniqueVersion(), "true");
  }

  private void registerProjectListeners() {
    //do not use ParentValueProvider here as it renders the other providers useless (siteProvider etc)
    ValueProvider<DistributionManagement> dmProvider = new ValueProvider.DefaultValueProvider<DistributionManagement>(distributionManagementProvider.getValue())
    {
      public DistributionManagement getValue() {
        return distributionManagementProvider.getValue();
      }

      public DistributionManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        return createDistributionManagement(editingDomain, compoundCommand);
      }
    };
    parent.setModifyListener(projectDownloadUrlText, dmProvider, POM_PACKAGE.getDistributionManagement_DownloadUrl(),
        ""); //$NON-NLS-1$

    ValueProvider<Site> siteProvider = new ValueProvider.ParentValueProvider<Site>(projectSiteIdText,
        projectSiteNameText, projectSiteUrlText) {
      public Site getValue() {
        DistributionManagement dm = distributionManagementProvider.getValue();
        return dm == null ? null : dm.getSite();
      }

      public Site create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = createDistributionManagement(editingDomain, compoundCommand);
        Site s = dm.getSite();
        if(s == null) {
          s = PomFactory.eINSTANCE.createSite();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Site(), s);
          compoundCommand.append(command);
        }
        return s;
      }
    };
    parent.setModifyListener(projectSiteIdText, siteProvider, POM_PACKAGE.getSite_Id(), ""); //$NON-NLS-1$
    parent.setModifyListener(projectSiteNameText, siteProvider, POM_PACKAGE.getSite_Name(), ""); //$NON-NLS-1$
    parent.setModifyListener(projectSiteUrlText, siteProvider, POM_PACKAGE.getSite_Url(), ""); //$NON-NLS-1$
  }

  private void registerRelocationListeners() {
    ValueProvider<Relocation> relocationProvider = new ValueProvider.ParentValueProvider<Relocation>(
        relocationGroupIdText, relocationArtifactIdText, relocationVersionText, relocationMessageText) {
      public Relocation getValue() {
        DistributionManagement dm = distributionManagementProvider.getValue();
        return dm == null ? null : dm.getRelocation();
      }

      public Relocation create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = createDistributionManagement(editingDomain, compoundCommand);
        Relocation r = dm.getRelocation();
        if(r == null) {
          r = PomFactory.eINSTANCE.createRelocation();
          Command command = SetCommand.create(editingDomain, dm, POM_PACKAGE.getDistributionManagement_Relocation(), r);
          compoundCommand.append(command);
        }
        return r;
      }
    };
    parent.setModifyListener(relocationGroupIdText, relocationProvider, POM_PACKAGE.getRelocation_GroupId(), ""); //$NON-NLS-1$
    parent.setModifyListener(relocationArtifactIdText, relocationProvider, POM_PACKAGE.getRelocation_ArtifactId(), ""); //$NON-NLS-1$
    parent.setModifyListener(relocationVersionText, relocationProvider, POM_PACKAGE.getRelocation_Version(), ""); //$NON-NLS-1$
    parent.setModifyListener(relocationMessageText, relocationProvider, POM_PACKAGE.getRelocation_Message(), ""); //$NON-NLS-1$
  }

  private void loadReleaseDistributionRepository() {
    DistributionManagement dm = distributionManagementProvider.getValue();
    DeploymentRepository repository = dm == null ? null : dm.getRepository();
    if(repository != null) {
      setText(releaseRepositoryIdText, repository.getId());
      setText(releaseRepositoryNameText, repository.getName());
      setText(releaseRepositoryUrlText, repository.getUrl());
      setText(releaseRepositoryLayoutCombo, "".equals(nvl(repository.getLayout())) ? "default" : nvl(repository.getLayout())); //$NON-NLS-1$ //$NON-NLS-2$
      setButton(releaseRepositoryUniqueVersionButton, "true".equals(repository.getUniqueVersion()));
    } else {
      setText(releaseRepositoryIdText, ""); //$NON-NLS-1$
      setText(releaseRepositoryNameText, ""); //$NON-NLS-1$
      setText(releaseRepositoryUrlText, ""); //$NON-NLS-1$
      setText(releaseRepositoryLayoutCombo, ""); //$NON-NLS-1$
      setButton(releaseRepositoryUniqueVersionButton, true); // default
    }
  }

  private void loadSnapshotDistributionRepository() {
    DistributionManagement dm = distributionManagementProvider.getValue();
    DeploymentRepository repository = dm == null ? null : dm.getSnapshotRepository();
    if(repository != null) {
      setText(snapshotRepositoryIdText, repository.getId());
      setText(snapshotRepositoryNameText, repository.getName());
      setText(snapshotRepositoryUrlText, repository.getUrl());
      setText(snapshotRepositoryLayoutCombo, "".equals(nvl(repository.getLayout())) ? "default" : nvl(repository.getLayout())); //$NON-NLS-1$ //$NON-NLS-2$
      setButton(snapshotRepositoryUniqueVersionButton, "true".equals(repository.getUniqueVersion()));
    } else {
      setText(snapshotRepositoryIdText, ""); //$NON-NLS-1$
      setText(snapshotRepositoryNameText, ""); //$NON-NLS-1$
      setText(snapshotRepositoryUrlText, ""); //$NON-NLS-1$
      setText(snapshotRepositoryLayoutCombo, ""); //$NON-NLS-1$
      setButton(snapshotRepositoryUniqueVersionButton, true); // default
    }
  }

  private void loadProjectSite() {
    DistributionManagement dm = distributionManagementProvider.getValue();
    Site site = dm == null ? null : dm.getSite();
    if(site != null) {
      setText(projectSiteIdText, site.getId());
      setText(projectSiteNameText, site.getName());
      setText(projectSiteUrlText, site.getUrl());
    } else {
      setText(projectSiteIdText, ""); //$NON-NLS-1$
      setText(projectSiteNameText, ""); //$NON-NLS-1$
      setText(projectSiteUrlText, ""); //$NON-NLS-1$
    }

    setText(projectDownloadUrlText, dm == null ? null : dm.getDownloadUrl());
  }

  private void loadRelocation() {
    DistributionManagement dm = distributionManagementProvider.getValue();
    Relocation relocation = dm == null ? null : dm.getRelocation();
    if(relocation != null) {
      setText(relocationGroupIdText, relocation.getGroupId());
      setText(relocationArtifactIdText, relocation.getArtifactId());
      setText(relocationVersionText, relocation.getVersion());
      setText(relocationMessageText, relocation.getMessage());
    } else {
      setText(relocationGroupIdText, ""); //$NON-NLS-1$
      setText(relocationArtifactIdText, ""); //$NON-NLS-1$
      setText(relocationVersionText, ""); //$NON-NLS-1$
      setText(relocationMessageText, ""); //$NON-NLS-1$
    }
  }

  private void loadRepositories() {
    repositoriesEditor.setInput(model.getRepositories());
    repositoriesEditor.setReadOnly(parent.isReadOnly());
    changingSelection = true;
    updateRepositoryDetailsSection(null);
    changingSelection = false;
  }

  private void loadPluginRepositories() {
    pluginRepositoriesEditor.setInput(model.getPluginRepositories());
    pluginRepositoriesEditor.setReadOnly(parent.isReadOnly());
    changingSelection = true;
    updateRepositoryDetailsSection(null);
    changingSelection = false;
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    Object feature = notification.getFeature();
    if(PomPackage.Literals.MODEL__REPOSITORIES == feature) {
      repositoriesEditor.refresh();
    }

    if(PomPackage.Literals.MODEL__PLUGIN_REPOSITORIES == feature) {
      pluginRepositoriesEditor.refresh();
    }

    if(object instanceof Repository) {
      repositoriesEditor.refresh();
      pluginRepositoriesEditor.refresh();
      
      Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
      if(currentRepository == object && (notificationObject == null || notificationObject instanceof Repository)) {
        updateRepositoryDetailsSection((Repository) notificationObject);
      }
    }

    if(object instanceof DistributionManagement) {
      if(object == distributionManagementProvider.getValue()) {
        loadProjectSite();
        loadRelocation();
        loadReleaseDistributionRepository();
        loadSnapshotDistributionRepository();
      }
    }

    if(object instanceof Site) {
      if(object.eContainer() == distributionManagementProvider.getValue()) {
        loadProjectSite();
      }
    }

    if(object instanceof Relocation) {
      if(object.eContainer() == distributionManagementProvider.getValue()) {
        loadRelocation();
      }
    }

    if(object instanceof DeploymentRepository) {
      if(object.eContainer() == distributionManagementProvider.getValue()) {
        loadReleaseDistributionRepository();
        loadSnapshotDistributionRepository();
      }
    }

    // XXX
  }

  protected void updateRepositoryDetailsSection(final Repository repository) {
    if(changingSelection) {
      return;
    }
//    if(repository != null && currentRepository == repository) {
//      return;
//    }
    currentRepository = repository;

    if(parent != null) {
      parent.removeNotifyListener(repositoryIdText);
      parent.removeNotifyListener(repositoryNameText);
      parent.removeNotifyListener(repositoryUrlText);
      parent.removeNotifyListener(repositoryLayoutCombo);

      parent.removeNotifyListener(releasesEnabledButton);
      parent.removeNotifyListener(releasesChecksumPolicyCombo);
      parent.removeNotifyListener(releasesUpdatePolicyCombo);

      parent.removeNotifyListener(snapshotsEnabledButton);
      parent.removeNotifyListener(snapshotsChecksumPolicyCombo);
      parent.removeNotifyListener(snapshotsUpdatePolicyCombo);
    }

    if(repository == null) {
      FormUtils.setEnabled(repositoryDetailsSection, false);

      setText(repositoryIdText, ""); //$NON-NLS-1$
      setText(repositoryNameText, ""); //$NON-NLS-1$
      setText(repositoryLayoutCombo, ""); //$NON-NLS-1$
      setText(repositoryUrlText, ""); //$NON-NLS-1$

      setButton(releasesEnabledButton, false);
      setText(releasesChecksumPolicyCombo, ""); //$NON-NLS-1$
      setText(releasesUpdatePolicyCombo, ""); //$NON-NLS-1$

      setButton(snapshotsEnabledButton, false);
      setText(snapshotsChecksumPolicyCombo, ""); // move into listener //$NON-NLS-1$
      setText(snapshotsUpdatePolicyCombo, ""); //$NON-NLS-1$

      // XXX swap repository details panel

      return;
    }

//    repositoryIdText.setEnabled(true);
//    repositoryNameText.setEnabled(true);
//    repositoryLayoutCombo.setEnabled(true);
//    repositoryUrlText.setEnabled(true);
//    releasesEnabledButton.setEnabled(true);
//    snapshotsEnabledButton.setEnabled(true); 

    setText(repositoryIdText, repository.getId());
    setText(repositoryNameText, repository.getName());
    setText(repositoryLayoutCombo, "".equals(nvl(repository.getLayout())) ? "default" : nvl(repository.getLayout()));//$NON-NLS-1$ //$NON-NLS-2$
    setText(repositoryUrlText, repository.getUrl());

    {
      RepositoryPolicy releases = repository.getReleases();
      if(releases != null) {
        setButton(releasesEnabledButton, releases.getEnabled() == null || "true".equals(releases.getEnabled()));
        setText(releasesChecksumPolicyCombo, releases.getChecksumPolicy());
        setText(releasesUpdatePolicyCombo, releases.getUpdatePolicy());
      } else {
        setButton(releasesEnabledButton, true);
      }
      boolean isReleasesEnabled = releasesEnabledButton.getSelection();
      releasesChecksumPolicyCombo.setEnabled(isReleasesEnabled);
      releasesUpdatePolicyCombo.setEnabled(isReleasesEnabled);
      releasesChecksumPolicyLabel.setEnabled(isReleasesEnabled);
      releasesUpdatePolicyLabel.setEnabled(isReleasesEnabled);
    }

    {
      RepositoryPolicy snapshots = repository.getSnapshots();
      if(snapshots != null) {
        setButton(snapshotsEnabledButton, snapshots.getEnabled() == null || "true".equals(snapshots.getEnabled()));
        setText(snapshotsChecksumPolicyCombo, snapshots.getChecksumPolicy());
        setText(snapshotsUpdatePolicyCombo, snapshots.getUpdatePolicy());
      } else {
        setButton(snapshotsEnabledButton, true);
      }
      boolean isSnapshotsEnabled = snapshotsEnabledButton.getSelection();
      snapshotsChecksumPolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyCombo.setEnabled(isSnapshotsEnabled);
      snapshotsChecksumPolicyLabel.setEnabled(isSnapshotsEnabled);
      snapshotsUpdatePolicyLabel.setEnabled(isSnapshotsEnabled);
    }

    FormUtils.setEnabled(repositoryDetailsSection, true);
    FormUtils.setReadonly(repositoryDetailsSection, parent.isReadOnly());

    ValueProvider<Repository> repositoryProvider = new ValueProvider.DefaultValueProvider<Repository>(repository);
    parent.setModifyListener(repositoryIdText, repositoryProvider, POM_PACKAGE.getRepository_Id(), ""); //$NON-NLS-1$
    parent.setModifyListener(repositoryNameText, repositoryProvider, POM_PACKAGE.getRepository_Name(), ""); //$NON-NLS-1$
    parent.setModifyListener(repositoryUrlText, repositoryProvider, POM_PACKAGE.getRepository_Url(), ""); //$NON-NLS-1$
    parent.setModifyListener(repositoryLayoutCombo, repositoryProvider, POM_PACKAGE.getRepository_Layout(), "default");

    ValueProvider<RepositoryPolicy> releasesProvider = new ValueProvider.ParentValueProvider<RepositoryPolicy>(
        releasesEnabledButton, releasesChecksumPolicyCombo, releasesUpdatePolicyCombo) {
      public RepositoryPolicy getValue() {
        return repository.getReleases();
      }

      public RepositoryPolicy create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        RepositoryPolicy policy = getValue();
        if(policy == null) {
          policy = PomFactory.eINSTANCE.createRepositoryPolicy();
          Command command = SetCommand.create(editingDomain, repository, POM_PACKAGE.getRepository_Releases(), policy);
          compoundCommand.append(command);
        }
        return policy;
      }
    };
    parent
        .setModifyListener(releasesEnabledButton, releasesProvider, POM_PACKAGE.getRepositoryPolicy_Enabled(), "true");
    parent.setModifyListener(releasesChecksumPolicyCombo, releasesProvider, POM_PACKAGE
        .getRepositoryPolicy_ChecksumPolicy(), ""); //$NON-NLS-1$
    parent.setModifyListener(releasesUpdatePolicyCombo, releasesProvider, POM_PACKAGE
        .getRepositoryPolicy_UpdatePolicy(), ""); //$NON-NLS-1$

    ValueProvider<RepositoryPolicy> snapshotsProvider = new ValueProvider.ParentValueProvider<RepositoryPolicy>(
        snapshotsEnabledButton, snapshotsChecksumPolicyCombo, snapshotsUpdatePolicyCombo) {
      public RepositoryPolicy getValue() {
        return repository.getSnapshots();
      }

      public RepositoryPolicy create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        RepositoryPolicy policy = getValue();
        if(policy == null) {
          policy = PomFactory.eINSTANCE.createRepositoryPolicy();
          Command command = SetCommand.create(editingDomain, repository, POM_PACKAGE.getRepository_Snapshots(), policy);
          compoundCommand.append(command);
        }
        return policy;
      }
    };
    parent.setModifyListener(snapshotsEnabledButton, snapshotsProvider, POM_PACKAGE.getRepositoryPolicy_Enabled(),
        "true");
    parent.setModifyListener(snapshotsChecksumPolicyCombo, snapshotsProvider, POM_PACKAGE
        .getRepositoryPolicy_ChecksumPolicy(), ""); //$NON-NLS-1$
    parent.setModifyListener(snapshotsUpdatePolicyCombo, snapshotsProvider, POM_PACKAGE
        .getRepositoryPolicy_UpdatePolicy(), ""); //$NON-NLS-1$
  }

  DistributionManagement createDistributionManagement(EditingDomain editingDomain, CompoundCommand compoundCommand) {
    DistributionManagement dm = distributionManagementProvider.getValue();
    if(dm == null) {
      dm = distributionManagementProvider.create(editingDomain, compoundCommand);
    }
    return dm;
  }

  /**
   * Repository label provider
   */
  public class RepositoryLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Repository) {
        Repository r = (Repository) element;
        return (isEmpty(r.getId()) ? "?" : r.getId()) + " : " + (isEmpty(r.getUrl()) ? "?" : r.getUrl());
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      return MavenEditorImages.IMG_REPOSITORY;
    }

  }

}
