/*******************************************************************************
 * Copyright (c) 2011-2014 Knowledge Computing Corp. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Karl M. Davis (Knowledge Computing Corp.) - initial API and implementation
 *    Red Hat, Inc - refactoring and abstraction of the logic
 *******************************************************************************/

package org.jboss.tools.maven.apt.internal;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.IPreferencesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.eclipse.m2e.jdt.IJavaProjectConfigurator;


/**
 * <p>
 * This {@link AbstractProjectConfigurator} implementation will set the APT configuration for an Eclipse Java project.
 * </p>
 * <p>
 * Please note that the <code>maven-compiler-plugin</code> (at least as of version 2.3.2) will automatically perform
 * annotation processing and generate annotation sources. This processing will include all annotation processors in the
 * project's compilation classpath.
 * </p>
 * <p>
 * However, there are a couple of problems that prevent the <code>maven-compiler-plugin</code>'s annotation processing
 * from being sufficient when run within m2eclipse:
 * </p>
 * <ul>
 * <li>The generated annotation sources are not added to the Maven project's source folders (nor should they be) and are
 * thus not found by m2eclipse.</li>
 * <li>Due to contention between Eclipse's JDT compilation and <code>maven-compiler-plugin</code> compilation, the Java
 * compiler used by Eclipse may not recognize when the generated annotation sources/classes are out of date.</li>
 * </ul>
 * <p>
 * The {@link AbstractAptProjectConfigurator} works around those limitations by configuring Eclipse's built-in annotation
 * processing: APT. Unfortunately, the APT configuration will not allow for libraries, such as m2eclipse's
 * "Maven Dependencies" to be used in the search path for annotation processors. Instead, the
 * {@link AbstractAptProjectConfigurator} adds all of the project's <code>.jar</code> dependencies to the annotation processor
 * search path.
 * </p>
 */
public abstract class AbstractAptProjectConfigurator extends AbstractProjectConfigurator implements IJavaProjectConfigurator {
  
  private static final Logger log = LoggerFactory.getLogger(AbstractAptProjectConfigurator.class);

  protected abstract AptConfiguratorDelegate getDelegate(AnnotationProcessingMode mode);
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // This method may be called with null parameters to ensure its API is correct. We
    // can ignore such calls.
    
    if(request == null || monitor == null)
      return;
    
    // Get the objects needed for APT configuration
    IMavenProjectFacade mavenProjectFacade = request.getMavenProjectFacade();

    AnnotationProcessingMode mode = getAnnotationProcessorMode(mavenProjectFacade);
    
    MavenSession mavenSession = request.getMavenSession();

    AptConfiguratorDelegate configuratorDelegate = getDelegate(mode);
    configuratorDelegate.setSession(mavenSession); 
    configuratorDelegate.setFacade(mavenProjectFacade);
    
    // Configure APT
    if (!configuratorDelegate.isIgnored(monitor)) {
      configuratorDelegate.configureProject(monitor);
    }
    
    configureAptReconcile(mavenProjectFacade.getProject());
    
  }

  /**
   * reconcile is enabled by default while enabling apt for maven-compiler-plugin,
   * As Annotation processing usually takes a long time for even a java file change,
   * and what's more, validate a jsp also triggers apt reconcile as jsp compiles into java,
   * this option is provided to switch off the "Processing on Edit" feature.
   * 
   * @throws CoreException 
   */
  private void configureAptReconcile(IProject project) throws CoreException {
    if(project.hasNature(JavaCore.NATURE_ID)) {
      IJavaProject jp = JavaCore.create(project);
      if(jp != null && AptConfig.isEnabled(jp)) {
        boolean shouldEnable = MavenJdtAptPlugin.getDefault().getPreferencesManager()
            .shouldEnableAnnotationProcessDuringReconcile(project);
        if(shouldEnable && !AptConfig.shouldProcessDuringReconcile(jp)) {
          AptConfig.setProcessDuringReconcile(jp, true);
        }
        if(!shouldEnable && AptConfig.shouldProcessDuringReconcile(jp)) {
          AptConfig.setProcessDuringReconcile(jp, false);
        }
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public void configureClasspath(IMavenProjectFacade facade, IClasspathDescriptor classpath, IProgressMonitor monitor) {
    /*
     * Implementations of this method are supposed to configure the Maven project
     * classpath: the "Maven Dependencies" container. We don't have any need to do
     * that here.
     */
  }

  /**
   * {@inheritDoc}
   */
  public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    /*
     * We need to prevent/recover from the JavaProjectConfigurator removing the
     * generated annotation sources directory from the classpath: it will be added
     * when we configure the Eclipse APT preferences and then removed when the
     * JavaProjectConfigurator runs.
     */
    // Get the various project references we'll need
    IProject eclipseProject = request.getProject();
    if(!eclipseProject.hasNature(JavaCore.NATURE_ID))
      return;

    AptConfiguratorDelegate delegate = getDelegate(request.getMavenProjectFacade());
    delegate.setFacade(request.getMavenProjectFacade());
    delegate.setSession(request.getMavenSession());
    // If this isn't a Java project, we have nothing to do

    if (!delegate.isIgnored(monitor)) {
      delegate.configureClasspath(classpath, monitor);
    }

  }
  
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    
    AptConfiguratorDelegate configuratorDelegate;
    try {
      configuratorDelegate = getDelegate(projectFacade);
      return configuratorDelegate.getMojoExecutionBuildParticipant(execution);
    } catch(CoreException ex) {
      log.error("Unable to get the build participant for annotation processing", ex);
    }

    return null;
  }
  
  private AptConfiguratorDelegate getDelegate(IMavenProjectFacade facade) throws CoreException {
    AnnotationProcessingMode mode = getAnnotationProcessorMode(facade);
    return getDelegate(mode);
  }
  
  protected AnnotationProcessingMode getAnnotationProcessorMode(IMavenProjectFacade facade) throws CoreException {
    IPreferencesManager preferencesManager = MavenJdtAptPlugin.getDefault().getPreferencesManager();
    AnnotationProcessingMode mode = preferencesManager.getAnnotationProcessorMode(facade.getProject());
    return mode;
  }
  

}
