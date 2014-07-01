/*******************************************************************************
 * Copyright (c) 2012-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.jboss.tools.maven.apt.preferences;

import org.eclipse.core.resources.IProject;


/**
 * IPreferencesManager
 *
 * @author Fred Bricon
 */
public interface IPreferencesManager {

  /**
   * Name of the Maven property in pom.xml overriding workspace preference for m2e-apt activation strategy.
   * 
   * @since 1.1 
   */
  String M2E_APT_ACTIVATION_PROPERTY = "m2e.apt.activation"; //$NON-NLS-1$
  
  String M2E_APT_PROCESS_DURING_RECONCILE_PROPERTY = "m2e.apt.processDuringReconcile"; //$NON-NLS-1$

  void setAnnotationProcessorMode(IProject project, AnnotationProcessingMode mode);

  AnnotationProcessingMode getAnnotationProcessorMode(IProject project);

  boolean hasSpecificProjectSettings(IProject project);

  void clearSpecificSettings(IProject project);

  /**
   * returns the {@link AnnotationProcessingMode} matching the project pom.xml's &lt;m2e.apt.activation&gt; property, or
   * <code>null</code> if the property is not set or is invalid.
   * 
   * @since 1.1
   */
  AnnotationProcessingMode getPomAnnotationProcessorMode(IProject project);
  
  /**
   * 
   * @since 1.2
   */
  String getPomAnnotationProcessDuringReconcile(IProject project);
  
  /**
   * 
   * @since 1.2
   */
  void setAnnotationProcessDuringReconcile(IProject project, boolean enable);

  /**
   * 
   * @since 1.2
   */
  boolean shouldEnableAnnotationProcessDuringReconcile(IProject project);
  

 

  
}
