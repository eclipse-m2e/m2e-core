/*******************************************************************************
 * Copyright (c) 2011 eBusiness Information, Excilys Group and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      eBusiness Information, Excilys Group - initial API and implementation
 *      Red Hat, Inc.
 *******************************************************************************/
package org.jboss.tools.maven.apt.internal.processor;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.Scanner;

import org.apache.maven.plugin.MojoExecution;

import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

/**
 * Executes maven-processor-plugin:process or process-test during incremental builds.
 *
 * @author Stéphane Landelle
 * @author Fred Bricon
 */
public class MavenProcessorBuildParticipant extends MojoExecutionBuildParticipant {

	public MavenProcessorBuildParticipant(MojoExecution execution) {
		super(execution, true);
	}

	@Override
	public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
		IMaven maven = MavenPlugin.getMaven();
		BuildContext buildContext = getBuildContext();

		monitor.setTaskName("Executing " + getMojoExecution().getArtifactId()+ ":" +getMojoExecution().getGoal());

		//Modifying the pom triggers a build, otherwise, check for java source modifications
		if (!buildContext.hasDelta(getMavenProjectFacade().getPomFile())) {

		  // check if any of the java files changed
		  File source = maven.getMojoParameterValue(getSession(), getMojoExecution(), MavenProcessorExecutionDelegate.SOURCE_DIRECTORY_PARAMETER, File.class);
		  Scanner ds = buildContext.newScanner(source); // delta or full scanner
		  ds.scan();
		  String[] includedFiles = ds.getIncludedFiles();
		  if (includedFiles == null || includedFiles.length <= 0) {
		    return null;
		  }

		  if (getBuildContext().isIncremental()) {
		    boolean interestingFileChanged = false;
		    for (String f : includedFiles) {
		      if (f.endsWith(".java")) {
		        interestingFileChanged = true;
		        break;
		      }
		    }

		    if (!interestingFileChanged) {
		      return Collections.emptySet();
		    }
		  }
		}
		// execute mojo
		Set<IProject> result = super.build(kind, monitor);

		// tell m2e builder to refresh generated files
		File generated = maven.getMojoParameterValue(getSession(), getMojoExecution(), MavenProcessorExecutionDelegate.OUTPUT_DIRECTORY_PARAMETER, File.class);
		if (generated == null) {
			generated = maven.getMojoParameterValue(getSession(), getMojoExecution(), MavenProcessorExecutionDelegate.DEFAULT_OUTPUT_DIRECTORY_PARAMETER, File.class);
		}
		if (generated != null) {
			buildContext.refresh(generated);
		}

		return result;
	}
}