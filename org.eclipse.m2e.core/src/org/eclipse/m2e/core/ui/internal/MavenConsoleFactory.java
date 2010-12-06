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

package org.eclipse.m2e.core.ui.internal;

import org.eclipse.ui.console.IConsoleFactory;

import org.eclipse.m2e.core.MavenPlugin;

/**
 * Maven Console factory is used to show the console from the "Open Console"
 * drop-down action in Console view.
 * 
 * @see org.eclipse.ui.console.consoleFactory extension point.
 * 
 * @author Eugene Kuleshov
 */
public class MavenConsoleFactory implements IConsoleFactory {

  public void openConsole() {
    MavenPlugin.getDefault().getConsole().showConsole();
  }

}
