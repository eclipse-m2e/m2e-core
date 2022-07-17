/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.conversion;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * AbstractProjectConversionEnabler.
 *
 * Convenience class that can be used by extenders of extension point org.eclipse.m2e.core.conversionEnabler
 *
 * @author Roberto Sanchez
 */
public abstract class AbstractProjectConversionEnabler implements IProjectConversionEnabler {

  private static final String JAR = "jar"; //$NON-NLS-1$

  private static final List<String> PACKAGING_OPTIONS = List.of(JAR);


  @Override
  public boolean accept(IProject project) {
    return true;
  }

  @Override
  public IStatus canBeConverted(IProject project) {
    return Status.OK_STATUS;
  }

  @Override
  public List<String> getPackagingTypes(IProject project) {
    return PACKAGING_OPTIONS;
  }

}
