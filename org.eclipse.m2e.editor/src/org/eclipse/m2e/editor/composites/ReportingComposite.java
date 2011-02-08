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

import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.ArrayList;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.actions.OpenUrlAction;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.util.M2EUtils;
import org.eclipse.m2e.core.util.ProposalUtil;
import org.eclipse.m2e.core.util.search.Packaging;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.PluginsComposite.PluginFilter;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.m2e.model.edit.pom.ReportSet;
import org.eclipse.m2e.model.edit.pom.Reporting;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;


/**
 * @author Eugene Kuleshov
 * @author Dmitry Platonoff
 */
public class ReportingComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  MavenPomEditorPage editorPage;
  
  Text outputFolderText;

  Text groupIdText;

  Text artifactIdText;

  Text versionText;

  Button pluginInheritedButton;

  Hyperlink pluginConfigureButton;

  ListEditorComposite<ReportSet> reportSetsEditor;

  ListEditorComposite<String> reportsEditor;

  ListEditorComposite<ReportPlugin> reportPluginsEditor;

  Section pluginDetailsSection;

  Button reportSetInheritedButton;

  Hyperlink reportSetConfigureButton;

  Button excludeDefaultsButton;
  
  Section reportSetDetailsSection;

  Section reportSetsSection;
  
  Action openWebPageAction;
  
//  Action reportPluginAddAction;

  Action reportPluginSelectAction;
  
  ViewerFilter searchFilter;
  
  SearchControl searchControl;
  
  SearchMatcher searchMatcher;
  
  // model
  
  ValueProvider<Reporting> reportingProvider;
  
  ReportPlugin currentReportPlugin = null;
  
  ReportSet currentReportSet = null;

  
  public ReportingComposite(Composite parent, MavenPomEditorPage page, int style) {
    super(parent, style);
    this.editorPage = page;
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(horizontalSash, true, true);

    createContentSection(horizontalSash);

    Composite verticalSash = toolkit.createComposite(horizontalSash);
    GridLayout reportingPluginDetailsLayout = new GridLayout();
    reportingPluginDetailsLayout.marginWidth = 0;
    reportingPluginDetailsLayout.marginHeight = 0;
    verticalSash.setLayout(reportingPluginDetailsLayout);

    createPluginDetailsSection(verticalSash);
    createReportSetDetails(verticalSash);

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createContentSection(SashForm horizontalSash) {
    Composite composite_1 = toolkit.createComposite(horizontalSash, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite_1.setLayout(gridLayout);
    toolkit.paintBordersFor(composite_1);
    
    Section contentSection = toolkit.createSection(composite_1, ExpandableComposite.TITLE_BAR);
    contentSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    contentSection.setText(Messages.ReportingComposite_section_Content);

    Composite composite = toolkit.createComposite(contentSection, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    contentSection.setClient(composite);
    toolkit.paintBordersFor(composite);

    toolkit.createLabel(composite, Messages.ReportingComposite_lblOutputFolder, SWT.NONE);

    outputFolderText = toolkit.createText(composite, null, SWT.NONE);
    outputFolderText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    excludeDefaultsButton = toolkit.createButton(composite, Messages.ReportingComposite_btnExcludeDefaults, SWT.CHECK);
    excludeDefaultsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));

    Section reportingPluginsSection = toolkit.createSection(composite_1, ExpandableComposite.TITLE_BAR);
    reportingPluginsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
    reportingPluginsSection.setText(Messages.ReportingComposite_sectionReportingPlugins);

    reportPluginsEditor = new ListEditorComposite<ReportPlugin>(reportingPluginsSection, SWT.NONE, true);
    reportingPluginsSection.setClient(reportPluginsEditor);
    toolkit.paintBordersFor(reportPluginsEditor);
    toolkit.adapt(reportPluginsEditor);

    final ReportPluginsLabelProvider labelProvider = new ReportPluginsLabelProvider();
    reportPluginsEditor.setLabelProvider(labelProvider);
    reportPluginsEditor.setContentProvider(new ListEditorContentProvider<ReportPlugin>());

    reportPluginsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<ReportPlugin> selection = reportPluginsEditor.getSelection();
        updateReportPluginDetails(selection.size() == 1 ? selection.get(0) : null);
      }
    });

    reportPluginsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createReportingPlugin(null, null, null);
      }
    });

    reportPluginsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        Reporting reporting = reportingProvider.getValue();
        
        if(reporting != null) {
          List<ReportPlugin> pluginList = reportPluginsEditor.getSelection();
          for(ReportPlugin reportPlugin : pluginList) {
            Command removeCommand = RemoveCommand.create(editingDomain, reporting, POM_PACKAGE
                .getReporting_Plugins(), reportPlugin);
            compoundCommand.append(removeCommand);
          }

          editingDomain.getCommandStack().execute(compoundCommand);
          updateContent(reporting);
        }
      }
    });
    
    reportPluginsEditor.setAddButtonListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(), //
            Messages.ReportingComposite_searchDialog_addPlugin, editorPage.getPomEditor().getMavenProject(), editorPage.getProject(), false);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createReportingPlugin(af.group, af.artifact, af.version);
          }
        }
      }
    });
//    reportPluginAddAction = new Action("Add Report Plugin", MavenEditorImages.ADD_PLUGIN) {
//      public void run() {
//
//      }
//    };
//    reportPluginAddAction.setEnabled(false);
//    toolBarManager.add(reportPluginAddAction);
//    toolBarManager.add(new Separator());
    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);

    
    toolBarManager.add(new Action(Messages.ReportingComposite_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        reportPluginsEditor.getViewer().refresh();
      }
    });
    
    toolBarManager.add(new Action(Messages.ReportingComposite_action_filter, MavenEditorImages.FILTER) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = reportPluginsEditor.getViewer();
        if(isChecked()) {
          viewer.addFilter(searchFilter);
        } else {
          viewer.removeFilter(searchFilter);
        }
        viewer.refresh();
        if(isChecked()) {
          searchControl.getSearchText().setFocus();
        }
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(reportingPluginsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    reportingPluginsSection.setTextClient(toolbarComposite);
    
  }

  private void createPluginDetailsSection(Composite verticalSash) {
    pluginDetailsSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    pluginDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    pluginDetailsSection.setText(Messages.ReportingComposite_section_reportingPluginDetails);

    Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
    GridLayout gridLayout_1 = new GridLayout(2, false);
    gridLayout_1.marginWidth = 2;
    gridLayout_1.marginHeight = 2;
    pluginDetailsComposite.setLayout(gridLayout_1);
    pluginDetailsSection.setClient(pluginDetailsComposite);
    toolkit.paintBordersFor(pluginDetailsComposite);

    toolkit.createLabel(pluginDetailsComposite, Messages.ReportingComposite_lblGroupId, SWT.NONE);

    groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    GridData gd_groupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_groupIdText.horizontalIndent = 4;
    groupIdText.setLayoutData(gd_groupIdText);
    groupIdText.setData("name", "groupIdText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addGroupIdProposal(editorPage.getProject(), groupIdText, Packaging.ALL);
    M2EUtils.addRequiredDecoration(groupIdText);
    
    Hyperlink artifactIdHyperlink = toolkit.createHyperlink(pluginDetailsComposite, Messages.ReportingComposite_lblArtifactId, SWT.NONE);
    artifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = groupIdText.getText();
        final String artifactId = artifactIdText.getText();
        final String version = versionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version, null);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });

    artifactIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_artifactIdText.horizontalIndent = 4;
    artifactIdText.setLayoutData(gd_artifactIdText);
    artifactIdText.setData("name", "artifactIdText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addArtifactIdProposal(editorPage.getProject(), groupIdText, artifactIdText, Packaging.ALL);
    M2EUtils.addRequiredDecoration(artifactIdText);

    toolkit.createLabel(pluginDetailsComposite, Messages.ReportingComposite_lblVersion, SWT.NONE);

    versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
    GridData gd_versionText = new GridData(SWT.FILL, SWT.CENTER, true, false);
    gd_versionText.horizontalIndent = 4;
    versionText.setLayoutData(gd_versionText);
    versionText.setData("name", "versionText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addVersionProposal(editorPage.getProject(), editorPage.getPomEditor().getMavenProject(), groupIdText, artifactIdText, versionText, Packaging.ALL);

    Composite pluginConfigureComposite = toolkit.createComposite(pluginDetailsComposite, SWT.NONE);
    GridData pluginConfigureCompositeData = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
    pluginConfigureComposite.setLayoutData(pluginConfigureCompositeData);
    GridLayout pluginConfigureCompositeLayout = new GridLayout(2, false);
    pluginConfigureCompositeLayout.marginWidth = 0;
    pluginConfigureCompositeLayout.marginHeight = 0;
    pluginConfigureComposite.setLayout(pluginConfigureCompositeLayout);
    toolkit.paintBordersFor(pluginConfigureComposite);

    pluginInheritedButton = toolkit.createButton(pluginConfigureComposite, Messages.ReportingComposite_btnInherited, SWT.CHECK);
    pluginInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    pluginConfigureButton = toolkit.createHyperlink(pluginConfigureComposite, Messages.ReportingComposite_link_Configuration, SWT.NONE);
    pluginConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    pluginConfigureButton.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        if(currentReportPlugin != null) {
          EObject element = currentReportPlugin.getConfiguration();
          editorPage.getPomEditor().showInSourceEditor(element == null ? currentReportPlugin : element);
        }
      }
    });
    pluginDetailsComposite.setTabList(new Control[] {groupIdText, artifactIdText, versionText, pluginConfigureComposite});
    
    openWebPageAction = new Action(Messages.ReportingComposite_action_openWeb, MavenEditorImages.WEB_PAGE) {
      public void run() {
        final String groupId = groupIdText.getText();
        final String artifactId = artifactIdText.getText();
        final String version = versionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor monitor) {
            OpenUrlAction.openBrowser(OpenUrlAction.ID_PROJECT, groupId, artifactId, version, monitor);
            return Status.OK_STATUS;
          }
        }.schedule();
        
      }      
    };
    openWebPageAction.setEnabled(false);
    
    reportPluginSelectAction = new Action(Messages.ReportingComposite_action_selectReportingPlugin, MavenEditorImages.SELECT_PLUGIN) {
      public void run() {
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(), //
            Messages.ReportingComposite_searchDialog_addPlugin, editorPage.getPomEditor().getMavenProject(), editorPage.getProject(), false);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            groupIdText.setText(nvl(af.group));
            artifactIdText.setText(nvl(af.artifact));
            versionText.setText(nvl(af.version));
          }
        }
      }
    };
    reportPluginSelectAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(reportPluginSelectAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(openWebPageAction);
    
    Composite toolbarComposite = toolkit.createComposite(pluginDetailsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginDetailsSection.setTextClient(toolbarComposite);
    
  }

  private void createReportSetDetails(Composite verticalSash) {
    reportSetsSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    reportSetsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    reportSetsSection.setText(Messages.ReportingComposite_section_reportSets);

    reportSetsEditor = new ListEditorComposite<ReportSet>(reportSetsSection, SWT.NONE);
    reportSetsSection.setClient(reportSetsEditor);
    toolkit.paintBordersFor(reportSetsEditor);
    toolkit.adapt(reportSetsEditor);

    reportSetsEditor.setContentProvider(new ListEditorContentProvider<ReportSet>());
    reportSetsEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof ReportSet) {
          ReportSet reportSet = (ReportSet) element;
          String id = reportSet.getId();
          return id == null || id.length() == 0 ? "?" : id;
        }
        return ""; //$NON-NLS-1$
      }

      public Image getImage(Object element) {
        // TODO add icon for report set
        return null;
      }
    });

    reportSetsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<ReportSet> selection = reportSetsEditor.getSelection();
        updateReportSetDetails(selection.size() == 1 ? selection.get(0) : null);
      }
    });

    reportSetsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportPlugin == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        boolean reportSetsCreated = false;

        ReportSet reportSet = PomFactory.eINSTANCE.createReportSet();
        Command addReportSet = AddCommand.create(editingDomain, currentReportPlugin, POM_PACKAGE.getReportPlugin_ReportSets(),
            reportSet);
        compoundCommand.append(addReportSet);
        editingDomain.getCommandStack().execute(compoundCommand);

        if(reportSetsCreated) {
          updateReportPluginDetails(currentReportPlugin);
        }
        reportSetsEditor.setSelection(Collections.singletonList(reportSet));
      }
    });

    reportSetsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportPlugin == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();


        List<ReportSet> reportSetList = reportSetsEditor.getSelection();
        for(ReportSet reportSet : reportSetList) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentReportPlugin, POM_PACKAGE
              .getReportPlugin_ReportSets(), reportSet);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateReportPluginDetails(currentReportPlugin);
      }
    });

    reportSetsEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        if(element instanceof ReportSet) {
          String id = ((ReportSet) element).getId();
          return id == null ? "" : id; //$NON-NLS-1$
        }
        return element;
      }

      public void modify(Object element, String property, Object value) {
        EditingDomain editingDomain = editorPage.getEditingDomain();
        if(!value.equals(currentReportSet.getId())) {
          Command command = SetCommand.create(editingDomain, currentReportSet, POM_PACKAGE.getReportSet_Id(), value);
          editingDomain.getCommandStack().execute(command);
        }
      }
    });

    reportSetDetailsSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    reportSetDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    reportSetDetailsSection.setText(Messages.ReportingComposite_section_reportSetReports);

    Composite reportSetDetailsComposite = toolkit.createComposite(reportSetDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 1;
    reportSetDetailsComposite.setLayout(gridLayout);
    reportSetDetailsSection.setClient(reportSetDetailsComposite);
    toolkit.paintBordersFor(reportSetDetailsComposite);

    reportsEditor = new ListEditorComposite<String>(reportSetDetailsComposite, SWT.NONE);
    reportsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.paintBordersFor(reportsEditor);
    toolkit.adapt(reportsEditor);

    reportsEditor.setContentProvider(new ListEditorContentProvider<String>());
    reportsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_REPORT));

    reportsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportSet == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        Command addReport = AddCommand.create(editingDomain, currentReportSet, POM_PACKAGE.getReportSet_Reports(), "?");
        compoundCommand.append(addReport);
        editingDomain.getCommandStack().execute(compoundCommand);

        reportsEditor.refresh();
      }
    });

    reportsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if(currentReportSet == null) {
          return;
        }

        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = editorPage.getEditingDomain();

        List<String> reportList = reportsEditor.getSelection();
        for(String report : reportList) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentReportSet, POM_PACKAGE.getReportSet_Reports(),
              report);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });

    reportsEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        return element;
      }

      public void modify(Object element, String property, Object value) {
        EditingDomain editingDomain = editorPage.getEditingDomain();
        Command command = SetCommand.create(editingDomain, currentReportSet, POM_PACKAGE
            .getReportSet_Reports(), value, reportsEditor.getViewer().getTable().getSelectionIndex());
        editingDomain.getCommandStack().execute(command);
      }
    });

    Composite reportSetConfigureComposite = toolkit.createComposite(reportSetDetailsComposite, SWT.NONE);
    reportSetConfigureComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
    GridLayout reportSetConfigureCompositeLayout = new GridLayout();
    reportSetConfigureCompositeLayout.numColumns = 2;
    reportSetConfigureCompositeLayout.marginWidth = 0;
    reportSetConfigureCompositeLayout.marginHeight = 0;
    reportSetConfigureComposite.setLayout(reportSetConfigureCompositeLayout);
    toolkit.paintBordersFor(reportSetConfigureComposite);

    reportSetInheritedButton = toolkit.createButton(reportSetConfigureComposite, Messages.ReportingComposite_btnInherited, SWT.CHECK);
    reportSetInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    reportSetConfigureButton = toolkit.createHyperlink(reportSetConfigureComposite, Messages.ReportingComposite_link_Configuration, SWT.NONE);
    reportSetConfigureButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
    reportSetConfigureButton.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        if(currentReportSet != null) {
          EObject element = currentReportSet.getConfiguration();
          editorPage.getPomEditor().showInSourceEditor(element == null ? currentReportSet : element);
        }
      }
    });

    // XXX implement editor actions
  }

  protected void updateReportPluginDetails(ReportPlugin reportPlugin) {
    currentReportPlugin = reportPlugin;

    if(editorPage != null) {
      editorPage.removeNotifyListener(groupIdText);
      editorPage.removeNotifyListener(artifactIdText);
      editorPage.removeNotifyListener(versionText);
      editorPage.removeNotifyListener(pluginInheritedButton);
    }

    if(editorPage == null || reportPlugin == null) {
      FormUtils.setEnabled(pluginDetailsSection, false);
      FormUtils.setEnabled(reportSetsSection, false);
      reportPluginSelectAction.setEnabled(false);
      openWebPageAction.setEnabled(false);

      setText(groupIdText, ""); //$NON-NLS-1$
      setText(artifactIdText, ""); //$NON-NLS-1$
      setText(versionText, ""); //$NON-NLS-1$

      pluginInheritedButton.setSelection(false);

      reportSetsEditor.setInput(null);

      updateReportSetDetails(null);

      return;
    }

    FormUtils.setEnabled(pluginDetailsSection, true);
    FormUtils.setEnabled(reportSetsSection, true);
    FormUtils.setReadonly(pluginDetailsSection, editorPage.isReadOnly());
    FormUtils.setReadonly(reportSetsSection, editorPage.isReadOnly());
    reportPluginSelectAction.setEnabled(!editorPage.isReadOnly());
    openWebPageAction.setEnabled(true);

    setText(groupIdText, reportPlugin.getGroupId());
    setText(artifactIdText, reportPlugin.getArtifactId());
    setText(versionText, reportPlugin.getVersion());

    pluginInheritedButton.setSelection(Boolean.parseBoolean(reportPlugin.getInherited()));

    ValueProvider<ReportPlugin> provider = new ValueProvider.DefaultValueProvider<ReportPlugin>(reportPlugin);
    editorPage.setModifyListener(groupIdText, provider, POM_PACKAGE.getReportPlugin_GroupId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(artifactIdText, provider, POM_PACKAGE.getReportPlugin_ArtifactId(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(versionText, provider, POM_PACKAGE.getReportPlugin_Version(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(pluginInheritedButton, provider, POM_PACKAGE.getReportPlugin_Inherited(), "false");
    editorPage.registerListeners();

    reportSetsEditor.setInput(reportPlugin.getReportSets());

    updateReportSetDetails(null);
  }

  protected void updateReportSetDetails(ReportSet reportSet) {
    if(editorPage != null) {
      editorPage.removeNotifyListener(reportSetInheritedButton);
    }

    currentReportSet = reportSet;

    if(reportSet == null || editorPage == null) {
      FormUtils.setEnabled(reportSetDetailsSection, false);
      reportSetInheritedButton.setSelection(false);
      reportsEditor.setInput(null);
      return;
    }

    FormUtils.setEnabled(reportSetDetailsSection, true);
    FormUtils.setReadonly(reportSetDetailsSection, editorPage.isReadOnly());

    reportSetInheritedButton.setSelection(Boolean.parseBoolean(reportSet.getInherited()));
    ValueProvider<ReportSet> provider = new ValueProvider.DefaultValueProvider<ReportSet>(reportSet);
    editorPage.setModifyListener(reportSetInheritedButton, provider, POM_PACKAGE.getReportSet_Inherited(), "false");
    editorPage.registerListeners();

    reportsEditor.setInput(reportSet.getReports());
  }

  public void loadData(MavenPomEditorPage editorPage, ValueProvider<Reporting> reportingProvider) {
    this.editorPage = editorPage;
    this.reportingProvider = reportingProvider;
    
//    reportPluginAddAction.setEnabled(!parent.getPomEditor().isReadOnly());

    updateContent(reportingProvider.getValue());
  }

  void updateContent(Reporting reporting) {
    if(editorPage != null) {
      editorPage.removeNotifyListener(outputFolderText);
      editorPage.removeNotifyListener(excludeDefaultsButton);
    }
    
    if(reporting == null) {
      setText(outputFolderText,""); //$NON-NLS-1$
      setButton(excludeDefaultsButton, false);
      reportPluginsEditor.setInput(null);
    } else {
      setText(outputFolderText,reporting.getOutputDirectory());
      setButton(excludeDefaultsButton, "true".equals(reporting.getExcludeDefaults()));
      reportPluginsEditor.setInput(reporting.getPlugins());
    }
    
    editorPage.setModifyListener(outputFolderText, reportingProvider, POM_PACKAGE.getReporting_OutputDirectory(), ""); //$NON-NLS-1$
    editorPage.setModifyListener(excludeDefaultsButton, reportingProvider, POM_PACKAGE.getReporting_ExcludeDefaults(), "false");
    editorPage.registerListeners();
    
    updateReportPluginDetails(null);
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    Object feature = notification.getFeature();

    if (feature == PomPackage.Literals.MODEL__REPORTING) {
      updateContent(reportingProvider.getValue());
    }
    
    if(object instanceof Reporting || feature == PomPackage.Literals.REPORTING__PLUGINS) {
      reportPluginsEditor.refresh();
    } else if(object instanceof ReportPlugin) {
      reportPluginsEditor.refresh();
      
      Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
      if(object == currentReportPlugin && (notificationObject == null || notificationObject instanceof ReportPlugin)) {
        updateReportPluginDetails((ReportPlugin) notificationObject);
      }
    } else if(feature == PomPackage.Literals.REPORT_PLUGIN__REPORT_SETS || object instanceof ReportSet) {
      reportSetsEditor.refresh();
    } else if(feature == PomPackage.Literals.REPORT_SET__REPORTS) {
      reportsEditor.refresh();
    }
  }

  void createReportingPlugin(String groupId, String artifactId, String version) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = editorPage.getEditingDomain();

    boolean reportsCreated = false;

    Reporting reporting = reportingProvider.getValue();
    if(reporting == null) {
      reporting = reportingProvider.create(editingDomain, compoundCommand);
      reportsCreated = true;
    }


    ReportPlugin reportPlugin = PomFactory.eINSTANCE.createReportPlugin();
    reportPlugin.setGroupId(groupId);
    reportPlugin.setArtifactId(artifactId);
    reportPlugin.setVersion(version);
    
    Command addReportPlugin = AddCommand.create(editingDomain, reporting,
        POM_PACKAGE.getReporting_Plugins(), reportPlugin);
    compoundCommand.append(addReportPlugin);
    
    editingDomain.getCommandStack().execute(compoundCommand);

    if(reportsCreated) {
      updateContent(reporting);
    } else {
      updateReportPluginDetails(reportPlugin);
    }
    reportPluginsEditor.setSelection(Collections.singletonList(reportPlugin));
    groupIdText.setFocus();
  }

  public void setSearchControl(SearchControl searchControl) {
    if(this.searchControl!=null) {
      return;
    }
    
    this.searchMatcher = new SearchMatcher(searchControl);
    this.searchFilter = new PluginFilter(searchMatcher);
    this.searchControl = searchControl;
    this.searchControl.getSearchText().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        selectPlugins(reportPluginsEditor, reportingProvider);
        updateReportPluginDetails(null);
      }

      private void selectPlugins(ListEditorComposite<ReportPlugin> editor, ValueProvider<Reporting> provider) {
        List<ReportPlugin> plugins = new ArrayList<ReportPlugin>();
        Reporting value = provider.getValue();
        if(value != null) {
          for(ReportPlugin p : value.getPlugins()) {
            if(searchMatcher.isMatchingArtifact(p.getGroupId(), p.getArtifactId())) {
              plugins.add(p);
            }
          }
        }
        editor.setSelection(plugins);
        editor.refresh();
      }
    });
    //we add filter here as the default behaviour is to filter..
    TableViewer viewer = reportPluginsEditor.getViewer();
    viewer.addFilter(searchFilter);
    
  }
  
  final class ReportPluginsLabelProvider extends LabelProvider {
  
    private boolean showGroupId = true;

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }
    
    public String getText(Object element) {
      if(element instanceof ReportPlugin) {
        ReportPlugin reportPlugin = (ReportPlugin) element;

        String groupId = reportPlugin.getGroupId();
        String artifactId = reportPlugin.getArtifactId();
        String version = reportPlugin.getVersion();

        String label = ""; //$NON-NLS-1$
        
        if(showGroupId) {
          label = (groupId == null ? "?" : groupId) + " : ";
        }
        
        label += artifactId == null ? "?" : artifactId;
        
        if(version != null) {
          label += " : " + version;
        }
        
        return label;
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      return MavenEditorImages.IMG_PLUGIN;
    }

  }
  
}
