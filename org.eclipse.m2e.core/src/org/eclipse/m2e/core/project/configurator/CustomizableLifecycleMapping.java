/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.configurator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * @author igor
 */
public class CustomizableLifecycleMapping extends AbstractLifecycleMapping {
  public static final String EXTENSION_ID = "customizable"; //$NON-NLS-1$

  private List<AbstractProjectConfigurator> configurators = new ArrayList<AbstractProjectConfigurator>();

  @SuppressWarnings("unused")
  public List<AbstractProjectConfigurator> getProjectConfigurators(IMavenProjectFacade facade, IProgressMonitor monitor)
      throws CoreException {
    return configurators;
  }

  public void addConfigurator(AbstractProjectConfigurator configurator) {
    this.configurators.add(configurator);
  }
}
