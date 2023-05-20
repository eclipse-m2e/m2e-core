/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.feature.AbstractFeatureModel;
import org.eclipse.pde.internal.core.feature.Feature;
import org.w3c.dom.Node;

@SuppressWarnings("restriction")
public class DomXmlFeature extends Feature {

	public DomXmlFeature(Node node) {
		DomXmlFeatureModel model = new DomXmlFeatureModel();
		setModel(model);
		parse(node);
		model.setEditable(false);
	}

	@Override
	public boolean isValid() {
		return hasRequiredAttributes();
	}

	private static final class DomXmlFeatureModel extends AbstractFeatureModel {

		private boolean editable = true;

		@Override
		public void load() throws CoreException {

		}

		public void setEditable(boolean editable) {
			this.editable = editable;
		}

		@Override
		protected NLResourceHelper createNLResourceHelper() {
			return null;
		}

		@Override
		public boolean isEditable() {
			return editable;
		}

	}

}
