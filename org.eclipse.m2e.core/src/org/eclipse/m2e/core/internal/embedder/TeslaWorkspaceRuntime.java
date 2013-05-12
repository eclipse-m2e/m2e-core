/*******************************************************************************
 * Copyright (c) 2008-2013 Sonatype, Inc., Jason van Zyl
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Jason van Zyl  - extension to account for Tesla
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;


/**
 * Tesla runtime loaded from the Eclipse Workspace
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 * @author Jason van Zyl
 */
public class TeslaWorkspaceRuntime extends AbstractWorkspaceRuntime {

  private static final ArtifactKey TESLA_DISTRIBUTION = new ArtifactKey(
      "io.tesla.maven", "apache-maven", "[3.1,)", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

  private static final String MAVEN_EXECUTOR_CLASS = "org.apache.maven.cli.MavenCli"; //$NON-NLS-1$

  public TeslaWorkspaceRuntime(IMavenProjectRegistry projectManager) {
    super(projectManager);
  }

  protected ArtifactKey getDistributionArtifactKey() {
    return TESLA_DISTRIBUTION;
  }

  protected String getMainClass() {
    return MAVEN_EXECUTOR_CLASS;
  }

  public String getLocation() {
    return "TESLA_WORKSPACE";
  }

  public String toString() {
    return "Tesla Workspace (" + getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
  }
}
