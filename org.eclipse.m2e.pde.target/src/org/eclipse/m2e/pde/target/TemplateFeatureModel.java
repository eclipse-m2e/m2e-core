/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.feature.AbstractFeatureModel;
import org.eclipse.pde.internal.core.feature.Feature;
import org.eclipse.pde.internal.core.ifeature.IFeature;

/**
 * creates a new model by copy the a given {@link IFeature} as its template
 */
@SuppressWarnings("restriction")
public final class TemplateFeatureModel extends AbstractFeatureModel {

	private static final long serialVersionUID = 1L;
	private String xml;
	private boolean editable = true;

	public TemplateFeatureModel(IFeature template) {
		if (template != null) {
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter writer = new PrintWriter(stringWriter)) {
				template.write("", writer);
			}
			this.xml = stringWriter.toString();
		}
	}

	@Override
	public synchronized void load() throws CoreException {
		if (xml != null && isEditable()) {
			load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), false);
			updateTimeStampWith(System.currentTimeMillis());
			setLoaded(true);
			xml = null;
		}
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	public void makeReadOnly() {
		this.editable = false;
	}

	@Override
	public IFeature getFeature() {
		if (feature == null) {
			feature = new TemplateFeature(this);
		}
		return feature;
	}

	private static final class TemplateFeature extends Feature {

		public TemplateFeature(TemplateFeatureModel templateFeatureModel) {
			setModel(templateFeatureModel);
		}

		@Override
		public boolean isValid() {
			return hasRequiredAttributes();
		}
	}
}
