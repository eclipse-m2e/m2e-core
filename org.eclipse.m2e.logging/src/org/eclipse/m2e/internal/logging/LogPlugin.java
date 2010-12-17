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
package org.eclipse.m2e.internal.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogPlugin extends Plugin {

	private static final String ID = "org.eclipse.m2e.logging";

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		configureLogger(context);
	}

	private void configureLogger(BundleContext context) {
		if (System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) != null) {
			return;
		}

		File stateDir = getStateLocation().toFile();

		File configFile = new File(stateDir, "logback.xml");

		if (!configFile.isFile()) {
			//Copy the config file
			try {
				InputStream is = context.getBundle().getEntry("defaultLogbackConfiguration/logback.xml").openStream();
				try {
					configFile.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(configFile);
					try {
						for (byte[] buffer = new byte[1024 * 4];;) {
							int n = is.read(buffer);
							if (n < 0) {
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
				
				loadConfiguration(configFile.toURL());
			} catch (Exception e) {
				getLog().log( new Status(IStatus.WARNING, ID, "Exception while setting up logging.", e));
				return;
			}
		}
	}

	private void loadConfiguration(URL configFile) throws JoranException {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();

		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(lc);
		configurator.doConfigure(configFile);

		StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

}
