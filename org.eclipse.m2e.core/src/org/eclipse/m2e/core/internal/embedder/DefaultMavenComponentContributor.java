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

package org.eclipse.m2e.core.internal.embedder;

import org.eclipse.aether.RepositoryListener;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.PluginArtifactsCache;
import org.apache.maven.plugin.PluginRealmCache;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;
import org.apache.maven.project.ProjectRealmCache;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.internal.project.EclipseExtensionRealmCache;
import org.eclipse.m2e.core.internal.project.EclipseMavenMetadataCache;
import org.eclipse.m2e.core.internal.project.EclipsePluginArtifactsCache;
import org.eclipse.m2e.core.internal.project.EclipsePluginRealmCache;
import org.eclipse.m2e.core.internal.project.EclipseProjectRealmCache;
import org.eclipse.m2e.core.internal.project.registry.EclipsePluginDependenciesResolver;


public class DefaultMavenComponentContributor implements IMavenComponentContributor {

  @Override
  public void contribute(IMavenComponentBinder binder) {
    binder.bind(MavenMetadataCache.class, EclipseMavenMetadataCache.class, null);
    binder.bind(ExtensionRealmCache.class, EclipseExtensionRealmCache.class, null);
    binder.bind(ProjectRealmCache.class, EclipseProjectRealmCache.class, null);
    binder.bind(PluginRealmCache.class, EclipsePluginRealmCache.class, null);
    binder.bind(PluginArtifactsCache.class, EclipsePluginArtifactsCache.class, null);
    binder.bind(PluginDependenciesResolver.class, EclipsePluginDependenciesResolver.class, null);
    binder.bind(BuildContext.class, EclipseBuildContext.class, null);
    binder.bind(ClassRealmManagerDelegate.class, EclipseClassRealmManagerDelegate.class,
        EclipseClassRealmManagerDelegate.ROLE_HINT);
    binder.bind(RepositoryListener.class, EclipseRepositoryListener.class, EclipseRepositoryListener.ROLE_HINT);
    binder.bind(ContextRepositorySystemSession.class, ContextRepositorySystemSessionImpl.class, null);

  }

}
