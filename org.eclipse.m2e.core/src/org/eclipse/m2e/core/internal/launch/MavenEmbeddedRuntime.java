/*******************************************************************************
 * Copyright (c) 2008-2021 Sonatype, Inc.
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.internal.Bundles;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Embedded Maven runtime
 *
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
@SuppressWarnings("deprecation")
public class MavenEmbeddedRuntime extends AbstractMavenRuntime {

  private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties"; //$NON-NLS-1$

  private static final Logger log = LoggerFactory.getLogger(MavenEmbeddedRuntime.class);

  private static final String MAVEN_EMBEDDER_BUNDLE_SYMBOLICNAME = "org.eclipse.m2e.maven.runtime"; //$NON-NLS-1$

  private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();

  public static final String PLEXUS_CLASSWORLD_NAME = "plexus.core"; //$NON-NLS-1$

  private static String[] LAUNCHER_CLASSPATH;

  private static String[] CLASSPATH;

  private static volatile String mavenVersion;

  private static final Bundle m2eCore = MavenPluginActivator.getDefault().getBundle();

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

    initClasspath(findMavenEmbedderBundle());

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

  private synchronized void initClasspath(Bundle mavenRuntimeBundle) {
    if(CLASSPATH == null) {
      Set<String> allEntries = new LinkedHashSet<>();

      addBundleClasspathEntries(allEntries, mavenRuntimeBundle);

      Set<Bundle> bundles = new LinkedHashSet<>();
      // find and add required bundles and bundles providing imported packages
      List<BundleWire> requiredWires = new ArrayList<>();
      BundleWiring wiring = mavenRuntimeBundle.adapt(BundleWiring.class);
      requiredWires.addAll(wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE));
      requiredWires.addAll(wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE));
      requiredWires.stream().map(BundleWire::getProvider).map(BundleRevision::getBundle).forEach(bundles::add);

      for(Bundle bundle : bundles) {
        addBundleClasspathEntries(allEntries, bundle);
      }

      List<String> cp = new ArrayList<>();
      List<String> lcp = new ArrayList<>();

      for(String entry : allEntries) {
        if(entry.contains("plexus-classworlds")) { //$NON-NLS-1$
          lcp.add(entry);
        } else {
          cp.add(entry);
        }
      }

      CLASSPATH = cp.toArray(new String[cp.size()]);
      LAUNCHER_CLASSPATH = lcp.toArray(new String[lcp.size()]);
    }
  }

  private void addBundleClasspathEntries(Set<String> entries, Bundle bundle) {
    entries.addAll(Bundles.getClasspathEntries(bundle));
    Bundle[] fragments = Platform.getFragments(bundle);
    if(fragments != null) {
      for(Bundle fragment : fragments) {
        entries.addAll(Bundles.getClasspathEntries(fragment));
      }
    }
  }

  private Bundle findMavenEmbedderBundle() {
    return Bundles.findDependencyBundle(m2eCore, MAVEN_EMBEDDER_BUNDLE_SYMBOLICNAME);
  }

  @Override
  public String toString() {
    Bundle embedder = findMavenEmbedderBundle();

    if(embedder != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(getVersion(embedder));
      String version = embedder.getHeaders().get(Constants.BUNDLE_VERSION);
      sb.append('/').append(version);
      return sb.toString();
    }

    return MAVEN_EMBEDDER_BUNDLE_SYMBOLICNAME;
  }

  private synchronized String getVersion(Bundle bundle) {
    if(mavenVersion != null) {
      return mavenVersion;
    }
    initClasspath(bundle);
    try {
      String mavenCoreJarPath = null;
      for(String path : CLASSPATH) {
        if(path.contains("maven-core")) {
          mavenCoreJarPath = path;
          break;
        }
      }

      if(mavenCoreJarPath == null) {
        throw new IllegalStateException("Could not find maven core jar file");
      }

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

  @Override
  public String getVersion() {
    Bundle bundle = findMavenEmbedderBundle();
    return getVersion(bundle);
  }
}
