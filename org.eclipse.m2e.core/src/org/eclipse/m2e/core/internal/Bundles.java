/*******************************************************************************
 * Copyright (c) 2014 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.ManifestElement;

import org.eclipse.m2e.core.internal.equinox.DevClassPathHelper;


/**
 * @since 1.5
 */
public class Bundles {

  private static final Logger log = LoggerFactory.getLogger(Bundles.class);

  private static Bundle findDependencyBundle(Bundle bundle, String dependencyName, Set<Bundle> visited) {
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    if(bundleWiring == null) {
      return null;
    }
    ArrayList<BundleWire> dependencies = new ArrayList<>();
    dependencies.addAll(bundleWiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE));
    dependencies.addAll(bundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE));
    for(BundleWire wire : dependencies) {
      Bundle requiredBundle = wire.getProviderWiring().getBundle();
      if(requiredBundle != null && visited.add(requiredBundle)) {
        if(dependencyName.equals(requiredBundle.getSymbolicName())) {
          return requiredBundle;
        }
        Bundle required = findDependencyBundle(requiredBundle, dependencyName, visited);
        if(required != null) {
          return required;
        }
      }
    }
    return null;
  }

  public static Bundle findDependencyBundle(Bundle bundle, String dependencyId) {
    return findDependencyBundle(bundle, dependencyId, new HashSet<Bundle>());
  }

  public static ClassLoader getBundleClassloader(Bundle bundle) {
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    if(bundleWiring == null) {
      return null;
    }
    return bundleWiring.getClassLoader();
  }

  public static List<String> getClasspathEntries(Bundle bundle) {
    log.debug("getClasspathEntries(Bundle={})", bundle.toString()); //$NON-NLS-1$
    Set<String> cp = new LinkedHashSet<>();
    if(DevClassPathHelper.inDevelopmentMode()) {
      cp.addAll(Arrays.asList(DevClassPathHelper.getDevClassPath(bundle.getSymbolicName())));
    }
    cp.addAll(Arrays.asList(parseBundleClasspath(bundle)));
    List<String> entries = new ArrayList<>();
    for(String cpe : cp) {
      String entry;
      if(".".equals(cpe)) { //$NON-NLS-1$
        entry = getNestedJarOrDir(bundle, "/"); //$NON-NLS-1$
      } else {
        entry = getNestedJarOrDir(bundle, cpe);
      }

      if(entry != null) {
        entry = new Path(entry).toOSString();
        log.debug("\tEntry:{}", entry); //$NON-NLS-1$
        entries.add(entry);
      }
    }
    return entries;
  }

  private static String[] parseBundleClasspath(Bundle bundle) {
    String[] result = new String[] {"."}; //$NON-NLS-1$
    String header = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
    ManifestElement[] classpathEntries = null;
    try {
      classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, header);
    } catch(BundleException ex) {
      log.warn("Could not parse bundle classpath of {}", bundle.toString(), ex); //$NON-NLS-1$
    }
    if(classpathEntries != null) {
      result = new String[classpathEntries.length];
      for(int i = 0; i < classpathEntries.length; i++ ) {
        result[i] = classpathEntries[i].getValue();
      }
    }
    return result;
  }

  private static String getNestedJarOrDir(Bundle bundle, String cp) {
    // try embeded entries first
    URL url = bundle.getEntry(cp);
    if(url != null) {
      try {
        return FileLocator.toFileURL(url).getFile();
      } catch(IOException ex) {
        log.warn("Could not get entry {} for bundle {}", cp, bundle.toString(), ex); //$NON-NLS-1$
      }
    }

    // in development mode entries can be absolute paths outside of bundle basedir
    if(DevClassPathHelper.inDevelopmentMode()) {
      File file = new File(cp);
      if(file.exists() && file.isAbsolute()) {
        return file.getAbsolutePath();
      }
    }

    log.debug("Bundle {} does not have entry {}", bundle.toString(), cp); //$NON-NLS-1$
    return null;
  }

  private static Bundle findDependencyBundleByPackage(Bundle bundle, String packageName, Set<Bundle> visited) {
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    if(bundleWiring == null) {
      return null;
    }
    ArrayList<BundleWire> dependencies = new ArrayList<>();
    dependencies.addAll(bundleWiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE));
    dependencies.addAll(bundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE));
    for(BundleWire wire : dependencies) {
      BundleCapability cap = wire.getCapability();
      String pkg = (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
      Bundle requiredBundle = wire.getProviderWiring().getBundle();
      if(requiredBundle != null) {
        if(packageName.equals(pkg)) {
          return requiredBundle;
        }
        if(visited.add(requiredBundle)) {
          Bundle required = findDependencyBundleByPackage(requiredBundle, packageName, visited);
          if(required != null) {
            return required;
          }
        }
      }
    }
    return null;
  }

  public static Bundle findDependencyBundleByPackage(Bundle bundle, String packageName) {
    return findDependencyBundleByPackage(bundle, packageName, new HashSet<Bundle>());
  }

}
