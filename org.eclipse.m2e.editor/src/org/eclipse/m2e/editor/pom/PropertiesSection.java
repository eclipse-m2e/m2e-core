/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PROPERTIES;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.eclipse.emf.ecore.xml.type.internal.DataValue.XMLChar;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.eclipse.m2e.core.ui.internal.dialogs.MavenPropertyDialog;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.composites.ListEditorComposite;
import org.eclipse.m2e.editor.composites.ListEditorContentProvider;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * This is properties editor (double click edits the property)
 *
 * @author Anton Kraev, Milos Kleint
 */
public class PropertiesSection {

  private static Logger LOG = LoggerFactory.getLogger(PropertiesSection.class);

  private final MavenPomEditorPage page;

  private final FormToolkit toolkit;

  private final Composite composite;

  private Section propertiesSection;

  ListEditorComposite<PropertyElement> propertiesEditor;

  private final VerifyListener listener = e -> e.doit = XMLChar.isValidName(e.text);

  private GridData propertiesSectionData;

  public PropertiesSection(FormToolkit toolkit, Composite composite, MavenPomEditorPage page) {
    this.toolkit = toolkit;
    this.composite = composite;
    this.page = page;
    createSection();
  }

  private List<PropertyElement> getProperties() {
    final List<PropertyElement> toRet = new ArrayList<>();

    try {
      performOnDOMDocument(new OperationTuple(page.getPomEditor().getDocument(), document -> {
        Element properties = findChild(document.getDocumentElement(), PROPERTIES);
        if(properties != null) {
          NodeList nl = properties.getChildNodes();
          for(int i = 0; i < nl.getLength(); i++ ) {
            Node child = nl.item(i);
            if(child instanceof Element) {
              toRet.add(new PropertyElement(child.getNodeName(), getTextValue(child)));
            }
          }
        }
      }, true));
    } catch(Exception e) {
      LOG.error("Cannot read properties", e); //$NON-NLS-1$
    }
    return toRet;
  }

  private Section createSection() {
    propertiesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    propertiesSectionData = new GridData(SWT.FILL, SWT.FILL, true, false);
    propertiesSection.setLayoutData(propertiesSectionData);
    propertiesSection.setText(Messages.PropertiesSection_section_properties);
    propertiesSection.setData("name", "propertiesSection"); //$NON-NLS-1$ //$NON-NLS-2$
    propertiesSection.addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        setExpanded(e.getState());
      }
    });

    toolkit.paintBordersFor(propertiesSection);

    propertiesEditor = new ListEditorComposite<>(propertiesSection, SWT.NONE);
    propertiesSection.setClient(propertiesEditor);
    propertiesEditor.getViewer().getTable().setData("name", "properties"); //$NON-NLS-1$ //$NON-NLS-2$

    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyElement>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    propertiesEditor.setCreateButtonListener(SelectionListener.widgetSelectedAdapter(e -> createNewProperty()));
    propertiesEditor.setRemoveButtonListener(
        SelectionListener.widgetSelectedAdapter(e -> deleteProperties(propertiesEditor.getSelection())));
    propertiesEditor.setDoubleClickListener(event -> editProperty(propertiesEditor.getSelection()));

    toolkit.paintBordersFor(propertiesEditor);
    toolkit.adapt(propertiesEditor);

    return propertiesSection;
  }

  public void refresh() {
    propertiesEditor.setInput(getProperties());
    propertiesEditor.refresh();
  }

  void editProperty(List<PropertyElement> list) {
    if(list.size() != 1) {
      return;
    }

    final PropertyElement pp = list.get(0);

    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        Messages.PropertiesSection_title_editProperty, pp.getName(), pp.getValue(), listener);
    if(dialog.open() == IDialogConstants.OK_ID) {
      final String key = dialog.getName();
      final String value = dialog.getValue();
      try {
        page.performEditOperation(document -> {
          Element properties = getChild(document.getDocumentElement(), PROPERTIES);
          Element old = findChild(properties, pp.getName());
          if(old == null) {
            //should never happen..
            old = getChild(properties, pp.getName());
          }
          if(!pp.getName().equals(key)) {
            Element newElement = document.createElement(key);
            properties.replaceChild(newElement, old);
            setText(newElement, pp.getValue());
            old = newElement;
          }
          if(!pp.getValue().equals(value)) {
            setText(old, value);
          }
        }, LOG, "error updating property");
      } finally {
        propertiesEditor.setInput(getProperties());
      }
    }
  }

  void createNewProperty() {
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        Messages.PropertiesSection_title_addProperty, "", "", listener); //$NON-NLS-1$//$NON-NLS-2$
    if(dialog.open() == IDialogConstants.OK_ID) {
      final String key = dialog.getName();
      final String value = dialog.getValue();
      try {
        page.performEditOperation(document -> {
          Element prop = getChild(document.getDocumentElement(), PROPERTIES, key);
          setText(prop, value);
        }, LOG, "error creating property");
      } finally {
        propertiesEditor.setInput(getProperties());
      }
    }
  }

  void deleteProperties(final List<PropertyElement> selection) {
    try {
      page.performEditOperation(document -> {
        Element props = findChild(document.getDocumentElement(), PROPERTIES);
        if(props != null) {
          //now what if we don't find the props? profile or parent? or out of sync?
          for(PropertyElement el : selection) {
            Element prop = findChild(props, el.getName());
            removeChild(props, prop);
          }
          removeIfNoChildElement(props);
        }
      }, LOG, "error deleting property");
    } finally {
      propertiesEditor.setInput(getProperties());
    }
  }

  public ExpandableComposite getSection() {
    return propertiesSection;
  }

  static class PropertyPairLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof PropertyElement) {
        PropertyElement pair = (PropertyElement) element;
        return NLS.bind(Messages.PropertyPairLabelProvider_0, pair.getName(), pair.getValue());
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      return MavenEditorImages.IMG_PROPERTY;
    }
  }

  static class PropertyElement {
    private final String name;

    private final String value;

    public PropertyElement(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public String getName() {
      return name;
    }
  }

  public void setExpanded(boolean expanded) {
    propertiesSection.setExpanded(expanded);
    if(propertiesSectionData != null) {
      propertiesSectionData.grabExcessVerticalSpace = expanded;
      propertiesSection.getParent().layout();
    }
  }
}
