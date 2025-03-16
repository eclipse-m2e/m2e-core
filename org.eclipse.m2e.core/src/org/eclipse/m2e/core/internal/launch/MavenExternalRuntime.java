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

import static org.eclipse.m2e.core.internal.M2EUtils.copyProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import org.codehaus.plexus.classworlds.ClassWorldException;
import org.codehaus.plexus.classworlds.launcher.ConfigurationException;
import org.codehaus.plexus.classworlds.launcher.ConfigurationHandler;
import org.codehaus.plexus.classworlds.launcher.ConfigurationParser;
import org.codehaus.plexus.util.DirectoryScanner;

import org.eclipse.m2e.core.internal.Messages;


/**
 * Maven external runtime using ClassWorlds launcher
 *
 * @author Eugene Kuleshov
 * @author Igor Fedorenko
 */
public class MavenExternalRuntime extends AbstractMavenRuntime {
  private static final Logger log = LoggerFactory.getLogger(MavenExternalRuntime.class);

  private static final String PROPERTY_MAVEN_HOME = "maven.home"; //$NON-NLS-1$

  private final String location;

  private transient String version;

  public MavenExternalRuntime(String location) {
    super(MavenRuntimeManagerImpl.EXTERNAL);
    this.location = location;
  }

  public MavenExternalRuntime(String name, String location) {
    super(name);
    this.location = location;
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  @Override
  public boolean isAvailable() {
    return new File(getLocation(), "bin").exists() && getLauncherClasspath() != null && isSupportedVersion(); //$NON-NLS-1$
  }

  @Override
  public String getLocation() {
    IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
    try {
      return manager.performStringSubstitution(location);
    } catch(CoreException ex) {
      //if we can't parse the location we need to return the unparsed raw value...
    }
    return location;
  }

  private File getLauncherConfigurationFile() {
    return new File(getLocation(), "bin/m2.conf"); //$NON-NLS-1$
  }

  @Override
  public void createLauncherConfiguration(IMavenLauncherConfiguration collector, IProgressMonitor monitor)
      throws CoreException {

    collector.addRealm(IMavenLauncherConfiguration.LAUNCHER_REALM);
    collector.addArchiveEntry(getLauncherClasspath());

    ConfigurationHandler handler = new ConfigurationHandler() {
      private String mainRealmName;

      @Override
      public void addImportFrom(String relamName, String importSpec) {
        throw new UnsupportedOperationException(Messages.MavenExternalRuntime_exc_unsupported);
      }

      @Override
      public void addLoadFile(File file) {
        try {
          collector.addArchiveEntry(file.getAbsolutePath());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }

      @Override
      public void addLoadURL(URL url) {
        try {
          collector.addArchiveEntry(url.toExternalForm());
        } catch(CoreException ex) {
          throw new ExceptionWrapper(ex);
        }
      }

      @Override
      public void addRealm(String realmName) {
        if(mainRealmName == null) {
          throw new IllegalStateException();
        }
        collector.addRealm(realmName);
        if(mainRealmName.equals(realmName)) {
          try {
            collectExtensions(collector, monitor);
          } catch(CoreException ex) {
            throw new ExceptionWrapper(ex);
          }
        }
      }

      @Override
      public void setAppMain(String mainClassName, String mainRealmName) {
        this.mainRealmName = mainRealmName;
        collector.setMainType(mainClassName, mainRealmName);
      }
    };

    ConfigurationParser parser = new ConfigurationParser(handler, getConfigParserProperties());

    try (FileInputStream is = new FileInputStream(getLauncherConfigurationFile())) {
      parser.parse(is);
    } catch(Exception e) {
      if(e instanceof ExceptionWrapper && e.getCause() instanceof CoreException coreException) {
        throw coreException;
      }
      throw new CoreException(Status.error(Messages.MavenExternalRuntime_error_cannot_parse, e));
    }

    // XXX show error dialog and fail launch
  }

  @Override
  public String toString() {
    return getLocation() + ' ' + getVersion();
  }

  private static class ExceptionWrapper extends RuntimeException {
    private static final long serialVersionUID = 8815818826909815028L;

    public ExceptionWrapper(Exception cause) {
      super(cause);
    }
  }

  private String getLauncherClasspath() {
    File mavenHome = new File(getLocation());
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

  @Override
  public synchronized String getVersion() {
    if(version == null) {
      version = getVersion0();
    }
    return version;
  }

  private String getVersion0() {
    try {
      File zipFile = getVersionedZipFile();
      if(zipFile != null) {
        try (ZipFile zip = new ZipFile(zipFile)) {
          ZipEntry zipEntry = zip.getEntry("META-INF/maven/org.apache.maven/maven-core/pom.properties"); //$NON-NLS-1$
          if(zipEntry != null) {
            Properties pomProperties = new Properties();
            pomProperties.load(zip.getInputStream(zipEntry));

            String versionProperty = pomProperties.getProperty("version"); //$NON-NLS-1$
            if(versionProperty != null) {
              return versionProperty;
            }
          }
        }
      }
    } catch(Exception e) {
      // most likely a bad location, but who knows
      log.error("Could not parse classwords configuration file", e);
    }
    return Messages.MavenExternalRuntime_unknown;
  }

  private File getVersionedZipFile() throws IOException, ClassWorldException, ConfigurationException {
    class VersionHandler implements ConfigurationHandler {
      File mavenCore;

      File uber;

      @Override
      public void addImportFrom(String relamName, String importSpec) {
      }

      @Override
      public void addLoadFile(File file) {
        if(file.getName().contains("maven-core")) { //$NON-NLS-1$
          mavenCore = file;
        } else if(file.getName().endsWith("uber.jar")) { //$NON-NLS-1$
          uber = file;
        }
      }

      @Override
      public void addLoadURL(URL url) {
      }

      @Override
      public void addRealm(String realmName) {
      }

      @Override
      public void setAppMain(String mainClassName, String mainRealmName) {
      }
    }

    VersionHandler handler = new VersionHandler();

    try (FileInputStream is = new FileInputStream(getLauncherConfigurationFile())) {
      new ConfigurationParser(handler, getConfigParserProperties()).parse(is);
    }
    if(handler.mavenCore != null) {
      return handler.mavenCore;
    } else if(handler.uber != null) {
      return handler.uber;
    }
    return null;
  }

  private Properties getConfigParserProperties() {
    Properties properties = new Properties();
    copyProperties(properties, System.getProperties());
    properties.put(PROPERTY_MAVEN_HOME, getLocation());
    properties.put("maven.mainClass", "org.apache.maven.cling.MavenCling"); //required for maven 4
    return properties;
  }

}
