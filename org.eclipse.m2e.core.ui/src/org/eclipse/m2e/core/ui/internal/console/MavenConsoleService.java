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

package org.eclipse.m2e.core.ui.internal.console;

import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


public class MavenConsoleService implements MavenConsole {
  public void logMessage(String msg) {
    M2EUIPluginActivator.getDefault().getMavenConsoleImpl().logMessage(msg);
  }

  public void logError(String msg) {
    M2EUIPluginActivator.getDefault().getMavenConsoleImpl().logError(msg);
  }

  public boolean wasInitialized() {
    return M2EUIPluginActivator.getDefault().hasMavenConsoleImpl();
  }
}
