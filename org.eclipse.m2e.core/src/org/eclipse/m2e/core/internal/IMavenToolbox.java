/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.DefaultMaven;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Problem;
import org.apache.maven.building.ProblemCollector;
import org.apache.maven.building.ProblemCollectorFactory;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.toolchain.building.DefaultToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.apache.maven.toolchain.building.ToolchainsBuildingException;
import org.apache.maven.toolchain.building.ToolchainsBuildingRequest;
import org.apache.maven.toolchain.building.ToolchainsBuildingResult;

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

  default MavenExecutionResult readMavenProject(File pomFile, ProjectBuildingRequest configuration)
      throws CoreException {
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    try {
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      ProjectBuildingResult projectBuildingResult = componentLookup.lookup(ProjectBuilder.class).build(pomFile,
          configuration);
      MavenProject project = projectBuildingResult.getProject();
      clearProjectBuildingRequest(project);
      result.setProject(project);
      result.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
    } catch(ProjectBuildingException ex) {
      if(ex.getResults() != null && ex.getResults().size() == 1) {
        ProjectBuildingResult projectBuildingResult = ex.getResults().get(0);
        MavenProject project = projectBuildingResult.getProject();
        clearProjectBuildingRequest(project);
        result.setProject(project);
        result.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
      }
      result.addException(ex);
    } catch(RuntimeException e) {
      result.addException(e);
    }
    return result;
  }

  default Map<File, MavenExecutionResult> readMavenProjects(Collection<File> pomFiles,
      ProjectBuildingRequest configuration) throws CoreException {
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    List<ProjectBuildingResult> projectBuildingResults = new ArrayList<>();
    Map<File, MavenExecutionResult> result = new LinkedHashMap<>(pomFiles.size(), 1.f);
    try {
      configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
      projectBuildingResults
          .addAll(componentLookup.lookup(ProjectBuilder.class).build(new ArrayList<>(pomFiles), false, configuration));
    } catch(ProjectBuildingException ex) {
      if(ex.getResults() != null) {
        projectBuildingResults.addAll(ex.getResults());
      }
    }
    for(ProjectBuildingResult projectBuildingResult : projectBuildingResults) {
      MavenExecutionResult mavenExecutionResult = new DefaultMavenExecutionResult();
      MavenProject project = projectBuildingResult.getProject();
      clearProjectBuildingRequest(project);
      mavenExecutionResult.setProject(project);
      mavenExecutionResult.setDependencyResolutionResult(projectBuildingResult.getDependencyResolutionResult());
      if(!projectBuildingResult.getProblems().isEmpty()) {
        mavenExecutionResult
            .addException(new ProjectBuildingException(Collections.singletonList(projectBuildingResult)));
      }
      result.put(projectBuildingResult.getPomFile(), mavenExecutionResult);
    }
    return result;
  }

  /**
   * clears the (deprecated) ProjectBuildingRequest in a maven project to not keep a hard reference to it
   * 
   * @param project
   */
  private static void clearProjectBuildingRequest(MavenProject project) {
    clearProjectBuildingRequest(project, Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  private static void clearProjectBuildingRequest(MavenProject project, Set<MavenProject> seen) {
    if(project != null && seen.add(project)) {
      project.setProjectBuildingRequest(null);
      clearProjectBuildingRequest(project.getParent(), seen);
    }
  }

  default MavenExecutionPlan calculateExecutionPlan(Collection<String> tasks, boolean setup) throws CoreException {
    MavenSession session = getSession().orElseThrow(ERROR_NO_SESSION);
    IComponentLookup componentLookup = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP);
    LifecycleExecutor lifecycleExecutor = componentLookup.lookup(LifecycleExecutor.class);
    try {
      return lifecycleExecutor.calculateExecutionPlan(session, setup, tasks.toArray(String[]::new));
    } catch(Exception e) {
      throw new CoreException(Status.error(NLS.bind(Messages.MavenImpl_error_calc_build_plan, e.getMessage()), e));
    }
  }

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
   * Validates the toolchains definition
   * 
   * @param toolchains The path to the toolchains definition file to test.
   * @return List of all problems. Is never <code>null</code>.
   * @throws CoreException if reading failed
   */
  default List<Problem> validateToolchains(String toolchains) {
    List<Problem> problems = new ArrayList<>();
    if(toolchains != null) {
      File toolchainsFile = new File(toolchains);
      ProblemCollector problemsFactory = ProblemCollectorFactory.newInstance(null);
      if(toolchainsFile.canRead()) {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new FileSource(toolchainsFile));
        try {
          ToolchainsBuildingResult result = getComponentLookup().orElseThrow(ERROR_NO_LOOKUP).lookup(ToolchainsBuilder.class).build(request);
          problems.addAll(result.getProblems());
        } catch(ToolchainsBuildingException ex) {
          problems.addAll(ex.getProblems());
        } catch(CoreException ex) {
          problemsFactory.add(Problem.Severity.FATAL, toolchains, -1, -1, ex);
          problems.addAll(problemsFactory.getProblems());
        }
      } else {
        problemsFactory.add(Problem.Severity.ERROR, NLS.bind(Messages.MavenImpl_error_read_toolchains, toolchains), -1,
            -1, null);
        problems.addAll(problemsFactory.getProblems());
      }
    }
    return problems;
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
