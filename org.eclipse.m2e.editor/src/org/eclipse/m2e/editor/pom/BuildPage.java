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

import static org.eclipse.m2e.editor.pom.FormUtils.nvl;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.actions.OpenUrlAction;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenRepositorySearchDialog;
import org.eclipse.m2e.core.ui.internal.search.util.Packaging;
import org.eclipse.m2e.core.ui.internal.util.ProposalUtil;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.BuildComposite;
import org.eclipse.m2e.editor.composites.DependencyLabelProvider;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.Extension;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
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

/**
 * @author Eugene Kuleshov
 */
public class BuildPage extends MavenPomEditorPage {
  
  // controls
  Text extensionGroupIdText;
  Text sourceText;
  Text outputText;
  Text testSourceText;
  Text testOutputText;
  Text scriptsSourceText;
  
  ListEditorComposite<Extension> extensionsEditor;
  BuildComposite buildComposite;
  Text extensionArtifactIdText;
  Text extensionVersionText;
  Button extensionSelectButton;
  Section foldersSection;
  Section extensionsSection;
  Section extensionDetailsSection;
  
  Extension currentExtension;
  protected boolean expandingTopSections;
  private Action extensionSelectAction;
  private Action extensionAddAction;
  private Action openWebPageAction;
  
  
  public BuildPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.build", Messages.BuildPage_title); //$NON-NLS-1$
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.BuildPage_form);
    // form.setExpandHorizontal(true);
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(3, true);
    gridLayout.horizontalSpacing = 3;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    SashForm buildSash = new SashForm(body, SWT.NONE);
    buildSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
    GridLayout buildSashLayout = new GridLayout();
    buildSashLayout.horizontalSpacing = 3;
    buildSashLayout.marginWidth = 0;
    buildSashLayout.marginHeight = 0;
    buildSashLayout.numColumns = 3;
    buildSash.setLayout(buildSashLayout);
    toolkit.adapt(buildSash);
    
    createFoldersSection(buildSash, toolkit);
    createExtensionsSection(buildSash, toolkit);
    createExtensionDetailsSection(buildSash, toolkit);

    buildComposite = new BuildComposite(body, SWT.NONE);
    GridData buildCompositeData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
    buildCompositeData.heightHint = 270;
    buildComposite.setLayoutData(buildCompositeData);
    toolkit.adapt(buildComposite);

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createFoldersSection(Composite buildSash, FormToolkit toolkit) {
    foldersSection = toolkit.createSection(buildSash, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    foldersSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    foldersSection.setText(Messages.BuildPage_section_folders);
    foldersSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        if(!expandingTopSections) {
          expandingTopSections = true;
          extensionsSection.setExpanded(foldersSection.isExpanded());
          extensionDetailsSection.setExpanded(foldersSection.isExpanded());
          expandingTopSections = false;
        }
      }
    });
  
    Composite composite = toolkit.createComposite(foldersSection, SWT.NONE);
    GridLayout compositeLayout = new GridLayout(2, false);
    compositeLayout.marginWidth = 2;
    compositeLayout.marginHeight = 2;
    composite.setLayout(compositeLayout);
    toolkit.paintBordersFor(composite);
    foldersSection.setClient(composite);
  
    toolkit.createLabel(composite, Messages.BuildPage_lblSources, SWT.NONE);
  
    sourceText = toolkit.createText(composite, null, SWT.NONE);
    sourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildPage_lblOutput, SWT.NONE);
  
    outputText = toolkit.createText(composite, null, SWT.NONE);
    outputText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildPage_lblTestSources, SWT.NONE);
  
    testSourceText = toolkit.createText(composite, null, SWT.NONE);
    testSourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildPage_lblTestOutput, SWT.NONE);
  
    testOutputText = toolkit.createText(composite, null, SWT.NONE);
    testOutputText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildPage_lblScripts, SWT.NONE);
  
    scriptsSourceText = toolkit.createText(composite, null, SWT.NONE);
    scriptsSourceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private void createExtensionsSection(Composite buildSash, FormToolkit toolkit) {
    extensionsSection = toolkit.createSection(buildSash, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    extensionsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    extensionsSection.setText(Messages.BuildPage_section_extensions);
    extensionsSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        if(!expandingTopSections) {
          expandingTopSections = true;
          foldersSection.setExpanded(extensionsSection.isExpanded());
          extensionDetailsSection.setExpanded(extensionsSection.isExpanded());
          expandingTopSections = false;
        }
      }
    });

    extensionsEditor = new ListEditorComposite<Extension>(extensionsSection, SWT.NONE);
    toolkit.paintBordersFor(extensionsEditor);
    toolkit.adapt(extensionsEditor);
    extensionsSection.setClient(extensionsEditor);
    
    final DependencyLabelProvider labelProvider = new DependencyLabelProvider();
    extensionsEditor.setLabelProvider(labelProvider);
    extensionsEditor.setContentProvider(new ListEditorContentProvider<Extension>());
    
    extensionsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Extension> selection = extensionsEditor.getSelection();
        updateExtensionDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
    
    extensionsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createExtension(null, null, null);
      }
    });
    
    extensionsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = getEditingDomain();
 
        List<Extension> list = extensionsEditor.getSelection();
        for(Extension extension : list) {
          Command removeCommand = RemoveCommand.create(editingDomain, model.getBuild(), //
              POM_PACKAGE.getBuild_Extensions(), extension);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
        updateExtensionDetails(null);
      }
    });
    
    extensionAddAction = new Action(Messages.BuildPage_action_addExtension, MavenEditorImages.ADD_ARTIFACT) {
      public void run() {
        // XXX calculate list available extensions
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(getEditorSite().getShell(), //
            Messages.BuildPage_searchDialog_addExtension, getPomEditor().getMavenProject(), getProject(), false );
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            createExtension(af.group, af.artifact, af.version);
          }
        }
      }
    };
    extensionAddAction.setEnabled(false);

    ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
    toolBarManager.add(extensionAddAction);
    toolBarManager.add(new Separator());
    
    toolBarManager.add(new Action(Messages.BuildPage_action_showGroupId, MavenEditorImages.SHOW_GROUP) {
      {
        setChecked(true);
      }
      public int getStyle() {
        return AS_CHECK_BOX;
      }
      public void run() {
        labelProvider.setShowGroupId(isChecked());
        extensionsEditor.getViewer().refresh();
      }
    });
    
    Composite toolbarComposite = toolkit.createComposite(extensionsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    extensionsSection.setTextClient(toolbarComposite);
  }

  private void createExtensionDetailsSection(Composite buildSash, FormToolkit toolkit) {
    extensionDetailsSection = toolkit.createSection(buildSash, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    extensionDetailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    extensionDetailsSection.setText(Messages.BuildPage_section_extensionDetails);
    extensionDetailsSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        if(!expandingTopSections) {
          expandingTopSections = true;
          foldersSection.setExpanded(extensionDetailsSection.isExpanded());
          extensionsSection.setExpanded(extensionDetailsSection.isExpanded());
          expandingTopSections = false;
        }
      }
    });

    Composite extensionDetialsComposite = toolkit.createComposite(extensionDetailsSection, SWT.NONE);
    GridLayout extensionDetialsCompositeLayout = new GridLayout(2, false);
    extensionDetialsCompositeLayout.marginWidth = 2;
    extensionDetialsCompositeLayout.marginHeight = 2;
    extensionDetialsComposite.setLayout(extensionDetialsCompositeLayout);
    toolkit.paintBordersFor(extensionDetialsComposite);
    extensionDetailsSection.setClient(extensionDetialsComposite);

    toolkit.createLabel(extensionDetialsComposite, Messages.BuildPage_lblGroupId);
    
    extensionGroupIdText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionGroupIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    extensionGroupIdText.setData("name", "extensionGroupIdText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addGroupIdProposal(getProject(), extensionGroupIdText, Packaging.ALL);
    
    Hyperlink extensionArtifactIdHyperlink = toolkit.createHyperlink(extensionDetialsComposite, Messages.BuildPage_lblArtifactId, SWT.NONE);
    extensionArtifactIdHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        final String groupId = extensionGroupIdText.getText();
        final String artifactId = extensionArtifactIdText.getText();
        final String version = extensionVersionText.getText();
        new Job("Opening " + groupId + ":" + artifactId + ":" + version) {
          protected IStatus run(IProgressMonitor arg0) {
            OpenPomAction.openEditor(groupId, artifactId, version, null);
            return Status.OK_STATUS;
          }
        }.schedule();
      }
    });
    
    extensionArtifactIdText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionArtifactIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    extensionArtifactIdText.setData("name", "extensionArtifactIdText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addArtifactIdProposal(getProject(), extensionGroupIdText, extensionArtifactIdText, Packaging.ALL);
    
    toolkit.createLabel(extensionDetialsComposite, Messages.BuildPage_lblVersion);
    
    extensionVersionText = toolkit.createText(extensionDetialsComposite, null, SWT.FLAT);
    extensionVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    extensionVersionText.setData("name", "extensionVersionText"); //$NON-NLS-1$ //$NON-NLS-2$
    ProposalUtil.addVersionProposal(getProject(), getPomEditor().getMavenProject(), extensionGroupIdText, extensionArtifactIdText, extensionVersionText, Packaging.ALL);
    extensionDetialsComposite.setTabList(new Control[] {extensionGroupIdText, extensionArtifactIdText, extensionVersionText});

//    extensionSelectButton = toolkit.createButton(extensionDetialsComposite, "Select...", SWT.FLAT);
//    extensionSelectButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
//    extensionSelectButton.addSelectionListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        // TODO calculate current list of artifacts for the project
//        Set<Dependency> artifacts = Collections.emptySet();
//        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getEditorSite().getShell(), //
//            "Add Dependency", IndexManager.SEARCH_ARTIFACT, artifacts);
//        if(dialog.open() == Window.OK) {
//          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
//          if(af != null) {
//            extensionGroupIdText.setText(nvl(af.group));
//            extensionArtifactIdText.setText(nvl(af.artifact));
//            extensionVersionText.setText(nvl(af.version));
//          }
//        }
//      }
//    });
    
    extensionSelectAction = new Action(Messages.BuildPage_action_selectExtension, MavenEditorImages.SELECT_ARTIFACT) {
      public void run() {
        // XXX calculate list available extensions
        MavenRepositorySearchDialog dialog = MavenRepositorySearchDialog.createSearchDependencyDialog(getEditorSite().getShell(), //
            Messages.BuildPage_searchDialog_selectExtension, getPomEditor().getMavenProject(), getProject(), false );
        
        if(dialog.open() == Window.OK) {
          IndexedArtifactFile af = (IndexedArtifactFile) dialog.getFirstResult();
          if(af != null) {
            extensionGroupIdText.setText(nvl(af.group));
            extensionArtifactIdText.setText(nvl(af.artifact));
            extensionVersionText.setText(nvl(af.version));
          }
        }
      }
    };
    extensionSelectAction.setEnabled(false);

    openWebPageAction = new Action(Messages.BuildPage_action_openWeb, MavenEditorImages.WEB_PAGE) {
      public void run() {
        final String groupId = extensionGroupIdText.getText();
        final String artifactId = extensionArtifactIdText.getText();
        final String version = extensionVersionText.getText();
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
    toolBarManager.add(extensionSelectAction);
    toolBarManager.add(new Separator());
    toolBarManager.add(openWebPageAction);
    
    Composite toolbarComposite = toolkit.createComposite(extensionDetailsSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    toolBarManager.createControl(toolbarComposite);
    extensionDetailsSection.setTextClient(toolbarComposite);
  }
  
  public void loadData() {
    loadBuild();
    loadBuildBase();
    
    extensionsEditor.setReadOnly(isReadOnly());

    updateExtensionDetails(null);
  }
  
  void updateExtensionDetails(Extension extension) {
    currentExtension = extension;
    
    removeNotifyListener(extensionGroupIdText);
    removeNotifyListener(extensionArtifactIdText);
    removeNotifyListener(extensionVersionText);
    
    if(extension==null) {
      FormUtils.setEnabled(extensionDetailsSection, false);
      extensionSelectAction.setEnabled(false);
      openWebPageAction.setEnabled(false);

      setText(extensionGroupIdText, ""); //$NON-NLS-1$
      setText(extensionArtifactIdText, ""); //$NON-NLS-1$
      setText(extensionVersionText, ""); //$NON-NLS-1$
      
      return;
    }

    FormUtils.setEnabled(extensionDetailsSection, true);
    FormUtils.setReadonly(extensionDetailsSection, isReadOnly());
    extensionSelectAction.setEnabled(!isReadOnly());
    openWebPageAction.setEnabled(true);
    
    setText(extensionGroupIdText, extension.getGroupId());
    setText(extensionArtifactIdText, extension.getArtifactId());
    setText(extensionVersionText, extension.getVersion());
    
    ValueProvider<Extension> extensionProvider = new ValueProvider.DefaultValueProvider<Extension>(extension); 
    setModifyListener(extensionGroupIdText, extensionProvider, POM_PACKAGE.getExtension_GroupId(), ""); //$NON-NLS-1$
    setModifyListener(extensionArtifactIdText, extensionProvider, POM_PACKAGE.getExtension_ArtifactId(), ""); //$NON-NLS-1$
    setModifyListener(extensionVersionText, extensionProvider, POM_PACKAGE.getExtension_Version(), ""); //$NON-NLS-1$
    
    registerListeners();
  }

  private void loadBuild() {
    removeNotifyListener(sourceText);
    removeNotifyListener(outputText);
    removeNotifyListener(testSourceText);
    removeNotifyListener(testOutputText);
    removeNotifyListener(scriptsSourceText);
    
    Build build = model == null ? null : model.getBuild();
    if(build==null) {
      setText(sourceText, ""); //$NON-NLS-1$
      setText(outputText, ""); //$NON-NLS-1$
      setText(testSourceText, ""); //$NON-NLS-1$
      setText(testOutputText, ""); //$NON-NLS-1$
      setText(scriptsSourceText, ""); //$NON-NLS-1$

      extensionsEditor.setInput(null);
      
    } else {
      setText(sourceText, build.getSourceDirectory());
      setText(outputText, build.getOutputDirectory());
      setText(testSourceText, build.getTestSourceDirectory());
      setText(testOutputText, build.getTestOutputDirectory());
      setText(scriptsSourceText, build.getScriptSourceDirectory());
      
      extensionsEditor.setInput(build.getExtensions() == null ? null : build.getExtensions());
    }
    
    FormUtils.setReadonly(foldersSection, isReadOnly());
    FormUtils.setReadonly(extensionsSection, isReadOnly());
    extensionAddAction.setEnabled(!isReadOnly());
    
    updateExtensionDetails(null);
      
    ValueProvider<Build> modelProvider = new ValueProvider.ParentValueProvider<Build>(sourceText, outputText, testSourceText, testOutputText, scriptsSourceText) {
      public Build getValue() {
        return model.getBuild();
      }
      public Build create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Build build = model.getBuild();
        if(build==null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
          compoundCommand.append(command);
        }
        return build;
      }
    };
    setModifyListener(sourceText, modelProvider, POM_PACKAGE.getBuild_SourceDirectory(), ""); //$NON-NLS-1$
    setModifyListener(outputText, modelProvider, POM_PACKAGE.getBuild_OutputDirectory(), ""); //$NON-NLS-1$
    setModifyListener(testSourceText, modelProvider, POM_PACKAGE.getBuild_TestSourceDirectory(), ""); //$NON-NLS-1$
    setModifyListener(testOutputText, modelProvider, POM_PACKAGE.getBuild_TestOutputDirectory(), ""); //$NON-NLS-1$
    setModifyListener(scriptsSourceText, modelProvider, POM_PACKAGE.getBuild_ScriptSourceDirectory(), ""); //$NON-NLS-1$

    loadBuildBase();
  }

  private void loadBuildBase() {
    ValueProvider<BuildBase> buildProvider = new ValueProvider<BuildBase>() {
      public Build getValue() {
        return model.getBuild();
      }
      public Build create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Build build = PomFactory.eINSTANCE.createBuild();
        Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
        compoundCommand.append(command);
        return build;
      }
    };
    buildComposite.loadData(this, buildProvider);
  }

  protected void doUpdate(Notification notification){
    EObject object = (EObject) notification.getNotifier();
    Object feature = notification.getFeature();
    if (object instanceof Build) {
      loadBuild();
    }
    if (object instanceof BuildBase) {
      loadBuildBase();
    }

    if (feature == PomPackage.Literals.BUILD__EXTENSIONS) {
      extensionsEditor.refresh();
    }
    
    if (object instanceof Extension) {
      extensionsEditor.refresh();
      if(currentExtension==object) {
        updateExtensionDetails(currentExtension);
      }
    }

    if(buildComposite!=null) {
      buildComposite.updateView(this, notification);
    }
  }
  
  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        doUpdate(notification);
      }
    });
  }

  void createExtension(String groupId, String artifactId, String version) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = getEditingDomain();
    
    Build build = model.getBuild();
    if(build == null) {
      build = PomFactory.eINSTANCE.createBuild();
      Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Build(), build);
      compoundCommand.append(command);
    }
    
    Extension extension = PomFactory.eINSTANCE.createExtension();
    extension.setGroupId(groupId);
    extension.setArtifactId(artifactId);
    extension.setVersion(version);
    
    Command addCommand = AddCommand.create(editingDomain, build, //
        POM_PACKAGE.getBuild_Extensions(), extension);
    compoundCommand.append(addCommand);
    
    editingDomain.getCommandStack().execute(compoundCommand);

    extensionsEditor.setSelection(Collections.singletonList(extension));
    extensionGroupIdText.setFocus();
  }
}

