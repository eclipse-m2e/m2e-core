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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractCustomizableLifecycleMapping;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;

@SuppressWarnings("restriction")
public class TychoLifecycleMapping extends AbstractCustomizableLifecycleMapping {

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

	@Override
	public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade projectFacade,
			IProgressMonitor monitor) {
		String packaging = projectFacade.getPackaging();
		if ("eclipse-plugin".equals(packaging) || "eclipse-test-plugin".equals(packaging)) {
			List<AbstractProjectConfigurator> list = new ArrayList<>(
					super.getProjectConfigurators(projectFacade, monitor));
			list.add(new EclipsePluginProjectConfigurator());
			return list;
		} else {
			return super.getProjectConfigurators(projectFacade, monitor);
		}
	}

	private static final class EclipsePluginProjectConfigurator extends AbstractJavaProjectConfigurator {

		@Override
		protected void addProjectSourceFolders(IClasspathDescriptor classpath, Map<String, String> options,
				ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
			MavenProject mavenProject = request.mavenProject();
			IProject project = request.mavenProjectFacade().getProject();
			IBuild build = getBuild(project);
			if (build == null) {
				super.addProjectSourceFolders(classpath, options, request, monitor);
			} else {
				IBuildEntry sourceEntry = build.getEntry(IBuildEntry.JAR_PREFIX + ".");
				if (sourceEntry == null) {
					return;
				}
				String outputDirectory;
				IBuildEntry outputEntry = build.getEntry(IBuildEntry.OUTPUT_PREFIX + ".");
				if (outputEntry != null && outputEntry.getTokens().length > 0) {
					outputDirectory = outputEntry.getTokens()[0];
				} else {
					outputDirectory = mavenProject.getBuild().getOutputDirectory();
				}
				IContainer outputFolder = getFolder(project, outputDirectory);
				M2EUtils.createFolder(outputFolder, true, subMonitor.split(10));
				IPath[] inclusion = new IPath[0];
				IPath[] exclusion = new IPath[0];
				String mainSourceEncoding = null;
				addSourceDirs(classpath, project, Arrays.asList(sourceEntry.getTokens()), outputFolder.getFullPath(), inclusion,
						exclusion, mainSourceEncoding, subMonitor.split(10), false);
			}
		}

		@Override
		protected IContainer getOutputLocation(ProjectConfigurationRequest request, IProject project)
				throws CoreException {
			IBuild build = getBuild(project);
			if (build == null) {
				return super.getOutputLocation(request, project);
			}
			IBuildEntry outputEntry = build.getEntry(IBuildEntry.OUTPUT_PREFIX + ".");
			if (outputEntry != null && outputEntry.getTokens().length > 0) {
				return getFolder(project, outputEntry.getTokens()[0]);
			} else {
				return super.getOutputLocation(request, project);
			}
		}

		@Override
		protected void addJavaProjectOptions(Map<String, String> options, ProjectConfigurationRequest request,
				IProgressMonitor monitor) throws CoreException {
			IProject project = request.mavenProjectFacade().getProject();
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				ClasspathComputer.setClasspath(project, model);
			} else {
				super.addJavaProjectOptions(options, request, monitor);
			}
		}

	}

	private static IBuild getBuild(IProject project) throws CoreException {
		IFile buildFile = PDEProject.getBuildProperties(project);
		IBuildModel buildModel = null;
		if (buildFile.exists()) {
			buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
		}
		return (buildModel != null) ? buildModel.getBuild() : null;
	}
}
