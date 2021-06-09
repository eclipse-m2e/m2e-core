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

import org.eclipse.ui.console.IConsoleFactory;

import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;


/**
 * Maven Console factory is used to show the console from the "Open Console" drop-down action in Console view.
 *
 * @see org.eclipse.ui.console.consoleFactory extension point.
 * @author Eugene Kuleshov
 */
public class MavenConsoleFactory implements IConsoleFactory {

    @Override
    public void openConsole() {
    M2EUIPluginActivator.getDefault().getMavenConsole().showConsole();
  }

}
