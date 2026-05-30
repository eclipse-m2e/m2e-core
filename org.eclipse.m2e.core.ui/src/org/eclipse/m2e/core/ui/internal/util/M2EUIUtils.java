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

package org.eclipse.m2e.core.ui.internal.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.aether.version.Version;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.internal.M2EUtils;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.IgnoreVersionMatcher;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleService;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleSetParser;


/**
 * M2EUtils
 *
 * @author dyocum
 */
public class M2EUIUtils {
  private static final ILog LOG = Platform.getLog(M2EUIUtils.class);

  public static Font deriveFont(Font f, int style, int height) {
    FontData[] fd = f.getFontData();
    FontData[] newFD = new FontData[fd.length];
    for(int i = 0; i < fd.length; i++ ) {
      newFD[i] = new FontData(fd[i].getName(), height, style);
    }
    return new Font(Display.getCurrent(), newFD);
  }

  public static void showErrorDialog(Shell shell, String title, String msg, Exception e) {
	StringBuilder buff = new StringBuilder(msg);
    Throwable t = M2EUtils.getRootCause(e);
    if(t != null && !nullOrEmpty(t.getMessage())) {
      buff.append(t.getMessage());
    }
    MessageDialog.openError(shell, title, buff.toString());
  }

  public static boolean nullOrEmpty(String s) {
    return s == null || s.length() == 0;
  }

  /**
   * @param shell
   * @param string
   * @param string2
   * @param updateErrors
   */
  public static void showErrorsForProjectsDialog(final Shell shell, final String title, final String message,
      final Map<String, Throwable> errorMap) {
    // TODO Auto-generated method showErrorsForProjectsDialog
    Display.getDefault().asyncExec(() -> {
      String[] buttons = {IDialogConstants.OK_LABEL};
      int ok_button = 0;
      M2EErrorDialog errDialog = new M2EErrorDialog(shell, title, Dialog.getImage(Dialog.DLG_IMG_MESSAGE_ERROR),
          message, MessageDialog.ERROR, buttons, ok_button, errorMap);
      errDialog.create();
      errDialog.open();
    });

  }

  public static void addRequiredDecoration(Control control) {
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
        FieldDecorationRegistry.DEC_REQUIRED);
    ControlDecoration controlDecoration = new ControlDecoration(control, SWT.LEFT | SWT.CENTER);
    controlDecoration.setDescriptionText(fieldDecoration.getDescription());
    controlDecoration.setImage(fieldDecoration.getImage());
  }

  /**
   * Returns the version rule-set used for updating Maven artifacts to their latest version.
   */
  public static RuleSet getCurrentRuleSet() throws CoreException {
    IPreferenceStore preferenceStore = M2EUIPluginActivator.getDefault().getPreferenceStore();
    String ruleSetString = preferenceStore.getString(MavenPreferenceConstants.P_MAVEN_VERSION_RULESET);
    if(ruleSetString.isEmpty()) {
      return new RuleSet();
    }
    return RuleSetParser.fromXMLString(ruleSetString);
  }

  /**
   * Returns a checker used to verify whether the a given version should be ignored based on the current rule-set.
   */
  public static Predicate<Version> getIgnoreVersionMatcher(String groupId, String artifactId) {
    RuleSet ruleSet = null;

    try {
      ruleSet = getCurrentRuleSet();
    } catch(CoreException e) {
      LOG.log(Status.error(e.getMessage(), e));
      ruleSet = new RuleSet();
    }

    RuleService ruleService = new RuleService(ruleSet);
    List<IgnoreVersion> ignoreVersions = ruleService.getIgnoredVersions(groupId, artifactId);

    return new IgnoreVersionMatcher(ignoreVersions);
  }
}
