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
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.classworlds.launcher.ConfigurationHandler;
import org.codehaus.plexus.classworlds.launcher.ConfigurationParser;
import org.codehaus.plexus.util.DirectoryScanner;

import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.embedder.MavenRuntime;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;


/**
 * Maven external runtime using ClassWorlds launcher
 * 
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenExternalRuntime implements MavenRuntime {
  private static final Logger log = LoggerFactory.getLogger(MavenExternalRuntime.class);

  private static final String PROPERTY_MAVEN_HOME = "maven.home"; //$NON-NLS-1$

  private final String location;

  public MavenExternalRuntime(String location) {
    this.location = location;
  }

  public boolean isEditable() {
    return true;
  }

  public boolean isAvailable() {
    return new File(location, "bin").exists() && getLauncherClasspath() != null; //$NON-NLS-1$
  }

  public String getLocation() {
    return location;
  }

  public String getSettings() {
    return location + File.separator + "conf" + File.separator + "settings.xml"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public String getMainTypeName() {
    return "org.codehaus.classworlds.Launcher"; //$NON-NLS-1$
  }

  private File getLauncherConfigurationFile() {
    return new File(location, "bin/m2.conf"); //$NON-NLS-1$
  }

  public void createLauncherConfiguration(final IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException {

    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    collector.addArchiveEntry(getLauncherClasspath());

    ConfigurationHandler handler = new ConfigurationHandler() {
      public void addImportFrom(String relamName, String importSpec) {
        throw new UnsupportedOperationException(Messages.MavenExternalRuntime_exc_unsupported);
      }

      public void addLoadFile(File file) {
        try {
          collector.addArchiveEntry(file.getAbsolutePath());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }

      public void addLoadURL(URL url) {
        try {
          collector.addArchiveEntry(url.toExternalForm());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }

      public void addRealm(String realmName) {
        collector.addRealm(realmName);
      }

      public void setAppMain(String mainClassName, String mainRealmName) {
        collector.setMainType(mainClassName, mainRealmName);
      }
    };

    Properties properties = new Properties();
    properties.put(PROPERTY_MAVEN_HOME, location);

    ConfigurationParser parser = new ConfigurationParser(handler, properties);

    try {
      FileInputStream is = new FileInputStream(getLauncherConfigurationFile());
      try {
        parser.parse(is);
      } finally {
        is.close();
      }
    } catch(Exception e) {
      if(e instanceof ExceptionWrapper && e.getCause() instanceof CoreException) {
        throw (CoreException) e.getCause();
      }
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1,
          Messages.MavenExternalRuntime_error_cannot_parse, e));
    }

    // XXX show error dialog and fail launch
  }

  public boolean equals(Object o) {
    if(o instanceof MavenExternalRuntime) {
      return location.equals(((MavenExternalRuntime) o).location);
    }
    return false;
  }

  public int hashCode() {
    return location.hashCode();
  }

  public String toString() {
    return "External" + " " + location + " (" + getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  private static class ExceptionWrapper extends RuntimeException {
    private static final long serialVersionUID = 8815818826909815028L;

    public ExceptionWrapper(Exception cause) {
      super(cause);
    }
  }

  private String getLauncherClasspath() {
    File mavenHome = new File(location);
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(mavenHome);
    ds.setIncludes(new String[] {"core/boot/classworlds*.jar", // 2.0.4 //$NON-NLS-1$
        "boot/classworlds*.jar", // 2.0.7 //$NON-NLS-1$
        "boot/plexus-classworlds*.jar", // 2.1 as of 2008-03-27 //$NON-NLS-1$
    });
    ds.scan();
    String[] includedFiles = ds.getIncludedFiles();

    if(includedFiles.length == 1) {
      return new File(mavenHome, includedFiles[0]).getAbsolutePath();
    }

    return null;
  }

  public String getVersion() {

    class VersionHandler implements ConfigurationHandler {
      File mavenCore;

      File uber;

      public void addImportFrom(String relamName, String importSpec) {
      }

      public void addLoadFile(File file) {
        if(file.getName().contains("maven-core")) { //$NON-NLS-1$
          mavenCore = file;
        } else if(file.getName().endsWith("uber.jar")) { //$NON-NLS-1$
          uber = file;
        }
      }

      public void addLoadURL(URL url) {
      }

      public void addRealm(String realmName) {
      }

      public void setAppMain(String mainClassName, String mainRealmName) {
      }
    }
    ;
    VersionHandler handler = new VersionHandler();

    Properties properties = new Properties();
    properties.put(PROPERTY_MAVEN_HOME, location);

    ConfigurationParser parser = new ConfigurationParser(handler, properties);

    try {
      FileInputStream is = new FileInputStream(getLauncherConfigurationFile());
      try {
        parser.parse(is);
      } finally {
        is.close();
      }

      ZipFile zip = null;
      if(handler.mavenCore != null) {
        zip = new ZipFile(handler.mavenCore);
      } else if(handler.uber != null) {
        zip = new ZipFile(handler.uber);
      }
      if(zip != null) {
        try {
          ZipEntry zipEntry = zip.getEntry("META-INF/maven/org.apache.maven/maven-core/pom.properties"); //$NON-NLS-1$
          if(zipEntry != null) {
            Properties pomProperties = new Properties();
            pomProperties.load(zip.getInputStream(zipEntry));

            String version = pomProperties.getProperty("version"); //$NON-NLS-1$
            if(version != null) {
              return version;
            }
          }
        } finally {
          zip.close();
        }
      }
    } catch(Exception e) {
      // most likely a bad location, but who knows
      log.error("Could not parse classwords configuration file", e);
    }

    return Messages.MavenExternalRuntime_unknown;
  }
}
