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

package org.eclipse.m2e.model.edit.pom.translators;

import java.util.List;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Translates a property list using the name and value instead of reflection.
 * 
 * @author Mike Poindexter
 * 
 */
public class PropertiesAdapter extends ListAdapter {
	protected List<PropertyElement> properties;

	public PropertiesAdapter(SSESyncResource resc, Element containerNode,
			List<PropertyElement> properties) {
		super(resc, containerNode, properties, null);
		this.node = containerNode;
		this.properties = properties;
	}

	@Override
	public void notifyChanged(INodeNotifier notifier, int eventType,
			Object changedFeature, Object oldValue, Object newValue, int pos) {
		if (resource.isProcessEvents()) {
			try {
				resource.setProcessEvents(false);
				if (INodeNotifier.ADD == eventType
						&& newValue instanceof Element) {
					if (notifier == node) {
						IDOMElement addedElement = (IDOMElement) newValue;
						int idx = absoluteIndexOf(node, addedElement);
						if (idx == -1)
							idx = 0;
						properties.add(idx, createObject(addedElement));
					}
				} else if (INodeNotifier.REMOVE == eventType
						&& oldValue instanceof Element) {
					if (notifier == node) {
						for (PropertyElement prop : properties) {
							PropertyChildAdapter adapter = (PropertyChildAdapter) EcoreUtil
									.getExistingAdapter(prop,
											PropertyChildAdapter.class);
							if (adapter.getElement().equals(oldValue)) {
								properties.remove(prop);
								break;
							}
						}
					}
				} else if (changedFeature instanceof Text) {
					if (notifier != node && notifier instanceof Element) {
						Element e = (Element) notifier;
						String name = e.getLocalName();
						for (PropertyElement prop : properties) {
							if (name.equals(prop.getName())) {
								prop.setValue(getElementText(e));
							}
						}
					}
				}
			} finally {
				resource.setProcessEvents(true);
			}

		}

	}

	public void add(Object newValue, int position) {
		final PropertyElement prop = (PropertyElement) newValue;
		Element newElement = node.getOwnerDocument().createElement(
				prop.getName());
		Text value = node.getOwnerDocument().createTextNode(prop.getValue());
		newElement.appendChild(value);

		if (position < 0)
			position = 0;
		Node n = getNthChildWithName(node, "*", position); //$NON-NLS-1$
		if (n != null) {
			node.insertBefore(newElement, n);
		} else {
			node.appendChild(newElement);
		}
		formatNode(newElement);
		if (null == EcoreUtil.getExistingAdapter(prop,
				PropertyChildAdapter.class)) {
			prop.eAdapters().add(new PropertyChildAdapter(prop, newElement));
		}
		((IDOMNode) newElement).addAdapter(this);
	}

	public void remove(Object oldValue, int position) {
		if (position == -1)
			position = 0;

		Element n = getNthChildWithName(node, "*", position); //$NON-NLS-1$
		if (n != null)
			removeChildElement(n);
	}

	public PropertyElement createObject(Element child) {
		PropertyElement propertyElement = PomFactory.eINSTANCE
				.createPropertyElement();
		propertyElement.setName(child.getLocalName());
		propertyElement.setValue(getElementText(child));
		if (null == ((IDOMNode) child)
				.getExistingAdapter(PropertiesAdapter.class)) {
			((IDOMNode) child).addAdapter(this);
		}
		propertyElement.eAdapters().add(
				new PropertyChildAdapter(propertyElement, child));
		return propertyElement;
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return PropertiesAdapter.class.equals(type);
	}

	@Override
	public void load() {
    //MNGECLIPSE-2345, MNGECLIPSE-2694 when load is called on a list adapter already containing items, 
    // the old items shall be discarded to avoid duplicates.
	  properties.clear();
		NodeList children = node.getChildNodes();
		int nChildren = children.getLength();
		for (int i = 0; i < nChildren; i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				properties.add(createObject((Element) child));
			}

		}

	}

	@Override
	public void save() {
		for (PropertyElement o : properties) {
			add(o, -1);
		}
	}

	private class PropertyChildAdapter implements Adapter {
		private Notifier target;
		private PropertyElement property;
		private Element element;

		/**
		 * @param propertyElement
		 * @param container
		 */
		public PropertyChildAdapter(PropertyElement propertyElement,
				Element element) {
			super();
			this.property = propertyElement;
			this.element = element;
		}

		public boolean isAdapterForType(Object type) {
			return PropertyChildAdapter.class.equals(type);
		}

		public void notifyChanged(Notification notification) {
			if (resource.isProcessEvents()) {
				try {
					resource.setProcessEvents(false);
					Element newElement = node.getOwnerDocument().createElement(
							property.getName());
					Text value = node.getOwnerDocument().createTextNode(
							property.getValue());
					newElement.appendChild(value);
					node.replaceChild(newElement, element);
					formatNode(newElement);
					this.element = newElement;
					((IDOMNode) newElement).addAdapter(PropertiesAdapter.this);
				} finally {
					resource.setProcessEvents(true);
				}
			}
		}

		/**
		 * @return the target
		 */
		public Notifier getTarget() {
			return target;
		}

		/**
		 * @param target
		 *            the target to set
		 */
		public void setTarget(Notifier target) {
			this.target = target;
		}

		/**
		 * @return the property
		 */
		public PropertyElement getProperty() {
			return property;
		}

		/**
		 * @return the element
		 */
		public Element getElement() {
			return element;
		}

	}

}
