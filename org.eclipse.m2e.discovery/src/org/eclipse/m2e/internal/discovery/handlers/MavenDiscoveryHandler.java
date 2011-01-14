/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery.handlers;

import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.ui.handlers.HandlerUtil;


public class MavenDiscoveryHandler extends AbstractHandler {

  @SuppressWarnings("unchecked")
  public Object execute(ExecutionEvent event) throws ExecutionException {
    MavenDiscovery.launchWizard(HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell(), Collections.EMPTY_LIST,
        Collections.EMPTY_LIST);
    return null;
  }
}
