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
package org.eclipse.m2e.core.internal.embedder;

import java.io.File;
import java.util.Optional;

import org.codehaus.plexus.PlexusContainer;

import org.eclipse.m2e.core.embedder.IComponentLookup;


/**
 * IMavenPlexusContainer
 *
 */
public interface IMavenPlexusContainer {

  static final String MVN_FOLDER = ".mvn";

  static final String EXTENSIONS_FILENAME = MVN_FOLDER + "/extensions.xml";

  /**
   * Maven allows to use a magic {@value #MVN_FOLDER} folder where one can configure several aspects of the maven run
   * and m2e scopes containers by this directory.
   * 
   * @return the folder this container is rooted by or an empty Optional if this container has no {@value #MVN_FOLDER}
   *         root
   */
  Optional<File> getMavenDirectory();

  /**
   * @return the underlying {@link PlexusContainer}.
   */
  PlexusContainer getContainer();

  /**
   * @return returns a {@link IComponentLookup} that is backed by this container
   */
  IComponentLookup getComponentLookup();

}
