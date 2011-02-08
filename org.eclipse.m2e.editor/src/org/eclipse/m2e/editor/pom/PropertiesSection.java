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

import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.XMLChar;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.m2e.core.ui.internal.dialogs.MavenPropertyDialog;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This is properties editor (double click edits the property)
 * 
 * @author Anton Kraev
 */
public class PropertiesSection {
  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  private EditingDomain editingDomain;
  private EObject model;
  private EStructuralFeature feature;
  private FormToolkit toolkit;
  private Composite composite;
  private Section propertiesSection;
  ListEditorComposite<PropertyElement> propertiesEditor;
  
  private VerifyListener listener = new VerifyListener() {
    public void verifyText(VerifyEvent e) {
      e.doit = XMLChar.isValidName(e.text);
    }
  };

  public PropertiesSection(FormToolkit toolkit, Composite composite, EditingDomain editingDomain) {
    this.toolkit = toolkit;
    this.composite = composite;
    this.editingDomain = editingDomain;
    createSection();
  }
  
  public void setModel(EObject model, EStructuralFeature feature) {
    this.model = model;
    this.feature = feature;
    this.propertiesEditor.setInput(getProperties());
  }

  private EList<PropertyElement> getProperties() {
    return (EList<PropertyElement>) model.eGet(feature);
  }
  
  private Section createSection() {
    propertiesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    propertiesSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    propertiesSection.setText(Messages.PropertiesSection_section_properties);
    propertiesSection.setText("Properties");
    propertiesSection.setData("name", "propertiesSection"); //$NON-NLS-1$ //$NON-NLS-2$
    toolkit.paintBordersFor(propertiesSection);

    propertiesEditor = new ListEditorComposite<PropertyElement>(propertiesSection, SWT.NONE);
    propertiesSection.setClient(propertiesEditor);
    propertiesEditor.getViewer().getTable().setData("name", "properties"); //$NON-NLS-1$ //$NON-NLS-2$
    
    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyElement>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    propertiesEditor.setCreateButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createNewProperty();
      }
    });
    propertiesEditor.setRemoveButtonListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        deleteProperties(propertiesEditor.getSelection());
      }
    });
    propertiesEditor.setDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        editProperty(propertiesEditor.getSelection());
      }
    }) ;
    
    toolkit.paintBordersFor(propertiesEditor);
    toolkit.adapt(propertiesEditor);
    
    return propertiesSection;
  }
  
  public void refresh() {
    propertiesEditor.refresh();
  }
  
  void editProperty(List<PropertyElement> list) {
    if (list.size() != 1) {
      return;
    }
    
    PropertyElement pp = list.get(0);
    
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        Messages.PropertiesSection_title_editProperty, pp.getName(), pp.getValue(), listener);
    if(dialog.open() == IDialogConstants.OK_ID) {
      String key = dialog.getName();
      String value = dialog.getValue();
      CompoundCommand command = new CompoundCommand();
      if (!key.equals(pp.getName())) {
        command.append(SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyElement_Name(), key));
      }
      if (!value.equals(pp.getValue())) {
        command.append(SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyElement_Value(), value));
      }
      editingDomain.getCommandStack().execute(command);
      propertiesEditor.setInput(getProperties());
    }
  }

  void createNewProperty() {
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        Messages.PropertiesSection_title_addProperty, "", "", listener); //$NON-NLS-2$ //$NON-NLS-3$
    if(dialog.open() == IDialogConstants.OK_ID) {
      CompoundCommand command = new CompoundCommand();
      
      PropertyElement propertyPair = PomFactory.eINSTANCE.createPropertyElement();
      propertyPair.setName(dialog.getName());
      propertyPair.setValue(dialog.getValue());
      command.append(AddCommand.create(editingDomain, model, feature, //
          propertyPair, getProperties().size()));
      
      editingDomain.getCommandStack().execute(command);
      propertiesEditor.setInput(getProperties());
    }
  }

  void deleteProperties(List<PropertyElement> selection) {
    Command deleteProperties = RemoveCommand.create(editingDomain, model, feature, selection);
    editingDomain.getCommandStack().execute(deleteProperties);
    propertiesEditor.setInput(getProperties());
  }

  public ExpandableComposite getSection() {
    return propertiesSection;
  }
}
