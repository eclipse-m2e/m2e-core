/*******************************************************************************
 * Copyright (c) 2026 Christoph Läubrich and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.plugin.MojoExecution;

import org.eclipse.m2e.core.internal.project.registry.MojoExecutionFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;


/**
 * Facade for a (fully setup) mojo execution bound to a project's build lifecycle. This decouples consumers that
 * already hold an {@link IMavenProjectFacade} from depending directly on Maven-core API such as
 * {@link MojoExecution}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.9
 */
public interface IMojoExecutionFacade {

  /**
   * @return the identity of the wrapped mojo execution
   */
  MojoExecutionKey getKey();

  /**
   * Resolves a configuration parameter from the wrapped mojo execution. It coerces from String to the given type and
   * considers expressions and default values.
   *
   * @param <T>
   * @param parameter the name of the parameter (may be nested with separating {@code .})
   * @param asType the type to coerce to
   * @param monitor the progress monitor
   * @return the parameter value or {@code null} if the parameter with the given name was not found
   * @throws CoreException
   */
  <T> T getMojoParameterValue(String parameter, Class<T> asType, IProgressMonitor monitor) throws CoreException;

  /**
   * Resolves a nested configuration parameter from the wrapped mojo execution. It coerces from String to the given
   * type and considers expressions and default values.
   *
   * @param <T>
   * @param parameterPath the path of the parameter to look up, the first item is the name of the element directly
   *          below {@code <configuration>} and the last one is the element containing the actual value
   * @param asType the type to coerce to
   * @param monitor the progress monitor
   * @return the parameter value or {@code null} if the parameter with the given name was not found
   * @throws CoreException
   * @since 2.9
   */
  <T> T getMojoParameterValue(List<String> parameterPath, Class<T> asType, IProgressMonitor monitor)
      throws CoreException;

  /**
   * Returns a new mojo instance configured according to the wrapped mojo execution. This allows introspection of the
   * mojo's configuration parameters (e.g. via its getters), it is not intended to execute the mojo.
   *
   * @param <T> the mojo interface/type
   * @param mojoInterface the mojo interface/type to cast the configured mojo instance to
   * @param monitor the progress monitor
   * @return a new, configured mojo instance
   * @throws CoreException
   * @since 2.9
   */
  <T> T getConfiguredMojo(Class<T> mojoInterface, IProgressMonitor monitor) throws CoreException;

  /**
   * Wraps a raw {@link MojoExecution} into an {@link IMojoExecutionFacade} bound to the given project facade.
   * <p>
   * This is intended as a transitional helper for client code that still acquires or receives (e.g. through a
   * framework callback) a raw {@link MojoExecution} instance, until such code can be migrated to acquire an
   * {@link IMojoExecutionFacade} directly from {@link IMavenProjectFacade}.
   * </p>
   *
   * @param projectFacade the project facade the execution belongs to
   * @param mojoExecution the raw mojo execution to wrap
   * @return a facade wrapping the given mojo execution, or {@code null} if {@code mojoExecution} is {@code null}
   */
  static IMojoExecutionFacade wrap(IMavenProjectFacade projectFacade, MojoExecution mojoExecution) {
    if(mojoExecution == null) {
      return null;
    }
    return new MojoExecutionFacade(projectFacade, mojoExecution);
  }

}
