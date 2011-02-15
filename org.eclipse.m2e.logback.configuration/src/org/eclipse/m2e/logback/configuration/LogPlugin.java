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

import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.datalocation.Location;

public class LogPlugin extends Plugin {
  private static final String PLUGIN_ID = "org.eclipse.m2e.logback.configuration"; //$NON-NLS-1$

  // This has to match the log directory in defaultLogbackConfiguration/logback.xml
  public static final String PROPERTY_LOG_DIRECTORY = "org.eclipse.m2e.log.dir"; //$NON-NLS-1$

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    configureLogger(context);

    LogHelper.logJavaProperties(LoggerFactory.getLogger(LogPlugin.class));
  }

  private void systemOut(String message) {
    System.out.println(PLUGIN_ID + ": " + message);
  }

  private void configureLogger(BundleContext context) {
    if(System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) != null) {
      systemOut(ContextInitializer.CONFIG_FILE_PROPERTY + "="
          + System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY));
      return;
    }
    Location instanceLocation = Platform.getInstanceLocation();
    if(!instanceLocation.isSet()) {
      new Exception("The " + PLUGIN_ID + " bundle was activated before the platform instance location was initialized.");
      return;
    }
    File stateDir = getStateLocation().toFile();

    File configFile = new File(stateDir, "logback." + context.getBundle().getVersion().toString() + ".xml");
    systemOut("Logback config file: " + configFile.getAbsolutePath());

    try {
      if(!configFile.isFile()) {
        // Copy the default config file to the actual config file
        InputStream is = context.getBundle().getEntry("defaultLogbackConfiguration/logback.xml").openStream();
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

      if(System.getProperty(PROPERTY_LOG_DIRECTORY, "").length() <= 0) {
        System.setProperty(PROPERTY_LOG_DIRECTORY, stateDir.getAbsolutePath());
      }
      loadConfiguration(configFile.toURL());
    } catch(Exception e) {
      e.printStackTrace();
      getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, "Exception while setting up logging:" + e.getMessage(), e));
      return;
    }
  }

  public static void loadConfiguration(URL configFile) throws JoranException {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    lc.reset();

    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    configurator.doConfigure(configFile);

    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
  }
}
