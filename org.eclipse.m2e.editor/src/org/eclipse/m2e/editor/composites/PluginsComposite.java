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
import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.actions.OpenUrlAction;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.plugins.DefaultPluginConfigurationEditor;
import org.eclipse.m2e.editor.plugins.IPluginConfigurationExtension;
import org.eclipse.m2e.editor.plugins.PluginExtensionDescriptor;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.SearchControl;
import org.eclipse.m2e.editor.pom.SearchMatcher;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Dependency;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PluginExecution;
import org.eclipse.m2e.model.edit.pom.PluginManagement;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.ReportPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eugene Kuleshov
 */
public class PluginsComposite extends Composite{
  private static final Logger log = LoggerFactory.getLogger(PluginsComposite.class);

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  public static final String EXTENSION_CONFIGURATION_EDITOR = "org.eclipse.m2e.editor.plugins.configurationEditorContribution"; //$NON-NLS-1$
  public static final String ELEMENT_CONFIGURATION_EDITOR = "editContributor"; //$NON-NLS-1$
  
  MavenPomEditorPage parentEditorPage;
  
  // controls
  CCombo executionPhaseCombo;
  Text executionIdText;
  Hyperlink pluginExecutionConfigurationHyperlink;
  Button executionInheritedButton;
  FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  ListEditorComposite<Plugin> pluginsEditor;
  ListEditorComposite<Plugin> pluginManagementEditor;
  
  Text groupIdText;
  Text artifactIdText;
  Text versionText;
  Section pluginDetailsSection;
  Button pluginExtensionsButton;
  Button pluginInheritedButton;

  ListEditorComposite<Dependency> pluginDependenciesEditor;
  ListEditorComposite<String> goalsEditor;
  ListEditorComposite<PluginExecution> pluginExecutionsEditor;

  Section pluginConfigurationSection;
  IPluginConfigurationExtension configurationEditor;
  IAction openConfigurationAction;

  Section pluginExecutionSection;
  Section pluginExecutionsSection;
  Section pluginDependenciesSection;

  Button pluginSelectButton;

  Action pluginSelectAction;
  
  PluginSelectAction pluginAddAction;
  
  PluginSelectAction pluginManagementAddAction;
  
  Action openWebPageAction;

  ViewerFilter searchFilter;
  
  SearchControl searchControl;
  
  SearchMatcher searchMatcher;
  
  // model
  
  Plugin currentPlugin;
  PluginExecution currentPluginExecution;

  ValueProvider<BuildBase> buildProvider;

  ValueProvider<PluginManagement> pluginManagementProvider;

  Map<String,PluginExtensionDescriptor> pluginConfigurators;
  IPluginConfigurationExtension defaultConfigurationEditor;
  
  boolean changingSelection = false;

  
  public PluginsComposite(Composite composite, MavenPomEditorPage page, int style) {
    super(composite, style);
    this.parentEditorPage = page;
    GridLayout gridLayout = new GridLayout(1, false);
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);
  
    SashForm horizontalSashForm = new SashForm(this, SWT.NONE);
    GridData gd_horizontalSashForm = new GridData(SWT.FILL, SWT.FILL, true, true);
    horizontalSashForm.setLayoutData(gd_horizontalSashForm);
    toolkit.adapt(horizontalSashForm, true, true);

    SashForm verticalSashForm = new SashForm(horizontalSashForm, SWT.VERTICAL);
    toolkit.adapt(verticalSashForm, true, true);
    
    loadPluginConfigurators();
  
    createPluginsSection(verticalSashForm);
    createPluginManagementSection(verticalSashForm);
    
    verticalSashForm.setWeights(new int[] {1, 1});

    createPluginDetailsSection(horizontalSashForm);
    horizontalSashForm.setWeights(new int[] {10, 15 });

    updatePluginDetails(null);
  }

  private void createPluginsSection(SashForm verticalSashForm) {
    Section pluginsSection = toolkit.createSection(verticalSashForm, ExpandableComposite.TITLE_BAR | ExpandableComposite.COMPACT);
    pluginsSection.setText(Messages.PluginsComposite_section_Plugins);
  
    pluginsEditor = new ListEditorComposite<Plugin>(pluginsSection, SWT.NONE, true);
    pluginsSection.setClient(pluginsEditor);
    toolkit.adapt(pluginsEditor);
    toolkit.paintBordersFor(pluginsEditor);
    
    final PluginLabelProvider labelProvider = new PluginLabelProvider();
    pluginsEditor.setLabelProvider(labelProvider);
    pluginsEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
  
    pluginsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Plugin> selection = pluginsEditor.getSelection();
        updatePluginDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginManagementEditor.setSelection(Collections.<Plugin>emptyList());
          changingSelection = false;
        }
      }
    });
    
    pluginsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createPlugin(pluginsEditor, buildProvider, POM_PACKAGE.getBuildBase_Plugins(), null, null, null);
      }
    });
    
    pluginsEditor.setAddButtonListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(), //
        Messages.PluginsComposite_searchDialog_selectPlugin, parentEditorPage.getPomEditor().getMavenProject(), parentEditorPage.getProject(), false);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createPlugin(pluginsEditor, buildProvider, POM_PACKAGE.getBuildBase_Plugins(), af.group, af.artifact, af.version);
          }
        }
      }
    });
    
    pluginsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parentEditorPage.getEditingDomain();
  
        List<Plugin> list = pluginsEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, buildProvider.getValue(), //
              POM_PACKAGE.getBuildBase_Plugins(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });

    pluginAddAction = new PluginSelectAction(pluginsEditor, POM_PACKAGE.getBuildBase_Plugins());

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    
    if(pluginConfigurators.size() > 0) {
      toolBarManager.add(pluginAddAction.getItem());
      toolBarManager.add(new Separator());
    }
    
    toolBarManager.add(new Action(Messages.PluginsComposite_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(false);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        pluginsEditor.getViewer().refresh();
      }
    });
    
    toolBarManager.add(new Action(Messages.PluginsComposite_action_Filter, MavenEditorImages.FILTER) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = pluginsEditor.getViewer();
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
    
    Composite toolbarComposite = toolkit.createComposite(pluginsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginsSection.setTextClient(toolbarComposite);
  }

  private void createPluginManagementSection(SashForm verticalSashForm) {
    Section pluginManagementSection = toolkit.createSection(verticalSashForm, ExpandableComposite.TITLE_BAR);
    pluginManagementSection.setText(Messages.PluginsComposite_section_PluginManagent);
  
    pluginManagementEditor = new ListEditorComposite<Plugin>(pluginManagementSection, SWT.NONE, true);
    pluginManagementSection.setClient(pluginManagementEditor);
    toolkit.adapt(pluginManagementEditor);
    toolkit.paintBordersFor(pluginManagementEditor);

    final PluginLabelProvider labelProvider = new PluginLabelProvider();
    pluginManagementEditor.setLabelProvider(labelProvider);
    pluginManagementEditor.setContentProvider(new ListEditorContentProvider<Plugin>());
  
    pluginManagementEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Plugin> selection = pluginManagementEditor.getSelection();
        updatePluginDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          pluginsEditor.setSelection(Collections.<Plugin>emptyList());
          changingSelection = false;
        }
      }
    });
    
    pluginManagementEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createPlugin(pluginManagementEditor, pluginManagementProvider, POM_PACKAGE.getPluginManagement_Plugins(), null, null, null);
      }
    });

    
    pluginManagementEditor.setAddButtonListener(new SelectionAdapter(){
      public void widgetSelected(SelectionEvent e){
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(
            getShell(), //
            Messages.PluginsComposite_searchDialog_selectPlugin, parentEditorPage.getPomEditor().getMavenProject(),
            parentEditorPage.getProject(), true);
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createPlugin(pluginManagementEditor, pluginManagementProvider, POM_PACKAGE.getPluginManagement_Plugins(),
                af.group, af.artifact, af.version);
          }
        }
      }
    });

    pluginManagementEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parentEditorPage.getEditingDomain();
  
        List<Plugin> list = pluginManagementEditor.getSelection();
        for(Plugin plugin : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, //
              pluginManagementProvider.getValue(), POM_PACKAGE.getPluginManagement_Plugins(), plugin);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updatePluginDetails(null);
      }
    });
    
    pluginManagementAddAction = new PluginSelectAction(pluginManagementEditor, POM_PACKAGE.getPluginManagement_Plugins());

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);

    if(pluginConfigurators.size() > 0) {
      toolBarManager.add(pluginManagementAddAction.getItem());
      toolBarManager.add(new Separator());
    }
    
    toolBarManager.add(new Action(Messages.PluginsComposite_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(false);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        pluginManagementEditor.getViewer().refresh();
      }
    });
    
    toolBarManager.add(new Action(Messages.PluginsComposite_action_Filter, MavenEditorImages.FILTER) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        TableViewer viewer = pluginManagementEditor.getViewer();
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
    
    Composite toolbarComposite = toolkit.createComposite(pluginManagementSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginManagementSection.setTextClient(toolbarComposite);
  }

  private void createPluginDetailsSection(Composite horizontalSashForm) {
      Composite detailsComposite = toolkit.createComposite(horizontalSashForm, SWT.NONE);
      GridLayout detailsCompositeLayout = new GridLayout();
      detailsCompositeLayout.marginWidth = 0;
      detailsCompositeLayout.marginHeight = 0;
      detailsComposite.setLayout(detailsCompositeLayout);
      toolkit.paintBordersFor(detailsComposite);
      
      pluginDetailsSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
      pluginDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
      pluginDetailsSection.setText(Messages.PluginsComposite_section_pluginDetails);
    
      Composite pluginDetailsComposite = toolkit.createComposite(pluginDetailsSection, SWT.NONE);
      GridLayout pluginDetailsLayout = new GridLayout(3, false);
      pluginDetailsLayout.marginWidth = 1;
      pluginDetailsLayout.marginHeight = 2;
      pluginDetailsComposite.setLayout(pluginDetailsLayout);
      toolkit.paintBordersFor(pluginDetailsComposite);
      pluginDetailsSection.setClient(pluginDetailsComposite);
    
      toolkit.createLabel(pluginDetailsComposite, Messages.PluginsComposite_lblGroupId, SWT.NONE);
    
      groupIdText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
      GridData gd_groupIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd_groupIdText.horizontalIndent = 4;
      groupIdText.setLayoutData(gd_groupIdText);
      groupIdText.setData("name", "groupIdText"); //$NON-NLS-1$ //$NON-NLS-2$
      parentEditorPage.createEvaluatorInfo(groupIdText);
      ProposalUtil.addGroupIdProposal(parentEditorPage.getProject(), groupIdText, Packaging.PLUGIN);
      M2EUIUtils.addRequiredDecoration(groupIdText);

      Hyperlink artifactIdHyperlink = toolkit.createHyperlink(pluginDetailsComposite, Messages.PluginsComposite_lblArtifactId, SWT.NONE);
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
      GridData gd_artifactIdText = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd_artifactIdText.horizontalIndent = 4;
      artifactIdText.setLayoutData(gd_artifactIdText);
      artifactIdText.setData("name", "artifactIdText"); //$NON-NLS-1$ //$NON-NLS-2$
      ProposalUtil.addArtifactIdProposal(parentEditorPage.getProject(), groupIdText, artifactIdText, Packaging.PLUGIN);
      M2EUIUtils.addRequiredDecoration(artifactIdText);
      parentEditorPage.createEvaluatorInfo(artifactIdText);
    
      Label label = toolkit.createLabel(pluginDetailsComposite, Messages.PluginsComposite_lblVersion, SWT.NONE);
      label.setLayoutData(new GridData());
    
      versionText = toolkit.createText(pluginDetailsComposite, null, SWT.NONE);
      GridData versionTextData = new GridData(SWT.LEFT, SWT.CENTER, true, false);
      versionTextData.horizontalIndent = 4;
      versionTextData.widthHint = 200;
      versionText.setLayoutData(versionTextData);
      versionText.setData("name", "versionText"); //$NON-NLS-1$ //$NON-NLS-2$
      ProposalUtil.addVersionProposal(parentEditorPage.getProject(), parentEditorPage.getPomEditor().getMavenProject(), groupIdText, artifactIdText, versionText, Packaging.PLUGIN);
      parentEditorPage.createEvaluatorInfo(versionText);
  
  //    pluginSelectButton = toolkit.createButton(pluginDetailsComposite, "Select...", SWT.NONE);
  //    pluginSelectButton.addSelectionListener(new SelectionAdapter() {
  //      public void widgetSelected(SelectionEvent e) {
  //        Set<Dependency> artifacts = Collections.emptySet();
  //        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(parent.getEditorSite().getShell(),
  //            "Add Plugin", IndexManager.SEARCH_PLUGIN, artifacts);
  //        if(dialog.open() == Window.OK) {
  //          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
  //          if(af != null) {
  //            groupIdText.setText(nvl(af.group));
  //            artifactIdText.setText(nvl(af.artifact));
  //            versionText.setText(nvl(af.version));
  //          }
  //        }
  //      }
  //    });
      
      pluginSelectAction = new Action(Messages.PluginsComposite_action_selectPlugin, MavenEditorImages.SELECT_PLUGIN) {
        public void run() {
          MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchPluginDialog(getShell(), //
              Messages.PluginsComposite_searchDialog_selectPlugin, parentEditorPage.getPomEditor().getMavenProject(), parentEditorPage.getProject(), false); //is false correct here?
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
      pluginSelectAction.setEnabled(false);
  
      openWebPageAction = new Action(Messages.PluginsComposite_action_openWeb, MavenEditorImages.WEB_PAGE) {
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
      
      ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
      toolBarManager.add(pluginSelectAction);
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
    
      Composite composite = new Composite(pluginDetailsComposite, SWT.NONE);
      GridLayout compositeLayout = new GridLayout();
      compositeLayout.marginWidth = 0;
      compositeLayout.marginHeight = 0;
      compositeLayout.numColumns = 3;
      composite.setLayout(compositeLayout);
      composite.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 1));
      toolkit.adapt(composite);
    
      pluginExtensionsButton = toolkit.createButton(composite, Messages.PluginsComposite_btnExtensions, SWT.CHECK);
      pluginExtensionsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    
      pluginInheritedButton = toolkit.createButton(composite, Messages.PluginsComposite_btnInherited, SWT.CHECK);
      pluginInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    
      pluginDetailsComposite.setTabList(new Control[] {groupIdText, artifactIdText, versionText, composite});
      
      createConfigurationSection(detailsComposite);
  
      SashForm executionSash = new SashForm(detailsComposite, SWT.NONE);
      executionSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
      GridLayout executionSashLayout = new GridLayout();
      executionSashLayout.horizontalSpacing = 3;
      executionSashLayout.marginWidth = 0;
      executionSashLayout.marginHeight = 0;
      executionSashLayout.numColumns = 2;
      executionSash.setLayout(executionSashLayout);
      toolkit.adapt(executionSash);
      
      pluginExecutionsSection = toolkit.createSection(executionSash,
          ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
      GridData gd_pluginExecutionsSection = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd_pluginExecutionsSection.minimumHeight = 50;
      pluginExecutionsSection.setLayoutData(gd_pluginExecutionsSection);
      pluginExecutionsSection.setText(Messages.PluginsComposite_section_Executions);
  
      pluginExecutionsEditor = new ListEditorComposite<PluginExecution>(pluginExecutionsSection, SWT.NONE);
      pluginExecutionsSection.setClient(pluginExecutionsEditor);
      toolkit.adapt(pluginExecutionsEditor);
      toolkit.paintBordersFor(pluginExecutionsEditor);
      pluginExecutionsEditor.setContentProvider(new ListEditorContentProvider<PluginExecution>());
      pluginExecutionsEditor.setLabelProvider(new LabelProvider() {
        public String getText(Object element) {
          if(element instanceof PluginExecution) {
            PluginExecution pluginExecution = (PluginExecution) element;
            String label = isEmpty(pluginExecution.getId()) ? "?" : pluginExecution.getId();
            if(pluginExecution.getPhase()!=null) {
              label +=  " : " + pluginExecution.getPhase();
            }
            return label;
          }
          return ""; //$NON-NLS-1$
        }
        public Image getImage(Object element) {
          return MavenEditorImages.IMG_EXECUTION;
        }
      });
      
      pluginExecutionsEditor.addSelectionListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          List<PluginExecution> selection = pluginExecutionsEditor.getSelection();
          updatePluginExecution(selection.size()==1 ? selection.get(0) : null);
        }
      });
      
      pluginExecutionsEditor.setCreateButtonListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parentEditorPage.getEditingDomain();
          
          
          PluginExecution pluginExecution = PomFactory.eINSTANCE.createPluginExecution();
          Command command = AddCommand.create(editingDomain, currentPlugin, POM_PACKAGE.getPlugin_Executions(), pluginExecution);
          compoundCommand.append(command);
          
          editingDomain.getCommandStack().execute(compoundCommand);
          
          pluginExecutionsEditor.setSelection(Collections.singletonList(pluginExecution));
          updatePluginExecution(pluginExecution);
          executionIdText.setFocus();
        }
      });
      
      pluginExecutionsEditor.setRemoveButtonListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parentEditorPage.getEditingDomain();
   
          List<PluginExecution> list = pluginExecutionsEditor.getSelection();
          Command removeCommand = RemoveCommand.create(editingDomain, //
              currentPlugin, POM_PACKAGE.getPlugin_Executions(), list);
          compoundCommand.append(removeCommand);
          
          editingDomain.getCommandStack().execute(compoundCommand);
          updatePluginExecution(null);
        }
      });
      
      pluginExecutionSection = toolkit.createSection(executionSash,
          ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
      GridData gd_pluginExecutionSection = new GridData(SWT.FILL, SWT.CENTER, true, false);
      gd_pluginExecutionSection.minimumHeight = 50;
      pluginExecutionSection.setLayoutData(gd_pluginExecutionSection);
      pluginExecutionSection.setText(Messages.PluginsComposite_section_executionDetails);
      new SectionExpansionAdapter(new Section[]{pluginExecutionSection, pluginExecutionsSection});

      Composite executionComposite = toolkit.createComposite(pluginExecutionSection, SWT.NONE);
      GridLayout executionCompositeLayout = new GridLayout(2, false);
      executionCompositeLayout.marginWidth = 2;
      executionCompositeLayout.marginHeight = 2;
      executionComposite.setLayout(executionCompositeLayout);
      pluginExecutionSection.setClient(executionComposite);
      toolkit.paintBordersFor(executionComposite);
  
      toolkit.createLabel(executionComposite, Messages.PluginsComposite_lblId, SWT.NONE);
  
      executionIdText = toolkit.createText(executionComposite, null, SWT.NONE);
      executionIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      parentEditorPage.createEvaluatorInfo(executionIdText);
  
      toolkit.createLabel(executionComposite, Messages.PluginsComposite_lblPhase, SWT.NONE);
  
      executionPhaseCombo = new CCombo(executionComposite, SWT.FLAT);
      executionPhaseCombo.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
      executionPhaseCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      executionPhaseCombo.setItems(new String[] { //
          "pre-clean", // Clean Lifecycle //$NON-NLS-1$
          "clean", // //$NON-NLS-1$
          "post-clean", // //$NON-NLS-1$
          "validate", // Default Lifecycle //$NON-NLS-1$
          "generate-sources", // //$NON-NLS-1$
          "process-sources", // //$NON-NLS-1$
          "generate-resources", // //$NON-NLS-1$
          "process-resources", // //$NON-NLS-1$
          "compile", // //$NON-NLS-1$
          "process-classes", // //$NON-NLS-1$
          "generate-test-sources", // //$NON-NLS-1$
          "process-test-sources", // //$NON-NLS-1$
          "generate-test-resources", // //$NON-NLS-1$
          "process-test-resources", // //$NON-NLS-1$
          "test-compile", // //$NON-NLS-1$
          "process-test-classes", // //$NON-NLS-1$
          "test", // //$NON-NLS-1$
          "prepare-package", // //$NON-NLS-1$
          "package", // //$NON-NLS-1$
          "pre-integration-test", // //$NON-NLS-1$
          "integration-test", // //$NON-NLS-1$
          "post-integration-test", // //$NON-NLS-1$
          "verify", // //$NON-NLS-1$
          "install", // //$NON-NLS-1$
          "deploy", // //$NON-NLS-1$
          "pre-site", // //$NON-NLS-1$
          "site", // Site Lifecycle //$NON-NLS-1$
          "post-site", // //$NON-NLS-1$
          "site-deploy"}); //$NON-NLS-1$
      toolkit.adapt(executionPhaseCombo, true, true);
  
      Label goalsLabel = toolkit.createLabel(executionComposite, Messages.PluginsComposite_lblGoals, SWT.NONE);
      goalsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
  
      goalsEditor = new ListEditorComposite<String>(executionComposite, SWT.NONE);
      goalsEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      toolkit.paintBordersFor(goalsEditor);
      goalsEditor.setContentProvider(new ListEditorContentProvider<String>());
      goalsEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_GOAL));
      
      goalsEditor.setCreateButtonListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parentEditorPage.getEditingDomain();
          
          String goal = "?";
          Command command = AddCommand.create(editingDomain, currentPluginExecution, POM_PACKAGE.getPluginExecution_Goals(), goal);
          compoundCommand.append(command);
          
          editingDomain.getCommandStack().execute(compoundCommand);
          
          goalsEditor.setSelection(Collections.singletonList(goal));
        }
      });
      
      goalsEditor.setRemoveButtonListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          CompoundCommand compoundCommand = new CompoundCommand();
          EditingDomain editingDomain = parentEditorPage.getEditingDomain();
   
          List<String> list = goalsEditor.getSelection();
          for(String goal : list) {
            Command removeCommand = RemoveCommand.create(editingDomain, //
                currentPluginExecution, POM_PACKAGE.getPluginExecution_Goals(), goal);
            compoundCommand.append(removeCommand);
          }
          
          editingDomain.getCommandStack().execute(compoundCommand);
        }
      });
      
      goalsEditor.setCellModifier(new ICellModifier() {
        public boolean canModify(Object element, String property) {
          return true;
        }
   
        public Object getValue(Object element, String property) {
          return element;
        }
   
        public void modify(Object element, String property, Object value) {
          int n = goalsEditor.getViewer().getTable().getSelectionIndex();
          if(!value.equals(currentPluginExecution.getGoals().get(n))) {
            EditingDomain editingDomain = parentEditorPage.getEditingDomain();
            Command command = SetCommand.create(editingDomain, currentPluginExecution, //
                POM_PACKAGE.getPluginExecution_Goals(), value, n);
            editingDomain.getCommandStack().execute(command);
    
            // currentPluginExecution.getGoals().getGoal().set(n, (String) value);
            goalsEditor.update();
          }
        }
      });
  
      Composite executionConfigureComposite = new Composite(executionComposite, SWT.NONE);
      executionConfigureComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      executionConfigureComposite.setLayout(gridLayout);
      toolkit.adapt(executionConfigureComposite);
  
      executionInheritedButton = toolkit.createButton(executionConfigureComposite, Messages.PluginsComposite_btnInherited, SWT.CHECK);
      executionInheritedButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
  
      pluginExecutionConfigurationHyperlink = toolkit.createHyperlink(executionConfigureComposite, Messages.PluginsComposite_linkConfiguration, SWT.NONE);
      pluginExecutionConfigurationHyperlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
      pluginExecutionConfigurationHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
        public void linkActivated(HyperlinkEvent e) {
          EObject element = currentPluginExecution.getConfiguration();
          parentEditorPage.getPomEditor().showInSourceEditor(element==null ? currentPluginExecution : element);
        }
      });
  
      pluginDependenciesSection = toolkit.createSection(detailsComposite, 
          ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
      GridData pluginDependenciesSectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
      pluginDependenciesSectionData.minimumHeight = 50;
      pluginDependenciesSection.setLayoutData(pluginDependenciesSectionData);
      pluginDependenciesSection.setText(Messages.PluginsComposite_section_PluginDependencies);
  
      pluginDependenciesEditor = new ListEditorComposite<Dependency>(pluginDependenciesSection, SWT.NONE);
      pluginDependenciesSection.setClient(pluginDependenciesEditor);
      toolkit.adapt(pluginDependenciesEditor);
      toolkit.paintBordersFor(pluginDependenciesEditor);
      pluginDependenciesEditor.setContentProvider(new ListEditorContentProvider<Dependency>());
      pluginDependenciesEditor.setLabelProvider(new DependencyLabelProvider());
      
      pluginDependenciesEditor.setReadOnly(true);
      
      // XXX implement plugin dependency editor actions and UI
      
      FormUtils.setEnabled(pluginDependenciesEditor, false);
    }

  private void createConfigurationSection(Composite detailsComposite) {
    pluginConfigurationSection = toolkit.createSection(detailsComposite,
        ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
    pluginConfigurationSection.setText(Messages.PluginsComposite_section_configuration);
    pluginConfigurationSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    pluginConfigurationSection.setEnabled(false);
    
    defaultConfigurationEditor = new DefaultPluginConfigurationEditor();
    setConfigurationEditor(defaultConfigurationEditor);
    
    openConfigurationAction = new Action(Messages.PluginsComposite_action_openXml, MavenEditorImages.ELEMENT_OBJECT) {
      public void run() {
        EObject element = currentPlugin.getConfiguration();
        parentEditorPage.getPomEditor().showInSourceEditor(element==null ? currentPlugin : element);
      }      
    };
    openConfigurationAction.setEnabled(false);
    
    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(openConfigurationAction);
    
    Composite toolbarComposite = toolkit.createComposite(pluginConfigurationSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    pluginConfigurationSection.setTextClient(toolbarComposite);
  }

  private IPluginConfigurationExtension findConfigurationEditor(String groupId, String artifactId) {
    String ga = groupId + ':' + artifactId;
    PluginExtensionDescriptor descriptor = pluginConfigurators.get(ga);
    return descriptor == null || descriptor.getExtension() == null ?
        defaultConfigurationEditor : descriptor.getExtension();
  }

  private void setConfigurationEditor(IPluginConfigurationExtension editor) {
    if(configurationEditor == editor) {
      return;
    }
    
    if(configurationEditor != null) {
      configurationEditor.cleanup();
    }
    
    boolean expanded = pluginConfigurationSection.isExpanded();
    if(expanded) {
      pluginConfigurationSection.setExpanded(false);
    }

    Control control = pluginConfigurationSection.getClient();
    if(control != null) {
      control.dispose();
    }
    configurationEditor = editor;
    configurationEditor.setPomEditor(parentEditorPage);
    
    Composite configurationComposite = configurationEditor.createComposite(pluginConfigurationSection);
    configurationComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    toolkit.adapt(configurationComposite);
    pluginConfigurationSection.setClient(configurationComposite);
    toolkit.paintBordersFor(configurationComposite);
    pluginConfigurationSection.layout();

    if(expanded) {
      pluginConfigurationSection.setExpanded(true);
    }
  }

  void updatePluginDetails(Plugin plugin) {
    if(changingSelection) {
      return;
    }
//    if(plugin!=null && currentPlugin==plugin) {
//      return;
//    }
    this.currentPlugin = plugin;
    
    if(parentEditorPage!=null) {
      parentEditorPage.removeNotifyListener(groupIdText);
      parentEditorPage.removeNotifyListener(artifactIdText);
      parentEditorPage.removeNotifyListener(versionText);
      parentEditorPage.removeNotifyListener(pluginExtensionsButton);
      parentEditorPage.removeNotifyListener(pluginInheritedButton);
    }
    
    if(plugin==null) {
      pluginConfigurationSection.setExpanded(false);
      pluginConfigurationSection.setEnabled(false);
      FormUtils.setEnabled(pluginDetailsSection, false);
      FormUtils.setEnabled(pluginExecutionsSection, false);
      FormUtils.setEnabled(pluginDependenciesSection, false);
      pluginSelectAction.setEnabled(false);
      openWebPageAction.setEnabled(false);
      openConfigurationAction.setEnabled(false);
    
      setText(groupIdText, ""); //$NON-NLS-1$
      setText(artifactIdText, ""); //$NON-NLS-1$
      setText(versionText, ""); //$NON-NLS-1$
      setButton(pluginExtensionsButton, false);
      setButton(pluginInheritedButton, false);
      
      pluginExecutionsEditor.setInput(null);
      pluginDependenciesEditor.setInput(null);
      updatePluginExecution(null);

      if(configurationEditor != null) {
        configurationEditor.cleanup();
      }
      return;
    }
    
    pluginConfigurationSection.setEnabled(true);
    FormUtils.setEnabled(pluginDetailsSection, true);
    FormUtils.setEnabled(pluginExecutionsSection, true);
    FormUtils.setEnabled(pluginDependenciesSection, true);

    FormUtils.setReadonly(pluginDetailsSection, parentEditorPage.isReadOnly());
    FormUtils.setReadonly(pluginConfigurationSection, parentEditorPage.isReadOnly());
    FormUtils.setReadonly(pluginExecutionsSection, parentEditorPage.isReadOnly());
    pluginSelectAction.setEnabled(!parentEditorPage.isReadOnly());
    openWebPageAction.setEnabled(true);
    openConfigurationAction.setEnabled(true);

    // XXX implement dependency editing
    FormUtils.setReadonly(pluginDependenciesSection, true);

    String groupId = plugin.getGroupId();
    String artifactId = plugin.getArtifactId();
    String version = plugin.getVersion();
    
    setText(groupIdText, groupId);
    setText(artifactIdText, artifactId);
    setText(versionText, version);
    
    setConfigurationEditor(findConfigurationEditor(groupId, artifactId));
    if(configurationEditor != defaultConfigurationEditor && plugin.getConfiguration() == null) {
      // if a configuration editor is available, create the config section right away
      Configuration configuration = PomFactory.eINSTANCE.createConfiguration();
      plugin.setConfiguration(configuration);
    }
    configurationEditor.setPlugin(plugin);
    
    setButton(pluginInheritedButton, plugin.getInherited()==null || "true".equals(plugin.getInherited()));
    setButton(pluginExtensionsButton, "true".equals(plugin.getExtensions()));
    
    pluginExecutionsEditor.setInput(plugin.getExecutions());
    
    pluginDependenciesEditor.setInput(plugin.getDependencies());
    
    updatePluginExecution(null);
    
    // register listeners
    
    ValueProvider<Plugin> provider = new ValueProvider.DefaultValueProvider<Plugin>(currentPlugin);
    parentEditorPage.setModifyListener(groupIdText, provider, POM_PACKAGE.getPlugin_GroupId(), ""); //$NON-NLS-1$
    parentEditorPage.setModifyListener(artifactIdText, provider, POM_PACKAGE.getPlugin_ArtifactId(), ""); //$NON-NLS-1$
    parentEditorPage.setModifyListener(versionText, provider, POM_PACKAGE.getPlugin_Version(), ""); //$NON-NLS-1$
    parentEditorPage.setModifyListener(pluginInheritedButton, provider, POM_PACKAGE.getPlugin_Inherited(), "true");
    parentEditorPage.setModifyListener(pluginExtensionsButton, provider, POM_PACKAGE.getPlugin_Extensions(), "false");
  }

  void updatePluginExecution(PluginExecution pluginExecution) {
//    if(pluginExecution!=null && currentPluginExecution==pluginExecution) {
//      return;
//    }
    currentPluginExecution = pluginExecution;
    
    if(parentEditorPage!=null) {
      parentEditorPage.removeNotifyListener(executionIdText);
      parentEditorPage.removeNotifyListener(executionPhaseCombo);
      parentEditorPage.removeNotifyListener(executionInheritedButton);
    }
    
    if(pluginExecution==null) {
      FormUtils.setEnabled(pluginExecutionSection, false);

      setText(executionIdText, ""); //$NON-NLS-1$
      setText(executionPhaseCombo, ""); //$NON-NLS-1$
      setButton(executionInheritedButton, false);
      goalsEditor.setInput(null);
      
      return;
    }
    
    FormUtils.setEnabled(pluginExecutionSection, true);
    FormUtils.setReadonly(pluginExecutionSection, parentEditorPage.isReadOnly());
    
    setText(executionIdText, pluginExecution.getId());
    setText(executionPhaseCombo, pluginExecution.getPhase());
    setButton(executionInheritedButton, pluginExecution.getInherited()==null || "true".equals(pluginExecution.getInherited()));

    goalsEditor.setInput(pluginExecution.getGoals());
    // goalsEditor.setSelection(Collections.<String>emptyList());
    
    // register listeners
    ValueProvider<PluginExecution> provider = new ValueProvider.DefaultValueProvider<PluginExecution>(pluginExecution);
    parentEditorPage.setModifyListener(executionIdText, provider, POM_PACKAGE.getPluginExecution_Id(), ""); //$NON-NLS-1$
    parentEditorPage.setModifyListener(executionPhaseCombo, provider, POM_PACKAGE.getPluginExecution_Phase(), ""); //$NON-NLS-1$
    parentEditorPage.setModifyListener(executionInheritedButton, provider, POM_PACKAGE.getPluginExecution_Inherited(), "true");
  }

  public void loadData(MavenPomEditorPage editorPage, ValueProvider<BuildBase> buildProvider,
      ValueProvider<PluginManagement> pluginManagementProvider) {
    this.parentEditorPage = editorPage;
    this.buildProvider = buildProvider;
    this.pluginManagementProvider = pluginManagementProvider;
    
    changingSelection = true;
    loadPlugins();
    loadPluginManagement();
    changingSelection = false;
    
    pluginsEditor.setReadOnly(parentEditorPage.isReadOnly());
    pluginManagementEditor.setReadOnly(parentEditorPage.isReadOnly());
    
    pluginAddAction.setEnabled(!parentEditorPage.isReadOnly());
    pluginAddAction.setProvider(buildProvider);
    pluginManagementAddAction.setEnabled(!parentEditorPage.isReadOnly());
    pluginManagementAddAction.setProvider(pluginManagementProvider);
    updatePluginDetails(null);
    
//    pluginExecutionsEditor.setReadOnly(parent.isReadOnly());
//    goalsEditor.setReadOnly(parent.isReadOnly());
//    pluginDependenciesEditor.setReadOnly(parent.isReadOnly());
  }

  private void loadPlugins() {
    BuildBase build = buildProvider.getValue();
    pluginsEditor.setInput(build == null ? null : build.getPlugins());
  }

  private void loadPluginManagement() {
    PluginManagement pluginManagement = pluginManagementProvider.getValue();
    pluginManagementEditor.setInput(pluginManagement == null ? null : pluginManagement.getPlugins());
  }
  
  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    Object object = notification.getNotifier();
    Object feature = notification.getFeature();
    Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
    
    
    if (feature == PomPackage.Literals.MODEL__BUILD) { //MNGECLIPSE-2080
      loadPlugins();
      loadPluginManagement();
    }
    
    if(feature == PomPackage.Literals.BUILD_BASE__PLUGINS || feature == PomPackage.Literals.PLUGIN_MANAGEMENT__PLUGINS) {
      pluginsEditor.refresh();
      pluginManagementEditor.refresh();
    }
    
    if(object instanceof PluginManagement) {
      pluginManagementEditor.refresh();
    }
    
    if(object instanceof Plugin) {
      pluginsEditor.refresh();
      pluginManagementEditor.refresh();
      if(object == currentPlugin && (notificationObject == null || notificationObject instanceof Plugin)) {
        updatePluginDetails((Plugin) notificationObject);
      }
    }
    
    if(feature == PomPackage.Literals.PLUGIN__EXECUTIONS) {
      pluginExecutionsEditor.refresh();
      goalsEditor.refresh();
    }
    
    if(object instanceof PluginExecution) {
      pluginExecutionsEditor.refresh();
      if(currentPluginExecution == object
          && (notificationObject == null || notificationObject instanceof PluginExecution)) {
        updatePluginExecution((PluginExecution) notificationObject);
      }
    }
    
    if(feature == PomPackage.Literals.PLUGIN_EXECUTION__GOALS) {
      goalsEditor.setInput(((PluginExecution) object).getGoals());
      goalsEditor.refresh();
    }
  }

  @SuppressWarnings("unchecked")
  void createPlugin(ListEditorComposite<Plugin> editor, ValueProvider<? extends EObject> parentObjectProvider, EStructuralFeature feature, String groupId, String artifactId, String version) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = parentEditorPage.getEditingDomain();
    
    EObject parentObject = parentObjectProvider.getValue();
    boolean created = false;
    if(null == parentObject) {
      parentObject = parentObjectProvider.create(editingDomain, compoundCommand);
      created = true;
    }
    
    Plugin plugin = PomFactory.eINSTANCE.createPlugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    plugin.setVersion(version);
    
    if(findConfigurationEditor(groupId, artifactId) != defaultConfigurationEditor) {
      // if a configuration editor is available, create the config section right away
      Configuration configuration = PomFactory.eINSTANCE.createConfiguration();
      plugin.setConfiguration(configuration);
    }
    
    Command command = AddCommand.create(editingDomain, parentObject, feature, plugin);
    compoundCommand.append(command);
    
    editingDomain.getCommandStack().execute(compoundCommand);
    
    if(created) {
      editor.setInput((EList<Plugin>)parentObject.eGet(feature));
    }
    editor.setSelection(Collections.singletonList(plugin));
    updatePluginDetails(plugin);
    groupIdText.setFocus();
  }

  /**
   * Plugin label provider
   */
  static final class PluginLabelProvider extends LabelProvider {

    private boolean showGroupId = false;

    public void setShowGroupId(boolean showGroupId) {
      this.showGroupId = showGroupId;
    }
    
    public String getText(Object element) {
      if(element instanceof Plugin) {
        Plugin plugin = (Plugin) element;
        String label = ""; //$NON-NLS-1$
        
        if(showGroupId) {
          if(!isEmpty(plugin.getGroupId())) {
            label += plugin.getGroupId() + " : ";
          }
        }
        
        label += isEmpty(plugin.getArtifactId()) ? "?" : plugin.getArtifactId();
        
        if(!isEmpty(plugin.getVersion())) {
          label += " : " + plugin.getVersion();
        }
        
        return label;
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_PLUGIN;
    }
    
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
        changingSelection = true;
        selectPlugins(pluginsEditor, buildProvider.getValue().getPlugins());
        selectPlugins(pluginManagementEditor, pluginManagementProvider.getValue().getPlugins());
        changingSelection = false;
        
        updatePluginDetails(null);
      }

      private void selectPlugins(ListEditorComposite<Plugin> editor, List<Plugin> value) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        if(value!=null) {
          for(Plugin p : value) {
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
    TableViewer viewer = pluginsEditor.getViewer();
    viewer.addFilter(searchFilter);
    viewer = pluginManagementEditor.getViewer();
    viewer.addFilter(searchFilter);
    
  }

  
  public static class PluginFilter extends ViewerFilter {

    private final SearchMatcher searchMatcher;

    public PluginFilter(SearchMatcher searchMatcher) {
      this.searchMatcher = searchMatcher;
    }

    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if(element instanceof Plugin) {
        Plugin p = (Plugin) element;
        return searchMatcher.isMatchingArtifact(p.getGroupId(), p.getArtifactId());
      }
      if(element instanceof ReportPlugin) {
        ReportPlugin p = (ReportPlugin) element;
        return searchMatcher.isMatchingArtifact(p.getGroupId(), p.getArtifactId());
      }
      return false;
    }

  }
  
  private void loadPluginConfigurators() {
    pluginConfigurators = new HashMap<String,PluginExtensionDescriptor>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint configuratorsExtensionPoint = registry.getExtensionPoint(EXTENSION_CONFIGURATION_EDITOR);
    if(configuratorsExtensionPoint != null) {
      IExtension[] configuratorExtensions = configuratorsExtensionPoint.getExtensions();
      for(IExtension extension : configuratorExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if(element.getName().equals(ELEMENT_CONFIGURATION_EDITOR)) {
            PluginExtensionDescriptor descriptor = new PluginExtensionDescriptor(element);
            pluginConfigurators.put(descriptor.toString(), descriptor);
          }
        }
      }
    }
  }

  // an action button with a list of supported plug-ins
  final class PluginSelectAction extends Action implements IMenuCreator {
    private Menu menu = null;
    private List<IAction> actions = null;
    ActionContributionItem item = null;
    ValueProvider<? extends EObject> provider = null;

    protected PluginSelectAction(ListEditorComposite<Plugin> editor, EReference pomPackage) {
      super("", IAction.AS_DROP_DOWN_MENU);  //$NON-NLS-1$
      setImageDescriptor(MavenEditorImages.ADD_PLUGIN);
      setMenuCreator(this);
      setEnabled(false);
      setToolTipText(Messages.PluginsComposite_tooltip_addPlugin);
      actions = new ArrayList<IAction>();
      for(PluginExtensionDescriptor descriptor : pluginConfigurators.values()) {
        actions.add(createContributedAction(editor, pomPackage, descriptor));
      }
//      actions.add(createDefaultAction(editor, pomPackage));
      item = new ActionContributionItem(this);
    }
    
//    private Action createDefaultAction(final ListEditorComposite<Plugin> editor, final EReference pomPackage) {
//      return new Action( Messages.PluginsComposite_action_other ) {
//        public void run() {
//          //TODO: mkleint when is this action actually triggered? I could not trace it in the ui..
//          MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(),
//              Messages.PluginsComposite_searchDialog_addPlugin, IIndex.SEARCH_PLUGIN, Collections.<ArtifactKey>emptySet());
//          if(dialog.open() == Window.OK) {
//            IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//            if(af != null) {
//              createPlugin(editor, provider, pomPackage, af.group, af.artifact, af.version);
//            }
//          }
//        }
//      };
//    }
    
    private Action createContributedAction(final ListEditorComposite<Plugin> editor, final EReference pomPackage, final PluginExtensionDescriptor descriptor) {
      return new Action( descriptor.getName() ) {
        public void run() {
          String groupId = descriptor.getGroupId();
          String artifactId = descriptor.getArtifactId();
          String version = ""; //$NON-NLS-1$
          try {
            Collection<String> versions = M2EUIPluginActivator.getDefault().getSearchEngine(parentEditorPage.getProject()).findVersions(groupId, artifactId, null, Packaging.PLUGIN);
            if(!versions.isEmpty()) {
              version = versions.iterator().next();
            }
          } catch(CoreException e) {
            // TODO Auto-generated catch block
            log.error("Error retrieving available versions for " + groupId + ':' + artifactId, e); //$NON-NLS-1$
          }
          
          createPlugin(editor, provider, pomPackage, groupId, artifactId, version);
        }
      };
    }

    public Menu getMenu(Menu menu) {
      return null;
    }
    
    public Menu getMenu(Control control) {
      dispose();
      menu = new Menu(control);
      for(IAction action : actions) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(menu, -1);
      }
      return menu;
    }
    
    public void dispose() {
      if(menu != null) {
        menu.dispose();
      }
    }
    
    public IContributionItem getItem() {
      return item;
    }
    
    public void run() {
      // a drop-down button would have a separate click action which does nothing by default,
      // we need to show the same menu as if the user clicked on the chevron
      Widget w = item.getWidget();
      if(w != null && w instanceof ToolItem) {
        ToolItem ti = (ToolItem)w;
        Rectangle r = ti.getBounds(); 
        Point point = ti.getParent().toDisplay(r.x, r.y + r.height);
        Menu m = getMenu(ti.getParent());
        m.setLocation(point.x, point.y);
        m.setVisible(true);
      }
    }
    
    protected void setProvider(final ValueProvider<? extends EObject> provider) {
      this.provider = provider;
    }
  }
}