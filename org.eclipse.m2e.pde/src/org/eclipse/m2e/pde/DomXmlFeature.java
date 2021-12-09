/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde;

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
