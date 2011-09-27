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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.ManifestElement;

import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.embedder.MavenRuntimeManager;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Embedded Maven runtime
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenEmbeddedRuntime implements MavenRuntime {
  private static final Logger log = LoggerFactory.getLogger(MavenEmbeddedRuntime.class);

  private static final String MAVEN_MAVEN_EMBEDDER_BUNDLE_ID = "org.eclipse.m2e.maven.runtime"; //$NON-NLS-1$

  private static final String MAVEN_EXECUTOR_CLASS = org.apache.maven.cli.MavenCli.class.getName();

  public static final String PLEXUS_CLASSWORLD_NAME = "plexus.core"; //$NON-NLS-1$

  private static String[] LAUNCHER_CLASSPATH;

  private static String[] CLASSPATH;

  private static volatile String mavenVersion;

  private BundleContext bundleContext;

  public MavenEmbeddedRuntime(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  public boolean isEditable() {
    return false;
  }

  public String getLocation() {
    return MavenRuntimeManager.EMBEDDED;
  }

  public String getSettings() {
    return null;
  }

  public boolean isAvailable() {
    return true;
  }

  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException {
    collector.setMainType(MAVEN_EXECUTOR_CLASS, PLEXUS_CLASSWORLD_NAME);

    initClasspath(findMavenEmbedderBundle());

    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    for(String entry : LAUNCHER_CLASSPATH) {
      collector.addArchiveEntry(entry);
    }

    collector.addRealm(PLEXUS_CLASSWORLD_NAME);
    for(String entry : CLASSPATH) {
      // https://issues.sonatype.org/browse/MNGECLIPSE-2507
      if(!entry.contains("plexus-build-api")) {
        collector.addArchiveEntry(entry);
      }
    }
  }

  private synchronized void initClasspath(Bundle mavenRuntimeBundle) {
    if(CLASSPATH == null) {
      LinkedHashSet<String> allentries = new LinkedHashSet<String>();

      addBundleClasspathEntries(allentries, mavenRuntimeBundle);

      // find and add more bundles
      State state = Platform.getPlatformAdmin().getState(false);
      BundleDescription description = state.getBundle(mavenRuntimeBundle.getBundleId());
      for(String sname : new String[] {"com.ning.async-http-client", "org.jboss.netty", "org.slf4j.api"}) {
        Bundle dependency = findDependencyBundle(description, sname, new HashSet<BundleDescription>());
        if(dependency != null) {
          addBundleClasspathEntries(allentries, dependency);
        } else {
          log.warn(
              "Could not find OSGi bundle with symbolic name ''{}'' required to launch embedded maven runtime in external process",
              sname);
        }
      }

      List<String> cp = new ArrayList<String>();
      List<String> lcp = new ArrayList<String>();

      for(String entry : allentries) {
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

  private Bundle findDependencyBundle(BundleDescription bundleDescription, String dependencyName,
      Set<BundleDescription> visited) {
    ArrayList<VersionConstraint> dependencies = new ArrayList<VersionConstraint>();
    dependencies.addAll(Arrays.asList(bundleDescription.getRequiredBundles()));
    dependencies.addAll(Arrays.asList(bundleDescription.getImportPackages()));
    for(VersionConstraint requiredSpecification : dependencies) {
      BundleDescription requiredDescription = getDependencyBundleDescription(requiredSpecification);
      if(requiredDescription != null && visited.add(requiredDescription)) {
        if(dependencyName.equals(requiredDescription.getName())) {
          return bundleContext.getBundle(requiredDescription.getBundleId());
        }
        Bundle required = findDependencyBundle(requiredDescription, dependencyName, visited);
        if(required != null) {
          return required;
        }
      }
    }
    return null;
  }

  private BundleDescription getDependencyBundleDescription(VersionConstraint requiredSpecification) {
    BaseDescription supplier = requiredSpecification.getSupplier();
    if(supplier instanceof BundleDescription) {
      return (BundleDescription) supplier;
    } else if(supplier instanceof ExportPackageDescription) {
      return ((ExportPackageDescription) supplier).getExporter();
    }
    return null;
  }

  private void addBundleClasspathEntries(Set<String> entries, Bundle bundle) {
    log.debug("addBundleClasspathEntries(Bundle={})", bundle.toString());

    Set<String> cp = new LinkedHashSet<String>();
    if(DevClassPathHelper.inDevelopmentMode()) {
      cp.addAll(Arrays.asList(DevClassPathHelper.getDevClassPath(bundle.getSymbolicName())));
    }
    cp.addAll(Arrays.asList(parseBundleClasspath(bundle)));
    for(String cpe : cp) {
      String entry;
      if(".".equals(cpe)) {
        entry = getNestedJarOrDir(bundle, "/");
      } else {
        entry = getNestedJarOrDir(bundle, cpe);
      }

      if(entry != null) {
        log.debug("\tEntry:{}", entry);
        entries.add(entry);
      }
    }
  }

  private String[] parseBundleClasspath(Bundle bundle) {
    String[] result = new String[] {"."};
    String header = (String) bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
    ManifestElement[] classpathEntries = null;
    try {
      classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, header);
    } catch(BundleException ex) {
      log.warn("Could not parse bundle classpath of {}", bundle.toString(), ex);
    }
    if(classpathEntries != null) {
      result = new String[classpathEntries.length];
      for(int i = 0; i < classpathEntries.length; i++ ) {
        result[i] = classpathEntries[i].getValue();
      }
    }
    return result;
  }

  private String getNestedJarOrDir(Bundle bundle, String cp) {
    // try embeded entries first
    URL url = bundle.getEntry(cp);
    if(url != null) {
      try {
        return FileLocator.toFileURL(url).getFile();
      } catch(IOException ex) {
        log.warn("Could not get entry {} for bundle {}", new Object[] {cp, bundle.toString(), ex});
      }
    }

    // in development mode entries can be absolute paths outside of bundle basedir
    if(DevClassPathHelper.inDevelopmentMode()) {
      File file = new File(cp);
      if(file.exists() && file.isAbsolute()) {
        return file.getAbsolutePath();
      }
    }

    log.debug("Bundle {} does not have entry {}", bundle.toString(), cp);
    return null;
  }

  private Bundle findMavenEmbedderBundle() {
    Bundle bundle = null;
    Bundle[] bundles = bundleContext.getBundles();
    for(int i = 0; i < bundles.length; i++ ) {
      if(MAVEN_MAVEN_EMBEDDER_BUNDLE_ID.equals(bundles[i].getSymbolicName())) {
        bundle = bundles[i];
        break;
      }
    }
    return bundle;
  }

  public String toString() {
    Bundle embedder = Platform.getBundle(MAVEN_MAVEN_EMBEDDER_BUNDLE_ID);

    StringBuilder sb = new StringBuilder();
    sb.append("Embedded (").append(getVersion()); //$NON-NLS-1$
    if(embedder != null) {
      String version = (String) embedder.getHeaders().get(Constants.BUNDLE_VERSION);
      sb.append('/').append(version);
    }
    sb.append(')');

    return sb.toString();
  }

  private synchronized String getVersion(Bundle bundle) {
    if(mavenVersion != null) {
      return mavenVersion;
    }
    initClasspath(bundle);
    try {
      String mavenCoreJarPath = null;
      for(String path : CLASSPATH) {
        if(path.contains("maven-core") && path.endsWith(".jar")) {
          mavenCoreJarPath = path;
          break;
        }
      }

      if(mavenCoreJarPath == null) {
        throw new RuntimeException("Could not find maven core jar file");
      }

      ZipFile zip = new ZipFile(mavenCoreJarPath);
      try {
        ZipEntry zipEntry = zip.getEntry("META-INF/maven/org.apache.maven/maven-core/pom.properties"); //$NON-NLS-1$
        if(zipEntry != null) {
          Properties pomProperties = new Properties();
          pomProperties.load(zip.getInputStream(zipEntry));

          String version = pomProperties.getProperty("version"); //$NON-NLS-1$
          if(version != null) {
            mavenVersion = version;
            return mavenVersion;
          }
        }
      } finally {
        zip.close();
      }
    } catch(Exception e) {
      log.warn("Could not determine embedded maven version", e);
    }

    return Messages.MavenEmbeddedRuntime_unknown;
  }

  public String getVersion() {
    Bundle bundle = findMavenEmbedderBundle();
    return getVersion(bundle);
  }
}
