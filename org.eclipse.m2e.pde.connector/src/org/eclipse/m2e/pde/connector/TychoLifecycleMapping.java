/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
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
package org.eclipse.m2e.pde.connector;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public class TychoLifecycleMapping extends AbstractCustomizableLifecycleMapping implements ILifecycleMapping {

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		MavenProject mavenProject = request.mavenProject();
		IMavenProjectFacade facade = request.mavenProjectFacade();
		IProject project = facade.getProject();
		String packaging = facade.getPackaging();
		if ("eclipse-plugin".equals(packaging) || "eclipse-test-plugin".equals(packaging)) {
			PDEProjectHelper.configurePDEBundleProject(project, mavenProject, monitor);
		} else if ("eclipse-feature".equals(packaging)) {
			PDEProjectHelper.configurePDEFeatureProject(facade, monitor);
		}
		super.configure(request, monitor);
	}
}
