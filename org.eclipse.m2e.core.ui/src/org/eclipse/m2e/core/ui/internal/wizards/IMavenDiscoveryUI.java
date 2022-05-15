/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;


public interface IMavenDiscoveryUI {

  /**
   * Returns true if postInstallHook has been scheduled for execution and false otherwise
   */
  boolean implement(List<IMavenDiscoveryProposal> proposals, IRunnableWithProgress postInstallHook,
      IRunnableContext context, Collection<String> projectsToConfigure);
}
