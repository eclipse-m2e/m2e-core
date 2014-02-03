/*******************************************************************************
 * Copyright (c) 2014 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;


/**
 * @since 1.5
 */
public class Bundles {

  private static Bundle findDependencyBundle(Bundle bundle, String dependencyName, Set<Bundle> visited) {
    BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
    if(bundleWiring == null) {
      return null;
    }
    ArrayList<BundleWire> dependencies = new ArrayList<BundleWire>();
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

}
