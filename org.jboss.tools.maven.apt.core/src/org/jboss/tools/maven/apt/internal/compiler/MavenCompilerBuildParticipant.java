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
package org.jboss.tools.maven.apt.internal.compiler;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.plugin.MojoExecution;

import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

/**
 * Executes maven-compiler-plugin:compile with -proc:only
 *
 * @author Fred Bricon
 */
public class MavenCompilerBuildParticipant extends MojoExecutionBuildParticipant {

  private static final String PROC = "proc";

  public MavenCompilerBuildParticipant(MojoExecution execution) {
		super(execution, true);
	}

	@Override
	public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
		IMaven maven = MavenPlugin.getMaven();
		BuildContext buildContext = getBuildContext();

		MojoExecution mojoExecution = getMojoExecution();

		monitor.setTaskName("Executing " +mojoExecution.getArtifactId()+ ":" +mojoExecution.getGoal());

		//TODO check delta / scan source for *.java

    String compilerArgument  = maven.getMojoParameterValue(getSession(), mojoExecution, "compilerArgument", String.class);
    boolean isAnnotationProcessingEnabled = compilerArgument == null || !compilerArgument.contains("-proc:none");
    if (isAnnotationProcessingEnabled ) {
      String proc = maven.getMojoParameterValue(getSession(), mojoExecution, PROC, String.class);
      isAnnotationProcessingEnabled = !"none".equals(proc);
    }
    if (!isAnnotationProcessingEnabled) {
      return Collections.emptySet();
    }

    IMavenProjectFacade mavenProjectFacade = getMavenProjectFacade();

    if (!buildContext.hasDelta(mavenProjectFacade.getPomFile())) {

      IPath[] sources = "compile".equals(mojoExecution.getGoal())?mavenProjectFacade.getCompileSourceLocations()
                                                                 :mavenProjectFacade.getTestCompileSourceLocations();

      boolean hasSourceChanged = false;
      for (IPath relPathSource : sources) {
        IFolder sourceFolder = mavenProjectFacade.getProject().getFolder(relPathSource);
        File folder = new File(sourceFolder.getRawLocationURI());
        Scanner ds = buildContext.newScanner(folder); // delta or full scanner
        ds.scan();
        String[] includedFiles = ds.getIncludedFiles();
        if (includedFiles != null && includedFiles.length > 0) {
          hasSourceChanged = true;
          break;
        }
      }
      if (!hasSourceChanged) {
        return Collections.emptySet();
      }
    }



    Xpp3Dom originalConfiguration = mojoExecution.getConfiguration();
    Set<IProject> result = Collections.emptySet();
    try {
      Xpp3Dom newConfiguration = new Xpp3Dom(originalConfiguration);
		  setProcOnly(newConfiguration);
      setVerbose(newConfiguration);
		  mojoExecution.setConfiguration(newConfiguration);
		  // execute mojo
		  System.err.println("execute " + mojoExecution.getArtifactId()+":"+ mojoExecution.getGoal() +" proc:only for "+mavenProjectFacade.getProject().getName());
		  result = super.build(kind, monitor);
		} finally {
		  mojoExecution.setConfiguration(originalConfiguration);
		}

		// tell m2e builder to refresh generated files
		File generated = maven.getMojoParameterValue(getSession(), getMojoExecution(), MavenCompilerJdtAptDelegate.OUTPUT_DIRECTORY_PARAMETER, File.class);
		if (generated != null) {
			buildContext.refresh(generated);
		}

		return result;
	}

  /**
   * @param newConfiguration
   */
  private void setVerbose(Xpp3Dom configuration) {
    Xpp3Dom verboseDom = configuration.getChild("verbose");
    if(verboseDom == null) {
      verboseDom = new Xpp3Dom("verbose");
      configuration.addChild(verboseDom);
    }
    verboseDom.setValue("true");
  }

  private void setProcOnly(Xpp3Dom configuration) {
    Xpp3Dom procDom = configuration.getChild(PROC);
    if(procDom == null) {
      procDom = new Xpp3Dom(PROC);
      configuration.addChild(procDom);
    }
    procDom.setValue("only");
  }
}