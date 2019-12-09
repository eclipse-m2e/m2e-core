/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
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

package org.eclipse.m2e.internal.discovery.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.m2e.internal.discovery.MavenDiscovery;


public class MavenDiscoveryHandler extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    MavenDiscovery.launchWizard(HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell());
    return null;
  }
}
