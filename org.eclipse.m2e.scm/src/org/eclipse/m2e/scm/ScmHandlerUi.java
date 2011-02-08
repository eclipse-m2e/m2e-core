/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.scm;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Shell;

/**
 * An SCM handler UI base class
 *
 * @author Eugene Kuleshov
 */
public abstract class ScmHandlerUi implements IExecutableExtension {
  public static final String ATTR_TYPE = "type"; //$NON-NLS-1$
  public static final String ATTR_CLASS = "class"; //$NON-NLS-1$
  
  private String type;
  
  public String getType() {
    return type;
  }

  /**
   * Show revision/tag browser dialog and allow user to select revision/tag
   * 
   * @param shell the shell for revison/tag browser dialog
   * @param scmUrl the current <code>ScmUrl</code>, or null if none
   * @param scmRevision the current revision, or null if none
   * @return String selected revision
   */
  public String selectRevision(Shell shell, ScmUrl scmUrl, String scmRevision) {
    return null;
  }

  /**
   * Show repository browser dialog and allow user to select location
   * 
   * @param shell the shell for repository browser dialog
   * @param scmUrl the current <code>ScmUrl</code>, or null if none
   * @return ScmUrl for selected location or null if dialog was canceled
   */
  public ScmUrl selectUrl(Shell shell, ScmUrl scmUrl) {
    return null;
  }
  
  public boolean isValidUrl(String scmUrl) {
    return false;
  }
  
  public boolean isValidRevision(ScmUrl scmUrl, String scmRevision) {
    return false;
  }

  public boolean canSelectUrl() {
    return false;
  }

  public boolean canSelectRevision() {
    return false;
  }

  
  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.type = config.getAttribute(ATTR_TYPE);
  }
  
}

