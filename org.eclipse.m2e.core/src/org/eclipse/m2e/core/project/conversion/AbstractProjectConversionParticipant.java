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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
 * @since 1.1
 */
public abstract class AbstractProjectConversionParticipant implements IExecutableExtension {

  public static final String ATTR_ID = "id"; //$NON-NLS-1$

  public static final String ATTR_NAME = "name"; //$NON-NLS-1$

  protected Set<String> restrictedPackagings;

  private String name;

  private String id;

  public String getName() {
    return name;
  }

  public String getId() {
    if(id == null) {
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
    return (name == null) ? getId() : name;
  }

  /**
   * Returns all the Maven packagings this conversion participant is restricted to.
   * 
   * @return an unmodifiable {@link Set} copy of Maven packagings, can be <code>null</code>.
   * @since 1.3
   */
  public Set<String> getRestrictedPackagings() {
    return restrictedPackagings == null ? null : Collections.unmodifiableSet(restrictedPackagings);
  }

  /**
   * Checks if this conversion participant allows the given Maven packaging to be converted :<br/>
   * If there are no packaging restrictions or the packaging restrictions contain this packaging, then it's considered
   * compatible.
   * 
   * @param packaging the Maven packaging to check
   * @return <code>true</code> if the packaging is compatible with this conversion participant.
   */
  public boolean isPackagingCompatible(String packaging) {
    boolean isCompatible = restrictedPackagings == null || restrictedPackagings.isEmpty() //no restrictions 
        || restrictedPackagings.contains(packaging);
    return isCompatible;
  }

  /**
   * Adds a Maven packaging to the set of restricted, compatible packagings for this converter.
   * 
   * @param packaging the compatible Maven packaging to add
   */
  public void addRestrictedPackaging(String packaging) {
    if(packaging != null) {
      if(restrictedPackagings == null) {
        restrictedPackagings = new HashSet<String>();
      }
      restrictedPackagings.add(packaging);
    }
  }

}
