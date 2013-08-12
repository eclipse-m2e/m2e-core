/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.logback.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;


public class LogPlugin extends Plugin {
  private static final String PLUGIN_ID = "org.eclipse.m2e.logback.configuration"; //$NON-NLS-1$

  private static final String RESOURCES_PLUGIN_ID = "org.eclipse.core.resources"; //$NON-NLS-1$

  // This has to match the log directory in defaultLogbackConfiguration/logback.xml
  public static final String PROPERTY_LOG_DIRECTORY = "org.eclipse.m2e.log.dir"; //$NON-NLS-1$

  private BundleContext bundleContext;

  private boolean isConfigured;

  private Timer timer = new Timer("logback configurator timer");

  private TimerTask timerTask = new TimerTask() {
    @SuppressWarnings("synthetic-access")
    public void run() {
      if(!isStateLocationInitialized()) {
        return;
      }

      // The state location was initialized
      timer.cancel();
      configureLogback();
    }
  };

  private boolean isStateLocationInitialized() {
    if(!Platform.isRunning()) {
      return false;
    }

    Bundle resourcesBundle = Platform.getBundle(RESOURCES_PLUGIN_ID);
    if(resourcesBundle == null) {
      return false;
    }

    return resourcesBundle.getState() == Bundle.ACTIVE;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    bundleContext = context;

    if(System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) != null) {
      // The standard logback config file property is set - don't force our configuration
      systemOut(ContextInitializer.CONFIG_FILE_PROPERTY + "=" //$NON-NLS-1$
          + System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY));
      return;
    }

    if(!isStateLocationInitialized()) {
      systemOut("The " + PLUGIN_ID + " bundle was activated before the state location was initialized.  Will retry after the state location is initialized."); //$NON-NLS-1$  //$NON-NLS-2$
      timer.schedule(timerTask, 0 /*delay*/, 50 /*period*/);
    } else {
      configureLogback();
    }
  }

  private static void systemOut(String message) {
    System.out.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$
  }

  private static void systemErr(String message) {
    System.err.println(PLUGIN_ID + ": " + message); //$NON-NLS-1$
  }

  private synchronized void configureLogback() {
    if(isConfigured) {
      systemOut("Logback was configured already"); //$NON-NLS-1$
      return;
    }

    try {
      File stateDir = getStateLocation().toFile();

      File configFile = new File(stateDir, "logback." + bundleContext.getBundle().getVersion().toString() + ".xml"); //$NON-NLS-1$  //$NON-NLS-2$
      systemOut("Logback config file: " + configFile.getAbsolutePath()); //$NON-NLS-1$

      if(!configFile.isFile()) {
        // Copy the default config file to the actual config file
        InputStream is = bundleContext.getBundle().getEntry("defaultLogbackConfiguration/logback.xml").openStream(); //$NON-NLS-1$
        try {
          configFile.getParentFile().mkdirs();
          FileOutputStream fos = new FileOutputStream(configFile);
          try {
            for(byte[] buffer = new byte[1024 * 4];;) {
              int n = is.read(buffer);
              if(n < 0) {
                break;
              }
              fos.write(buffer, 0, n);
            }
          } finally {
            fos.close();
          }
        } finally {
          is.close();
        }
      }

      if(System.getProperty(PROPERTY_LOG_DIRECTORY, "").length() <= 0) { //$NON-NLS-1$
        System.setProperty(PROPERTY_LOG_DIRECTORY, stateDir.getAbsolutePath());
      }
      loadConfiguration(configFile.toURL());

      isConfigured = true;
    } catch(Exception e) {
      e.printStackTrace();
      getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "Exception while setting up logging:" + e.getMessage(), e)); //$NON-NLS-1$
      return;
    }
  }

  public static void loadConfiguration(URL configFile) throws JoranException {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    int i = 0;
    while(loggerFactory instanceof SubstituteLoggerFactory && i < 100) {
      // slf4j is initialization phase
      systemOut("SLF4J logger factory class: " + loggerFactory.getClass().getName()); //$NON-NLS-1$
      try {
        Thread.sleep(50);
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
      i++ ;
      loggerFactory = LoggerFactory.getILoggerFactory();
    }
    if(!(loggerFactory instanceof LoggerContext)) {
      if(loggerFactory == null) {
        // Is it possible?
        systemErr("SLF4J logger factory is null"); //$NON-NLS-1$
        return;
      }
      systemErr("SLF4J logger factory is not an instance of LoggerContext: " //$NON-NLS-1$
          + loggerFactory.getClass().getName());
      return;
    }

    systemOut("Initializing logback"); //$NON-NLS-1$
    LoggerContext lc = (LoggerContext) loggerFactory;
    lc.reset();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    configurator.doConfigure(configFile);

    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

    LogHelper.logJavaProperties(LoggerFactory.getLogger(LogPlugin.class));
  }
}
