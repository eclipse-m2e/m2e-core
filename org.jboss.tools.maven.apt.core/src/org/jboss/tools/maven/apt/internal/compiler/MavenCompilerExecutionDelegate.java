/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.maven.apt.internal.compiler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.markers.IMavenMarkerManager;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;

/**
 * MavenCompilerExecutionDelegate
 *
 * @author Fred Bricon
 */
public class MavenCompilerExecutionDelegate extends MavenCompilerJdtAptDelegate {

  public MavenCompilerExecutionDelegate(IMavenMarkerManager markerManager) {
    super(markerManager);
  }

  private static final VersionRange VALID_COMPILER_PLUGIN_RANGE;
  
  static {
    try {
      VALID_COMPILER_PLUGIN_RANGE = VersionRange.createFromVersionSpec("[2.2,)");
    } catch(InvalidVersionSpecificationException ex) {
      throw new RuntimeException("Unable to create maven-compiler-plugin version range from [2.2,)", ex);
    }
  }
  
  public void configureProject(IProgressMonitor monitor) throws CoreException {
    //Disable JDT Apt
    //ProjectUtils.disableApt(mavenFacade.getProject());  
    super.configureProject(monitor);//FIXME Fallback on JDt APT for now
  }
  
  /* (non-Javadoc)
   * @see org.jboss.tools.maven.apt.internal.AbstractAptConfiguratorDelegate#getMojoExecutionBuildParticipant(org.apache.maven.plugin.MojoExecution)
   */
  public AbstractBuildParticipant getMojoExecutionBuildParticipant(MojoExecution execution) {
    //<proc></proc> is not available for maven-compiler-plugin < 2.2
    if(VALID_COMPILER_PLUGIN_RANGE.containsVersion(new DefaultArtifactVersion(execution.getVersion()))) {
      // Disabled for now return new MavenCompilerBuildParticipant(execution);
    }
    return null;
  }
  
}
