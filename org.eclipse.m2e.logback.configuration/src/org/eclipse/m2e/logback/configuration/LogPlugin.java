/*******************************************************************************
 * Copyright (c) 2010, 2021 Sonatype, Inc.
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

package org.eclipse.m2e.logback.configuration;

import static ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;


public class LogPlugin extends Plugin {
  private static final String PLUGIN_ID = "org.eclipse.m2e.logback.configuration"; //$NON-NLS-1$

  private static final String RESOURCES_PLUGIN_ID = "org.eclipse.core.resources"; //$NON-NLS-1$

  // This has to match the log directory in defaultLogbackConfiguration/logback.xml
  public static final String PROPERTY_LOG_DIRECTORY = "org.eclipse.m2e.log.dir"; //$NON-NLS-1$

  private boolean isConfigured;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    if(System.getProperty(CONFIG_FILE_PROPERTY) != null) {
      // The standard logback config file property is set - don't force our configuration
      systemOut(CONFIG_FILE_PROPERTY + "=" + System.getProperty(CONFIG_FILE_PROPERTY)); //$NON-NLS-1$
      return;
    }

    // Bug 337167: Configuring Logback requires the state-location. If not yet initialized it will be initialized to the default value, 
    // but this prevents the workspace-chooser dialog to show up in a stand-alone Eclipse-product. Therefore we have to wait until the resources plug-in has started
    if(!isStateLocationInitialized()) {
      systemOut("Activated before the state location was initialized. Retry after the state location is initialized."); //$NON-NLS-1$
      runConditionally(this::configureLogback, LogPlugin::isStateLocationInitialized, "logback configurator timer");
    } else {
      configureLogback();
    }
  }

  private synchronized void configureLogback() {
    if(isConfigured) {
      systemOut("Logback was configured already"); //$NON-NLS-1$
      return;
    }

    try {
      File stateDir = getStateLocation().toFile();
      File configFile = new File(stateDir, "logback." + getBundle().getVersion() + ".xml"); //$NON-NLS-1$  //$NON-NLS-2$
      systemOut("Logback config file: " + configFile.getAbsolutePath()); //$NON-NLS-1$

      if(!configFile.isFile()) {
        // Copy the default config file to the actual config file
        try (InputStream is = getBundle().getEntry("defaultLogbackConfiguration/logback.xml").openStream()) { //$NON-NLS-1$
          configFile.getParentFile().mkdirs();
          Files.copy(is, configFile.toPath());
        }
      }
      if(System.getProperty(PROPERTY_LOG_DIRECTORY, "").length() <= 0) { //$NON-NLS-1$
        System.setProperty(PROPERTY_LOG_DIRECTORY, stateDir.getAbsolutePath());
      }

      loadConfiguration(configFile.toURI().toURL());

      isConfigured = true;
    } catch(Exception e) {
      getLog().log(Status.warning("Exception while setting up logging:" + e.getMessage(), e)); //$NON-NLS-1$
    }
  }

  public static void loadConfiguration(URL configFile) throws JoranException {
    LoggerContext lc = getLoggerContext();
    if(lc == null) {
      return;
    }
    systemOut("Initializing logback"); //$NON-NLS-1$
    lc.reset();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    configurator.doConfigure(configFile);

    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

    logJavaProperties(LoggerFactory.getLogger(LogPlugin.class));
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

  private static LoggerContext getLoggerContext() {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

    for(int i = 0; loggerFactory instanceof SubstituteLoggerFactory && i < 100; i++ ) {
      // slf4j is initialization phase
      systemOut("SLF4J logger factory class: " + loggerFactory.getClass().getName()); //$NON-NLS-1$
      try {
        Thread.sleep(50);
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt(); // ignore but re-interrupt
      }
      loggerFactory = LoggerFactory.getILoggerFactory();
    }

    if(loggerFactory instanceof LoggerContext) {
      return (LoggerContext) loggerFactory;
    }
    String msg = loggerFactory == null ? // Is null possible?
        "SLF4J logger factory is null" //$NON-NLS-1$
        : "SLF4J logger factory is not an instance of LoggerContext: " + loggerFactory.getClass().getName(); // $NON-NLS-1$
    systemErr(msg);
    return null;
  }

  public static void logJavaProperties(Logger log) {
    Properties javaProperties = System.getProperties();
    SortedMap<String, String> sortedProperties = new TreeMap<>();
    for(String key : javaProperties.stringPropertyNames()) {
      sortedProperties.put(key, javaProperties.getProperty(key));
    }
    log.debug("Java properties (ordered by property name):"); //$NON-NLS-1$
    sortedProperties.forEach((k, v) -> log.debug("   {}={}", k, v));
  }

  private static void systemOut(String message) {
    System.out.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$
  }

  private static void systemErr(String message) {
    System.err.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$
  }

}
