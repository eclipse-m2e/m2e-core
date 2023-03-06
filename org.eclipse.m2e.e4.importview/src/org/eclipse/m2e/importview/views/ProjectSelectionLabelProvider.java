/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Fred Bricon (Red Hat, Inc.) - auto update project configuration
 *******************************************************************************/

package org.eclipse.m2e.importview.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.m2e.core.project.MavenProjectInfo;

/**
 * Provides Labels for Tree View to select projects.
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
final class ProjectSelectionLabelProvider extends LabelProvider {

	@Override
	public String getText(Object element) {
		// TODO: formatted text (highlighting of differentiation)
		if (!(element instanceof MavenProjectInfo)) {
			return "unknown";
		}
		return ((MavenProjectInfo) element).getModel().getArtifactId();
	}
}