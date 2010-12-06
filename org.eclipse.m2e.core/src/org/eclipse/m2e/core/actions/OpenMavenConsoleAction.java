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

import org.eclipse.m2e.core.MavenPlugin;

/**
 * Open Maven Console Action
 *
 * @author Eugene Kuleshov
 */
public class OpenMavenConsoleAction extends Action {
  
  public void run() {
    MavenPlugin.getDefault().getConsole().showConsole();
  }

}
