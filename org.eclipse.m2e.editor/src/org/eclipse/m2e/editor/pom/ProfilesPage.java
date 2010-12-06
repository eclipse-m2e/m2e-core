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
import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.actions.OpenPomAction;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.wizards.MavenModuleWizard;
import org.eclipse.m2e.core.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.BuildComposite;
import org.eclipse.m2e.editor.composites.DependenciesComposite;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.composites.PluginsComposite;
import org.eclipse.m2e.editor.composites.ReportingComposite;
import org.eclipse.m2e.editor.composites.RepositoriesComposite;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.Activation;
import org.eclipse.m2e.model.edit.pom.ActivationFile;
import org.eclipse.m2e.model.edit.pom.ActivationOS;
import org.eclipse.m2e.model.edit.pom.ActivationProperty;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.DependencyManagement;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.PluginManagement;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Profile;
import org.eclipse.m2e.model.edit.pom.Reporting;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;

/**
 * @author Eugene Kuleshov
 */
public class ProfilesPage extends MavenPomEditorPage {

  // controls
  Button activeByDefaultbutton;
  Text activationFileMissingText;
  Text activationFileExistText;
  Text activationPropertyValueText;
  Text activationPropertyNameText;
  Text activationOsVersionText;
  Text activationOsArchitectureText;
  Text activationOsFamilyText;
  Text activationOsNameText;
  Text activationJdkText;
  ListEditorComposite<Profile> profilesEditor;
  ListEditorComposite<String> modulesEditor;
  Section modulesSection;
  PropertiesSection propertiesSection;
  
  CTabFolder tabFolder;
  BuildComposite buildComposite;
  PluginsComposite pluginsComposite;
  DependenciesComposite dependenciesComposite;
  RepositoriesComposite repositoriesComposite;
  ReportingComposite reportingComposite;

  Color defaultForegroundColor;
  Color defaultSelectionColor;
  Color disabledTabColor = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
  
  // model
  Profile currentProfile;
  private IAction newModuleProjectAction;
  
  public ProfilesPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.profiles", Messages.ProfilesPage_title); //$NON-NLS-1$
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.ProfilesPage_form);
    // form.setExpandHorizontal(true);
    
    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(2, true);
    gridLayout.horizontalSpacing = 7;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);
  
    createProfilesSection(toolkit, body);

    createModulesSection(toolkit, body);
    createPropertiesSection(toolkit, body);

    tabFolder = new CTabFolder(body, SWT.FLAT | SWT.MULTI);
    GridData tabFolderData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    tabFolderData.verticalIndent = 5;
    tabFolder.setLayoutData(tabFolderData);
    toolkit.adapt(tabFolder, true, true);
    
    toolkit.getColors().initializeSectionToolBarColors();
    Color selectedColor = toolkit.getColors().getColor("org.eclipse.ui.forms.TB_BG"); //$NON-NLS-1$
    tabFolder.setSelectionBackground(new Color[] {selectedColor, toolkit.getColors().getBackground()}, //
        new int[] {100}, true);
    defaultForegroundColor = tabFolder.getForeground();
    defaultSelectionColor = tabFolder.getSelectionForeground();

    tabFolder.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        // updateTabSelection();
      }
    });

    createActivationTab(tabFolder, toolkit);
    createDependenciesTab(tabFolder, toolkit);
    createRepositoriesTab(toolkit, tabFolder);
    createBuildTab(toolkit, tabFolder);
    createPluginsTab(toolkit, tabFolder);
    createReportsTab(toolkit, tabFolder);
    
    tabFolder.setSelection(0);

//    form.pack();

    super.createFormContent(managedForm);
  }

  private void createProfilesSection(FormToolkit toolkit, Composite body) {
    Section profilesSection = toolkit.createSection(body, ExpandableComposite.TITLE_BAR);
    profilesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
    profilesSection.setText(Messages.ProfilesPage_section_profiles);
    profilesEditor = new ListEditorComposite<Profile>(profilesSection, SWT.NONE);
    profilesSection.setClient(profilesEditor);
    toolkit.adapt(profilesEditor);
    toolkit.paintBordersFor(profilesEditor);
    
    profilesEditor.setContentProvider(new ListEditorContentProvider<Profile>());
    profilesEditor.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof Profile) {
          String profileId = ((Profile) element).getId();
          return isEmpty(profileId) ? "?" : profileId;
        }
        return super.getText(element);
      }
      
      public Image getImage(Object element) {
        return MavenEditorImages.IMG_PROFILE;
      }
    });
    
    profilesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Profile> selection = profilesEditor.getSelection();
        updateProfileDetails(selection.size()==1 ? selection.get(0) : null);
      }
    });
  
    profilesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = getEditingDomain();
        
        boolean created = false;

        Profile profile = PomFactory.eINSTANCE.createProfile();
        Command addCommand = AddCommand.create(editingDomain, model, POM_PACKAGE.getModel_Profiles(), profile);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        profilesEditor.setSelection(Collections.singletonList(profile));
      }
    });
    
    profilesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = getEditingDomain();
 
        List<Profile> selection = profilesEditor.getSelection();
        for(Profile filter : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, model, //
              POM_PACKAGE.getModel_Profiles(), filter);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    profilesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
      public Object getValue(Object element, String property) {
        if(element instanceof Profile) {
          String id = ((Profile) element).getId();
          return isEmpty(id) ? "" : id; //$NON-NLS-1$
        }
        return ""; //$NON-NLS-1$
      }

      public void modify(Object element, String property, Object value) {
        int n = profilesEditor.getViewer().getTable().getSelectionIndex();
        Profile profile = model.getProfiles().get(n);
        if(!value.equals(profile.getId())) {
          EditingDomain editingDomain = getEditingDomain();
          Command command = SetCommand.create(editingDomain, profile, POM_PACKAGE.getProfile_Id(), value);
          editingDomain.getCommandStack().execute(command);
          profilesEditor.refresh();
        }
      }
    });
    

    profilesEditor.setReadOnly(pomEditor.isReadOnly());
  }

  private void createPropertiesSection(FormToolkit toolkit, Composite body) {
    propertiesSection = new PropertiesSection(toolkit, body, getEditingDomain());
  }

  private void createModulesSection(FormToolkit toolkit, Composite body) {
    modulesSection = toolkit.createSection(body, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    modulesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));
    modulesSection.setText(Messages.ProfilesPage_section_modules);

    modulesEditor = new ListEditorComposite<String>(modulesSection, SWT.NONE);
    modulesEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    modulesSection.setClient(modulesEditor);
    toolkit.adapt(modulesEditor);
    toolkit.paintBordersFor(modulesEditor);
    
    modulesEditor.setContentProvider(new ListEditorContentProvider<String>());
    modulesEditor.setLabelProvider(new ModulesLabelProvider(this));
    
    modulesEditor.setOpenListener(new IOpenListener() {
      public void open(OpenEvent openevent) {
        List<String> selection = modulesEditor.getSelection();
        for(String module : selection) {
          IMavenProjectFacade projectFacade = findModuleProject(module);
          if(projectFacade!=null) {
            ArtifactKey mavenProject = projectFacade.getArtifactKey();
            OpenPomAction.openEditor(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion(), null);
          }
        }
      }
    });
    
    modulesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createNewModule("?");
      }
    });
    
    modulesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = getEditingDomain();
 
        List<String> selection = modulesEditor.getSelection();
        for(String module : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentProfile, //
              POM_PACKAGE.getProfile_Modules(), module);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    modulesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }

      public Object getValue(Object element, String property) {
        return element;
      }

      public void modify(Object element, String property, Object value) {
        int n = modulesEditor.getViewer().getTable().getSelectionIndex();
        EList<String> modules = currentProfile.getModules(); 
        if(n >= 0 && n < modules.size() && !value.equals(modules.get(n))) {
          EditingDomain editingDomain = getEditingDomain();
          Command command = SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Modules(), value, n);
          editingDomain.getCommandStack().execute(command);
          modulesEditor.refresh();
        }
      }
    });
    
    newModuleProjectAction = new Action(Messages.ProfilesPage_action_newModuleProject, MavenEditorImages.ADD_MODULE) {
      public void run() {
        IEditorInput editorInput = pomEditor.getEditorInput();
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
    };

    ToolBarManager modulesToolBarManager = new ToolBarManager(SWT.FLAT);
    modulesToolBarManager.add(newModuleProjectAction);
    
    Composite toolbarComposite = toolkit.createComposite(modulesSection);
    GridLayout toolbarLayout = new GridLayout(1, true);
    toolbarLayout.marginHeight = 0;
    toolbarLayout.marginWidth = 0;
    toolbarComposite.setLayout(toolbarLayout);
    toolbarComposite.setBackground(null);
 
    modulesToolBarManager.createControl(toolbarComposite);
    modulesSection.setTextClient(toolbarComposite);
    
    modulesEditor.setReadOnly(pomEditor.isReadOnly());
    newModuleProjectAction.setEnabled(!pomEditor.isReadOnly());
    
  }

  protected void updateProfileDetails(Profile profile) {
    currentProfile = profile;
    
    if(profile==null) {
      FormUtils.setEnabled(propertiesSection.getSection(), false);
      FormUtils.setEnabled(modulesSection, false);
      modulesEditor.setInput(null);
      updateProfileTabs(profile);
      
      return;
    }

    FormUtils.setEnabled(propertiesSection.getSection(), true);
    FormUtils.setEnabled(modulesSection, true);
    
    FormUtils.setReadonly(propertiesSection.getSection(), isReadOnly());
    FormUtils.setReadonly(modulesSection, isReadOnly());
    
    modulesEditor.setInput(profile.getModules());
    modulesEditor.setReadOnly(isReadOnly());

    propertiesSection.setModel(profile, POM_PACKAGE.getProfile_Properties());

    updateProfileTabs(profile);
  }

  private void updateProfileTabs(Profile profile) {
    if(profile==null) {
      FormUtils.setEnabled(tabFolder, false);
      tabFolder.setForeground(disabledTabColor);
      tabFolder.setSelectionForeground(disabledTabColor);
    } else {
      FormUtils.setEnabled(tabFolder, true);
      FormUtils.setReadonly(tabFolder, isReadOnly());
      tabFolder.setForeground(defaultForegroundColor);
      tabFolder.setSelectionForeground(defaultSelectionColor);
    }
    
    updateActivationTab();
    updateDependenciesTab();
    updateRepositoriesTab();
    updateBuildTab();
    updatePluginsTab();
    updateReportsTab();
  }

  private void updateActivationTab() {
    removeNotifyListener(activeByDefaultbutton);
    removeNotifyListener(activationJdkText);
    removeNotifyListener(activationPropertyNameText);
    removeNotifyListener(activationPropertyValueText);
    removeNotifyListener(activationFileExistText);
    removeNotifyListener(activationFileMissingText);
    removeNotifyListener(activationOsArchitectureText);
    removeNotifyListener(activationOsFamilyText);
    removeNotifyListener(activationOsNameText);
    removeNotifyListener(activationOsVersionText);

    Activation activation = currentProfile == null ? null : currentProfile.getActivation();
    if(activation == null) {
      setButton(activeByDefaultbutton, false);
      setText(activationJdkText, ""); //$NON-NLS-1$

      setText(activationPropertyNameText, ""); //$NON-NLS-1$
      setText(activationPropertyValueText, ""); //$NON-NLS-1$

      setText(activationFileExistText, ""); //$NON-NLS-1$
      setText(activationFileMissingText, ""); //$NON-NLS-1$

      setText(activationOsArchitectureText, ""); //$NON-NLS-1$
      setText(activationOsFamilyText, ""); //$NON-NLS-1$
      setText(activationOsNameText, ""); //$NON-NLS-1$
      setText(activationOsVersionText, ""); //$NON-NLS-1$
      
    } else {
      setButton(activeByDefaultbutton, "true".equals(activation.getActiveByDefault()));
      setText(activationJdkText, activation.getJdk());
      
      ActivationProperty property = activation.getProperty();
      if(property==null) {
        setText(activationPropertyNameText, ""); //$NON-NLS-1$
        setText(activationPropertyValueText, ""); //$NON-NLS-1$
      } else {
        setText(activationPropertyNameText, property.getName());
        setText(activationPropertyValueText, property.getValue());
      }
      
      ActivationFile file = activation.getFile();
      if(file==null) {
        setText(activationFileExistText, ""); //$NON-NLS-1$
        setText(activationFileMissingText, ""); //$NON-NLS-1$
      } else {
        setText(activationFileExistText, file.getExists());
        setText(activationFileMissingText, file.getMissing());
      }
      
      ActivationOS os = activation.getOs();
      if(os==null) {
        setText(activationOsArchitectureText, ""); //$NON-NLS-1$
        setText(activationOsFamilyText, ""); //$NON-NLS-1$
        setText(activationOsNameText, ""); //$NON-NLS-1$
        setText(activationOsVersionText, ""); //$NON-NLS-1$
      } else {
        setText(activationOsArchitectureText, os.getArch());
        setText(activationOsFamilyText, os.getFamily());
        setText(activationOsNameText, os.getName());
        setText(activationOsVersionText, os.getVersion());
      }
    }
    
    ValueProvider<Activation> activationProvider = new ValueProvider<Activation>() {
      public Activation getValue() {
        return currentProfile.getActivation();
      }
      public Activation create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        return createActivation(editingDomain, compoundCommand);
      }
    };
    setModifyListener(activeByDefaultbutton, activationProvider, POM_PACKAGE.getActivation_ActiveByDefault(), "false");
    setModifyListener(activationJdkText, activationProvider, POM_PACKAGE.getActivation_Jdk(), ""); //$NON-NLS-1$
    
    ValueProvider<ActivationProperty> activationPropertyProvider = new ValueProvider<ActivationProperty>() {
      public ActivationProperty getValue() {
        Activation activation = currentProfile.getActivation();
        return activation==null ? null : activation.getProperty();
      }
      public ActivationProperty create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Activation activation = createActivation(editingDomain, compoundCommand);
        ActivationProperty activationProperty = activation.getProperty();
        if(activationProperty == null) {
          activationProperty = PomFactory.eINSTANCE.createActivationProperty();
          compoundCommand.append(SetCommand.create(editingDomain, activation, POM_PACKAGE.getActivation_Property(),
              activationProperty));
        }
        return activationProperty;
      }
    };
    setModifyListener(activationPropertyNameText, activationPropertyProvider, POM_PACKAGE.getActivationProperty_Name(), ""); //$NON-NLS-1$
    setModifyListener(activationPropertyValueText, activationPropertyProvider, POM_PACKAGE.getActivationProperty_Value(), ""); //$NON-NLS-1$
    
    ValueProvider<ActivationFile> activationFileProvider = new ValueProvider<ActivationFile>() {
      public ActivationFile getValue() {
        Activation activation = currentProfile.getActivation();
        return activation==null ? null : activation.getFile();
      }
      public ActivationFile create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Activation activation = createActivation(editingDomain, compoundCommand);
        ActivationFile activationFile = activation.getFile();
        if(activationFile == null) {
          activationFile = PomFactory.eINSTANCE.createActivationFile();
          compoundCommand.append(SetCommand.create(editingDomain, activation, POM_PACKAGE.getActivation_File(),
              activationFile));
        }
        return activationFile;
      }
    };
    setModifyListener(activationFileExistText, activationFileProvider, POM_PACKAGE.getActivationFile_Exists(), ""); //$NON-NLS-1$
    setModifyListener(activationFileMissingText, activationFileProvider, POM_PACKAGE.getActivationFile_Missing(), ""); //$NON-NLS-1$
    
    ValueProvider<ActivationOS> activationOsProvider = new ValueProvider<ActivationOS>() {
      public ActivationOS getValue() {
        Activation activation = currentProfile.getActivation();
        return activation==null ? null : activation.getOs();
      }
      public ActivationOS create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Activation activation = createActivation(editingDomain, compoundCommand);
        ActivationOS activationOS = activation.getOs();
        if(activationOS == null) {
          activationOS = PomFactory.eINSTANCE.createActivationOS();
          compoundCommand.append(SetCommand.create(editingDomain, activation, POM_PACKAGE.getActivation_Os(),
              activationOS));
        }
        return activationOS;
      }
    };
    setModifyListener(activationOsArchitectureText, activationOsProvider, POM_PACKAGE.getActivationOS_Arch(), ""); //$NON-NLS-1$
    setModifyListener(activationOsFamilyText, activationOsProvider, POM_PACKAGE.getActivationOS_Family(), ""); //$NON-NLS-1$
    setModifyListener(activationOsNameText, activationOsProvider, POM_PACKAGE.getActivationOS_Name(), ""); //$NON-NLS-1$
    setModifyListener(activationOsVersionText, activationOsProvider, POM_PACKAGE.getActivationOS_Version(), ""); //$NON-NLS-1$
    
    registerListeners();
  }

  Activation createActivation(EditingDomain editingDomain, CompoundCommand compoundCommand) {
    Activation activation = currentProfile.getActivation();
    if(activation == null) {
      activation = PomFactory.eINSTANCE.createActivation();
      compoundCommand.append(SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Activation(),
          activation));
    }
    return activation;
  }
  
  private void updateDependenciesTab() {
    ValueProvider<DependencyManagement> dependencyManagementProvider = new ValueProvider<DependencyManagement>() {
      public DependencyManagement getValue() {
        DependencyManagement management = currentProfile == null ? null : currentProfile.getDependencyManagement();
        return management;
      }

      public DependencyManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DependencyManagement management = currentProfile.getDependencyManagement();
        if(management == null) {
          management = PomFactory.eINSTANCE.createDependencyManagement();
          Command command = SetCommand.create(editingDomain, currentProfile, //
              POM_PACKAGE.getProfile_DependencyManagement(), management);
          compoundCommand.append(command);
        }

        return management;
      }
    };

    dependenciesComposite.loadData(model, dependencyManagementProvider);
  }

  private void updateRepositoriesTab() {
    ValueProvider<DistributionManagement> dmProvider = new ValueProvider<DistributionManagement>() {
      public DistributionManagement getValue() {
        return currentProfile==null ? null : currentProfile.getDistributionManagement();
      }

      public DistributionManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = PomFactory.eINSTANCE.createDistributionManagement();
        Command command = SetCommand.create(editingDomain, currentProfile, //
            POM_PACKAGE.getProfile_DistributionManagement(), dm);
        compoundCommand.append(command);
        return dm;
      }
    };

    repositoriesComposite.loadData(this, model, dmProvider);
  }

  private void updateBuildTab() {
    ValueProvider<BuildBase> buildProvider = new ValueProvider<BuildBase>() {
      public BuildBase getValue() {
        return currentProfile == null ? null : currentProfile.getBuild();
      }
      
      public BuildBase create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        BuildBase buildBase = PomFactory.eINSTANCE.createBuildBase();
        Command command = SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Build(), buildBase);
        compoundCommand.append(command);
        return buildBase;
      }
    };
    buildComposite.loadData(this, buildProvider);
  }

  private void updatePluginsTab() {
    ValueProvider<BuildBase> buildProvider = new ValueProvider<BuildBase>() {
      public BuildBase getValue() {
        BuildBase build = currentProfile == null ? null : currentProfile.getBuild();
        return build;
      }

      public BuildBase create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        BuildBase build = currentProfile.getBuild();
        if(build == null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Build(), build);
          compoundCommand.append(command);
        }

        return build;
      }
    };

    ValueProvider<PluginManagement> pluginManagementProvider = new ValueProvider<PluginManagement>() {
      public PluginManagement getValue() {
        BuildBase build = currentProfile == null ? null : currentProfile.getBuild();
        PluginManagement management = build == null ? null : build.getPluginManagement();
        return management;
      }

      public PluginManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        BuildBase build = currentProfile.getBuild();
        if(build == null) {
          build = PomFactory.eINSTANCE.createBuild();
          Command command = SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Build(), build);
          compoundCommand.append(command);
        }

        PluginManagement management = build.getPluginManagement();
        if(management == null) {
          management = PomFactory.eINSTANCE.createPluginManagement();
          Command command = SetCommand.create(editingDomain, build, //
              POM_PACKAGE.getBuildBase_PluginManagement(), management);
          compoundCommand.append(command);
        }

        return management;
      }
    };
    
    pluginsComposite.loadData(this, buildProvider, pluginManagementProvider);
  }

  private void updateReportsTab() {
    ValueProvider<Reporting> reportingProvider = new ValueProvider<Reporting>() {

      public Reporting getValue() {
        return currentProfile==null ? null : currentProfile.getReporting();
      }
      
      public Reporting create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Reporting reporting = currentProfile.getReporting();
        if(reporting == null) {
          reporting = PomFactory.eINSTANCE.createReporting();
          Command command = SetCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Reporting(), reporting);
          compoundCommand.append(command);
        }
        return reporting;
      }
    };
    
    reportingComposite.loadData(this, reportingProvider);
  }

  private void createBuildTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem buildTabItem = new CTabItem(tabFolder, SWT.NONE);
    buildTabItem.setText(Messages.ProfilesPage_tab_build);
  
    buildComposite = new BuildComposite(tabFolder, SWT.NONE);
    buildTabItem.setControl(buildComposite);
    toolkit.adapt(buildComposite);
  }

  private void createPluginsTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem pluginsTabItem = new CTabItem(tabFolder, SWT.NONE);
    pluginsTabItem.setText(Messages.ProfilesPage_tab_plugins);

    pluginsComposite = new PluginsComposite(tabFolder, this, SWT.NONE);
    pluginsTabItem.setControl(pluginsComposite);
    toolkit.adapt(pluginsComposite);
  }

  private void createDependenciesTab(CTabFolder tabFolder, FormToolkit toolkit) {
    CTabItem dependenciesTabItem = new CTabItem(tabFolder, SWT.NONE);
    dependenciesTabItem.setText(Messages.ProfilesPage_tab_dependencies);
  
    dependenciesComposite = new DependenciesComposite(tabFolder, this, SWT.NONE, pomEditor);
    dependenciesTabItem.setControl(dependenciesComposite);
    toolkit.adapt(dependenciesComposite);
  }

  private void createRepositoriesTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem repositoriesTabItem = new CTabItem(tabFolder, SWT.NONE);
    repositoriesTabItem.setText(Messages.ProfilesPage_tab_repositories);

    repositoriesComposite = new RepositoriesComposite(tabFolder, SWT.NONE);
    repositoriesTabItem.setControl(repositoriesComposite);
    toolkit.adapt(repositoriesComposite);
  }

  private void createReportsTab(FormToolkit toolkit, CTabFolder tabFolder) {
    CTabItem reportingTabItem = new CTabItem(tabFolder, SWT.NONE);
    reportingTabItem.setText(Messages.ProfilesPage_tab_reporting);

    reportingComposite = new ReportingComposite(tabFolder, this, SWT.NONE);
    toolkit.adapt(reportingComposite);
    reportingTabItem.setControl(reportingComposite);
  }

  private void createActivationTab(CTabFolder tabFolder, FormToolkit toolkit) {
    CTabItem activationTabItem = new CTabItem(tabFolder, SWT.NONE);
    activationTabItem.setText(Messages.ProfilesPage_tab_activation);

    Composite activationComposite = new Composite(tabFolder, SWT.NONE);
    toolkit.paintBordersFor(activationComposite);
    GridLayout activationLayout = new GridLayout(2, false);
    activationLayout.marginWidth = 0;
    activationComposite.setLayout(activationLayout);
    activationTabItem.setControl(activationComposite);
    toolkit.adapt(activationComposite);
    
    activeByDefaultbutton = toolkit.createButton(activationComposite, Messages.ProfilesPage_btnActivateByDefault, SWT.CHECK);
    activeByDefaultbutton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    Section propertySection = toolkit.createSection(activationComposite, ExpandableComposite.TITLE_BAR);
    propertySection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    propertySection.setText(Messages.ProfilesPage_section_property);

    Composite propertyComposite = toolkit.createComposite(propertySection, SWT.NONE);
    GridLayout propertyLayout = new GridLayout(2, false);
    propertyLayout.marginHeight = 2;
    propertyLayout.marginWidth = 1;
    propertyComposite.setLayout(propertyLayout);
    propertySection.setClient(propertyComposite);
    toolkit.paintBordersFor(propertyComposite);

    Label propertyNameLabel = toolkit.createLabel(propertyComposite, Messages.ProfilesPage_lblName, SWT.NONE);

    activationPropertyNameText = toolkit.createText(propertyComposite, null, SWT.NONE);
    activationPropertyNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label propertyValueLabel = toolkit.createLabel(propertyComposite, Messages.ProfilesPage_lblValue, SWT.NONE);

    activationPropertyValueText = toolkit.createText(propertyComposite, null, SWT.NONE);
    activationPropertyValueText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Section osSection = toolkit.createSection(activationComposite, ExpandableComposite.TITLE_BAR);
    osSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));
    osSection.setText(Messages.ProfilesPage_section_os);

    Composite osComposite = new Composite(osSection, SWT.NONE);
    GridLayout osLayout = new GridLayout(2, false);
    osLayout.marginWidth = 1;
    osLayout.marginHeight = 2;
    osComposite.setLayout(osLayout);
    toolkit.paintBordersFor(osComposite);
    toolkit.adapt(osComposite);
    osSection.setClient(osComposite);

    toolkit.createLabel(osComposite, Messages.ProfilesPage_lblName, SWT.NONE);

    activationOsNameText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, Messages.ProfilesPage_lblFamily, SWT.NONE);

    activationOsFamilyText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsFamilyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, Messages.ProfilesPage_lblArchitecture, SWT.NONE);

    activationOsArchitectureText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsArchitectureText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    toolkit.createLabel(osComposite, Messages.ProfilesPage_lblVersion, SWT.NONE);

    activationOsVersionText = toolkit.createText(osComposite, null, SWT.NONE);
    activationOsVersionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Section fileSection = toolkit.createSection(activationComposite, ExpandableComposite.TITLE_BAR);
    GridData fileGridData = new GridData(SWT.FILL, SWT.TOP, false, false);
    fileGridData.verticalIndent = 5;
    fileSection.setLayoutData(fileGridData);
    fileSection.setText(Messages.ProfilesPage_section_file);

    Composite fileComposite = toolkit.createComposite(fileSection, SWT.NONE);
    GridLayout fileCompositeLayout = new GridLayout();
    fileCompositeLayout.marginWidth = 1;
    fileCompositeLayout.marginHeight = 2;
    fileCompositeLayout.numColumns = 2;
    fileComposite.setLayout(fileCompositeLayout);
    toolkit.paintBordersFor(fileComposite);
    fileSection.setClient(fileComposite);

    Label fileExistLabel = toolkit.createLabel(fileComposite, Messages.ProfilesPage_lblExists, SWT.NONE);

    activationFileExistText = toolkit.createText(fileComposite, null, SWT.NONE);
    activationFileExistText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label fileMissingLabel = toolkit.createLabel(fileComposite, Messages.ProfilesPage_lblMissing, SWT.NONE);

    activationFileMissingText = toolkit.createText(fileComposite, null, SWT.NONE);
    activationFileMissingText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    
    Section jdkSection = toolkit.createSection(activationComposite, ExpandableComposite.TITLE_BAR);
    jdkSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    jdkSection.setText(Messages.ProfilesPage_section_jdk);

    Composite composite = toolkit.createComposite(jdkSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 2;
    gridLayout.marginWidth = 1;
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);
    toolkit.paintBordersFor(composite);
    jdkSection.setClient(composite);

    Label jdkLabel = toolkit.createLabel(composite, Messages.ProfilesPage_lblJdk, SWT.NONE);

    activationJdkText = toolkit.createText(composite, null, SWT.NONE);
    activationJdkText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    WidthGroup widthGroup = new WidthGroup();
    activationComposite.addControlListener(widthGroup);
    widthGroup.addControl(propertyNameLabel);
    widthGroup.addControl(propertyValueLabel);
    widthGroup.addControl(fileExistLabel);
    widthGroup.addControl(fileMissingLabel);
    widthGroup.addControl(jdkLabel);
  }

  public void loadData() {
    profilesEditor.setInput(model.getProfiles());
  }

  protected void doUpdate(Notification notification){
    Object object = notification.getNotifier();
    Object feature = notification.getFeature();
    
    if(feature == PomPackage.Literals.MODEL__PROFILES) {
      profilesEditor.refresh();
    }
    
    if(object instanceof Profile) {
      profilesEditor.refresh();
      if(currentProfile == object)
        updateProfileDetails((Profile) object);
    }
    
    if(feature == PomPackage.Literals.PROFILE__MODULES) {
      modulesEditor.refresh();
    }

    if(object instanceof Activation) {
      EObject container = ((Activation) object).eContainer();
      if(container==currentProfile) {
        updateActivationTab();
      }
    }
    
    if(object instanceof ActivationFile || object instanceof ActivationOS || object instanceof ActivationProperty) {
      Activation activation = (Activation) ((EObject) object).eContainer();
      EObject container = activation.eContainer();
      if(container == currentProfile) {
        updateActivationTab();
      }
    }
    dependenciesComposite.updateView(this, notification);
    repositoriesComposite.updateView(this, notification);
    buildComposite.updateView(this, notification);
    pluginsComposite.updateView(this, notification);
    reportingComposite.updateView(this, notification);
  }
  
  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        doUpdate(notification);
      }
    });

  }

  void createNewModule(String moduleName) {
    CompoundCommand compoundCommand = new CompoundCommand();
    EditingDomain editingDomain = getEditingDomain();

    Command addCommand = AddCommand.create(editingDomain, currentProfile, POM_PACKAGE.getProfile_Modules(), moduleName);
    compoundCommand.append(addCommand);
    
    editingDomain.getCommandStack().execute(compoundCommand);

    modulesEditor.setSelection(Collections.singletonList(moduleName));
  }

}
