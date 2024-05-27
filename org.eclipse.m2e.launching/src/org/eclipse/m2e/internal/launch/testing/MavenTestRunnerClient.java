/********************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.launch.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.xml.sax.SAXException;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.apache.maven.execution.ExecutionEvent.Type;

import org.eclipse.m2e.internal.launch.MavenBuildProjectDataConnection;
import org.eclipse.m2e.internal.launch.testing.copied.TestRunHandler;
import org.eclipse.m2e.internal.maven.listener.MavenBuildListener;
import org.eclipse.m2e.internal.maven.listener.MavenProjectBuildData;
import org.eclipse.m2e.internal.maven.listener.MavenTestEvent;


/**
 * 
 */
public class MavenTestRunnerClient implements ITestRunnerClient, MavenBuildListener {

  private ITestRunSession session;

  private ThreadLocal<MavenProjectBuildData> projectData = new ThreadLocal<>();

  private Map<MavenProjectBuildData, ITestSuiteElement> projectElementMap = new ConcurrentHashMap<>();

  /**
   * @param session
   */
  public MavenTestRunnerClient(ITestRunSession session) {
    this.session = session;
  }

  public void startMonitoring() {
    System.out.println("---- start monitoring ---");
    MavenBuildProjectDataConnection.getConnection(session.getLaunch())
        .ifPresent(con -> con.addMavenBuildListener(this));

  }

  public void stopTest() {
    stopMonitoring();
    ILaunch launch = session.getLaunch();
    try {
      launch.terminate();
    } catch(Exception ex) {
    }
  }

  public void stopMonitoring() {
    System.out.println("---- stop Monitoring ----\r\n");
    MavenBuildProjectDataConnection.getConnection(session.getLaunch())
        .ifPresent(con -> con.removeMavenBuildListener(this));
  }

  /**
   * @return the session
   */
  public ITestRunSession getSession() {
    return this.session;
  }

  public void projectStarted(MavenProjectBuildData data) {
    this.projectData.set(data);
  }

  boolean started;

  public void onTestEvent(MavenTestEvent mavenTestEvent) {
    System.out.println("MavenTestRunnerClient.onTestEvent()");
    if(mavenTestEvent.getType() == Type.MojoSucceeded || mavenTestEvent.getType() == Type.MojoFailed) {
      MavenProjectBuildData buildData = projectData.get();
//      Display.getDefault().execute(() -> {

      //in any case look for the tests...
      Path reportDirectory = mavenTestEvent.getReportDirectory();
      if(Files.isDirectory(reportDirectory)) {
        SAXParser parser = getParser();
        if(parser == null) {
          return;
        }
        ensureStarted();
        ITestSuiteElement projectSuite = getProjectSuite(buildData);
        try (Stream<Path> list = Files.list(reportDirectory)) {
          Iterator<Path> iterator = list.iterator();
          while(iterator.hasNext()) {
            Path path = iterator.next();
            System.out.println("Scan result file " + path);
            parseFile(path, parser, projectSuite);
          }
        } catch(IOException ex) {
        }
      }
//      });
    }
  }

  private synchronized void ensureStarted() {
    if(!started) {
      session.notifyTestSessionStarted(null);
      started = true;
    }
  }

  /**
   * @return
   */
  private ITestSuiteElement getProjectSuite(MavenProjectBuildData buildData) {
    return projectElementMap.computeIfAbsent(buildData, data -> {
      Path basedir = data.projectBasedir;
      ITestSuiteElement suite = session.newTestSuite(System.currentTimeMillis() + basedir.toString(),
          basedir.getFileName().toString(), null, null, data.groupId + ":" + data.artifactId, null);
      session.notifyTestStarted(suite);
      return suite;
    });
  }

  @SuppressWarnings("restriction")
  private SAXParser getParser() {
    try {
      return org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
    } catch(ParserConfigurationException ex) {
    } catch(SAXException ex) {
    }
    return null;
  }

  private void parseFile(Path path, SAXParser parser, ITestSuiteElement parent) {
    final TestRunHandler handler = new TestRunHandler(session, parent);
    try {
      parser.parse(Files.newInputStream(path), handler);
    } catch(SAXException | IOException ex) {
      //can't read then...
    }
  }

  public void close() {
    if(started) {
      session.notifyTestSessionCompleted(null);
    }
  }

}
