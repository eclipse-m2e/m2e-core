/*******************************************************************************
 * Copyright (c) 2008-2015 Sonatype, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.importview.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.m2e.core.project.MavenProjectInfo;

/**
 * Compares two {@link MavenProjectInfo} objects for ordering in lists.
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
final class ProjectSelectionViewerComparator extends ViewerComparator {
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		MavenProjectInfo info1 = (MavenProjectInfo) e1;
		MavenProjectInfo info2 = (MavenProjectInfo) e2;
		return info1.getModel().getArtifactId().compareTo(info2.getModel().getArtifactId());
	}
}