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

package org.eclipse.m2e.core.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


/**
 * MavenShowConsoleAction
 *
 * @author dyocum
 */
public abstract class MavenShowConsoleAction extends Action implements IPropertyChangeListener {

  public MavenShowConsoleAction(String name) {
    super(name, IAction.AS_CHECK_BOX);
    setToolTipText(name);
    getPreferenceStore().addPropertyChangeListener(this);
    update();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    if(property.equals(getKey())) {
      update();
    }
  }

  protected abstract String getKey();

  private void update() {
    IPreferenceStore store = getPreferenceStore();
    if(store.getBoolean(getKey())) {
      // on
      setChecked(true);
    } else {
      // off
      setChecked(false);
    }
  }

  /**
   * @return
   */
  private IPreferenceStore getPreferenceStore() {
    return M2EUIPluginActivator.getDefault().getPreferenceStore();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    IPreferenceStore store = getPreferenceStore();
    boolean show = isChecked();
    store.removePropertyChangeListener(this);
    store.setValue(getKey(), show);
    store.addPropertyChangeListener(this);
  }

  /**
   * Must be called to dispose this action.
   */
  public void dispose() {
    getPreferenceStore().removePropertyChangeListener(this);
  }
}
