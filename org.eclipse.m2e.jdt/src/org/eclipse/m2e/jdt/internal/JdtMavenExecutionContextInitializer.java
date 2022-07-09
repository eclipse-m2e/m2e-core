/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Component;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.embedder.IMavenExecutionContextInitializer;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * JdtMavenExecutionContextInitializer
 *
 * @author karypid
 */
@Component(service = {IMavenExecutionContextInitializer.class})
public class JdtMavenExecutionContextInitializer implements IMavenExecutionContextInitializer {

  private final AtomicBoolean jreDetectionActive = new AtomicBoolean(false);

  /* (non-Javadoc)
   * @see org.eclipse.m2e.core.embedder.IMavenExecutionContextInitializer#initializeExecutionRequest(org.eclipse.m2e.core.embedder.IMavenExecutionContext, org.apache.maven.execution.MavenExecutionRequest)
   */
  @Override
  public void initializeExecutionRequest(IMavenExecutionContext context, MavenExecutionRequest request) {
    if(jreDetectionActive.get())
      return; // hackk to prevent endless recursion; maybe a thread-local would be better?
    try {
      jreDetectionActive.set(true);
      try {
        // request.getSystemProperties();
        IMavenProjectFacade facade = null;
        try {
          facade = findMavenProjectFacade(request.getPom());
        } catch(IOException ex) {
        }
        if(facade == null)
          return;

        Properties systemProperties = request.getSystemProperties();
        List<MojoExecution> executions = AbstractJavaProjectConfigurator.getCompilerMojoExecutions(facade,
            new NullProgressMonitor());
        if(executions != null) {
          String compilerLevel = null;
          for(MojoExecution execution : executions) {
            // this creates an execution context to resolve the dependencies
            // but currently the initializer is cleared so will not be invoked circularly
            compilerLevel = AbstractJavaProjectConfigurator.getCompilerLevel(MavenPlugin.getMaven(),
                facade.getMavenProject(), execution, "source", compilerLevel);
          }
          IVMInstall vm = findVM(compilerLevel);
          if(vm != null) {
            File location = vm.getInstallLocation();
            systemProperties.put("java.home", location.getPath() + File.separator + "bin");
          }
        }
      } catch(CoreException ex) {
      }
    } finally {
      jreDetectionActive.set(false);
    }
  }

  private static IVMInstall findVM(String compilerLevel) {
    String envId = AbstractJavaProjectConfigurator.getExecutionEnvironmentId(compilerLevel);
    IExecutionEnvironment env = AbstractJavaProjectConfigurator.getExecutionEnvironment(envId);
    if(env != null) {
      IPath jreContainerPath = JavaRuntime.newJREContainerPath(env);
      IVMInstall vm = JavaRuntime.getVMInstall(jreContainerPath);
      if(vm != null) {
        return vm;
      }
    }
    return null;
  }

  private static IMavenProjectFacade findMavenProjectFacade(File pom) throws IOException {
    if(pom == null)
      return null;
    pom = pom.getCanonicalFile();
    try {
      List<IMavenProjectFacade> projects = MavenPlugin.getMavenProjectRegistry().getProjects();
      for(IMavenProjectFacade project : projects) {
        if(project.getPomFile().getCanonicalFile().equals(pom))
          return project;
      }
    } catch(IOException ex) {
    }
    return null;
  }
}
