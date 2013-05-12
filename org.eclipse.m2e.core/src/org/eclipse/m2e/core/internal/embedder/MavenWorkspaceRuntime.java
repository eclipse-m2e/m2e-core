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

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Maven 3.0-SNAPSHOT runtime loaded from the Eclipse Workspace
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenWorkspaceRuntime extends AbstractWorkspaceRuntime {

  private static final ArtifactKey MAVEN_DISTRIBUTION = new ArtifactKey(
      "org.apache.maven", "apache-maven", "[3.0,)", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

  private static final String MAVEN_EXECUTOR_CLASS = "org.apache.maven.cli.MavenCli"; //$NON-NLS-1$

  public MavenWorkspaceRuntime(IMavenProjectRegistry projectManager) {
    super(projectManager);
  }

  protected ArtifactKey getDistributionArtifactKey() {
    return MAVEN_DISTRIBUTION;
  }

  protected String getMainClass() {
    return MAVEN_EXECUTOR_CLASS;
  }

  public String getLocation() {
    return MavenRuntimeManager.WORKSPACE;
  }

  public String toString() {
    return "Maven Workspace (" + getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
  }

}
