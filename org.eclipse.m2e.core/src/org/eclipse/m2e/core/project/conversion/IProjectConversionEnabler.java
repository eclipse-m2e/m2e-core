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

/**
 * IProjectConversionEnabler
 *
 * @author Roberto Sanchez
 */
public interface IProjectConversionEnabler {
  
  /**
   * Test if this enabler is interested in analyzing this project
   * 
   * @return true if the analyzer wants to work on this project.
   */
  public boolean accept(IProject project);
  
  /**
   * Test if project should be converted to Maven. Enablers might have reasons for 
   * not allowing certain types of project to be converted to Maven
   * 
   * @return IStatus indicating if project can be converted or not. if the project should 
   * not be converted, the severity must be set to IStatus.ERROR. If the project should be converted, 
   * the severity must be set to IStatus.OK. This method should not return null.  
   */
  public IStatus canBeConverted(IProject project);
  
 
  /**
   * If project can be converted to Maven, the enabler should provide the suggested packaging types
   * for the project.
   * 
   * @return array of suggested packaging types. This should not return null.
   */
  public String[] getPackagingTypes(IProject project);
  
  

}
