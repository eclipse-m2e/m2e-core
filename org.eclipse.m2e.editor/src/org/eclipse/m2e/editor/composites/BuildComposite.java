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

import static org.eclipse.m2e.editor.pom.FormUtils.setButton;
import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.BuildBase;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Eugene Kuleshov
 */
public class BuildComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
  
  MavenPomEditorPage parent;
  
  // controls
  Text defaultGoalText;
  Text directoryText;
  Text finalNameText;

  ListEditorComposite<String> filtersEditor;

  ListEditorComposite<Resource> resourcesEditor;
  ListEditorComposite<Resource> testResourcesEditor;

  Text resourceDirectoryText;
  Text resourceTargetPathText;
  ListEditorComposite<String> resourceIncludesEditor;
  ListEditorComposite<String> resourceExcludesEditor;

  Button resourceFilteringButton;
  Section resourceDetailsSection;
  
  // model
  Resource currentResource;

  boolean changingSelection = false;

  ValueProvider<BuildBase> buildProvider;

  
  public BuildComposite(Composite parent, int flags) {
    super(parent, flags);
    
    toolkit.adapt(this);
  
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.makeColumnsEqualWidth = true;
    setLayout(layout);
  
    createBuildSection();
  }

  private void createBuildSection() {
    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    toolkit.adapt(horizontalSash);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    Section buildSection = toolkit.createSection(horizontalSash, ExpandableComposite.TITLE_BAR);
    buildSection.setText(Messages.BuildComposite_section_build);
  
    Composite composite = toolkit.createComposite(buildSection, SWT.NONE);
    GridLayout compositeLayout = new GridLayout(2, false);
    compositeLayout.marginWidth = 1;
    compositeLayout.marginHeight = 2;
    composite.setLayout(compositeLayout);
    toolkit.paintBordersFor(composite);
    buildSection.setClient(composite);
  
    toolkit.createLabel(composite, Messages.BuildComposite_lblDefaultGoal, SWT.NONE);
  
    defaultGoalText = toolkit.createText(composite, null, SWT.NONE);
    defaultGoalText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildComposite_lblDirectory, SWT.NONE);
  
    directoryText = toolkit.createText(composite, null, SWT.NONE);
    directoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    toolkit.createLabel(composite, Messages.BuildComposite_lblFinalName, SWT.NONE);
  
    finalNameText = toolkit.createText(composite, null, SWT.NONE);
    finalNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label filtersLabel = toolkit.createLabel(composite, Messages.BuildComposite_lblFilters, SWT.NONE);
    filtersLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
  
    filtersEditor = new ListEditorComposite<String>(composite, SWT.NONE);
    GridData filtersEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    filtersEditorData.heightHint = 47;
    filtersEditor.setLayoutData(filtersEditorData);
    toolkit.adapt(filtersEditor);
    toolkit.paintBordersFor(filtersEditor);

    filtersEditor.setContentProvider(new ListEditorContentProvider<String>());
    filtersEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_FILTER));
    
    filtersEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        boolean created = false;
        BuildBase build = createBuildBase(compoundCommand, editingDomain);
        EList<String> filters = build.getFilters();
        
        String filter = "?";
        
        Command addCommand = AddCommand.create(editingDomain, build, POM_PACKAGE.getBuildBase_Filters(), filter);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        if(created) {
          filtersEditor.setInput(filters);
        }
        filtersEditor.setSelection(Collections.singletonList(filter));
      }
    });
    
    filtersEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<String> selection = filtersEditor.getSelection();
        for(String filter : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, buildProvider.getValue(), //
              POM_PACKAGE.getBuildBase_Filters(), filter);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    filtersEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        int n = filtersEditor.getViewer().getTable().getSelectionIndex();
        EList<String> filters = buildProvider.getValue().getFilters();
        if(!value.equals(filters.get(n))) {
          EditingDomain editingDomain = parent.getEditingDomain();
          Command command = SetCommand.create(editingDomain, buildProvider.getValue(), //
              POM_PACKAGE.getBuildBase_Filters(), value, n);
          editingDomain.getCommandStack().execute(command);
          filtersEditor.refresh();
        }
      }
    });
    
    ///
    
    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);

    createResourceSection(verticalSash);
    createTestResourcesSection(verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    createResourceDetailsSection(horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1, 1});
  }

  private void createResourceDetailsSection(SashForm horizontalSash) {
    resourceDetailsSection = toolkit.createSection(horizontalSash, ExpandableComposite.TITLE_BAR);
    resourceDetailsSection.setText(Messages.BuildComposite_sectionResourceDetails);
  
    Composite resourceDetailsComposite = toolkit.createComposite(resourceDetailsSection, SWT.NONE);
    GridLayout gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 1;
    gridLayout.marginHeight = 2;
    resourceDetailsComposite.setLayout(gridLayout);
    toolkit.paintBordersFor(resourceDetailsComposite);
    resourceDetailsSection.setClient(resourceDetailsComposite);
  
    Label resourceDirectoryLabel = toolkit.createLabel(resourceDetailsComposite, Messages.BuildComposite_lblDirectory, SWT.NONE);
    resourceDirectoryLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  
    resourceDirectoryText = toolkit.createText(resourceDetailsComposite, null, SWT.NONE);
    resourceDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    Label resourceTargetPathLabel = toolkit.createLabel(resourceDetailsComposite, Messages.BuildComposite_lblTargetPath, SWT.NONE);
    resourceTargetPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
  
    resourceTargetPathText = toolkit.createText(resourceDetailsComposite, null, SWT.NONE);
    resourceTargetPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  
    resourceFilteringButton = toolkit.createButton(resourceDetailsComposite, Messages.BuildComposite_btnFiltering, SWT.CHECK);
    resourceFilteringButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    Label includesLabel = toolkit.createLabel(resourceDetailsComposite, Messages.BuildComposite_lblIncludes, SWT.NONE);
    includesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    resourceIncludesEditor = new ListEditorComposite<String>(resourceDetailsComposite, SWT.NONE);
    GridData includesEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    includesEditorData.heightHint = 60;
    resourceIncludesEditor.setLayoutData(includesEditorData);
    toolkit.adapt(resourceIncludesEditor);
    toolkit.paintBordersFor(resourceIncludesEditor);
  
    resourceIncludesEditor.setContentProvider(new ListEditorContentProvider<String>());
    resourceIncludesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_INCLUDE));
    
    resourceIncludesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        boolean created = false;
        EList<String> includes = currentResource.getIncludes();
        

        String include = "?";
        Command addCommand = AddCommand.create(editingDomain, currentResource, POM_PACKAGE.getResource_Includes(), include);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        if(created) {
          resourceIncludesEditor.setInput(includes);
        }
        resourceIncludesEditor.setSelection(Collections.singletonList(include));
      }
    });
    
    resourceIncludesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<String> selection = resourceIncludesEditor.getSelection();
        for(String include : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentResource, //
              POM_PACKAGE.getResource_Includes(), include);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    resourceIncludesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        int n = resourceIncludesEditor.getViewer().getTable().getSelectionIndex();
        EList<String> includes = currentResource.getIncludes();
        if(!value.equals(includes.get(n))) {
          EditingDomain editingDomain = parent.getEditingDomain();
          Command command = SetCommand.create(editingDomain, currentResource, //
              POM_PACKAGE.getResource_Includes(), value, n);
          editingDomain.getCommandStack().execute(command);
          resourceIncludesEditor.refresh();
        }
      }
    });
    
    Label excludesLabel = toolkit.createLabel(resourceDetailsComposite, Messages.BuildComposite_lblExcludes, SWT.NONE);
    excludesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
  
    resourceExcludesEditor = new ListEditorComposite<String>(resourceDetailsComposite, SWT.NONE);
    GridData excludesEditorData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
    excludesEditorData.heightHint = 60;
    resourceExcludesEditor.setLayoutData(excludesEditorData);
    toolkit.adapt(resourceExcludesEditor);
    toolkit.paintBordersFor(resourceExcludesEditor);
    
    resourceExcludesEditor.setContentProvider(new ListEditorContentProvider<String>());
    resourceExcludesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_EXCLUDE));
    
    resourceExcludesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        boolean created = false;
        EList<String> excludes = currentResource.getExcludes();

        String exclude = "?";
        Command addCommand = AddCommand.create(editingDomain, currentResource, POM_PACKAGE.getResource_Excludes(), exclude);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        if(created) {
          resourceExcludesEditor.setInput(excludes);
        }
        resourceExcludesEditor.setSelection(Collections.singletonList(exclude));
      }
    });
    
    resourceExcludesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<String> selection = resourceExcludesEditor.getSelection();
        for(String exclude : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, currentResource, //
              POM_PACKAGE.getResource_Excludes(), exclude);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
    
    resourceExcludesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        int n = resourceExcludesEditor.getViewer().getTable().getSelectionIndex();
        EList<String> excludes = currentResource.getExcludes();
        if(!value.equals(excludes.get(n))) {
          EditingDomain editingDomain = parent.getEditingDomain();
          Command command = SetCommand.create(editingDomain, currentResource, //
              POM_PACKAGE.getResource_Excludes(), value, n);
          editingDomain.getCommandStack().execute(command);
          resourceExcludesEditor.refresh();
        }
      }
    });
    
  }

  private void createResourceSection(SashForm verticalSash) {
    Section resourcesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    resourcesSection.setText(Messages.BuildComposite_sectionResources);
  
    resourcesEditor = new ListEditorComposite<Resource>(resourcesSection, SWT.NONE);
    resourcesSection.setClient(resourcesEditor);
    toolkit.adapt(resourcesEditor);
    toolkit.paintBordersFor(resourcesEditor);
    
    resourcesEditor.setContentProvider(new ListEditorContentProvider<Resource>());
    resourcesEditor.setLabelProvider(new ResourceLabelProvider());
    
    resourcesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Resource> selection = resourcesEditor.getSelection();
        loadResourceDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            testResourcesEditor.setSelection(Collections.<Resource>emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });
    
    resourcesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        boolean created = false;
        BuildBase build = createBuildBase(compoundCommand, editingDomain);
        EList<Resource> resources = build.getResources();
        
        Resource resource = PomFactory.eINSTANCE.createResource();        
        Command addCommand = AddCommand.create(editingDomain, build, POM_PACKAGE.getBuildBase_Resources(), resource);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        if(created) {
          resourcesEditor.setInput(resources);
        }
        resourcesEditor.setSelection(Collections.singletonList(resource));
        resourceDirectoryText.setFocus();
      }
    });
    
    resourcesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Resource> selection = resourcesEditor.getSelection();
        for(Resource resource : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, buildProvider.getValue(), //
              POM_PACKAGE.getBuildBase_Resources(), resource);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
  }

  private void createTestResourcesSection(SashForm verticalSash) {
    Section testResourcesSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    testResourcesSection.setText(Messages.BuildComposite_sectionTestResources);
    toolkit.adapt(verticalSash, true, true);
    
    testResourcesEditor = new ListEditorComposite<Resource>(testResourcesSection, SWT.NONE);
    testResourcesSection.setClient(testResourcesEditor);
    toolkit.adapt(testResourcesEditor);
    toolkit.paintBordersFor(testResourcesEditor);

    testResourcesEditor.setContentProvider(new ListEditorContentProvider<Resource>());
    testResourcesEditor.setLabelProvider(new ResourceLabelProvider());

    testResourcesEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Resource> selection = testResourcesEditor.getSelection();
        loadResourceDetails(selection.size()==1 ? selection.get(0) : null);
        
        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            resourcesEditor.setSelection(Collections.<Resource>emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });
    
    testResourcesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
        
        boolean created = false;
        BuildBase build = createBuildBase(compoundCommand, editingDomain);
        EList<Resource> testResources = build.getTestResources();
        
        Resource resource = PomFactory.eINSTANCE.createResource();        
        Command addCommand = AddCommand.create(editingDomain, build, POM_PACKAGE.getBuildBase_TestResources(), resource);
        compoundCommand.append(addCommand);
        
        editingDomain.getCommandStack().execute(compoundCommand);
        
        if(created) {
          testResourcesEditor.setInput(testResources);
        }
        testResourcesEditor.setSelection(Collections.singletonList(resource));
        resourceDirectoryText.setFocus();
      }
    });
    
    testResourcesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();
 
        List<Resource> selection = testResourcesEditor.getSelection();
        for(Resource resource : selection) {
          Command removeCommand = RemoveCommand.create(editingDomain, buildProvider.getValue(), //
              POM_PACKAGE.getBuildBase_TestResources(), resource);
          compoundCommand.append(removeCommand);
        }
        
        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });
  }
  
  public void loadData(MavenPomEditorPage editorPage, ValueProvider<BuildBase> buildProvider) {
    this.parent = editorPage;
    this.buildProvider = buildProvider;
    
    loadBuild();
    loadResources();
    loadTestResources();
    
    loadResourceDetails(null);
    
    filtersEditor.setReadOnly(parent.isReadOnly());    
    resourcesEditor.setReadOnly(parent.isReadOnly());    
    testResourcesEditor.setReadOnly(parent.isReadOnly());
    
    resourceIncludesEditor.setReadOnly(parent.isReadOnly());
    resourceExcludesEditor.setReadOnly(parent.isReadOnly());
  }

  public void updateView(MavenPomEditorPage editorPage, Notification notification) {
    Object object = notification.getNotifier();
    
    Object feature = notification.getFeature();
    
    if(object instanceof BuildBase) {
      loadBuild();
    }
    
    if(feature == PomPackage.Literals.BUILD_BASE__FILTERS) {
      filtersEditor.refresh();
    }
    
    if(feature == PomPackage.Literals.BUILD_BASE__RESOURCES) {
      resourcesEditor.refresh();
    }
    
    if(feature == PomPackage.Literals.BUILD_BASE__TEST_RESOURCES) {
      testResourcesEditor.refresh();
    }
    
    if(object instanceof Resource) {
      resourcesEditor.refresh();
      testResourcesEditor.refresh();
      if(object == currentResource) {
        Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
        if(notificationObject == null || notificationObject instanceof Resource) {
          loadResourceDetails((Resource) notificationObject);
        }
      }
    }
    
    if(feature == PomPackage.Literals.RESOURCE__INCLUDES) {
      resourceIncludesEditor.refresh();
    }

    if(feature == PomPackage.Literals.RESOURCE__EXCLUDES) {
      resourceExcludesEditor.refresh();
    }

    // XXX handle other notification types
  }
  
  private void loadBuild() {
    if(parent != null) {
      parent.removeNotifyListener(defaultGoalText);
      parent.removeNotifyListener(directoryText);
      parent.removeNotifyListener(finalNameText);
    }
    
    BuildBase build = buildProvider.getValue();
    if(build!=null) {
      setText(defaultGoalText, build.getDefaultGoal());
      setText(directoryText, build.getDirectory());
      setText(finalNameText, build.getFinalName());
    } else {
      setText(defaultGoalText, ""); //$NON-NLS-1$
      setText(directoryText, ""); //$NON-NLS-1$
      setText(finalNameText, ""); //$NON-NLS-1$
    }
    
    filtersEditor.setInput(build == null //
        || build.getFilters() == null ? null : build.getFilters());
    
    parent.setModifyListener(defaultGoalText, buildProvider, POM_PACKAGE.getBuildBase_DefaultGoal(), ""); //$NON-NLS-1$
    parent.setModifyListener(directoryText, buildProvider, POM_PACKAGE.getBuildBase_Directory(), ""); //$NON-NLS-1$
    parent.setModifyListener(finalNameText, buildProvider, POM_PACKAGE.getBuildBase_FinalName(), ""); //$NON-NLS-1$
  }
  
  private void loadResources() {
    BuildBase build = buildProvider.getValue();
    resourcesEditor.setInput(build == null //
        || build.getResources() == null ? null : build.getResources());
  }
  
  private void loadTestResources() {
    BuildBase build = buildProvider.getValue();
    testResourcesEditor.setInput(build == null //
        || build.getTestResources() == null ? null : build.getTestResources());
  }

  void loadResourceDetails(Resource resource) {
    if(changingSelection) {
      return;
    }
    
    currentResource = resource;
    
    if(parent != null) {
      parent.removeNotifyListener(resourceDirectoryText);
      parent.removeNotifyListener(resourceTargetPathText);
      parent.removeNotifyListener(resourceFilteringButton);
    }
    
    if(resource == null) {
      FormUtils.setEnabled(resourceDetailsSection, false);
      
      setText(resourceDirectoryText, ""); //$NON-NLS-1$
      setText(resourceTargetPathText, ""); //$NON-NLS-1$
      setButton(resourceFilteringButton, false);
      
      resourceIncludesEditor.setInput(null);
      resourceExcludesEditor.setInput(null);
      
      return;
    }

    FormUtils.setEnabled(resourceDetailsSection, true);
    FormUtils.setReadonly(resourceDetailsSection, parent.isReadOnly());
    
    setText(resourceDirectoryText, resource.getDirectory());
    setText(resourceTargetPathText, resource.getTargetPath());
    setButton(resourceFilteringButton, "true".equals(resource.getFiltering()));
    
    resourceIncludesEditor.setInput(resource.getIncludes()==null ? null : resource.getIncludes());
    resourceExcludesEditor.setInput(resource.getExcludes()==null ? null : resource.getExcludes());
    
    ValueProvider<Resource> provider = new ValueProvider.DefaultValueProvider<Resource>(resource);
    parent.setModifyListener(resourceDirectoryText, provider, POM_PACKAGE.getResource_Directory(), ""); //$NON-NLS-1$
    parent.setModifyListener(resourceTargetPathText, provider, POM_PACKAGE.getResource_TargetPath(), ""); //$NON-NLS-1$
    parent.setModifyListener(resourceFilteringButton, provider, POM_PACKAGE.getResource_Filtering(), "false");
    
    parent.registerListeners();
  }

  BuildBase createBuildBase(CompoundCommand compoundCommand, EditingDomain editingDomain) {
     BuildBase build = buildProvider.getValue();
    if(build == null) {
      build = buildProvider.create(editingDomain, compoundCommand);
    }
    return build;
  }

  /**
   * Label provider for {@link Resource}
   */
  public class ResourceLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Resource) {
        return ((Resource) element).getDirectory();
      }
      return super.getText(element);
    }
    
    public Image getImage(Object element) {
      return MavenEditorImages.IMG_RESOURCE;
    }
    
  }
  
}
