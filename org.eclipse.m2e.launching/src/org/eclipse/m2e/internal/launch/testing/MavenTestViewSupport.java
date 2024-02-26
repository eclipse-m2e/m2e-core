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

import java.util.Collection;
import java.util.List;

import org.eclipse.core.text.StringMatcher;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;


public class MavenTestViewSupport implements ITestViewSupport {

  public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
    return new MavenTestRunnerClient(session);
  }

  public Collection<StringMatcher> getTraceExclusionFilterPatterns() {
    return null;
  }

  public IAction getOpenTestAction(Shell shell, ITestCaseElement testCase) {
    return null;
  }

  public IAction getOpenTestAction(Shell shell, ITestSuiteElement testSuite) {
    return null;
  }

  public IAction createOpenEditorAction(Shell shell, ITestElement failure, String traceLine) {
    return null;
  }

  public Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest) {
    return null;
  }

  public ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements) {
    return null;
  }

  public String getDisplayName() {
    return "Maven";
  }

}
