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

import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.impl.ConfigurationImpl;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.w3c.dom.Element;

/**
 * Synchronizes configuration elements between XML and model
 * 
 * @author Mike Poindexter
 */
class ConfigurationAdapter extends TranslatorAdapter implements INodeAdapter {

	private Configuration modelObject;

	public ConfigurationAdapter(SSESyncResource resource, Element node,
	    Configuration object) {
		super(resource);
		this.node = node;
		this.modelObject = object;
		load();
	}

	public boolean isAdapterForType(Object type) {
		return ConfigurationAdapter.class.equals(type);
	}

	public void notifyChanged(INodeNotifier notifier, int eventType,
			Object changedFeature, Object oldValue, Object newValue, int pos) {
    // A catch-all notificator. 
    // The configuration section can differ with every plugin, so we cannot really have a
    // static EMF model. So we'll just notify the subscribers and let them act accordingly.
	  ((ConfigurationImpl)modelObject).doNotify(eventType, changedFeature, oldValue, newValue);
	}

	@Override
	public void load() {
	  ((ConfigurationImpl)modelObject).setConfigurationNode(node);
	}

	@Override
	public void save() {
	}

	@Override
	public void update(Object oldValue, Object newValue, int index) {
	}
}