/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


/**
 * Convenience abstract baseclass for project configurators that "wrap" code generation maven plugins, like modello and
 * similar. Adds generated sources folders to project raw classpath and provides hooks to customise location of
 * generated sources directories. Implementation assumes mojos that use BuildContext API to participate in workspace
 * build. For mojos that do not use BuildContext API, subclasses <strong>MUST</strong> check for input model changes
 * before executing the mojo and <strong>MUST</strong> refresh output folders from local filesystem after executing the
 * mojo. BuildContext API is the recommending way to implement both check for model changes and refresh output folders
 * from local filesystem.
 * 
 * @see {@link AbstractBuildParticipant#getBuildContext()}
 * @since 1.4
 */
public abstract class AbstractSourcesGenerationProjectConfigurator extends AbstractProjectConfigurator implements
    IJavaProjectConfigurator {
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method stub

  }

  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor)
      throws CoreException {
    // TODO Auto-generated method stub

  }

  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = request.getMavenProjectFacade();

    assertHasNature(request.getProject(), JavaCore.NATURE_ID);

    for(MojoExecution mojoExecution : getMojoExecutions(request, monitor)) {
      File[] sources = getSourceFolders(request, mojoExecution, monitor);

      for(File source : sources) {
        IPath sourcePath = getFullPath(facade, source);

        if(sourcePath != null) {
          IClasspathEntryDescriptor entry = classpath.addSourceEntry(sourcePath, facade.getOutputLocation(), true);
          entry.setClasspathAttribute(IClasspathAttribute.IGNORE_OPTIONAL_PROBLEMS, "true"); //$NON-NLS-1$
        }
      }
    }
  }

  protected IPath getFullPath(IMavenProjectFacade facade, File file) {
    IProject project = facade.getProject();
    IPath path = MavenProjectUtils.getProjectRelativePath(project, file.getAbsolutePath());
    return project.getFullPath().append(path);
  }

  protected File[] getSourceFolders(ProjectConfigurationRequest request, MojoExecution mojoExecution,
      IProgressMonitor monitor) throws CoreException {
    return new File[] {getParameterValue(request.getMavenProject(), getOutputFolderParameterName(), File.class,
        mojoExecution, monitor)};
  }

  protected String getOutputFolderParameterName() {
    return "outputDirectory";
  }

}
