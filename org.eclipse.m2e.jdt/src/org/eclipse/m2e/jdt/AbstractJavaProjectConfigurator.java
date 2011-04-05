/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

public abstract class AbstractJavaProjectConfigurator
    extends AbstractProjectConfigurator
    implements IJavaProjectConfigurator
{
    @Override
    public void configure( ProjectConfigurationRequest request, IProgressMonitor monitor )
        throws CoreException
    {
        // TODO Auto-generated method stub

    }

    public void configureClasspath( IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor )
        throws CoreException
    {
        // TODO Auto-generated method stub

    }

    public void configureRawClasspath( ProjectConfigurationRequest request, IClasspathDescriptor classpath,
                                       IProgressMonitor monitor )
        throws CoreException
    {
        IMavenProjectFacade facade = request.getMavenProjectFacade();

        assertHasNature( request.getProject(), JavaCore.NATURE_ID );

        for ( MojoExecution mojoExecution : getMojoExecutions( request, monitor ) )
        {
            File[] sources = getSourceFolders( request, mojoExecution );

            for ( File source : sources )
            {
                IPath sourcePath = getFullPath( facade, source );

                if ( sourcePath != null && !classpath.containsPath( sourcePath ) )
                {
                    classpath.addSourceEntry( sourcePath, facade.getOutputLocation(), true );
                }
            }
        }
    }

    protected IPath getFullPath( IMavenProjectFacade facade, File file )
    {
        IProject project = facade.getProject();
        IPath path = MavenProjectUtils.getProjectRelativePath( project, file.getAbsolutePath() );
        return project.getFullPath().append( path );
    }

    protected File[] getSourceFolders( ProjectConfigurationRequest request, MojoExecution mojoExecution )
        throws CoreException
    {
        return new File[] { getParameterValue( getOutputFolderParameterName(), File.class, request.getMavenSession(),
                                               mojoExecution ) };
    }

    protected String getOutputFolderParameterName()
    {
        return "outputDirectory";
    }

}
