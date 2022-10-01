/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
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

package org.eclipse.m2e.core.internal.launch;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.apache.maven.Maven;

import org.eclipse.m2e.core.internal.Bundles;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Embedded Maven runtime
 *
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenEmbeddedRuntime extends AbstractMavenRuntime {

  private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties"; //$NON-NLS-1$

  private static final Logger log = LoggerFactory.getLogger(MavenEmbeddedRuntime.class);

  private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();

  public static final String PLEXUS_CLASSWORLD_NAME = "plexus.core"; //$NON-NLS-1$

  private static final String M2E_SL4J_BINDING_HEADER = "M2E-SLF4JBinding"; // header defined in org.eclipse.m2e.maven.runtime/pom.xml

  private static List<String> LAUNCHER_CLASSPATH;

  private static List<String> CLASSPATH;

  private static volatile String mavenVersion;

  public MavenEmbeddedRuntime() {
    super(MavenRuntimeManagerImpl.EMBEDDED);
  }

  @Override
  public boolean isEditable() {
    return false;
  }

  @Override
  public String getLocation() {
    return MavenRuntimeManagerImpl.EMBEDDED;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException {
    collector.setMainType(MAVEN_EXECUTOR_CLASS, PLEXUS_CLASSWORLD_NAME);

    initClasspath();

    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    for(String entry : LAUNCHER_CLASSPATH) {
      collector.addArchiveEntry(entry);
    }

    collector.addRealm(PLEXUS_CLASSWORLD_NAME);
    collectExtensions(collector, monitor);
    for(String entry : CLASSPATH) {
      // https://issues.sonatype.org/browse/MNGECLIPSE-2507
      if(!entry.contains("plexus-build-api")) {
        collector.addArchiveEntry(entry);
      }
    }
  }

  public static final Set<String> SLF4J_BUNDLE_NAMES = Set.of("org.slf4j.api", "slf4j.api");

  private synchronized void initClasspath() {
    if(CLASSPATH == null) {
      Bundle mavenRuntimeBundle = findMavenEmbedderBundle();
      Set<String> allEntries = new LinkedHashSet<>();

      addBundleClasspathEntries(allEntries, mavenRuntimeBundle, true);
      allEntries.add(getEmbeddedSLF4JBinding(mavenRuntimeBundle));

      // Don't include dependencies of slf4j bundle to ensure no other than the slf4j-binding embedded into m2e.maven.runtime is available.
      Set<Bundle> bundles = getRequiredBundles(mavenRuntimeBundle, SLF4J_BUNDLE_NAMES);

      for(Bundle bundle : bundles) {
        addBundleClasspathEntries(allEntries, bundle, false);
      }

      List<String> cp = new ArrayList<>();
      List<String> lcp = new ArrayList<>();

      for(String entry : allEntries) {
        List<String> path = entry.contains("plexus-classworlds") ? lcp : cp; //$NON-NLS-1$ 
        path.add(entry);
      }
      CLASSPATH = List.copyOf(cp);
      LAUNCHER_CLASSPATH = List.copyOf(lcp);
    }
  }

  private void addBundleClasspathEntries(Set<String> entries, Bundle bundle, boolean addFragments) {
    boolean extractRoot = SLF4J_BUNDLE_NAMES.contains(bundle.getSymbolicName());
    // Use extracted/directory form of slf4j bundles so that their jar-signing signature by the Maven-runtime classloader
    entries.addAll(Bundles.getClasspathEntries(bundle, extractRoot));
    Bundle[] fragments;
    if(addFragments && (fragments = Platform.getFragments(bundle)) != null) {
      for(Bundle fragment : fragments) {
        entries.addAll(Bundles.getClasspathEntries(fragment, SLF4J_BUNDLE_NAMES.contains(fragment.getSymbolicName())));
      }
    }
  }

  private static String getEmbeddedSLF4JBinding(Bundle mavenBundle) {
    String bindingPath = mavenBundle.getHeaders().get(M2E_SL4J_BINDING_HEADER);
    Objects.requireNonNull(bindingPath,
        () -> "Missing '" + M2E_SL4J_BINDING_HEADER + "' header in embedded Maven-runtime bundle");
    String bindingJarPath = Bundles.getClasspathEntryPath(mavenBundle, bindingPath, false);
    return Objects.requireNonNull(bindingJarPath, () -> M2E_SL4J_BINDING_HEADER + " '" + bindingPath + "' not found");
  }

  private static final List<String> CLASSPATH_CONSIDERED_REQUIREMENT_NAMESPACES = List
      .of(BundleNamespace.BUNDLE_NAMESPACE, PackageNamespace.PACKAGE_NAMESPACE);

  private Set<Bundle> getRequiredBundles(Bundle root, Set<String> noDependencyBundles) {
    // find and add required bundles and bundles providing imported packages
    Set<Bundle> bundles = new LinkedHashSet<>();
    Queue<Bundle> pending = new ArrayDeque<>();
    pending.add(root);
    while(!pending.isEmpty()) { // breath-first search to collect all (transitively) required bundles
      Bundle bundle = pending.remove();
      if(noDependencyBundles.contains(bundle.getSymbolicName())) {
        continue;
      }
      BundleWiring wiring = bundle.adapt(BundleWiring.class);
      for(String namespace : CLASSPATH_CONSIDERED_REQUIREMENT_NAMESPACES) {
        for(BundleWire wire : wiring.getRequiredWires(namespace)) {
          Bundle requiredBundle = wire.getProvider().getBundle();
          if(requiredBundle.getBundleId() != Constants.SYSTEM_BUNDLE_ID && bundles.add(requiredBundle)) {
            // Don't add OSGi System-Bundle, which is wired if a bundle imports java.* or sun.* packages
            pending.add(requiredBundle);
          }
        }
      }
    }
    return bundles;
  }

  private static Bundle findMavenEmbedderBundle() {
    return FrameworkUtil.getBundle(Maven.class);
  }

  @Override
  public String toString() {
    Bundle embedder = findMavenEmbedderBundle();
    return embedder != null ? getVersion() + '/' + embedder.getVersion() : "org.eclipse.m2e.maven.runtime";
  }

  @Override
  public synchronized String getVersion() {
    if(mavenVersion != null) {
      return mavenVersion;
    }
    initClasspath();
    try {
      String mavenCoreJarPath = CLASSPATH.stream().filter(p -> p.contains("maven-core")).findFirst()
          .orElseThrow(() -> new IllegalStateException("Could not find maven core jar file"));

      Properties pomProperties = new Properties();

      Path mavenCoreJar = Path.of(mavenCoreJarPath);
      if(Files.isRegularFile(mavenCoreJar)) {
        try (ZipFile zip = new ZipFile(mavenCoreJarPath)) {
          ZipEntry zipEntry = zip.getEntry(MAVEN_CORE_POM_PROPERTIES);
          if(zipEntry != null) {
            pomProperties.load(zip.getInputStream(zipEntry));
          }
        }
      } else if(Files.isDirectory(mavenCoreJar)) {
        try (Reader r = Files.newBufferedReader(mavenCoreJar.resolve(MAVEN_CORE_POM_PROPERTIES))) {
          pomProperties.load(r);
        }
      }
      String version = pomProperties.getProperty("version"); //$NON-NLS-1$
      if(version != null) {
        mavenVersion = version;
        return mavenVersion;
      }
    } catch(Exception e) {
      log.warn("Could not determine embedded maven version", e);
    }
    return Messages.MavenEmbeddedRuntime_unknown;
  }
}
