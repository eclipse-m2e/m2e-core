/*******************************************************************************
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Takari, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences.launch;

import org.eclipse.core.resources.IProject;


public class ProjectEntryNode extends ClasspathEntryNode {

  private IProject project;

  public ProjectEntryNode(ClassRealmNode realm, IProject project) {
    super(realm);
    this.project = project;
  }

  @Override
  public String getName() {
    return project.getName();
  }
}
