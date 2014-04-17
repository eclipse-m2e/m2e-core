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

import java.util.List;


public class ClassRealmNode {
  private String name;

  private List<ClasspathEntryNode> classpath;

  public ClassRealmNode(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public List<ClasspathEntryNode> getClasspath() {
    return classpath;
  }

  public void setClasspath(List<ClasspathEntryNode> classpath) {
    this.classpath = classpath;
  }

  public int getIndex(ClasspathEntryNode entry) {
    for(int i = 0; i < classpath.size(); i++ ) {
      if(classpath.get(i) == entry) {
        return i;
      }
    }
    return 0;
  }
}
