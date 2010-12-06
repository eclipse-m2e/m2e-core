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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;


/**
 * Maven Console line tracker
 * 
 * @author Eugene Kuleshov
 */
public class MavenConsoleLineTracker implements IConsoleLineTracker {

  private static final String PLUGIN_ID = "org.eclipse.m2e.launching"; //$NON-NLS-1$

  private static final String LISTENING_MARKER = "Listening for transport dt_socket at address: ";
  
  private static final String RUNNING_MARKER = "Running ";

  private static final String TEST_TEMPLATE = "(?:  )test.+\\(([\\w\\.]+)\\)"; //$NON-NLS-1$
  
  private static final Pattern PATTERN2 = Pattern.compile(TEST_TEMPLATE);
  
  private IConsole console;

  public void init(IConsole console) {
    this.console = console;
  }

  public void lineAppended(IRegion line) {
    IProcess process = console.getProcess();
    ILaunch launch = process.getLaunch();
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();

    if(launchConfiguration!=null && isMavenProcess(launchConfiguration)) {
      try {
        int offset = line.getOffset();
        int length = line.getLength();

        String text = console.getDocument().get(offset, length);
        
        String testName = null;
        
        int index = text.indexOf(RUNNING_MARKER);
        if(index > -1) {
          testName = text.substring(RUNNING_MARKER.length());
          offset += RUNNING_MARKER.length();

        } else if(text.startsWith(LISTENING_MARKER)) {
          // create and start remote Java app launch configuration
          String baseDir = getBaseDir(launchConfiguration);
          if(baseDir!=null) {
            String portString = text.substring(LISTENING_MARKER.length()).trim();
            MavenDebugHyperLink link = new MavenDebugHyperLink(baseDir, portString);
            console.addLink(link, offset, LISTENING_MARKER.length() + portString.length());
            // launchRemoteJavaApp(baseDir, portString);
          }
          
        } else {
          Matcher m = PATTERN2.matcher(text);
          if(m.find()) {
            testName = m.group(1);
            offset += m.start(1);
          }          
        }

        if(testName != null) {
          String baseDir = getBaseDir(launchConfiguration);
          if(baseDir!=null) {
            MavenConsoleHyperLink link = new MavenConsoleHyperLink(baseDir, testName);
            console.addLink(link, offset, testName.length());
          }
        }

      } catch(BadLocationException ex) {
        // ignore
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  private String getBaseDir(ILaunchConfiguration launchConfiguration) throws CoreException {
    return launchConfiguration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null);
  }

  public void dispose() {
  }

  private boolean isMavenProcess(ILaunchConfiguration launchConfiguration) {
    try {
      ILaunchConfigurationType type = launchConfiguration.getType();
      return PLUGIN_ID.equals(type.getPluginIdentifier());
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return false;
    }
  }

  static void launchRemoteJavaApp(String baseDir, String portString) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_REMOTE_JAVA_APPLICATION);

    /*
    <launchConfiguration type="org.eclipse.jdt.launching.remoteJavaApplication">
      <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="foo-launch"/>
      <stringAttribute key="org.eclipse.jdt.launching.VM_CONNECTOR_ID" value="org.eclipse.jdt.launching.socketAttachConnector"/>
      <booleanAttribute key="org.eclipse.jdt.launching.ALLOW_TERMINATE" value="false"/>
      <mapAttribute key="org.eclipse.jdt.launching.CONNECT_MAP">
        <mapEntry key="port" value="8000"/>
        <mapEntry key="hostname" value="localhost"/>
      </mapAttribute>
    
      <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
        <listEntry value="/foo-launch"/>
      </listAttribute>
      <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
        <listEntry value="4"/>
      </listAttribute>
      
      <listAttribute key="org.eclipse.debug.ui.favoriteGroups">
        <listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
      </listAttribute>
     */
    
    ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, //
        "Connecting debugger to port " + portString);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_ALLOW_TERMINATE, false);
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR, IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);
    
    Map<String, String> connectMap = new HashMap<String, String>();
    connectMap.put("port", portString); //$NON-NLS-1$
    connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);

    IProject project = getProject(baseDir);
    if(project!=null) {
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
    }

    DebugUITools.launch(workingCopy, "debug"); //$NON-NLS-1$
  }

  static IProject getProject(String baseDir) {
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    for(IMavenProjectFacade projectFacade : projectManager.getProjects()) {
      IContainer base = projectFacade.getPom().getParent();
      String baseLocation = base.getLocation().toPortableString();
      if(baseDir.equals(baseLocation)) {
        return projectFacade.getProject();
      }
    }
    return null;
  }
  
  
  /**
   * Opens a text editor for Maven test report 
   */
  public class MavenConsoleHyperLink implements IHyperlink {

    private final String baseDir;
    private final String testName;

    public MavenConsoleHyperLink(String baseDir, String testName) {
      this.baseDir = baseDir;
      this.testName = testName;
    }

    public void linkActivated() {
      DirectoryScanner ds = new DirectoryScanner();
      ds.setBasedir(baseDir);
      ds.setIncludes(new String[] {"**/" + testName + ".txt"}); //$NON-NLS-1$ //$NON-NLS-2$
      ds.scan();
      String[] includedFiles = ds.getIncludedFiles();

      // TODO show selection dialog when there is more then one result found
      if(includedFiles != null && includedFiles.length > 0) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("foo.txt"); //$NON-NLS-1$
        
        File reportFile = new File(baseDir, includedFiles[0]);
        
        try {
          IDE.openEditor(page, new MavenFileEditorInput(reportFile.getAbsolutePath()), desc.getId());
        } catch(PartInitException ex) {
          MavenLogger.log(ex);
        }
      }
    }

    public void linkEntered() {
    }

    public void linkExited() {
    }

  }

  /**
   * Creates debug launch configuration for remote Java application. For example,
   * with surefire plugin the following property can be specified: 
   * -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 -Xnoagent -Djava.compiler=NONE"
   */
  public class MavenDebugHyperLink implements IHyperlink {

    private final String baseDir;
    private final String portString;

    public MavenDebugHyperLink(String baseDir, String portString) {
      this.baseDir = baseDir;
      this.portString = portString;
    }
    
    public void linkActivated() {
      try {
        launchRemoteJavaApp(baseDir, portString);
      } catch (CoreException ex) {
        MavenLogger.log(ex);
      }
    }

    public void linkEntered() {
    }

    public void linkExited() {
    }
    
  }
  
}

