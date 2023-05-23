/*******************************************************************************
 * Copyright (c) 2010, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Hannes Wellmann - Merge m2e.logback.configuration into .appender
 *******************************************************************************/

package org.eclipse.m2e.logback.configuration;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;


public class M2ELogbackConfigurator extends BasicConfigurator implements Configurator {

  private static final ILog LOG = Platform.getLog(M2ELogbackConfigurator.class);

  private static final String RESOURCES_PLUGIN_ID = "org.eclipse.core.resources"; //$NON-NLS-1$

  // This has to match the log directory in defaultLogbackConfiguration/logback.xml
  private static final String PROPERTY_LOG_DIRECTORY = "org.eclipse.m2e.log.dir"; //$NON-NLS-1$

  // This has to match the log directory in defaultLogbackConfiguration/logback.xml
  private static final String PROPERTY_LOG_CONSOLE_THRESHOLD = "org.eclipse.m2e.log.console.threshold"; //$NON-NLS-1$

  @Override
  public void configure(LoggerContext lc) {
    // Bug 337167: Configuring Logback requires the state-location. If not yet initialized it will be initialized to the default value,
    // but this prevents the workspace-chooser dialog to show up in a stand-alone Eclipse-product. Therefore we have to wait until the resources plug-in has started.
    // This happens if a Plug-in that uses SLF4J is started before the workspace has been selected.
    if(!isStateLocationInitialized()) {
      super.configure(lc); // Preliminary apply default configuration
      LOG.info("Activated before the state location was initialized. Retry after the state location is initialized."); //$NON-NLS-1$

      runConditionally(() -> configureLogback(lc), M2ELogbackConfigurator::isStateLocationInitialized,
          "logback configurator timer");
    } else {
      configureLogback(lc);
    }
  }

  private synchronized void configureLogback(LoggerContext lc) {
    try {
      Bundle bundle = Platform.getBundle("org.eclipse.m2e.logback"); // This is a fragment -> FrameworkUtil.getBundle() returns host
      Path stateDir = Platform.getStateLocation(bundle).toFile().toPath();
      Path configFile = stateDir.resolve("logback." + bundle.getVersion() + ".xml"); //$NON-NLS-1$  //$NON-NLS-2$
      LOG.info("Logback config file: " + configFile.toAbsolutePath()); //$NON-NLS-1$

      if(!Files.isRegularFile(configFile)) {
        // Copy the default config file to the actual config file, to allow user adjustments
        try (InputStream is = bundle.getEntry("defaultLogbackConfiguration/logback.xml").openStream()) { //$NON-NLS-1$
          Files.createDirectories(configFile.getParent());
          Files.copy(is, configFile);
        }
      }
      if(System.getProperty(PROPERTY_LOG_DIRECTORY, "").length() <= 0) { //$NON-NLS-1$
        System.setProperty(PROPERTY_LOG_DIRECTORY, stateDir.toAbsolutePath().toString());
      }
      if(System.getProperty(PROPERTY_LOG_CONSOLE_THRESHOLD, "").length() <= 0) { //$NON-NLS-1$
        if(isConsoleLogEnable()) {
          System.setProperty(PROPERTY_LOG_CONSOLE_THRESHOLD, Level.DEBUG.levelStr);
        }
      }
      loadConfiguration(lc, configFile.toUri().toURL());

      //Delete old logs in legacy logback plug-in's state location. Can sum up to 1GB of disk-space.
      // TODO: can be removed when some time has passed and it is unlikely old workspaces that need clean-up are used.
      Path legacyLogbackState = stateDir.resolveSibling("org.eclipse.m2e.logback.configuration");
      if(Files.isDirectory(legacyLogbackState)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(legacyLogbackState)) {
          for(Path path : stream) {
            if(Files.isRegularFile(path)) {
              Files.delete(path);
            }
          }
        }
        Files.delete(legacyLogbackState);
      }
    } catch(Exception e) {
      LOG.log(Status.warning("Exception while setting up logging:" + e.getMessage(), e));
    }
  }

  private boolean isConsoleLogEnable() {
    ServiceTracker<EnvironmentInfo, Object> tracker = openServiceTracker(EnvironmentInfo.class);
    try {
      EnvironmentInfo environmentInfo = (EnvironmentInfo) tracker.getService();
      return environmentInfo != null && "true".equals(environmentInfo.getProperty("eclipse.consoleLog")); //$NON-NLS-1$
    } finally {
      tracker.close();
    }
  }

  private static void loadConfiguration(LoggerContext lc, URL configFile) throws JoranException {
    lc.reset();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    configurator.doConfigure(configFile);

    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

    applyDebugLogLevels(lc);

    logJavaProperties(LoggerFactory.getLogger(M2ELogbackConfigurator.class));
  }

  private static void applyDebugLogLevels(LoggerContext lc) {
    ServiceTracker<DebugOptions, Object> tracker = openServiceTracker(DebugOptions.class);
    try {
      DebugOptions debugOptions = (DebugOptions) tracker.getService();
      if(debugOptions != null) {
        Map<String, String> options = debugOptions.getOptions();
        for(Entry<String, String> entry : options.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if(key.endsWith("/debugLog") && "true".equals(value)) {
            lc.getLogger(key.replace("/debugLog", "")).setLevel(Level.DEBUG);
          }
        }
      }
    } finally {
      tracker.close();
    }
  }

  private static <T> ServiceTracker<T, Object> openServiceTracker(Class<T> serviceClass) {
    Bundle bundle = Platform.getBundle("org.eclipse.m2e.core"); // fragments don't have a BundleContext
    ServiceTracker<T, Object> tracker = new ServiceTracker<>(bundle.getBundleContext(), serviceClass, null);
    tracker.open();
    return tracker;
  }

  // --- utility methods ---

  private static boolean isStateLocationInitialized() {
    if(!Platform.isRunning()) {
      return false;
    }
    Bundle resourcesBundle = Platform.getBundle(RESOURCES_PLUGIN_ID);
    return resourcesBundle != null && resourcesBundle.getState() == Bundle.ACTIVE;
  }

  private static void runConditionally(Runnable action, BooleanSupplier condition, String name) {
    Timer timer = new Timer(name);
    timer.schedule(new TimerTask() {
      public void run() {
        if(condition.getAsBoolean()) {
          timer.cancel();
          action.run();
        }
      }
    }, 0 /*delay*/, 50 /*period*/);
  }

  private static void logJavaProperties(Logger log) {
    Properties javaProperties = System.getProperties();
    SortedMap<String, String> sortedProperties = new TreeMap<>();
    for(String key : javaProperties.stringPropertyNames()) {
      sortedProperties.put(key, javaProperties.getProperty(key));
    }
    log.debug("Java properties (ordered by property name):"); //$NON-NLS-1$
    sortedProperties.forEach((k, v) -> log.debug("   {}={}", k, v));
  }

}
