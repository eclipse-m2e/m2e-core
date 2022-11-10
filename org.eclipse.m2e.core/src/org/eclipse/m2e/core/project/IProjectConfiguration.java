/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.project;

import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * {@link IProjectConfiguration} represents the project specific configuration, many projects can share the same
 * configuration and are usually resolved together.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IProjectConfiguration {

  Map<String, String> getConfigurationProperties();

  boolean isResolveWorkspaceProjects();

  String getSelectedProfiles();

  List<String> getActiveProfileList();

  List<String> getInactiveProfileList();

  String getLifecycleMappingId();

  File getMultiModuleProjectDirectory();

}
