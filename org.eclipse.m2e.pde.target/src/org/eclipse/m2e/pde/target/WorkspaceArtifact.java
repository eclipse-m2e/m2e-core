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

import java.io.File;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.DelegatingArtifact;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class WorkspaceArtifact extends DelegatingArtifact {

	private IMavenProjectFacade mavenProject;

	public WorkspaceArtifact(Artifact delegate, IMavenProjectFacade mavenProject) {
		super(delegate);
		this.mavenProject = mavenProject;
	}

	@Override
	protected WorkspaceArtifact newInstance(Artifact delegate) {
		return new WorkspaceArtifact(delegate, mavenProject);
	}

	@Override
	public File getFile() {
		File file = super.getFile();
		if (file == null) {
			return new File(mavenProject.getMavenProject().getBuild().getOutputDirectory());
		}
		return file;
	}

	public IMavenProjectFacade getMavenProject() {
		return mavenProject;
	}

}
