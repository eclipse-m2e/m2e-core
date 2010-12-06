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

package org.eclipse.m2e.editor.pom;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 * An artifact filter supporting all dependency scopes
 * 
 * @author Eugene Kuleshov
 */
public class ScopeArtifactFilter implements ArtifactFilter {
  private final boolean compileScope;
  private final boolean runtimeScope;
  private final boolean testScope;
  private final boolean providedScope;
  private final boolean systemScope;

  public ScopeArtifactFilter(String scope) {
    if(Artifact.SCOPE_COMPILE.equals(scope)) {
      systemScope = true;
      providedScope = true;
      compileScope = true;
      runtimeScope = false;
      testScope = false;
    } else if(Artifact.SCOPE_RUNTIME.equals(scope)) {
      systemScope = false;
      providedScope = false;
      compileScope = true;
      runtimeScope = true;
      testScope = false;
    } else if(Artifact.SCOPE_TEST.equals(scope)) {
      systemScope = true;
      providedScope = true;
      compileScope = true;
      runtimeScope = true;
      testScope = true;
    } else if(Artifact.SCOPE_PROVIDED.equals(scope)) {
      systemScope = false;
      providedScope = true;
      compileScope = false;
      runtimeScope = false;
      testScope = false;
    } else if(Artifact.SCOPE_SYSTEM.equals(scope)) {
      systemScope = true;
      providedScope = false;
      compileScope = false;
      runtimeScope = false;
      testScope = false;
    } else {
      systemScope = false;
      providedScope = false;
      compileScope = false;
      runtimeScope = false;
      testScope = false;
    }
  }

  public boolean include(Artifact artifact) {
    if(Artifact.SCOPE_COMPILE.equals(artifact.getScope())) {
      return compileScope;
    } else if(Artifact.SCOPE_RUNTIME.equals(artifact.getScope())) {
      return runtimeScope;
    } else if(Artifact.SCOPE_TEST.equals(artifact.getScope())) {
      return testScope;
    } else if(Artifact.SCOPE_PROVIDED.equals(artifact.getScope())) {
      return providedScope;
    } else if(Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
      return systemScope;
    }
    return true;
  }
  
}

