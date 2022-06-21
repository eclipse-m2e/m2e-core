/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.apache.maven.DefaultMaven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import org.eclipse.m2e.core.embedder.IComponentLookup;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


/**
 * {@link IMavenToolbox} provides methods to perform common tasks from the maven world.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IMavenToolbox {

  Supplier<CoreException> ERROR_NO_SESSION = () -> new CoreException(
      Status.error(Messages.MavenToolbox_sessionrequired));

  Supplier<CoreException> ERROR_NO_LOOKUP = () -> new CoreException(Status.error(Messages.MavenToolbox_lookuprequired));

  /**
   * @return the lookup or an empty optional if this toolbox was not constructed in a way that a lookup could be
   *         acquired
   */
  Optional<IComponentLookup> getComponentLookup();

  /**
   * @return the session or <code>null</code> or an empty optional if this toolbox was not constructed in a way that a
   *         lookup could be acquired
   */
  Optional<MavenSession> getSession();

  /**
   * Loads the maven standalone project, the default implementation of this requires a session and a lookup.
   * 
   * @return the maven standalone project
   * @throws CoreException
   */
  default MavenProject getStandaloneProject() throws CoreException {
    MavenSession session = getSession().orElseThrow(ERROR_NO_SESSION);
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    MavenExecutionRequest request = session.getRequest();
    ProjectBuilder projectBuilder = componentLookup.lookup(ProjectBuilder.class);

    request.getProjectBuildingRequest().setRepositorySession(session.getRepositorySession());
    try {
      @SuppressWarnings("deprecation")
      MavenProject project = projectBuilder
          .build(new org.apache.maven.model.building.UrlModelSource(
              DefaultMaven.class.getResource("project/standalone.xml")), request.getProjectBuildingRequest())
          .getProject();
      project.setExecutionRoot(true);
      return project;
    } catch(ProjectBuildingException ex) {
      throw new CoreException(Status.error("Can't build standalone project!", ex));

    }
  }

  /**
   * Locate the pom file for the given basedirectory in a way that takes polyglot projects into account, the default
   * implementation of this needs a lookup if pomless/polyglot should be found.
   * 
   * @param baseDir the basedir to investigate
   * @return an empty optional if no pom could be located, or otherwise an optional containing the file of the pom
   * @throws CoreException
   */
  default Optional<File> locatePom(File baseDir) {
    if(baseDir == null || !baseDir.isDirectory()) {
      return Optional.empty();
    }
    File file = new File(baseDir, IMavenConstants.POM_FILE_NAME);
    if(file.isFile()) {
      //check the obvious case first...
      return Optional.of(file);
    }
    return getComponentLookup().map(componentLookup -> {
      try {
        ModelProcessor modelProcessor = componentLookup.lookup(ModelProcessor.class);
        File pom = modelProcessor.locatePom(baseDir);
        if(pom != null && pom.isFile()) {
          return pom;
        }
      } catch(Exception ex) {
        //can't locate the pom... but don't bail to the caller...
        MavenPluginActivator.getDefault().getLog().warn("Error while locating pom for basedir " + baseDir, ex);
      }
      return null;
    });
  }

  /**
   * Read a maven model from the given input stream, the default implementation of this requires a lookup.
   * 
   * @param in the stream to read the model from
   * @return
   * @throws CoreException if reading failed
   */
  default Model readModel(InputStream in) throws CoreException {
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    try {
      return componentLookup.lookup(ModelReader.class).read(in, null);
    } catch(IOException e) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_read_pom, e));
    }
  }

  /**
   * Writes the given model to the output stream, the default implementation of this requires a lookup.
   * 
   * @param model
   * @param out
   * @throws CoreException if reading failed
   */
  default void writeModel(Model model, OutputStream out) throws CoreException {
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    try {
      componentLookup.lookup(ModelWriter.class).write(out, null, model);
    } catch(IOException ex) {
      throw new CoreException(Status.error(Messages.MavenImpl_error_write_pom, ex));
    }
  }

  /**
   * Creates a toolbox from the given execution context using its component lookup and session
   * 
   * @param executionContext
   * @return
   */
  static IMavenToolbox of(IMavenExecutionContext executionContext) {
    return new IMavenToolbox() {

      @Override
      public Optional<IComponentLookup> getComponentLookup() {
        return Optional.of(executionContext.getComponentLookup());
      }

      @Override
      public Optional<MavenSession> getSession() {
        return Optional.of(executionContext.getSession());
      }

    };
  }

  /**
   * Creates a toolbox of the given component lookup
   * 
   * @param componentLookup
   * @return
   */
  static IMavenToolbox of(IComponentLookup componentLookup) {
    return of(componentLookup, null);
  }

  /**
   * Creates a toolbox from the given lookup and session
   * 
   * @param componentLookup
   * @param mavenSession
   * @return
   */
  static IMavenToolbox of(IComponentLookup componentLookup, MavenSession mavenSession) {
    Optional<IComponentLookup> optionalLookup = Optional.ofNullable(componentLookup);
    Optional<MavenSession> optioanlSession = Optional.ofNullable(mavenSession);
    return new IMavenToolbox() {

      @Override
      public Optional<MavenSession> getSession() {
        return optioanlSession;
      }

      @Override
      public Optional<IComponentLookup> getComponentLookup() {
        return optionalLookup;
      }
    };
  }
}
