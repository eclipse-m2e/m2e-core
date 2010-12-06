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

package org.eclipse.m2e.core.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


/**
 * @author Eugene Kuleshov
 */
public class MavenDebugOutputAction extends Action {

  private IPropertyChangeListener listener = new IPropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent event) {
      if(MavenPreferenceConstants.P_DEBUG_OUTPUT.equals(event.getProperty())) {
        setChecked(isDebug());
      }
    }
  };

  public MavenDebugOutputAction() {
    setToolTipText(Messages.MavenDebugOutputAction_0);
    setImageDescriptor(MavenImages.DEBUG);
    
    getPreferenceStore().addPropertyChangeListener(listener);
    setChecked(isDebug());
  }

  public void run() {
    getPreferenceStore().setValue(MavenPreferenceConstants.P_DEBUG_OUTPUT, isChecked());
  }
  
  public void dispose() {
    getPreferenceStore().removePropertyChangeListener(listener);
  }

  IPreferenceStore getPreferenceStore() {
    return MavenPlugin.getDefault().getPreferenceStore();
  }

  boolean isDebug() {
    return getPreferenceStore().getBoolean(MavenPreferenceConstants.P_DEBUG_OUTPUT);
  }
  
}

