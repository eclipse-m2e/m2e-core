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

import java.util.regex.Pattern;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.m2e.core.project.MavenProjectInfo;

/**
 * Filters Maven Project Info by contained text
 *
 * @author Nikolaus Winter, comdirect bank AG
 */
final class MavenProjectInfoFilter extends ViewerFilter {

	private final Pattern filterPattern;

	MavenProjectInfoFilter(String filterText) {
		this.filterPattern = createPatternFromInput(filterText);
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (!(element instanceof MavenProjectInfo)) {
			return false;
		}

		MavenProjectInfo mavenProjectInfo = (MavenProjectInfo) element;
		return select(mavenProjectInfo);
	}

	private Pattern createPatternFromInput(String filterText) {
		filterText = "\\Q" + filterText + "\\E";
		filterText = replaceEscaped(filterText, "*", ".*");
		filterText = replaceEscaped(filterText, "?", ".");

		return Pattern.compile(filterText, Pattern.CASE_INSENSITIVE);
	}

	private boolean select(MavenProjectInfo mavenProjectInfo) {
		if (mavenProjectInfo.getProjects().isEmpty()) {
			return artifactMatches(mavenProjectInfo.getModel().getArtifactId());
		}

		if (artifactMatches(mavenProjectInfo.getModel().getArtifactId())) {
			return true;
		}

		return mavenProjectInfo.getProjects().stream().anyMatch(this::select);
	}

	private boolean artifactMatches(String artifactId) {
		return filterPattern.matcher(artifactId).find();
	}

	private String replaceEscaped(String input, String substring, String replacement) {
		return input.replace(substring, "\\E" + replacement + "\\Q");
	}
}