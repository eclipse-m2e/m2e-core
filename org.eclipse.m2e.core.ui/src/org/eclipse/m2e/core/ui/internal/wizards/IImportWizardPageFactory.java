/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.List;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;


@SuppressWarnings("restriction")
public interface IImportWizardPageFactory {

  /**
   * Returns true if postInstallHook has been scheduled for execution and false otherwise
   */
  public boolean implement(List<IMavenDiscoveryProposal> proposals, IRunnableWithProgress postInstallHook,
      IRunnableContext context);

}
