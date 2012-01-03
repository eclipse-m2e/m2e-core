/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.project.conversion;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.model.Model;

/**
 * Used to convert existing Eclipse project configuration to the corresponding Maven Model. 
 *
 * @author Fred Bricon
 */
public abstract class AbstractProjectConversionParticipant implements IExecutableExtension {

  public static final String ATTR_ID = "id"; //$NON-NLS-1$

  public static final String ATTR_NAME = "name"; //$NON-NLS-1$

  private String name; 
  private String id; 
  
  public String getName() {
    return name;
  }

  public String getId() {
    if (id == null) {
      id = getClass().getName();
    }
    return id;
  }

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
  }

  /**
   * Checks if this participant can change the Maven Model from this Eclipse project configuration
   */
  public abstract boolean accept(IProject project) throws CoreException;

  /**
   * Converts existing Eclipse project configuration to Maven model
   */
  public abstract void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException;
 
  
  public String toString() {
   return (name == null)?getId():name; 
  }
}
