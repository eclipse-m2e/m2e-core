/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Hannes Wellmann - Major rework of MavenConsoleLineTracker
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Maven Console line tracker
 *
 * @author Eugene Kuleshov
 */
public class MavenConsoleLineTracker implements IConsoleLineTracker {
  private static final Logger log = LoggerFactory.getLogger(MavenConsoleLineTracker.class);

  private static final String PLUGIN_ID = "org.eclipse.m2e.launching"; //$NON-NLS-1$

  private static final String LISTENING_MARKER = "Listening for transport dt_socket at address: ";

  private static final String RUNNING_MARKER = "Running ";

  private boolean isMavenBuildProcess;

  private IConsole console;

  private boolean initialized = false;

  @Override
  public void init(IConsole console) {
    if(initialized) { // make sure that for each process launch a new tracker is created (which is the case a.t.m.)
      throw new IllegalStateException("MavenConsoleLineTracker already connected to console");
    }
    this.console = console;
    ILaunchConfiguration launchConfiguration = console.getProcess().getLaunch().getLaunchConfiguration();
    isMavenBuildProcess = launchConfiguration != null && isMavenProcess(launchConfiguration);
    this.initialized = true; // Initialized
  }

  private IMavenProjectFacade mavenProject;

  @Override
  public void lineAppended(IRegion line) {
    if(isMavenBuildProcess) {
      try {
        int offset = line.getOffset();
        int length = line.getLength();
        String text = console.getDocument().get(offset, length).strip();

        readProjectDefinition(text);
        if(mavenProject == null) {
          return;
        }

        String testName = null;

        int index = text.indexOf(RUNNING_MARKER);
        if(index > -1) {
          testName = text.substring(index + RUNNING_MARKER.length());
          offset += index + RUNNING_MARKER.length();

        } else if((index = text.indexOf(LISTENING_MARKER)) > -1) {
          // create and start remote Java app launch configuration
          String portString = text.substring(index + LISTENING_MARKER.length()).trim();
          launchRemoteJavaApp(mavenProject.getProject(), portString);
        }

        if(testName != null) {
          MavenConsoleHyperLink link = new MavenConsoleHyperLink(mavenProject, testName);
          console.addLink(link, offset, testName.length());
        }

      } catch(BadLocationException ex) {
        // ignore
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
  }

  private boolean isMavenProcess(ILaunchConfiguration launchConfiguration) {
    try {
      ILaunchConfigurationType type = launchConfiguration.getType();
      return PLUGIN_ID.equals(type.getPluginIdentifier());
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
      return false;
    }
  }

  private static final Pattern GROUP_ARTIFACT_LINE = Pattern
      .compile("^\\[INFO\\] -+< (?<groupId>[\\w\\.\\-]+):(?<artifactId>[\\w\\.\\-]+) >-+$");

  private static final Pattern VERSION_LINE = Pattern
      .compile("^\\[INFO\\] Building .+ (?<version>[\\w\\.\\-]+)( +\\[\\d+/\\d+\\])?$");

  private static final Pattern PACKAGING_TYPE_LINE = Pattern.compile("^\\[INFO\\] -+\\[ [\\w\\-\\. ]+ \\]-+$");

  private final Deque<String> projectDefinitionLines = new ArrayDeque<>(3);

  private void readProjectDefinition(String lineText) {
    projectDefinitionLines.add(lineText);
    if(projectDefinitionLines.size() < 3) {
      return;
    }
    // Read groupId, artifactId and version from a sequence like the following lines:
    // [INFO] -----------< org.eclipse.m2e:org.eclipse.m2e.maven.runtime >------------
    // [INFO] Building M2E Embedded Maven Runtime (includes Incubating components) 1.18.2-SNAPSHOT [4/5]
    // [INFO] ---------------------------[ eclipse-plugin ]---------------------------

    Iterator<String> descendingIterator = projectDefinitionLines.descendingIterator();
    String line3 = descendingIterator.next();
    if(PACKAGING_TYPE_LINE.matcher(line3).matches()) {

      String line2 = descendingIterator.next();
      Matcher vMatcher = VERSION_LINE.matcher(line2);
      if(vMatcher.matches()) {
        String version = vMatcher.group("version");

        String line1 = descendingIterator.next();
        Matcher gaMatcher = GROUP_ARTIFACT_LINE.matcher(line1);
        if(gaMatcher.matches()) {
          String groupId = gaMatcher.group("groupId");
          String artifactId = gaMatcher.group("artifactId");

          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          mavenProject = projectManager.getMavenProject(groupId, artifactId, version);
        }
      }
    }
    projectDefinitionLines.remove(); // only latest three lines are relevant -> use it as ring-buffer
  }

  @Override
  public void dispose() {
    isMavenBuildProcess = false;
    projectDefinitionLines.clear();
    mavenProject = null;
    initialized = false;
  }

  private static void launchRemoteJavaApp(IProject project, String portString) throws CoreException {
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
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_CONNECTOR,
        IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR);

    Map<String, String> connectMap = new HashMap<>();
    connectMap.put("port", portString); //$NON-NLS-1$
    connectMap.put("hostname", "localhost"); //$NON-NLS-1$ //$NON-NLS-2$
    workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, connectMap);

    if(project != null && project.exists()) {
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
    }

    DebugUITools.launch(workingCopy, ILaunchManager.DEBUG_MODE); //$NON-NLS-1$
  }

  /**
   * Opens a text editor for Maven test report
   */
  private static class MavenConsoleHyperLink implements IHyperlink {

    private final String testName;

    private Path baseDir;

    public MavenConsoleHyperLink(IMavenProjectFacade mavenProjectFacade, String testName) {
      this.testName = testName;
      MavenProject mavenProject = mavenProjectFacade.getMavenProject();
      baseDir = Path.of(mavenProject.getBuild().getDirectory());
    }

    @Override
    public void linkActivated() {
      List<Path> reportFiles = getTestSummaryFiles(baseDir, testName);
      // TODO show selection dialog when there is more then one result found
      if(!reportFiles.isEmpty()) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        for(Path reportFile : reportFiles) {
          IFile[] files = wsRoot.findFilesForLocationURI(reportFile.toUri());
          for(IFile file : files) {
            try {
              file.refreshLocal(IResource.DEPTH_ZERO, null);
              IDE.openEditor(page, file);
              break;
            } catch(CoreException ex) {
              log.error(ex.getMessage(), ex);
            }
          }
        }
      }
    }

    private static List<Path> getTestSummaryFiles(Path baseDir, String testName) {
      List<Path> jUnitXMLFiles = new ArrayList<>();
      List<Path> plainTextFiles = new ArrayList<>();
      Path jUnitReportFile = Path.of("TEST-" + testName + ".xml");
      Path plainTextSummaryFile = Path.of(testName + ".txt");
      try (Stream<Path> s = Files.walk(baseDir)) {
        s.forEach(p -> {
          if(p.endsWith(jUnitReportFile) && Files.isRegularFile(p)) {
            jUnitXMLFiles.add(p);
          } else if(p.endsWith(plainTextSummaryFile) && Files.isRegularFile(p)) {
            plainTextFiles.add(p);
          }
        });
      } catch(IOException e) {
        log.error("Failed to search test summary files", e);
        return Collections.emptyList();
      }
      return !jUnitXMLFiles.isEmpty() ? jUnitXMLFiles : plainTextFiles;
    }

    @Override
    public void linkEntered() { // nothing to do
    }

    @Override
    public void linkExited() { // nothing to do
    }
  }
}
