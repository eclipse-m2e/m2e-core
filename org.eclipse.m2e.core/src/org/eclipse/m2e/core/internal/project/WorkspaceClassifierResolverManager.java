/*******************************************************************************
 * Copyright (c) 2017 Walmartlabs
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.project;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolver;
import org.eclipse.m2e.core.project.IWorkspaceClassifierResolverManager;


/**
 * @author atanasenko
 * @since 1.9
 */
public class WorkspaceClassifierResolverManager implements IWorkspaceClassifierResolverManager {

  protected static final Logger log = LoggerFactory.getLogger(WorkspaceClassifierResolverManager.class);

  private static final String EXTENSION_WORKSPACE_CLASSIFIER_RESOLVERS = "org.eclipse.m2e.core.workspaceClassifierResolvers";

  private volatile List<IWorkspaceClassifierResolver> classifierResolvers;

  private final IWorkspaceClassifierResolver defaultResolver = new IWorkspaceClassifierResolver() {
    @Override
    public IPath resolveClassifier(IMavenProjectFacade project, String classifier) {
      for(IWorkspaceClassifierResolver resolver : getResolvers()) {
        IPath res = resolver.resolveClassifier(project, classifier);
        if(res != null) {
          log.info("Resolving {} with classifier {} to {}", project, classifier, res);
          return res;
        }
      }
      return null;
    }

    public int getPriority() {
      return 0;
    }
  };

  protected List<IWorkspaceClassifierResolver> getResolvers() {
    if(classifierResolvers == null) {
      synchronized(this) {
        if(classifierResolvers == null) {
          classifierResolvers = readExtensions();
        }
      }
    }
    return classifierResolvers;
  }

  public IWorkspaceClassifierResolver getResolver() {
    return defaultResolver;
  }

  protected static List<IWorkspaceClassifierResolver> readExtensions() {
    List<IWorkspaceClassifierResolver> resolvers = new ArrayList<>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint ccpExtensionPoint = registry.getExtensionPoint(EXTENSION_WORKSPACE_CLASSIFIER_RESOLVERS);
    if(ccpExtensionPoint != null) {
      IExtension[] ccpExtensions = ccpExtensionPoint.getExtensions();
      for(IExtension extension : ccpExtensions) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          try {
            resolvers.add((IWorkspaceClassifierResolver) element.createExecutableExtension("class"));
          } catch(CoreException ex) {
            log.error("Cannot instantiate IWorkspaceClassifierResolver", ex);
          }
        }
      }
    }

    resolvers.sort((l, r) -> l.getPriority() - r.getPriority());
    return resolvers;
  }

}
