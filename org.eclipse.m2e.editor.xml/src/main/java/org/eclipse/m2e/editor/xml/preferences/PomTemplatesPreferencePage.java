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
