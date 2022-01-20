/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;

@SuppressWarnings( "restriction" )
public class PDEProjectHelper
{
    private static boolean isListeningForPluginModelChanges = false;

    private static final List<IProject> projectsForUpdateClasspath = new ArrayList<IProject>();

    private static final PDEProjectHelper INSTANCE = new PDEProjectHelper();

    private PDEProjectHelper()
    {

    }

    public static PDEProjectHelper getInstance()
    {
        return INSTANCE;
    }

    private static final IPluginModelListener classpathUpdater = new IPluginModelListener()
    {
        @Override
		public void modelsChanged( PluginModelDelta delta )
        {
            synchronized ( projectsForUpdateClasspath )
            {
                if ( projectsForUpdateClasspath.size() == 0 )
                {
                    return;
                }

                Iterator<IProject> projectsIter = projectsForUpdateClasspath.iterator();
                while ( projectsIter.hasNext() )
                {
                    IProject project = projectsIter.next();
                    IPluginModelBase model = PluginRegistry.findModel( project );
                    if ( model == null )
                    {
                        continue;
                    }

                    UpdateClasspathWorkspaceJob job = new UpdateClasspathWorkspaceJob( project, model );
                    job.schedule();
                    projectsIter.remove();
                }
            }
        }
    };

    private static class UpdateClasspathWorkspaceJob
        extends WorkspaceJob
    {
        private final IProject project;

        private final IPluginModelBase model;

        public UpdateClasspathWorkspaceJob( IProject project, IPluginModelBase model )
        {
            super( "Updating classpath" );
            this.project = project;
            this.model = model;
        }

        @Override
        public IStatus runInWorkspace( IProgressMonitor monitor )
            throws CoreException
        {
            setClasspath( project, model, monitor );
            return Status.OK_STATUS;
        }
    }

    public void configurePDEBundleProject( IProject project, MavenProject mavenProject, IProgressMonitor monitor )
        throws CoreException
    {
        // see org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation

        if ( !project.hasNature( PDE.PLUGIN_NATURE ) )
        {
            CoreUtility.addNatureToProject( project, PDE.PLUGIN_NATURE, null );
        }

        if ( !project.hasNature( JavaCore.NATURE_ID ) )
        {
            CoreUtility.addNatureToProject( project, JavaCore.NATURE_ID, null );
        }

        // PDE can't handle default JDT classpath
        IJavaProject javaProject = JavaCore.create( project );
        javaProject.setOutputLocation( getOutputLocation( project, mavenProject, monitor ), monitor );

        // see org.eclipse.pde.internal.ui.wizards.tools.UpdateClasspathJob
        // PDE populates the model cache lazily from WorkspacePluginModelManager.visit() ResourceChangeListenter
        // That means the model may be available or not at this point in the lifecycle.
        // If it is, update its classpath right away.
        // If not add the project to the list to be updated later based on model change events.
        IPluginModelBase model = PluginRegistry.findModel( project );
        if ( model != null )
        {
            setClasspath( project, model, monitor );
        }
        else
        {
            addProjectForUpdateClasspath( project );
        }
    }

    private void addProjectForUpdateClasspath( IProject project )
    {
        synchronized ( projectsForUpdateClasspath )
        {
            projectsForUpdateClasspath.add( project );
            if ( !isListeningForPluginModelChanges )
            {
                PDECore.getDefault().getModelManager().addPluginModelListener( classpathUpdater );
                isListeningForPluginModelChanges = true;
            }
        }
    }

    private IPath getOutputLocation( IProject project, MavenProject mavenProject, IProgressMonitor monitor )
        throws CoreException
    {
        File outputDirectory = new File( mavenProject.getBuild().getOutputDirectory() );
        outputDirectory.mkdirs();
        IPath relPath =
            MavenProjectUtils.getProjectRelativePath( project, mavenProject.getBuild().getOutputDirectory() );
        IFolder folder = project.getFolder( relPath );
        folder.refreshLocal( IResource.DEPTH_INFINITE, monitor );
        return folder.getFullPath();
    }

    public static void addPDENature( IProject project, IPath manifestPath, IProgressMonitor monitor )
        throws CoreException
    {
        AbstractProjectConfigurator.addNature( project, PDE.PLUGIN_NATURE, monitor );
        IProjectDescription description = project.getDescription();
        ICommand[] prevBuilders = description.getBuildSpec();
        ArrayList<ICommand> newBuilders = new ArrayList<ICommand>();
        for ( ICommand builder : prevBuilders )
        {
            if ( !builder.getBuilderName().startsWith( "org.eclipse.pde" ) )
            {
                newBuilders.add( builder );
            }
        }
        description.setBuildSpec( newBuilders.toArray( new ICommand[newBuilders.size()] ) );
        project.setDescription( description, monitor );

        setManifestLocaton( project, manifestPath, monitor );
    }

    protected static void setManifestLocaton( IProject project, IPath manifestPath, IProgressMonitor monitor )
        throws CoreException
    {
		IBundleProjectService projectService = Activator.getBundleProjectService().get();
        if ( manifestPath != null && manifestPath.segmentCount() > 1 )
        {
            IPath metainfPath = manifestPath.removeLastSegments( 1 );
            project.getFile( metainfPath ).refreshLocal( IResource.DEPTH_INFINITE, monitor );
            projectService.setBundleRoot( project, metainfPath );
        }
        else
        {
            // in case of configuration update, reset to the default value
            projectService.setBundleRoot( project, null );
        }
    }

    /**
     * Returns bundle manifest as known to PDE project metadata. Returned file may not exist in the workspace or on the
     * filesystem. Never returns null.
     */
    public static IFile getBundleManifest( IProject project )
        throws CoreException
    {
        // PDE API is very inconvenient, lets use internal classes instead
        IContainer metainf = PDEProject.getBundleRoot( project );
        if ( metainf == null || metainf instanceof IProject )
        {
            metainf = project.getFolder( "META-INF" );
        }
        else
        {
            metainf = metainf.getFolder( new Path( "META-INF" ) );
        }

        return metainf.getFile( new Path( "MANIFEST.MF" ) );
    }

    private static void setClasspath( IProject project, IPluginModelBase model, IProgressMonitor monitor )
        throws CoreException
    {
        IClasspathEntry[] entries = ClasspathComputer.getClasspath(project, model, null, true /*clear existing entries*/, true);
        JavaCore.create(project).setRawClasspath(entries, null);
        // workaround PDE sloppy model management during the first multimodule project import in eclipse session
        // 1. m2e creates all modules as simple workspace projects without JDT or PDE natures
        // 2. first call to org.eclipse.pde.internal.core.PluginModelManager.initializeTable() reads all workspace
        // projects regardless of their natures (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=319268)
        // 3. going through all projects one by one
        // 3.1. m2e enables JDE and PDE natures and adds PDE classpath container
        // 3.2. org.eclipse.pde.internal.core.PDEClasspathContainer.addProjectEntry ignores all project's dependencies
        // that do not have JAVA nature. at this point PDE classpath is missing some/all workspace dependencies.
        // 4. PDE does not re-resolve classpath when dependencies get JAVA nature enabled

        // as a workaround, touch project bundle manifests to force PDE re-read the model, re-resolve dependencies
        // and recalculate PDE classpath

        IFile manifest = PDEProjectHelper.getBundleManifest( project );
        if ( manifest.isAccessible() )
        {
            manifest.touch( monitor );
        }
    }
}
