/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.AbstractClassifierClasspathProvider;
import org.eclipse.m2e.jdt.IClassifierClasspathProvider;
import org.eclipse.m2e.jdt.IMavenClassifierManager;


/**
 * MavenClassifierManager
 * 
 * @author Fred Bricon
 */
public class MavenClassifierManager implements IMavenClassifierManager {

  private static final String EXTENSION_CLASSIFIER_CLASSPATH_PROVIDERS = "org.eclipse.m2e.jdt.classifierClasspathProviders";

  private static final Logger log = LoggerFactory.getLogger(MavenClassifierManager.class);

  private static final IClassifierClasspathProvider NO_OP_CLASSIFIER_CLASSPATH_PROVIDER = new AbstractClassifierClasspathProvider() {

    public String getClassifier() {
      return "(__ignore_classifier__)";
    }

    public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
      return false;
    }

    public String toString() {
      return "No-Op Classifier Classpath Provider";
    }
  };

  private Map<String, List<IClassifierClasspathProvider>> classifierClasspathProvidersMap;

  public IClassifierClasspathProvider getClassifierClasspathProvider(IMavenProjectFacade project, String classifier) {
    List<IClassifierClasspathProvider> allProviders = getClassifierClasspathProviders(classifier);
    List<IClassifierClasspathProvider> compatibleProviders = new ArrayList<IClassifierClasspathProvider>();

    if(allProviders != null) {
      for(IClassifierClasspathProvider p : allProviders) {
        if(p.applies(project, classifier)) {
          compatibleProviders.add(p);
        }
      }
    }

    switch(compatibleProviders.size()) {
      case 0:
        //nothing here
        break;
      case 1:
        return compatibleProviders.get(0);
      default:
        //TODO display/log error message
    }
    return NO_OP_CLASSIFIER_CLASSPATH_PROVIDER;
  }

  protected List<IClassifierClasspathProvider> getClassifierClasspathProviders(String classifier) {
    if(classifierClasspathProvidersMap == null) {
      classifierClasspathProvidersMap = readExtensions();
    }
    return classifierClasspathProvidersMap.get(classifier);
  }

  protected static synchronized Map<String, List<IClassifierClasspathProvider>> readExtensions() {
    Map<String, List<IClassifierClasspathProvider>> map = new HashMap<String, List<IClassifierClasspathProvider>>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint ccpExtensionPoint = registry.getExtensionPoint(EXTENSION_CLASSIFIER_CLASSPATH_PROVIDERS);
    if(ccpExtensionPoint != null) {
      IExtension[] ccpExtensions = ccpExtensionPoint.getExtensions();
      for(IExtension extension : ccpExtensions) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          IClassifierClasspathProvider classifierClasspathProvider = null;
          try {
            classifierClasspathProvider = (IClassifierClasspathProvider) element.createExecutableExtension("class");
            String classifier = classifierClasspathProvider.getClassifier();
            List<IClassifierClasspathProvider> providers = map.get(classifier);
            if(providers == null) {
              providers = new ArrayList<IClassifierClasspathProvider>(1);
              map.put(classifier, providers);
            }
            providers.add(classifierClasspathProvider);
          } catch(CoreException ex) {
            log.debug("Can not instanciate IClassifierClasspathProvider", ex);
          }
        }
      }
    }

    return map;
  }

}
