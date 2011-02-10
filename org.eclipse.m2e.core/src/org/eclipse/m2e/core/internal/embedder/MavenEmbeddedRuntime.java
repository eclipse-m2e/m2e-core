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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

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

  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor) throws CoreException {
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

  private static synchronized void initClasspath(Bundle bundle) {
    if(CLASSPATH == null) {
      List<String> cp = new ArrayList<String>();
      List<String> lcp = new ArrayList<String>();

      @SuppressWarnings("unchecked")
      Enumeration<URL> entries = bundle.findEntries("/", "*", true); //$NON-NLS-1$ //$NON-NLS-2$
      while(entries.hasMoreElements()) {
        URL url = entries.nextElement();
        String path = url.getPath();
        if(path.endsWith(".jar") || path.endsWith("bin/")) { //$NON-NLS-1$ //$NON-NLS-2$
          try {
            String file = FileLocator.toFileURL(url).getFile();
            if (file.contains("plexus-classworlds")) { //$NON-NLS-1$
              lcp.add(file);
            } else {
              cp.add(file);
            }
          } catch(IOException ex) {
            log.error("Error adding classpath entry " + url.toString(), ex);
          }
        }
      }

      CLASSPATH = cp.toArray(new String[cp.size()]);
      LAUNCHER_CLASSPATH = lcp.toArray(new String[lcp.size()]);
    }
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
    if (embedder != null) {
      String version = (String) embedder.getHeaders().get(Constants.BUNDLE_VERSION);
      sb.append('/').append(version);
    }
    sb.append(')');

    return  sb.toString();
  }

  private static synchronized String getVersion(Bundle bundle) {
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
      log.error("Could not determine embedded maven version", e);
    }

    return Messages.MavenEmbeddedRuntime_unknown;
  }

  public String getVersion() {
    Bundle bundle = findMavenEmbedderBundle();
    return getVersion(bundle);
  }
}