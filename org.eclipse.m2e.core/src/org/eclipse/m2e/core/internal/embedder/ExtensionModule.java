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

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.internal.IMavenConstants;


/**
 * A custom Guice module that picks the components contributed by extensions.
 */
class ExtensionModule extends AbstractModule implements IMavenComponentContributor.IMavenComponentBinder {
  private static final Logger log = LoggerFactory.getLogger(ExtensionModule.class);

  @Override
  public <T> void bind(Class<T> role, Class<? extends T> impl, String hint) {
    ScopedBindingBuilder builder;
    if(hint == null || hint.length() <= 0 || "default".equals(hint)) { //$NON-NLS-1$
      builder = bind(role).to(impl);
    } else {
      builder = bind(role).annotatedWith(Names.named(hint)).to(impl);
    }
    if(impl.getAnnotation(Singleton.class) != null) {
      builder.in(com.google.inject.Singleton.class);
    }
  }

  @Override
  protected void configure() {
    IExtensionRegistry r = Platform.getExtensionRegistry();
    for(IConfigurationElement c : r.getConfigurationElementsFor(IMavenConstants.MAVEN_COMPONENT_CONTRIBUTORS_XPT)) {
      if("configurator".equals(c.getName())) { //$NON-NLS-1$
        try {
          IMavenComponentContributor contributor = (IMavenComponentContributor) c.createExecutableExtension("class"); //$NON-NLS-1$
          contributor.contribute(this);
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
      }
    }
  }
}
