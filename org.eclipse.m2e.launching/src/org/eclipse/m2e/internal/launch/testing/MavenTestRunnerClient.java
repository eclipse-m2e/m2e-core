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
import org.eclipse.unittest.internal.junitXmlReport.TestRunHandler;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.apache.maven.execution.ExecutionEvent.Type;

import org.eclipse.m2e.internal.launch.MavenBuildProjectDataConnection;
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

  public void onTestEvent(MavenTestEvent mavenTestEvent) {
    if(mavenTestEvent.getType() == Type.MojoSucceeded || mavenTestEvent.getType() == Type.MojoFailed) {
      //in any case look for the tests...
      Path reportDirectory = mavenTestEvent.getReportDirectory();
      if(Files.isDirectory(reportDirectory)) {
        SAXParser parser = getParser();
        if(parser == null) {
          return;
        }
        try (Stream<Path> list = Files.list(reportDirectory)) {
          Iterator<Path> iterator = list.iterator();
          while(iterator.hasNext()) {
            Path path = iterator.next();
            System.out.println("Scan result file " + path);
            ITestRunSession importedSession = parseFile(path, parser);
            if(importedSession != null) {
              ITestSuiteElement project = getProjectSuite();
              ITestSuiteElement file = session.newTestSuite(path.toString(), path.getFileName().toString(), null,
                  project, path.getFileName().toString(), null);
              for(ITestElement element : importedSession.getChildren()) {
                importTestElement(element, file);
              }
            }
          }
        } catch(IOException ex) {
        }
      }
    }
  }

  /**
   * @param element
   * @param file
   */
  private void importTestElement(ITestElement element, ITestSuiteElement parent) {
    if(element instanceof ITestCaseElement testcase) {
      ITestCaseElement importedTestCase = session.newTestCase(parent.getId() + "." + testcase.getId(),
          testcase.getTestName(), parent, testcase.getDisplayName(), testcase.getData());
      session.notifyTestStarted(importedTestCase);
      FailureTrace failureTrace = testcase.getFailureTrace();
      if(failureTrace == null) {
        session.notifyTestEnded(importedTestCase, testcase.isIgnored());
      } else {
        session.notifyTestFailed(importedTestCase, Result.ERROR/*TODO how do we know?*/, false /*TODO how do we know?*/,
            failureTrace);
      }
    } else if(element instanceof ITestSuiteElement suite) {
      ITestSuiteElement importedTestSuite = session.newTestSuite(parent.getId() + "." + suite.getId(),
          suite.getTestName(), null, parent, suite.getDisplayName(), suite.getData());
      session.notifyTestStarted(importedTestSuite);
      for(ITestElement child : suite.getChildren()) {
        importTestElement(child, importedTestSuite);
      }
      session.notifyTestEnded(importedTestSuite, false);
    }
  }

  /**
   * @return
   */
  private ITestSuiteElement getProjectSuite() {
    return projectElementMap.computeIfAbsent(projectData.get(), data -> {
      Path basedir = data.projectBasedir;
      return session.newTestSuite(basedir.toString(), basedir.getFileName().toString(), null, null,
          data.groupId + ":" + data.artifactId, null);
    });
  }

  private SAXParser getParser() {
    try {
      return org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
    } catch(ParserConfigurationException ex) {
    } catch(SAXException ex) {
    }
    return null;
  }

  private ITestRunSession parseFile(Path path, SAXParser parser) {
    //TODO Currently NOT working as this is internal API that is not exported!
    final TestRunHandler handler = new TestRunHandler();
    try {
      parser.parse(Files.newInputStream(path), handler);
      return handler.getTestRunSession();
    } catch(SAXException | IOException ex) {
      //can't read then...
      return null;
    }
  }

  public void close() {
    // nothing to do yet...
  }

}
