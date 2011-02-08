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

import static org.eclipse.m2e.editor.pom.FormUtils.setText;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.m2e.core.ui.internal.wizards.WidthGroup;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.pom.FormUtils;
import org.eclipse.m2e.editor.pom.MavenPomEditorPage;
import org.eclipse.m2e.editor.pom.PropertiesSection;
import org.eclipse.m2e.editor.pom.ValueProvider;
import org.eclipse.m2e.model.edit.pom.Contributor;
import org.eclipse.m2e.model.edit.pom.Developer;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;


/**
 * @author Dmitry Platonoff
 */
public class TeamComposite extends Composite {

  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

  MavenPomEditorPage parent;

  // controls

  Model model;
  
  ListEditorComposite<Developer> developersEditor;

  ListEditorComposite<Contributor> contributorsEditor;

  Composite detailsComposite;

  Text userIdText;

  Text userNameText;

  Text userEmailText;

  Text userUrlText;

  CCombo userTimezoneText;

  Text organizationNameText;

  Text organizationUrlText;

  ListEditorComposite<String> rolesEditor;

  Label userIdLabel;

  // model
  EObject currentSelection;
  
  boolean changingSelection = false;

  private PropertiesSection propertiesSection;

  public TeamComposite(MavenPomEditorPage editorPage, Composite composite, int flags) {
    super(composite, flags);
    this.parent = editorPage;
    createComposite();
  }

  private void createComposite() {
    GridLayout gridLayout = new GridLayout();
    gridLayout.makeColumnsEqualWidth = true;
    gridLayout.marginWidth = 0;
    setLayout(gridLayout);
    toolkit.adapt(this);

    SashForm horizontalSash = new SashForm(this, SWT.NONE);
    horizontalSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(horizontalSash, true, true);

    SashForm verticalSash = new SashForm(horizontalSash, SWT.VERTICAL);
    toolkit.adapt(verticalSash, true, true);

    createDevelopersSection(toolkit, verticalSash);
    createContributorsSection(toolkit, verticalSash);

    verticalSash.setWeights(new int[] {1, 1});

    createDetailsPanel(toolkit, horizontalSash);

    horizontalSash.setWeights(new int[] {1, 1});
  }

  private void createDevelopersSection(FormToolkit toolkit, SashForm verticalSash) {
    Section developersSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    developersSection.setText(Messages.TeamComposite_section_developers);

    developersEditor = new ListEditorComposite<Developer>(developersSection, SWT.NONE);

    developersEditor.setContentProvider(new ListEditorContentProvider<Developer>());
    developersEditor.setLabelProvider(new TeamLabelProvider());

    developersEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Developer developer = PomFactory.eINSTANCE.createDeveloper();
        Command addDependencyCommand = AddCommand.create(editingDomain, model, POM_PACKAGE
            .getModel_Developers(), developer);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        developersEditor.setSelection(Collections.singletonList(developer));
        updateDetails(developer);
        userIdText.setFocus();
      }
    });

    developersEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Developer> developerList = developersEditor.getSelection();
        for(Developer developer : developerList) {
          Command removeCommand = RemoveCommand.create(editingDomain, model, POM_PACKAGE
              .getModel_Developers(), developer);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateDetails(null);
      }
    });

    developersEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Developer> selection = developersEditor.getSelection();
        updateDetails(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            contributorsEditor.setSelection(Collections.<Contributor> emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    developersSection.setClient(developersEditor);
    toolkit.paintBordersFor(developersEditor);
    toolkit.adapt(developersEditor);
  }

  private void createContributorsSection(FormToolkit toolkit, SashForm verticalSash) {
    Section contributorsSection = toolkit.createSection(verticalSash, ExpandableComposite.TITLE_BAR);
    contributorsSection.setText(Messages.TeamComposite_section_contributors);

    contributorsEditor = new ListEditorComposite<Contributor>(contributorsSection, SWT.NONE);
    contributorsEditor.setContentProvider(new ListEditorContentProvider<Contributor>());
    contributorsEditor.setLabelProvider(new TeamLabelProvider());

    contributorsEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        Contributor contributor = PomFactory.eINSTANCE.createContributor();
        Command addDependencyCommand = AddCommand.create(editingDomain, model, POM_PACKAGE
            .getModel_Contributors(), contributor);
        compoundCommand.append(addDependencyCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        contributorsEditor.setSelection(Collections.singletonList(contributor));
        updateDetails(contributor);
        userNameText.setFocus();
      }
    });

    contributorsEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        List<Contributor> contributorList = contributorsEditor.getSelection();
        for(Contributor contributor : contributorList) {
          Command removeCommand = RemoveCommand.create(editingDomain, model, POM_PACKAGE
              .getModel_Contributors(), contributor);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
        updateDetails(null);
      }
    });

    contributorsEditor.addSelectionListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        List<Contributor> selection = contributorsEditor.getSelection();
        updateDetails(selection.size() == 1 ? selection.get(0) : null);

        if(!selection.isEmpty()) {
          changingSelection = true;
          try {
            developersEditor.setSelection(Collections.<Developer> emptyList());
          } finally {
            changingSelection = false;
          }
        }
      }
    });

    contributorsSection.setClient(contributorsEditor);
    toolkit.paintBordersFor(contributorsEditor);
    toolkit.adapt(contributorsEditor);
  }

  private void createDetailsPanel(FormToolkit toolkit, SashForm horizontalSash) {
    detailsComposite = toolkit.createComposite(horizontalSash, SWT.NONE);
    GridLayout detailsCompositeGridLayout = new GridLayout();
    detailsCompositeGridLayout.marginLeft = 5;
    detailsCompositeGridLayout.marginWidth = 0;
    detailsCompositeGridLayout.marginHeight = 0;
    detailsComposite.setLayout(detailsCompositeGridLayout);
    toolkit.paintBordersFor(detailsComposite);

    Section userDetailsSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
    GridData gd_userDetailsSection = new GridData(SWT.FILL, SWT.CENTER, true, false);
    userDetailsSection.setLayoutData(gd_userDetailsSection);
    userDetailsSection.setText(Messages.TeamComposite_section_userdetails);

    Composite userDetailsComposite = toolkit.createComposite(userDetailsSection, SWT.NONE);
    userDetailsComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(userDetailsComposite);
    userDetailsSection.setClient(userDetailsComposite);

    userIdLabel = toolkit.createLabel(userDetailsComposite, Messages.TeamComposite_lblId, SWT.NONE);

    userIdText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userNameLabel = toolkit.createLabel(userDetailsComposite, Messages.TeamComposite_lblName, SWT.NONE);

    userNameText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userEmailLabel = toolkit.createLabel(userDetailsComposite, Messages.TeamComposite_lblEmail, SWT.NONE);

    userEmailText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userEmailText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink userUrlLabel = toolkit.createHyperlink(userDetailsComposite, Messages.TeamComposite_lblUrl, SWT.NONE);
    userUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(userUrlText.getText());
      }
    });

    userUrlText = toolkit.createText(userDetailsComposite, null, SWT.NONE);
    userUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label userTimezoneLabel = toolkit.createLabel(userDetailsComposite, Messages.TeamComposite_lblTimezone, SWT.NONE);

    userTimezoneText = new CCombo(userDetailsComposite, SWT.FLAT);
    userTimezoneText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    userTimezoneText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);

    Section organizationSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
    organizationSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    organizationSection.setText(Messages.TeamComposite_section_organization);

    Composite organizationComposite = toolkit.createComposite(organizationSection, SWT.NONE);
    organizationComposite.setLayout(new GridLayout(2, false));
    toolkit.paintBordersFor(organizationComposite);
    organizationSection.setClient(organizationComposite);

    Label organizationNameLabel = toolkit.createLabel(organizationComposite, Messages.TeamComposite_lblName, SWT.NONE);

    organizationNameText = toolkit.createText(organizationComposite, null, SWT.NONE);
    organizationNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Hyperlink organizationUrlLabel = toolkit.createHyperlink(organizationComposite, Messages.TeamComposite_lblUrl, SWT.NONE);
    organizationUrlLabel.addHyperlinkListener(new HyperlinkAdapter() {
      public void linkActivated(HyperlinkEvent e) {
        FormUtils.openHyperlink(organizationUrlText.getText());
      }
    });

    organizationUrlText = toolkit.createText(organizationComposite, null, SWT.NONE);
    organizationUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    WidthGroup widthGroup = new WidthGroup();
    widthGroup.addControl(userIdLabel);
    widthGroup.addControl(userNameLabel);
    widthGroup.addControl(userEmailLabel);
    widthGroup.addControl(userUrlLabel);
    widthGroup.addControl(userTimezoneLabel);
    widthGroup.addControl(organizationNameLabel);
    widthGroup.addControl(organizationUrlLabel);
    userDetailsComposite.addControlListener(widthGroup);
    userDetailsComposite.setTabList(new Control[] {userIdText, userNameText, userEmailText, userUrlText, userTimezoneText});
    organizationComposite.addControlListener(widthGroup);
    organizationComposite.setTabList(new Control[] {organizationNameText, organizationUrlText});

    createRolesSection(toolkit, detailsComposite);
    createPropertiesSection(toolkit, detailsComposite);
  }

  private void createRolesSection(FormToolkit toolkit, Composite detailsComposite) {
    Section rolesSection = toolkit.createSection(detailsComposite, ExpandableComposite.TITLE_BAR);
    rolesSection.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    rolesSection.setText(Messages.TeamComposite_section_roles);

    rolesEditor = new ListEditorComposite<String>(rolesSection, SWT.NONE);
    toolkit.paintBordersFor(rolesEditor);
    toolkit.adapt(rolesEditor);
    rolesSection.setClient(rolesEditor);

    rolesEditor.setContentProvider(new ListEditorContentProvider<String>());
    rolesEditor.setLabelProvider(new StringLabelProvider(MavenEditorImages.IMG_ROLE));

    rolesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        EObject parentObject = currentSelection;
        EStructuralFeature feature = null;
        if(currentSelection != null) {
          if(currentSelection instanceof Contributor) {
            feature = POM_PACKAGE.getContributor_Roles();
          } else if(currentSelection instanceof Developer) {
            feature = POM_PACKAGE.getDeveloper_Roles();
          }
        }

        Command addRoleCommand = AddCommand.create(editingDomain, parentObject, feature, "?");
        compoundCommand.append(addRoleCommand);

        editingDomain.getCommandStack().execute(compoundCommand);

        updateRoles((EList<String>)parentObject.eGet(feature));
      }
    });

    rolesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        CompoundCommand compoundCommand = new CompoundCommand();
        EditingDomain editingDomain = parent.getEditingDomain();

        EObject parentObject = currentSelection;
        EStructuralFeature feature = null;
        if(currentSelection != null) {
          if(currentSelection instanceof Contributor) {
            feature = POM_PACKAGE.getContributor_Roles();
          } else if(currentSelection instanceof Developer) {
            feature = POM_PACKAGE.getDeveloper_Roles();
          }
        }
        List<String> roleList = rolesEditor.getSelection();
        for(String role : roleList) {
          Command removeCommand = RemoveCommand.create(editingDomain, parentObject, feature, role);
          compoundCommand.append(removeCommand);
        }

        editingDomain.getCommandStack().execute(compoundCommand);
      }
    });

    rolesEditor.setCellModifier(new ICellModifier() {
      public boolean canModify(Object element, String property) {
        return true;
      }
 
      public Object getValue(Object element, String property) {
        return element;
      }
 
      public void modify(Object element, String property, Object value) {
        int n = rolesEditor.getViewer().getTable().getSelectionIndex();
        if(!value.equals(getRoles().get(n))) {
          EditingDomain editingDomain = parent.getEditingDomain();
          EObject parentObject = currentSelection;
          EStructuralFeature feature = null;
          if(currentSelection != null) {
            if(currentSelection instanceof Contributor) {
              feature = POM_PACKAGE.getContributor_Roles();
            } else if(currentSelection instanceof Developer) {
              feature = POM_PACKAGE.getDeveloper_Roles();
            }
          }
          Command command = SetCommand.create(editingDomain, parentObject,
              feature, value, n);
          editingDomain.getCommandStack().execute(command);
        }
      }
    });

  }

  private void createPropertiesSection(FormToolkit toolkit, Composite composite) {
    propertiesSection = new PropertiesSection(toolkit, composite, parent.getEditingDomain());
  }

  public void loadContributors() {
    changingSelection = true;
    contributorsEditor.setInput(model.getContributors());
    changingSelection = false;
  }

  void loadDevelopers() {
    changingSelection = true;
    developersEditor.setInput(model.getDevelopers());
    changingSelection = false;
  }

  protected void updateDetails(EObject eo) {
    if(changingSelection) {
      return;
    }

    this.currentSelection = eo;

    if(parent != null) {
      parent.removeNotifyListener(userIdText);
      parent.removeNotifyListener(userNameText);
      parent.removeNotifyListener(userEmailText);
      parent.removeNotifyListener(userUrlText);
      parent.removeNotifyListener(userTimezoneText);
      parent.removeNotifyListener(organizationNameText);
      parent.removeNotifyListener(organizationUrlText);
    }

    if(parent == null || eo == null) {
      FormUtils.setEnabled(detailsComposite, false);

      setText(userIdText, ""); //$NON-NLS-1$
      setText(userNameText, ""); //$NON-NLS-1$
      setText(userEmailText, ""); //$NON-NLS-1$
      setText(userUrlText, ""); //$NON-NLS-1$
      setText(userTimezoneText, ""); //$NON-NLS-1$

      setText(organizationNameText, ""); //$NON-NLS-1$
      setText(organizationUrlText, ""); //$NON-NLS-1$

      rolesEditor.setInput(null);

      return;
    }

    FormUtils.setEnabled(detailsComposite, true);
    FormUtils.setReadonly(detailsComposite, parent.isReadOnly());

    EList<String> roles = null;
    if(eo instanceof Contributor) {
      Contributor contributor = (Contributor) eo;
      updateContributorDetails(contributor);
      roles = contributor.getRoles();
      propertiesSection.setModel(contributor, POM_PACKAGE.getContributor_Properties());
    } else if(eo instanceof Developer) {
      Developer developer = (Developer) eo;
      updateDeveloperDetails(developer);
      roles = developer.getRoles();
      propertiesSection.setModel(developer, POM_PACKAGE.getDeveloper_Properties());
    }

    parent.registerListeners();

    updateRoles(roles);
  }

  protected void updateContributorDetails(Contributor contributor) {
    setText(userIdText, ""); //$NON-NLS-1$
    setText(userNameText, contributor.getName());
    setText(userEmailText, contributor.getEmail());
    setText(userUrlText, contributor.getUrl());
    setText(userTimezoneText, contributor.getTimezone());
    setText(organizationNameText, contributor.getOrganization());
    setText(organizationUrlText, contributor.getOrganizationUrl());

    userIdLabel.setEnabled(false);
    userIdText.setEnabled(false);

    ValueProvider<Contributor> contributorProvider = new ValueProvider.DefaultValueProvider<Contributor>(contributor);
    parent.setModifyListener(userNameText, contributorProvider, POM_PACKAGE.getContributor_Name(), ""); //$NON-NLS-1$
    parent.setModifyListener(userEmailText, contributorProvider, POM_PACKAGE.getContributor_Email(), ""); //$NON-NLS-1$
    parent.setModifyListener(userUrlText, contributorProvider, POM_PACKAGE.getContributor_Url(), ""); //$NON-NLS-1$
    parent.setModifyListener(userTimezoneText, contributorProvider, POM_PACKAGE.getContributor_Timezone(), ""); //$NON-NLS-1$
    parent.setModifyListener(organizationNameText, contributorProvider, POM_PACKAGE.getContributor_Organization(), ""); //$NON-NLS-1$
    parent
        .setModifyListener(organizationUrlText, contributorProvider, POM_PACKAGE.getContributor_OrganizationUrl(), ""); //$NON-NLS-1$
  }

  protected void updateDeveloperDetails(Developer developer) {
    setText(userIdText, developer.getId());
    setText(userNameText, developer.getName());
    setText(userEmailText, developer.getEmail());
    setText(userUrlText, developer.getUrl());
    setText(userTimezoneText, developer.getTimezone());
    setText(organizationNameText, developer.getOrganization());
    setText(organizationUrlText, developer.getOrganizationUrl());

    ValueProvider<Developer> developerProvider = new ValueProvider.DefaultValueProvider<Developer>(developer);
    parent.setModifyListener(userIdText, developerProvider, POM_PACKAGE.getDeveloper_Id(), ""); //$NON-NLS-1$
    parent.setModifyListener(userNameText, developerProvider, POM_PACKAGE.getDeveloper_Name(), ""); //$NON-NLS-1$
    parent.setModifyListener(userEmailText, developerProvider, POM_PACKAGE.getDeveloper_Email(), ""); //$NON-NLS-1$
    parent.setModifyListener(userUrlText, developerProvider, POM_PACKAGE.getDeveloper_Url(), ""); //$NON-NLS-1$
    parent.setModifyListener(userTimezoneText, developerProvider, POM_PACKAGE.getDeveloper_Timezone(), ""); //$NON-NLS-1$
    parent.setModifyListener(organizationNameText, developerProvider, POM_PACKAGE.getDeveloper_Organization(), ""); //$NON-NLS-1$
    parent.setModifyListener(organizationUrlText, developerProvider, POM_PACKAGE.getDeveloper_OrganizationUrl(), ""); //$NON-NLS-1$
  }

  public void updateView(Notification notification) {
    EObject object = (EObject) notification.getNotifier();
    Object feature = notification.getFeature();
    
    if(feature == PomPackage.Literals.MODEL__DEVELOPERS) {
      developersEditor.refresh();
    } else if(feature == PomPackage.Literals.MODEL__CONTRIBUTORS) {
      contributorsEditor.refresh();
    } else {
      Object notificationObject = MavenPomEditorPage.getFromNotification(notification);
      if(object instanceof Contributor) {
        contributorsEditor.refresh();

        if(object == currentSelection)
          updateDetails(object);
      } else if(object instanceof Developer) {
        developersEditor.refresh();

        if(object == currentSelection)
          updateDetails(object);
      } else if(feature == PomPackage.Literals.DEVELOPER__ROLES || feature == PomPackage.Literals.CONTRIBUTOR__ROLES) {
        EList<String> roles = (EList<String>)object.eGet((EStructuralFeature)feature);
        if(object == getRoles()) {
          updateRoles(roles);
        }
      }
    }
  }

  public void loadData(Model model) {
    this.model = model;
    loadDevelopers();
    loadContributors();

    developersEditor.setReadOnly(parent.isReadOnly());
    contributorsEditor.setReadOnly(parent.isReadOnly());

    updateDetails(null);
  }

  protected EList<String> getRoles() {
    if(currentSelection != null) {
      if(currentSelection instanceof Contributor) {
        return ((Contributor) currentSelection).getRoles();
      } else if(currentSelection instanceof Developer) {
        return ((Developer) currentSelection).getRoles();
      }
    }
    return null;
  }
  
  protected void updateRoles(EList<String> roles) {
    rolesEditor.setInput(roles);
  }

  protected EList<PropertyElement> getProperties() {
    if(currentSelection != null) {
      if(currentSelection instanceof Contributor) {
        return ((Contributor) currentSelection).getProperties();
      } else if(currentSelection instanceof Developer) {
        return ((Developer) currentSelection).getProperties();
      }
    }
    return null;
  }
}
