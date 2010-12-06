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

package org.eclipse.m2e.core.core;

import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;

/**
 * Maven Console
 *
 * @author Eugene Kuleshov
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface MavenConsole extends IConsole {

  void logMessage(String msg);

  void logError(String msg);

  IConsoleListener newLifecycle();

  void shutdown();

  void showConsole();
  
  void closeConsole();

  void addMavenConsoleListener(IMavenConsoleListener listener);

  void removeMavenConsoleListener(IMavenConsoleListener listener);

}
