/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.embedder;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;
import org.apache.maven.project.artifact.MavenMetadataCache;

import org.sonatype.aether.impl.LocalRepositoryMaintainer;
import org.sonatype.plexus.build.incremental.BuildContext;

import org.eclipse.m2e.core.internal.project.EclipseMavenMetadataCache;
import org.eclipse.m2e.core.internal.project.registry.EclipsePluginDependenciesResolver;


/**
 */
public class DefaultMavenComponentContributor implements IMavenComponentContributor {

  public void contribute(IMavenComponentBinder binder) {
    binder.bind(MavenMetadataCache.class, EclipseMavenMetadataCache.class, null);
    binder.bind(PluginDependenciesResolver.class, EclipsePluginDependenciesResolver.class, null);
    binder.bind(BuildContext.class, EclipseBuildContext.class, null);
    binder.bind(ClassRealmManagerDelegate.class, EclipseClassRealmManagerDelegate.class, EclipseClassRealmManagerDelegate.ROLE_HINT);
    binder.bind(LocalRepositoryMaintainer.class, EclipseLocalRepositoryMaintainer.class, EclipseLocalRepositoryMaintainer.ROLE_HINT);
    binder.bind(ContextRepositorySystemSession.class, ContextRepositorySystemSessionImpl.class, null);
  }

}
