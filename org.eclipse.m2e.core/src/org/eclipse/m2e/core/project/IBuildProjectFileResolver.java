/********************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.project;

import org.eclipse.core.runtime.IPath;


/**
 * A service to resolve a project's real main file from a polyglot pom-model file used in a Maven build.
 * <p>
 * Instances have be registered as OSGi service.
 * </p>
 */
public interface IBuildProjectFileResolver {
  /**
   * Returns the relative path to the 'real' project file as sibling of the polyglot pom file with the given name.
   * 
   * @param pomFilename the polyglot pom file's name
   * @return the path to the 'real' project file, which is resolved against the polyglot pom file's parent
   */
  IPath resolveProjectFile(String pomFilename);
}
