/*******************************************************************************
 * Copyright (c) 2026 Tyce Herrman and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;

import org.codehaus.plexus.logging.LoggerManager;

import org.eclipse.m2e.core.embedder.IMavenConfiguration;


@FunctionalInterface
interface PlexusContainerFactory {

  IMavenPlexusContainer create(File multiModuleProjectDirectory, LoggerManager loggerManager,
      IMavenConfiguration mavenConfiguration) throws Exception;

}
