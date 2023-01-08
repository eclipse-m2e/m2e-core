/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt;

import java.io.File;
import java.util.List;

import org.osgi.framework.BundleContext;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenConfigurationChangeEvent;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.MavenClassifierManager;
import org.eclipse.m2e.jdt.internal.Messages;
import org.eclipse.m2e.jdt.internal.launch.MavenLaunchConfigurationListener;


/**
 * Only {@link #getDefault()} and {@link #getBuildpathManager()} are part of public API. All other methods, includes
 * methods inherited from AbstractUIPlugin should not be referenced by the client and can be removed without notice.
 */
public class MavenJdtPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.jdt"; //$NON-NLS-1$

  public static final String PREFERENCES_JRE_SYSTEM_LIBRARY_VERSION = "jreSystemLibraryVersion"; //$NON-NLS-1$

  private static MavenJdtPlugin instance;

  MavenLaunchConfigurationListener launchConfigurationListener;

  BuildPathManager buildpathManager;

  IMavenClassifierManager mavenClassifierManager;

  WorkspaceSourceDownloadJob workspaceSourceDownloadJob;

  private final IPreferencesService preferencesService;

  private final IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[2];

  /**
   * @noreference see class javadoc
   */
  public MavenJdtPlugin() {
    preferencesService = Platform.getPreferencesService();
    preferencesLookup[0] = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
    preferencesLookup[1] = DefaultScope.INSTANCE.getNode(PLUGIN_ID);
  }

  /**
   * @noreference see class javadoc
   */
  @Override
  @SuppressWarnings("static-access")
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    instance = this;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();

    IMavenProjectRegistry projectManager = MavenPluginActivator.getDefault().getMavenProjectManager();
    IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();

    File stateLocationDir = getStateLocation().toFile();

    this.buildpathManager = new BuildPathManager(projectManager, bundleContext, stateLocationDir);
    workspace.addResourceChangeListener(buildpathManager,
        IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);

    projectManager.addMavenProjectChangedListener(this.buildpathManager);

    workspaceSourceDownloadJob = new WorkspaceSourceDownloadJob();

    mavenConfiguration.addConfigurationChangeListener(event -> {
      String key = event.key();

      // use those constants from the event class is to have an overview of supported event keys
      if((MavenConfigurationChangeEvent.P_DOWNLOAD_JAVADOC.equals(key) && mavenConfiguration.isDownloadJavaDoc())
          || (MavenConfigurationChangeEvent.P_DOWNLOAD_SOURCES.equals(key) && mavenConfiguration.isDownloadSources())) {
        if(workspaceSourceDownloadJob.getState() == Job.SLEEPING
            || workspaceSourceDownloadJob.getState() == Job.WAITING) {
          //Cancel previously scheduled job
          workspaceSourceDownloadJob.cancel();
        }
        workspaceSourceDownloadJob.schedule(500);
        return;
      }

      if(!MavenConfigurationChangeEvent.P_USER_SETTINGS_FILE.equals(key)) {
        return;
      }

      if(buildpathManager.setupVariables() && buildpathManager.variablesAreInUse()) {
        WorkspaceJob job = new WorkspaceJob(Messages.MavenJdtPlugin_job_name) {

          @Override
          public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            return Status.OK_STATUS;
          }

        };
        job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
        job.schedule();
      }
    });

    this.launchConfigurationListener = new MavenLaunchConfigurationListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(launchConfigurationListener);
    projectManager.addMavenProjectChangedListener(launchConfigurationListener);

    this.mavenClassifierManager = new MavenClassifierManager();
  }

  /**
   * @noreference see class javadoc
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.removeResourceChangeListener(this.buildpathManager);
    workspaceSourceDownloadJob = null;
    MavenPluginActivator mplugin = MavenPluginActivator.getDefault();
    if(mplugin != null) {
      IMavenProjectRegistry projectManager = mplugin.getMavenProjectManager();
      projectManager.removeMavenProjectChangedListener(buildpathManager);
      projectManager.removeMavenProjectChangedListener(launchConfigurationListener);
    }

    DebugPlugin dplugin = DebugPlugin.getDefault();
    if(dplugin != null) {
      dplugin.getLaunchManager().removeLaunchConfigurationListener(launchConfigurationListener);
    }

    this.buildpathManager = null;
    this.launchConfigurationListener = null;
    this.mavenClassifierManager = null;

    instance = null;
  }

  public static MavenJdtPlugin getDefault() {
    return instance;
  }

  public IClasspathManager getBuildpathManager() {
    return buildpathManager;
  }

  /**
   * @return Returns the mavenClassifierManager.
   */
  public IMavenClassifierManager getMavenClassifierManager() {
    return this.mavenClassifierManager;
  }

  public JreSystemVersion getJreSystemVersion() {
    return JreSystemVersion
        .valueOf(preferencesService.get(PREFERENCES_JRE_SYSTEM_LIBRARY_VERSION, PLUGIN_ID, preferencesLookup));
  }

  @SuppressWarnings("restriction")
  private class WorkspaceSourceDownloadJob extends MavenJob
      implements org.eclipse.m2e.core.internal.jobs.IBackgroundProcessingQueue {

    private boolean done;

    public WorkspaceSourceDownloadJob() {
      super("Scheduling source/javadoc downloads");
      setPriority(BuildPathManager.SOURCE_DOWNLOAD_PRIORITY);//low priority job
    }

    @Override
    public boolean isEmpty() {
      return done;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      done = false;
      IMavenConfiguration mavenConfiguration = MavenPlugin.getMavenConfiguration();
      if(mavenConfiguration.isDownloadSources() || mavenConfiguration.isDownloadJavaDoc()) {
        List<IMavenProjectFacade> facades = MavenPlugin.getMavenProjectRegistry().getProjects();
        for(IMavenProjectFacade facade : facades) {
          if(monitor.isCanceled()) {
            break;
          }
          IProject project = facade.getProject();
          buildpathManager.scheduleDownload(project, mavenConfiguration.isDownloadSources(),
              mavenConfiguration.isDownloadJavaDoc());
        }
      }
      done = true;
      return Status.OK_STATUS;
    }

  }
}
