/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.mavenarchiver.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;

/**
 * MavenArchiver Configurator for maven-jar-plugin.<br/>
 * This configurator will generate files (MANIFEST.MF, pom/properties files) 
 * under the project's build output directory. 
 *
 * @author Fred Bricon
 */
public class JarArchiverConfigurator extends AbstractMavenArchiverConfigurator {

  @Override
  protected IPath getOutputDir(IMavenProjectFacade facade) {
    IPath outputLocation = facade.getOutputLocation();
    return outputLocation;
  }

  @Override
  protected String getArchiverFieldName() {
    return "jarArchiver";
  }

  @Override
  protected MojoExecutionKey getExecutionKey() {
    MojoExecutionKey key = new MojoExecutionKey("org.apache.maven.plugins", "maven-jar-plugin", "", "jar", null, null);
    return key;
  }

}
