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

package org.eclipse.m2e.editor.xml.preferences;

import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

import org.eclipse.m2e.editor.xml.MvnIndexPlugin;


/**
 * @author Eugene Kuleshov
 */
public class PomTemplatesPreferencePage extends TemplatePreferencePage {

  public PomTemplatesPreferencePage() {
    setPreferenceStore(MvnIndexPlugin.getDefault().getPreferenceStore());
    setTemplateStore(MvnIndexPlugin.getDefault().getTemplateStore());
    setContextTypeRegistry(MvnIndexPlugin.getDefault().getContextTypeRegistry());
  }

  @Override
  public boolean performOk() {
    boolean ok = super.performOk();
    MvnIndexPlugin.getDefault().savePluginPreferences();
    return ok;
  }

  @Override
  protected boolean isShowFormatterSetting() {
    return false;
  }

}
