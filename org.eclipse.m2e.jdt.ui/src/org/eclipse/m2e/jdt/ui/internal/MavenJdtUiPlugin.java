/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;


public class MavenJdtUiPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.eclipse.m2e.jdt.ui"; //$NON-NLS-1$

  private static MavenJdtUiPlugin instance;

  public MavenJdtUiPlugin() {
    MavenJdtUiPlugin.instance = this;
  }

  public static MavenJdtUiPlugin getDefault() {
    return instance;
  }
}
