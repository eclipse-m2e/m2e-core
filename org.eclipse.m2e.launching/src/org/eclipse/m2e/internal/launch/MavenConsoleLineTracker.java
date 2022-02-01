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

  private static final String IDENTIFIER = "([\\w\\.\\-]+)";

  private static final Pattern LISTENING_MARKER = Pattern
      .compile("Listening for transport dt_socket at address: (\\d+)$");

  private static final int DEBUGGER_PORT = 1;

  private static final Pattern RUNNING_TEST_CLASS = Pattern.compile("Running ([\\w\\.]+)$");

  private static final int TEST_CLASS_NAME = 1;

  private static final Pattern EXECUTION_FAILURE = Pattern
      .compile("^\\[ERROR\\] Failed to execute goal .+ on project " + IDENTIFIER);

  private static final int FAILED_ARTIFACT_ID = 1;

  private boolean isMavenBuildProcess;

  private IConsole console;

  private boolean initialized = false;

  private IMavenProjectFacade mavenProject;

  private final Deque<IRegion> projectDefinitionLines = new ArrayDeque<>(2);

  @Override
  public void init(IConsole console) {
    if(initialized) { // make sure that for each process launch a new tracker is created (which is the case a.t.m.)
      throw new IllegalStateException("MavenConsoleLineTracker already connected to console");
    }
    this.console = console;
    ILaunchConfiguration launchConfiguration = console.getProcess().getLaunch().getLaunchConfiguration();
    isMavenBuildProcess = launchConfiguration != null && isMavenProcess(launchConfiguration);
    initialized = true; // Initialized
  }

  @Override
  public void lineAppended(IRegion line) {
    if(isMavenBuildProcess) {
      try {
        String text = getText(line);
        if(mavenProject == null) {
          return;
        }

        Matcher runningTestMatcher = RUNNING_TEST_CLASS.matcher(text);
        if(runningTestMatcher.find()) {
          String testName = runningTestMatcher.group(TEST_CLASS_NAME);
          int start = runningTestMatcher.start(TEST_CLASS_NAME);

          IHyperlink link = new MavenTestReportHyperLink(mavenProject, testName);
          console.addLink(link, line.getOffset() + start, testName.length());
          return;
        }

        Matcher listeningMatcher = LISTENING_MARKER.matcher(text);
        if(listeningMatcher.find()) {
          String portString = listeningMatcher.group(DEBUGGER_PORT);
          // create and start remote Java app launch configuration
          launchRemoteJavaApp(mavenProject.getProject(), portString);
          return;
        }

        Matcher failureMatcher = EXECUTION_FAILURE.matcher(text);
        if(failureMatcher.find()) {
          addProjectLink(line, failureMatcher, FAILED_ARTIFACT_ID, FAILED_ARTIFACT_ID);
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
      .compile("^\\[INFO\\] -+< " + IDENTIFIER + ":" + IDENTIFIER + " >-+$");

  private static final int GROUP_ID = 1;

  private static final int ARTIFACT_ID = 2;

  private static final Pattern VERSION_LINE = Pattern
      .compile("^\\[INFO\\] Building .+ " + IDENTIFIER + "( +\\[\\d+/\\d+\\])?$");

  private static final int VERSION = 1;

  private static final Pattern PACKAGING_TYPE_LINE = Pattern.compile("^\\[INFO\\] -+\\[ [\\w\\-\\. ]+ \\]-+$");

  private String getText(IRegion lineRegion) throws BadLocationException {
    String line0 = getLineText(lineRegion);

    if(projectDefinitionLines.size() < 2) {
      projectDefinitionLines.add(lineRegion);
      return line0;
    }
    // Read groupId, artifactId and version from a sequence like the following lines:
    // [INFO] -----------< org.eclipse.m2e:org.eclipse.m2e.maven.runtime >------------
    // [INFO] Building M2E Embedded Maven Runtime (includes Incubating components) 1.18.2-SNAPSHOT [4/5]
    // [INFO] ---------------------------[ eclipse-plugin ]---------------------------

    if(PACKAGING_TYPE_LINE.matcher(line0).matches()) {
      Iterator<IRegion> previousLines = projectDefinitionLines.descendingIterator();

      IRegion line1Region = previousLines.next();
      String line1 = getLineText(line1Region);

      Matcher vMatcher = VERSION_LINE.matcher(line1);
      if(vMatcher.matches()) {
        String version = vMatcher.group(VERSION);

        IRegion line2Region = previousLines.next();
        String line2 = getLineText(line2Region);
        Matcher gaMatcher = GROUP_ARTIFACT_LINE.matcher(line2);
        if(gaMatcher.matches()) {
          String groupId = gaMatcher.group(GROUP_ID);
          String artifactId = gaMatcher.group(ARTIFACT_ID);

          IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
          mavenProject = projectManager.getMavenProject(groupId, artifactId, version);

          addProjectLink(line2Region, gaMatcher, GROUP_ID, ARTIFACT_ID);
        }
      }
    }
    projectDefinitionLines.remove(); // only latest three lines are relevant -> use it as ring-buffer
    projectDefinitionLines.add(lineRegion);
    return line0;
  }

  private String getLineText(IRegion lineRegion) throws BadLocationException {
    return console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength()).strip();
  }

  private void addProjectLink(IRegion line2Region, Matcher gaMatcher, int startGroup, int endGroup) {
    int start = gaMatcher.start(startGroup);
    int end = gaMatcher.end(endGroup);
    IHyperlink link = new MavenProjectHyperLink(mavenProject);
    console.addLink(link, line2Region.getOffset() + start, end - start);
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
  private static class MavenTestReportHyperLink implements IHyperlink {

    private final String testName;

    private final IMavenProjectFacade mavenProjectFacade;

    public MavenTestReportHyperLink(IMavenProjectFacade mavenProjectFacade, String testName) {
      this.testName = testName;
      this.mavenProjectFacade = mavenProjectFacade;
    }

    @Override
    public void linkActivated() {
      MavenProject mavenProject = mavenProjectFacade.getMavenProject();
      if(mavenProject == null) {
        return; // happens when the project was not yet build
      }
      Path baseDir = Path.of(mavenProject.getBuild().getDirectory());
      List<Path> reportFiles = getTestReportFiles(baseDir, testName);
      if(!reportFiles.isEmpty()) {
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        for(Path reportFile : reportFiles) {
          IFile[] files = wsRoot.findFilesForLocationURI(reportFile.toUri());
          if(files.length > 0) {
            openFileInStandardEditor(files[0]);
          }
        }
      }
    }

    private static List<Path> getTestReportFiles(Path baseDir, String testName) {
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

  private static class MavenProjectHyperLink implements IHyperlink {

    private final IMavenProjectFacade mavenProjectFacade;

    public MavenProjectHyperLink(IMavenProjectFacade mavenProjectFacade) {
      this.mavenProjectFacade = mavenProjectFacade;
    }

    public void linkActivated() {
      IFile pom = mavenProjectFacade.getPom();
      openFileInStandardEditor(pom);
    }

    @Override
    public void linkEntered() { // nothing to do
    }

    @Override
    public void linkExited() { // nothing to do
    }
  }

  private static void openFileInStandardEditor(IFile file) {
    try {
      file.refreshLocal(IResource.DEPTH_ZERO, null);
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      IDE.openEditor(page, file);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }
  }
}
