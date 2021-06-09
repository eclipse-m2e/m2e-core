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

package org.eclipse.m2e.core.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class MavenDebugOutputAction extends Action {

  private final IPropertyChangeListener listener = event -> {
    if(MavenPreferenceConstants.P_DEBUG_OUTPUT.equals(event.getProperty())) {
      setChecked(isDebug());
    }
  };

  public MavenDebugOutputAction() {
    setToolTipText(Messages.MavenDebugOutputAction_0);
    setImageDescriptor(MavenImages.DEBUG);

    getPreferenceStore().addPropertyChangeListener(listener);
    setChecked(isDebug());
  }

  @Override
  public void run() {
    getPreferenceStore().setValue(MavenPreferenceConstants.P_DEBUG_OUTPUT, isChecked());
  }

  public void dispose() {
    getPreferenceStore().removePropertyChangeListener(listener);
  }

  IPreferenceStore getPreferenceStore() {
    return M2EUIPluginActivator.getDefault().getPreferenceStore();
  }

  boolean isDebug() {
    return getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }

}
