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

package org.eclipse.m2e.core.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolver configuration holder.
 * 
 * TODO need a better name, this configures all aspects of maven project in eclipse, 
 *      not just dependency resolution.
 *
 * @author Eugene Kuleshov
 */
public class ResolverConfiguration implements Serializable {
  private static final long serialVersionUID = 1258510761534886581L;

  private boolean resolveWorkspaceProjects = true;

  private String activeProfiles = ""; //$NON-NLS-1$

  public boolean shouldResolveWorkspaceProjects() {
    return this.resolveWorkspaceProjects;
  }

  public String getActiveProfiles() {
    return this.activeProfiles;
  }
  
  public List<String> getActiveProfileList() {
    if (activeProfiles.trim().length() > 0) {
      return Arrays.asList(activeProfiles.split("[,\\s\\|]")); //$NON-NLS-1$
    }
    return new ArrayList<String>();
  }

  public void setResolveWorkspaceProjects(boolean resolveWorkspaceProjects) {
    this.resolveWorkspaceProjects = resolveWorkspaceProjects;
  }
  
  public void setActiveProfiles(String activeProfiles) {
    this.activeProfiles = activeProfiles;
  }
}
