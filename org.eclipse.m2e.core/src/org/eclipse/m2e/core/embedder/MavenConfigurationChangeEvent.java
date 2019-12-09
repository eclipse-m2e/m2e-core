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

package org.eclipse.m2e.core.embedder;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;


/**
 * MavenConfigurationChangeEvent
 * 
 * @author igor
 */
public class MavenConfigurationChangeEvent implements MavenPreferenceConstants {

  public static final String P_USER_SETTINGS_FILE = MavenPreferenceConstants.P_USER_SETTINGS_FILE;

  private final String key;

  private final Object newValue;

  private final Object oldValue;

  public MavenConfigurationChangeEvent(String key, Object newValue, Object oldValue) {
    this.key = key;
    this.newValue = newValue;
    this.oldValue = oldValue;
  }

  public String getKey() {
    return key;
  }

  public Object getNewValue() {
    return newValue;
  }

  public Object getOldValue() {
    return oldValue;
  }
}
