/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch.testing.copied;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;


public class TestRunHandler extends DefaultHandler {

  private static final AtomicInteger fid = new AtomicInteger();

  private ITestRunSession fTestRunSession;

  private ITestSuiteElement fTestSuite;

  private ITestCaseElement fTestCase;

  private final Stack<Boolean> fNotRun = new Stack<>();

  private StringBuilder fFailureBuffer;

  private boolean fInExpected;

  private boolean fInActual;

  private StringBuilder fExpectedBuffer;

  private StringBuilder fActualBuffer;

  private Locator fLocator;

  private Result fStatus;

  private int fLastReportedLine;

  /**
   * Constructs a default {@link TestRunHandler} object instance
   */
  public TestRunHandler(ITestRunSession session, ITestSuiteElement parent) {
    fTestRunSession = session;
    this.fTestSuite = parent;
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    fLocator = locator;
  }

  @Override
  public void startDocument() throws SAXException {
    // Nothing to do
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    try {
      if(fLocator != null) {
        int line = fLocator.getLineNumber();
        if(line - 20 >= fLastReportedLine) {
          line -= line % 20;
          fLastReportedLine = line;
        }
      }

      if(Thread.interrupted())
        throw new OperationCanceledException();

      switch(qName) {
        case IXMLTags.NODE_TESTRUN:
          break;
        case IXMLTags.NODE_TESTSUITES:
          break;
        case IXMLTags.NODE_TESTSUITE: {
          String name = attributes.getValue(IXMLTags.ATTR_NAME);
          String pack = attributes.getValue(IXMLTags.ATTR_PACKAGE);
          String suiteName = pack == null ? name : pack + "." + name; //$NON-NLS-1$
          String displayName = attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
          String data = attributes.getValue(IXMLTags.ATTR_DATA);
          if(data != null && data.isBlank()) {
            data = null;
          }
          fTestSuite = fTestRunSession.newTestSuite(getNextId(), suiteName, null, fTestSuite, displayName, data);
          fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
          fTestRunSession.notifyTestStarted(fTestSuite);
          break;
        }
        // not interested
        case IXMLTags.NODE_PROPERTIES:
        case IXMLTags.NODE_PROPERTY:
          break;
        case IXMLTags.NODE_TESTCASE: {
          String name = attributes.getValue(IXMLTags.ATTR_NAME);
          String classname = attributes.getValue(IXMLTags.ATTR_CLASSNAME);
          String testName = name + '(' + classname + ')';
          //TODO dynamic test not supported by API!
//        boolean isDynamicTest = Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_DYNAMIC_TEST)).booleanValue();
          String displayName = attributes.getValue(IXMLTags.ATTR_DISPLAY_NAME);
          String data = attributes.getValue(IXMLTags.ATTR_DATA);
          if(data != null && data.isBlank()) {
            data = null;
          }
          fTestCase = fTestRunSession.newTestCase(getNextId(), testName, fTestSuite, displayName, data);
          fNotRun.push(Boolean.valueOf(attributes.getValue(IXMLTags.ATTR_INCOMPLETE)));
          //TODO incomplete versus ignored?!?
//        fTestCase.setIgnored(Boolean.parseBoolean(attributes.getValue(IXMLTags.ATTR_IGNORED)));
          fTestRunSession.notifyTestStarted(fTestCase);
          break;
        }
        case IXMLTags.NODE_ERROR:
          // TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
          fStatus = Result.ERROR;
          fFailureBuffer = new StringBuilder();
          break;
        case IXMLTags.NODE_FAILURE:
          // TODO: multiple failures: https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
          fStatus = Result.FAILURE;
          fFailureBuffer = new StringBuilder();
          break;
        case IXMLTags.NODE_EXPECTED:
          fInExpected = true;
          fExpectedBuffer = new StringBuilder();
          break;
        case IXMLTags.NODE_ACTUAL:
          fInActual = true;
          fActualBuffer = new StringBuilder();
          break;
        // not interested
        case IXMLTags.NODE_SYSTEM_OUT:
        case IXMLTags.NODE_SYSTEM_ERR:
          break;
        case IXMLTags.NODE_SKIPPED:
          // before Ant 1.9.0: not an Ant JUnit tag, see
          // https://bugs.eclipse.org/bugs/show_bug.cgi?id=276068
          // later: child of <suite> or <test>, see
          // https://issues.apache.org/bugzilla/show_bug.cgi?id=43969
          fStatus = Result.OK;
          fFailureBuffer = new StringBuilder();
          String message = attributes.getValue(IXMLTags.ATTR_MESSAGE);
          if(message != null) {
            fFailureBuffer.append(message).append('\n');
          }
          break;
        default:
          throw new SAXParseException("unknown node '" + qName + "'", fLocator); //$NON-NLS-1$//$NON-NLS-2$
      }
    } catch(RuntimeException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if(fInExpected) {
      fExpectedBuffer.append(ch, start, length);

    } else if(fInActual) {
      fActualBuffer.append(ch, start, length);

    } else if(fFailureBuffer != null) {
      fFailureBuffer.append(ch, start, length);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      switch(qName) {
        // OK
        case IXMLTags.NODE_TESTRUN:
          break;
        // OK
        case IXMLTags.NODE_TESTSUITES:
          break;
        case IXMLTags.NODE_TESTSUITE:
          handleTestElementEnd(fTestSuite);
          fTestSuite = fTestSuite.getParent();
          // TODO: end suite: compare counters?
          break;
        // OK
        case IXMLTags.NODE_PROPERTIES:
        case IXMLTags.NODE_PROPERTY:
          break;
        case IXMLTags.NODE_TESTCASE:
          handleTestElementEnd(fTestCase);
          fTestCase = null;
          break;
        case IXMLTags.NODE_FAILURE:
        case IXMLTags.NODE_ERROR: {
          ITestElement testElement = fTestCase;
          if(testElement == null)
            testElement = fTestSuite;
          handleFailure(testElement, false);
          break;
        }
        case IXMLTags.NODE_EXPECTED:
          fInExpected = false;
          if(fFailureBuffer != null) {
            // skip whitespace from before <expected> and <actual> nodes
            fFailureBuffer.setLength(0);
          }
          break;
        case IXMLTags.NODE_ACTUAL:
          fInActual = false;
          if(fFailureBuffer != null) {
            // skip whitespace from before <expected> and <actual> nodes
            fFailureBuffer.setLength(0);
          }
          break;
        // OK
        case IXMLTags.NODE_SYSTEM_OUT:
        case IXMLTags.NODE_SYSTEM_ERR:
          break;
        case IXMLTags.NODE_SKIPPED: {
          ITestElement testElement = fTestCase;
          if(testElement == null) {
            testElement = fTestSuite;
          }
          if(fFailureBuffer != null && fFailureBuffer.length() > 0) {
            handleFailure(testElement, true);
          } else if(fTestCase != null) {
            fStatus = Result.IGNORED;
          } else { // not expected
            fTestRunSession.notifyTestFailed(testElement, Result.FAILURE, false, null);
          }
          break;
        }
        default:
          handleUnknownNode(qName);
          break;
      }
    } catch(RuntimeException e) {
      e.printStackTrace();
    }
  }

  private void handleTestElementEnd(ITestElement testElement) {
    boolean notrun = fNotRun.pop().booleanValue();
    if(notrun) {
      return;
    }
    fTestRunSession.notifyTestEnded(testElement, fStatus == Result.IGNORED);
  }

  private void handleFailure(ITestElement testElement, boolean assumptionFailed) {
    if(fFailureBuffer != null) {
      fTestRunSession.notifyTestFailed(testElement, fStatus, assumptionFailed,
          new FailureTrace(fFailureBuffer.toString(), toString(fExpectedBuffer), toString(fActualBuffer)));
      fFailureBuffer = null;
      fExpectedBuffer = null;
      fActualBuffer = null;
      fStatus = null;
    }
  }

  private String toString(StringBuilder buffer) {
    return buffer != null ? buffer.toString() : null;
  }

  private void handleUnknownNode(String qName) throws SAXException {
    // TODO: just log if debug option is enabled?
    String msg = "unknown node '" + qName + "'"; //$NON-NLS-1$//$NON-NLS-2$
    if(fLocator != null) {
      msg += " at line " + fLocator.getLineNumber() + ", column " + fLocator.getColumnNumber(); //$NON-NLS-1$//$NON-NLS-2$
    }
    throw new SAXException(msg);
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    throw e;
  }

  @Override
  public void warning(SAXParseException e) throws SAXException {
    throw e;
  }

  private String getNextId() {
    return fTestSuite.getId() + "." + fid.incrementAndGet();
  }

}
