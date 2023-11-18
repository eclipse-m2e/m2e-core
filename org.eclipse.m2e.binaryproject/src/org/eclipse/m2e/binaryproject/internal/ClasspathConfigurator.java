/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.binaryproject.internal;

import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.internal.AbstractJavaProjectConfigurator;
import org.eclipse.m2e.jdt.internal.BuildPathManager;

@SuppressWarnings("restriction")
public class ClasspathConfigurator extends AbstractJavaProjectConfigurator {

  @Override
  protected void addProjectSourceFolders(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {}

  @Override
  protected void addProjectSourceFolders(IClasspathDescriptor classpath, Map<String, String> options, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {}

  @Override
  protected void addJavaNature(IProject project, IProgressMonitor monitor) throws CoreException {
    addNature(project, JavaCore.NATURE_ID, IResource.KEEP_HISTORY | IResource.AVOID_NATURE_CONFIG, monitor);
  }

  @Override
  protected void invokeJavaProjectConfigurators(IClasspathDescriptor classpath, ProjectConfigurationRequest request,
      IProgressMonitor monitor) throws CoreException {
    // TODO need repository information

    IProject project = request.mavenProjectFacade().getProject();

    IMaven maven = MavenPlugin.getMaven();

    IScopeContext projectScope = new ProjectScope(project);
    IEclipsePreferences projectNode = projectScope.getNode(BinaryProjectPlugin.PLUGIN_ID);

    String groupId = projectNode.get(BinaryProjectPlugin.P_GROUPID, (String) null);
    String artifactId = projectNode.get(BinaryProjectPlugin.P_ARTIFACTID, (String) null);
    String version = projectNode.get(BinaryProjectPlugin.P_VERSION, (String) null);
    String type = projectNode.get(BinaryProjectPlugin.P_TYPE, "jar");
    String classifier = projectNode.get(BinaryProjectPlugin.P_CLASSIFIER, (String) null);

    List<ArtifactRepository> repositories = null; // TODO store in project preferences

    Artifact jar = maven.resolve(groupId, artifactId, version, type, classifier, repositories, monitor);
    String jarLocation = jar.getFile().getAbsolutePath();

    project.setPersistentProperty(BinaryProjectPlugin.QNAME_JAR, jarLocation);

    Artifact sources =
        maven.resolve(groupId, artifactId, version, type, getSourcesClassifier(classifier), repositories, monitor);
	IClasspathEntryDescriptor libEntry = classpath.addLibraryEntry(IPath.fromOSString(jarLocation));
    libEntry.setExported(true);
	libEntry.setSourceAttachment(IPath.fromOSString(sources.getFile().getAbsolutePath()), null);
    libEntry.setArtifactKey(new ArtifactKey(groupId, artifactId, version, classifier));
  }

  @Override
  protected IContainer getOutputLocation(ProjectConfigurationRequest request, IProject project) {
    return project;
  }

  static String getSourcesClassifier(String baseClassifier) {
    return BuildPathManager.CLASSIFIER_TESTS.equals(baseClassifier)
        ? BuildPathManager.CLASSIFIER_TESTSOURCES
        : BuildPathManager.CLASSIFIER_SOURCES;
  }

}
