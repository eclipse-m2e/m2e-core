/*******************************************************************************
 * Copyright (c) 2012-2019 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal.compiler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;


/**
 * MavenCompilerExecutionDelegate
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class MavenCompilerExecutionDelegate extends MavenCompilerJdtAptDelegate {

  public MavenCompilerExecutionDelegate(IMavenMarkerManager markerManager) {
    super(markerManager);
  }

  private static final ArtifactVersion MINIMUM_COMPILER_PLUGIN_VERSION = new DefaultArtifactVersion("2.2");

  @Override
  public void configureProject(IProgressMonitor monitor) throws CoreException {
    //Disable JDT Apt
    //ProjectUtils.disableApt(mavenFacade.getProject());
    super.configureProject(monitor);//FIXME Fallback on JDt APT for now
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.apt.internal.AbstractAptConfiguratorDelegate#getMojoExecutionBuildParticipant(org.apache.maven.plugin.MojoExecution)
   */
  @Override
  public AbstractBuildParticipant getMojoExecutionBuildParticipant(MojoExecution execution) {
    //<proc></proc> is not available for maven-compiler-plugin < 2.2
    DefaultArtifactVersion version = new DefaultArtifactVersion(execution.getVersion());
    if(version.compareTo(MINIMUM_COMPILER_PLUGIN_VERSION) >= 0) {
      // Disabled for now return new MavenCompilerBuildParticipant(execution);
    }
    return null;
  }

}
