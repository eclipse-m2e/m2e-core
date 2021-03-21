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

package org.eclipse.m2e.internal.launch;

import static org.eclipse.m2e.actions.MavenLaunchConstants.ATTR_WORKSPACE_RESOLUTION;
import static org.eclipse.m2e.actions.MavenLaunchConstants.PLUGIN_ID;
import static org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration.LAUNCHER_REALM;
import static org.eclipse.m2e.internal.launch.MavenLaunchUtils.quote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.launching.IVMRunner;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.workspace.WorkspaceState;


/**
 * Helper class to configure and launch MavenRuntime instance.
 * <p>
 * Generates classworld configuration file, i.e. m2.conf. Generated classworld configuration file will include
 * cliresolver for launch configuration that have workspace resolution enabled.
 * <p>
 * Sets the following conventional launch configuration attributes.
 * <ul>
 * <li>WorkspaceState.SYSPROP_STATEFILE_LOCATION, full absolute path of m2e workspace state file. See
 * {@link WorkspaceState} for details of the state file format. Only set if workspace dependency resolution is enabled
 * for the launch configuration.</li>
 * <li>maven.bootclasspath, maven runtime bootstrap classpath, normally only contains classworlds jar.</li>
 * <li>maven.home, location of maven runtime, logical name is used for embedded and workspace runtimes</li>
 * <li>classworlds.conf, location of classworlds configuration file, i.e. m2.conf</li>
 * </ul>
 * 
 * @since 1.4
 */
@SuppressWarnings("restriction")
public class MavenRuntimeLaunchSupport {

  private final AbstractMavenRuntime runtime;

  private final MavenLauncherConfigurationHandler cwconf;

  private final boolean resolveWorkspaceArtifacts;

  private final File cwconfFile;

  public static class VMArguments {

    private final StringBuilder properties = new StringBuilder();

    public void append(String str) {
      if(str != null) {
        str = str.trim();
      }
      if(str != null && str.length() > 0) {
        if(properties.length() > 0) {
          properties.append(' ');
        }
        properties.append(str);
      }
    }

    public void appendProperty(String key, String value) {
      append("-D" + key + "=" + value);
    }

    @Override
    public String toString() {
      return properties.toString();
    }
  }

  public static class Builder {
    private final ILaunchConfiguration configuration;

    private boolean resolveWorkspaceArtifacts;

    private boolean injectWorkspaceResolver;

    Builder(ILaunchConfiguration configuration) throws CoreException {
      this.configuration = configuration;
      enableWorkspaceResolution(configuration.getAttribute(ATTR_WORKSPACE_RESOLUTION, false));
    }

    public Builder enableWorkspaceResolution(boolean enable) {
      this.resolveWorkspaceArtifacts = enable;
      this.injectWorkspaceResolver = enable;
      return this;
    }

    public Builder enableWorkspaceResolver(boolean enable) {
      this.injectWorkspaceResolver = enable;
      return this;
    }

    public MavenRuntimeLaunchSupport build(IProgressMonitor monitor) throws CoreException {

      final AbstractMavenRuntime runtime = MavenLaunchUtils.getMavenRuntime(configuration);

      final MavenLauncherConfigurationHandler cwconf = new MavenLauncherConfigurationHandler();
      runtime.createLauncherConfiguration(cwconf, monitor);
      if(injectWorkspaceResolver) {
        for(String entry : MavenLaunchUtils.getCliResolver(runtime)) {
          cwconf.forceArchiveEntry(entry);
        }
      }

      final File cwconfFile;
      try {
        File state = MavenLaunchPlugin.getDefault().getStateLocation().toFile();
        File dir = new File(state, "launches"); //$NON-NLS-1$
        dir.mkdirs();
        cwconfFile = File.createTempFile("m2conf", ".tmp", dir); //$NON-NLS-1$ //$NON-NLS-2$
        OutputStream os = new FileOutputStream(cwconfFile);
        try {
          cwconf.save(os);
        } finally {
          os.close();
        }
      } catch(IOException e) {
        throw new CoreException(
            new Status(IStatus.ERROR, PLUGIN_ID, -1, Messages.MavenLaunchDelegate_error_cannot_create_conf, e));
      }

      return new MavenRuntimeLaunchSupport(runtime, cwconf, cwconfFile, resolveWorkspaceArtifacts);
    }
  }

  /**
   * Refreshes resources as specified by a launch configuration, when an associated process terminates. Adapted from
   * org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher
   */
  private class BackgroundResourceRefresher implements IDebugEventSetListener {
    final ILaunchConfiguration configuration;

    final IProcess process;

    public BackgroundResourceRefresher(ILaunchConfiguration configuration, ILaunch launch) {
      this.configuration = configuration;
      this.process = launch.getProcesses()[0];
    }

    /**
     * If the process has already terminated, resource refreshing is done immediately in the current thread. Otherwise,
     * refreshing is done when the process terminates.
     */
    public void init() {
      synchronized(process) {
        if(process.isTerminated()) {
          processResources();
        } else {
          DebugPlugin.getDefault().addDebugEventListener(this);
        }
      }
    }

    public void handleDebugEvents(DebugEvent[] events) {
      for(DebugEvent event : events) {
        if(event.getSource() == process && event.getKind() == DebugEvent.TERMINATE) {
          DebugPlugin.getDefault().removeDebugEventListener(this);
          processResources();
          break;
        }
      }
    }

    protected void processResources() {
      getClassworldConfFile().delete();
      Job job = new Job(Messages.MavenLaunchDelegate_job_name) {
        public IStatus run(IProgressMonitor monitor) {
          try {
            RefreshTab.refreshResources(configuration, monitor);
            return Status.OK_STATUS;
          } catch(CoreException e) {
            return e.getStatus();
          }
        }
      };
      job.schedule();
    }
  }

  MavenRuntimeLaunchSupport(AbstractMavenRuntime runtime, MavenLauncherConfigurationHandler cwconf, File cwconfFile,
      boolean resolveWorkspaceArtifacts) {
    this.runtime = runtime;
    this.cwconf = cwconf;
    this.cwconfFile = cwconfFile;
    this.resolveWorkspaceArtifacts = resolveWorkspaceArtifacts;
  }

  public static Builder builder(ILaunchConfiguration configuration) throws CoreException {
    return new Builder(configuration);
  }

  public static MavenRuntimeLaunchSupport create(ILaunchConfiguration configuration, ILaunch launch,
      IProgressMonitor monitor) throws CoreException {
    return builder(configuration).build(monitor);
  }

  /**
   * MAVEN_HOME or equivalent location of the Maven runtime.
   */
  public String getLocation() {
    return runtime.getLocation();
  }

  /**
   * Location of classworld configuration file, i.e. m2.conf, of the Maven runtime.
   */
  public File getClassworldConfFile() {
    return cwconfFile;
  }

  /**
   * Bootstrap classpath of the Maven runtime, normally only contains classworlds jar.
   */
  @SuppressWarnings("deprecation")
  public List<String> getBootClasspath() {
    return cwconf.getRealmEntries(LAUNCHER_REALM);
  }

  public String getSettings() {
    return runtime.getSettings();
  }

  public String getVersion() {
    return runtime.getVersion();
  }

  public VMArguments getVMArguments() {
    VMArguments properties = new VMArguments();

    applyMavenRuntime(properties);

    if(resolveWorkspaceArtifacts) {
      applyWorkspaceArtifacts(properties); // workspace artifact resolution
    }

    return properties;
  }

  public void applyMavenRuntime(VMArguments properties) {
    // maven.home
    String location = runtime.getLocation();
    if(location != null) {
      properties.appendProperty("maven.home", quote(location)); //$NON-NLS-1$
    }

    // m2.conf
    properties.appendProperty("classworlds.conf", quote(cwconfFile.getAbsolutePath())); //$NON-NLS-1$
  }

  public static void applyWorkspaceArtifacts(VMArguments properties) {
    File state = MavenPluginActivator.getDefault().getMavenProjectManager().getWorkspaceStateFile();
    properties.appendProperty(WorkspaceState.SYSPROP_STATEFILE_LOCATION, quote(state.getAbsolutePath())); //$NON-NLS-1$
  }

  public IVMRunner decorateVMRunner(final IVMRunner runner) {
    return (runnerConfiguration, launch, monitor) -> {
      runner.run(runnerConfiguration, launch, monitor);

      IProcess[] processes = launch.getProcesses();
      if(processes != null && processes.length > 0) {
        ILaunchConfiguration configuration = launch.getLaunchConfiguration();
        BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, launch);
        refresher.init();
      } else {
        // the process didn't start, remove temp classworlds.conf right away
        getClassworldConfFile().delete();
      }
    };
  }
}
