/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project.conversion;

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
  private static final String[] PACKAGING_OPTIONS = {JAR};
  
  
  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler#accept(org.eclipse.core.resources.IProject)
   */
  public boolean accept(IProject project) {
    return true;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler#shouldProjectBeConverted(org.eclipse.core.resources.IProject)
   */
  public IStatus canBeConverted(IProject project) {
    return Status.OK_STATUS;
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.project.conversion.IProjectConversionEnabler#getPackagingTypes(org.eclipse.core.resources.IProject)
   */
  public String[] getPackagingTypes(IProject project) {
    return PACKAGING_OPTIONS;
  }

}
