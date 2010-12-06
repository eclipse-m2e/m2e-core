/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.util.Util;


public class MavenLaunchDelegate extends JavaLaunchDelegate implements MavenLaunchConstants {

  private static final String LAUNCHER_TYPE = "org.codehaus.classworlds.Launcher"; //$NON-NLS-1$
  private static final String LAUNCHER_TYPE3 = "org.codehaus.plexus.classworlds.launcher.Launcher"; // classwordls 2.0 //$NON-NLS-1$
  private static final String LAUNCH_M2CONF_FILE = "org.eclipse.m2e.internal.launch.M2_CONF"; //$NON-NLS-1$

  private org.eclipse.m2e.core.embedder.MavenRuntime runtime;
  private MavenLauncherConfigurationHandler m2conf;
  private File confFile;

  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    MavenConsole console = MavenPlugin.getDefault().getConsole();
    console.logMessage("" + getWorkingDirectory(configuration)); //$NON-NLS-1$
    console.logMessage(" mvn" + getProgramArguments(configuration)); //$NON-NLS-1$

    runtime = MavenLaunchUtils.getMavenRuntime(configuration);

    m2conf = new MavenLauncherConfigurationHandler();
    if (shouldResolveWorkspaceArtifacts(configuration)) {
      m2conf.addArchiveEntry(MavenLaunchUtils.getCliResolver(runtime));
    }
    MavenLaunchUtils.addUserComponents(configuration, m2conf);
    runtime.createLauncherConfiguration(m2conf, new NullProgressMonitor());

    File state = MavenPlugin.getDefault().getStateLocation().toFile();
    try {
      File dir = new File(state, "launches"); //$NON-NLS-1$
      dir.mkdirs();
      confFile = File.createTempFile("m2conf", ".tmp", dir); //$NON-NLS-1$ //$NON-NLS-2$
      launch.setAttribute(LAUNCH_M2CONF_FILE, confFile.getCanonicalPath());
      OutputStream os = new FileOutputStream(confFile);
      try {
        m2conf.save(os);
      } finally {
        os.close();
      }
    } catch (IOException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, Messages.MavenLaunchDelegate_error_cannot_create_conf, e));
    }
    
    super.launch(configuration, mode, launch, monitor);
  }

  public IVMRunner getVMRunner(final ILaunchConfiguration configuration, String mode) throws CoreException {
    final IVMRunner runner = super.getVMRunner(configuration, mode);
    
    return new IVMRunner() {
      public void run(VMRunnerConfiguration runnerConfiguration, ILaunch launch, IProgressMonitor monitor)
          throws CoreException {
        runner.run(runnerConfiguration, launch, monitor);
        
        IProcess[] processes = launch.getProcesses();
        if(processes!=null && processes.length>0) {
          BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, launch);
          refresher.init();
        } else {
          removeTempFiles(launch);
        }
      }
    };
  }

  public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    return runtime.getVersion().startsWith("3.0")? LAUNCHER_TYPE3: LAUNCHER_TYPE; //$NON-NLS-1$
  }

  public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    List<String> cp = m2conf.getRealmEntries(IMavenLauncherConfiguration.LAUNCHER_REALM);
    return cp.toArray(new String[cp.size()]);
  }

  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    return getProperties(configuration) + //
        getPreferences(configuration) + " " + // //$NON-NLS-1$
        getGoals(configuration);
  }

  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    /*
    * <pre>
    * %MAVEN_JAVA_EXE% %MAVEN_OPTS% 
    *   -classpath %CLASSWORLDS_JAR% 
    *   "-Dclassworlds.conf=%M2_HOME%\bin\m2.conf" 
    *   "-Dmaven.home=%M2_HOME%" 
    *   org.codehaus.classworlds.Launcher 
    *   %MAVEN_CMD_LINE_ARGS%
    * </pre>
    */

    StringBuffer sb = new StringBuffer();

    // workspace artifact resolution
    if (shouldResolveWorkspaceArtifacts(configuration)) {
      File state = MavenPlugin.getDefault().getMavenProjectManager().getWorkspaceStateFile();
      sb.append("-Dm2eclipse.workspace.state=").append(quote(state.getAbsolutePath())); //$NON-NLS-1$
    }

    // maven.home
    String location = runtime.getLocation();
    if (location != null) {
      sb.append(" -Dmaven.home=").append(quote(location)); //$NON-NLS-1$
    }

    // m2.conf
    sb.append(" -Dclassworlds.conf=").append(quote(confFile.getAbsolutePath())); //$NON-NLS-1$

    // user configured entries
    sb.append(" ").append(super.getVMArguments(configuration)); //$NON-NLS-1$

    return sb.toString();
  }

  private String quote(String string) {
    return string.indexOf(' ') > -1 ? "\"" + string + "\"" : string; //$NON-NLS-1$ //$NON-NLS-2$
  }

  private boolean shouldResolveWorkspaceArtifacts(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(ATTR_WORKSPACE_RESOLUTION, false);
  }

  protected String getGoals(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, ""); //$NON-NLS-1$
  }

  public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) {
    return false;
  }
  
  /**
   * Construct string with properties to pass to JVM as system properties
   */
  private String getProperties(ILaunchConfiguration configuration) {
    StringBuffer sb = new StringBuffer();

    try {
      @SuppressWarnings("unchecked")
      List<String> properties = configuration.getAttribute(ATTR_PROPERTIES, Collections.EMPTY_LIST);
      for(String property : properties) {
        int n = property.indexOf('=');
        String name = property;
        String value = null;

        if(n > -1) {
          name = property.substring(0, n);
          if(n > 1) {
            value = Util.substituteVar(property.substring(n + 1));
          }
        }

        sb.append(" -D").append(name); //$NON-NLS-1$
        if(value != null) {
          sb.append('=').append(quote(value));
        }
      }
    } catch(CoreException e) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROPERTIES;
      MavenLogger.log(msg, e);
    }

    try {
      String profiles = configuration.getAttribute(ATTR_PROFILES, (String) null);
      if(profiles != null && profiles.trim().length() > 0) {
        sb.append(" -P").append(profiles.replaceAll("\\s+", ",")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    } catch(CoreException ex) {
      String msg = "Exception while getting configuration attribute " + ATTR_PROFILES;
      MavenLogger.log(msg, ex);
    }

    return sb.toString();
  }

  /**
   * Construct string with preferences to pass to JVM as system properties
   */
  private String getPreferences(ILaunchConfiguration configuration) throws CoreException {
    IMavenConfiguration mavenConfiguration = MavenPlugin.getDefault().getMavenConfiguration();

    StringBuffer sb = new StringBuffer();

    sb.append(" -B"); //$NON-NLS-1$

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_DEBUG_OUTPUT, mavenConfiguration.isDebugOutput())) {
      sb.append(" -X").append(" -e"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_DEBUG_OUTPUT).append("=").append(debugOutput);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_OFFLINE, mavenConfiguration.isOffline())) {
      sb.append(" -o"); //$NON-NLS-1$
    }
    // sb.append(" -D").append(MavenPreferenceConstants.P_OFFLINE).append("=").append(offline);

    if(configuration.getAttribute(MavenLaunchConstants.ATTR_UPDATE_SNAPSHOTS, false)) {
      sb.append(" -U"); //$NON-NLS-1$
    }
    
    if(configuration.getAttribute(MavenLaunchConstants.ATTR_NON_RECURSIVE, false)) {
      sb.append(" -N"); //$NON-NLS-1$
    }
    
    if(configuration.getAttribute(MavenLaunchConstants.ATTR_SKIP_TESTS, false)) {
      sb.append(" -Dmaven.test.skip=true"); //$NON-NLS-1$
    }

    String settings = configuration.getAttribute(MavenLaunchConstants.ATTR_USER_SETTINGS, (String) null);
    if(settings == null || settings.trim().length() <= 0) {
      settings = mavenConfiguration.getUserSettingsFile();
      if(settings != null && settings.trim().length() > 0 && !new File(settings.trim()).exists()) {
        settings = null;
      }
    }
    if(settings != null && settings.trim().length() > 0) {
      sb.append(" -s ").append(quote(settings)); //$NON-NLS-1$
    }

    // boolean b = preferenceStore.getBoolean(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION);
    // sb.append(" -D").append(MavenPreferenceConstants.P_CHECK_LATEST_PLUGIN_VERSION).append("=").append(b);

    // b = preferenceStore.getBoolean(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS);
    // sb.append(" -D").append(MavenPreferenceConstants.P_UPDATE_SNAPSHOTS).append("=").append(b);

    // String s = preferenceStore.getString(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY);
    // if(s != null && s.trim().length() > 0) {
    //   sb.append(" -D").append(MavenPreferenceConstants.P_GLOBAL_CHECKSUM_POLICY).append("=").append(s);
    // }
    
    return sb.toString();
  }

  static void removeTempFiles(ILaunch launch) {
    String m2confName = launch.getAttribute(LAUNCH_M2CONF_FILE);
    if (m2confName != null) {
      new File(m2confName).delete();
    }
  }

  /**
   * Refreshes resources as specified by a launch configuration, when 
   * an associated process terminates.
   * 
   * Adapted from org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher
   */
  public static class BackgroundResourceRefresher implements IDebugEventSetListener  {
    final ILaunchConfiguration configuration;
    final IProcess process;
    final ILaunch launch;
    
    public BackgroundResourceRefresher(ILaunchConfiguration configuration, ILaunch launch) {
      this.configuration = configuration;
      this.process = launch.getProcesses()[0];
      this.launch = launch;
    }
    
    /**
     * If the process has already terminated, resource refreshing is done
     * immediately in the current thread. Otherwise, refreshing is done when the
     * process terminates.
     */
    public void init() {
      synchronized (process) {
        if (process.isTerminated()) {
          processResources();
        } else {
          DebugPlugin.getDefault().addDebugEventListener(this);
        }
      }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.core.IDebugEventSetListener#handleDebugEvents(org.eclipse.debug.core.DebugEvent[])
     */
    public void handleDebugEvents(DebugEvent[] events) {
      for (int i = 0; i < events.length; i++) {
        DebugEvent event = events[i];
        if (event.getSource() == process && event.getKind() == DebugEvent.TERMINATE) {
          DebugPlugin.getDefault().removeDebugEventListener(this);
          processResources();
          break;
        }
      }
    }
    
    protected void processResources() {
      removeTempFiles(launch);

      Job job = new Job(Messages.MavenLaunchDelegate_job_name) {
        public IStatus run(IProgressMonitor monitor) {
          try {
            RefreshTab.refreshResources(configuration, monitor);
            return Status.OK_STATUS;
          } catch (CoreException e) {
            MavenLogger.log(e);
            return e.getStatus();
          } 
        }
      };
      job.schedule();
    }
  }

  
}
