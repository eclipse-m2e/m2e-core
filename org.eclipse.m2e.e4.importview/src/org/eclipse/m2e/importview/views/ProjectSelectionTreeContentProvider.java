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

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.project.MavenProjectInfo;

/**
 * Provides content for Tree View to select projects.
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
final class ProjectSelectionTreeContentProvider implements ITreeContentProvider {

	static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	@Override
	public Object[] getElements(Object element) {
		if (element instanceof List) {
			@SuppressWarnings("unchecked")
			List<MavenProjectInfo> projects = (List<MavenProjectInfo>) element;
			return projects.toArray(new MavenProjectInfo[projects.size()]);
		}
		return ProjectSelectionTreeContentProvider.EMPTY_OBJECT_ARRAY;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof List) {
			@SuppressWarnings("unchecked")
			List<MavenProjectInfo> projects = (List<MavenProjectInfo>) parentElement;
			return projects.toArray(new MavenProjectInfo[projects.size()]);
		} else if (parentElement instanceof MavenProjectInfo) {
			MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
			Collection<MavenProjectInfo> projects = mavenProjectInfo.getProjects();
			return projects.toArray(new MavenProjectInfo[projects.size()]);
		}
		return ProjectSelectionTreeContentProvider.EMPTY_OBJECT_ARRAY;
	}

	@Override
	public Object getParent(Object element) {
		// TODO: why is this not necessary?
		return null;
	}

	@Override
	public boolean hasChildren(Object parentElement) {
		if (parentElement instanceof List) {
			List<?> projects = (List<?>) parentElement;
			return !projects.isEmpty();
		} else if (parentElement instanceof MavenProjectInfo) {
			MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) parentElement;
			return !mavenProjectInfo.getProjects().isEmpty();
		}
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}