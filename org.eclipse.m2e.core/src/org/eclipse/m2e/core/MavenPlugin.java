/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.index.IndexManager;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolverManager;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;
import org.eclipse.m2e.core.repository.IRepositoryRegistry;


public final class MavenPlugin {

  private static final MavenPlugin INSTANCE = new MavenPlugin();

  private MavenPlugin() {

  }

  /**
   * @deprecated Use static getCOMPONENT methods instead
   */
  @Deprecated
  public static MavenPlugin getDefault() {
    return INSTANCE;
  }

  public static IMaven getMaven() {
    return MavenPluginActivator.getDefault().getMaven();
  }

  public static IMavenProjectRegistry getMavenProjectRegistry() {
    return MavenPluginActivator.getDefault().getMavenProjectManager();
  }

  public static IProjectConfigurationManager getProjectConfigurationManager() {
    return MavenPluginActivator.getDefault().getProjectConfigurationManager();
  }

  public static IRepositoryRegistry getRepositoryRegistry() {
    return MavenPluginActivator.getDefault().getRepositoryRegistry();
  }

  public static IndexManager getIndexManager() {
    return MavenPluginActivator.getDefault().getIndexManager();
  }

  public static IMavenConfiguration getMavenConfiguration() {
    return MavenPluginActivator.getDefault().getMavenConfiguration();
  }

  /**
   * @deprecated as of version 1.5, m2e does not provide API to access or configure Maven Installations
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static MavenRuntimeManager getMavenRuntimeManager() {
    return new MavenRuntimeManager(MavenPluginActivator.getDefault().getMavenRuntimeManager());
  }

  public static MavenModelManager getMavenModelManager() {
    return MavenPluginActivator.getDefault().getMavenModelManager();
  }

  public static IProjectConversionManager getProjectConversionManager() {
    return MavenPluginActivator.getDefault().getProjectConversionManager();
  }

  /**
   * @since 1.9
   */
  public static IWorkspaceClassifierResolverManager getWorkspaceClassifierResolverManager() {
    return MavenPluginActivator.getDefault().getWorkspaceClassifierResolverManager();
  }

}
