/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.bnd.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.maven.lib.configuration.BeanProperties;
import aQute.bnd.maven.lib.resolve.ImplicitFileSetRepository;
import aQute.bnd.osgi.Processor;

/**
 * Adapts eclipse projects managed by m2e to bnd projects
 */
@Component
@AdapterTypes(adaptableClass = IProject.class, adapterNames = Project.class)
public class BndPluginAdapter implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		try {
			if (adaptableObject instanceof IProject eclipseProject) {
				if (adapterType == Project.class) {
					IMavenProjectFacade mavenProject = Adapters.adapt(eclipseProject, IMavenProjectFacade.class);
					if (isRelevantProject(mavenProject)) {
						System.out.println(eclipseProject.getName() + " uses bnd plugin!");
						BeanProperties beanProperties = new BeanProperties();
						MavenProject mp = mavenProject.getMavenProject();
						if (mp != null) {
							beanProperties.put("project", mp);
						}
						// TODO beanProperties.put("settings", settings);
						Properties processorProperties = new Properties(beanProperties);
						if (mp != null) {
							Properties projectProperties = mp.getProperties();
							for (String key : projectProperties.stringPropertyNames()) {
								processorProperties.setProperty(key, projectProperties.getProperty(key));
							}
						}
						Processor processor = new Processor(processorProperties, false);
						Collection<File> list = new ArrayList<File>();
						if (mp != null) {
							for (Artifact artifact : mp.getArtifacts()) {
								list.add(artifact.getFile());
							}
						}
						ImplicitFileSetRepository repository = new ImplicitFileSetRepository("Project Artifacts", list);
						// TODO propertiesFile = new BndConfiguration(project,
						// mojoExecution).loadProperties(builder);
						// TODO
						// aQute.bnd.maven.lib.resolve.BndrunContainer.getFileSetRepository(MavenProject)

						Workspace standaloneWorkspace = Workspace.createStandaloneWorkspace(processor,
								mavenProject.getPomFile().getParentFile().toURI());
						standaloneWorkspace.addBasicPlugin(repository);
						if (mp == null) {
							standaloneWorkspace.set("workspaceName", mavenProject.getArtifactKey().toPortableString());
						} else {
							// TODO make part of IMavenProjectFacade
							standaloneWorkspace.set("workspaceName", mp.getName());
							String description = mp.getDescription();
							if (description != null) {
								standaloneWorkspace.set("workspaceDescription", description);
							}
						}
						standaloneWorkspace.refresh();
						Project bndProject = new Project(standaloneWorkspace, null, null);
						bndProject.setBase(null);
						return adapterType.cast(bndProject);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean isRelevantProject(IMavenProjectFacade mavenProject) {
		// TODO cache result inside IProject store
		if (mavenProject != null) {
			Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping = mavenProject.getMojoExecutionMapping();
			for (MojoExecutionKey key : mapping.keySet()) {
				if ("biz.aQute.bnd".equals(key.groupId()) && "bnd-maven-plugin".equals(key.artifactId())) {
					return true;
				}
			}
		}
		return false;
	}

}
