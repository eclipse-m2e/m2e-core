/********************************************************************************
 * Copyright (c) 2023, 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.slf4j2.provider;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.impl.SimpleLoggerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class MavenSLF4JProvider implements SLF4JServiceProvider {
	// Based on org.slf4j.simple.SimpleServiceProvider from org.slf4j:slf4j-simple

	private ILoggerFactory loggerFactory;
	private final IMarkerFactory markerFactory = new BasicMarkerFactory();
	private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequestedApiVersion() {
		// Declare the version of the maximum SLF4J API this implementation is
		// compatible with. The value of this field is modified with each major release.
		return "2.0.99";
	}

	@Override
	public void initialize() {
		loggerFactory = new SimpleLoggerFactory();
	}

}
