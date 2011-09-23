/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;


public class MavenProjectMutableState {

  private static final String CTX_SNAPSHOT = MavenProjectMutableState.class.getName() + "/SNAPSHOT";

  private List<String> compileSourceRoots;

  private List<String> testCompileSourceRoots;

  private List<Resource> resources;

  private List<Resource> testResources;

  private Properties properties;

  private MavenProjectMutableState() {
  }

  public static MavenProjectMutableState takeSnapshot(MavenProject project) {
    MavenProjectMutableState snapshot = new MavenProjectMutableState();

    if(project.getContextValue(CTX_SNAPSHOT) == null) {
      snapshot.compileSourceRoots = new ArrayList<String>(project.getCompileSourceRoots());
      snapshot.testCompileSourceRoots = new ArrayList<String>(project.getTestCompileSourceRoots());
      snapshot.resources = new ArrayList<Resource>(project.getResources());
      snapshot.testResources = new ArrayList<Resource>(project.getTestResources());

      snapshot.properties = new Properties();
      snapshot.properties.putAll(project.getProperties());

      project.setContextValue(CTX_SNAPSHOT, Boolean.TRUE);
    }

    return snapshot;
  }

  public void restore(MavenProject project) {
    setElements(project.getCompileSourceRoots(), compileSourceRoots);
    setElements(project.getTestCompileSourceRoots(), testCompileSourceRoots);
    setElements(project.getResources(), resources);
    setElements(project.getTestResources(), testResources);

    if(properties != null) {
      project.getProperties().clear();
      project.getProperties().putAll(properties);
    }

    project.setContextValue(CTX_SNAPSHOT, null);
  }

  private <T> void setElements(List<T> collection, List<T> elements) {
    if(elements != null) {
      collection.clear();
      collection.addAll(elements);
    }
  }

}
