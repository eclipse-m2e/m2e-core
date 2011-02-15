/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.internal.udc;

import org.eclipse.epp.usagedata.internal.gathering.monitors.UsageMonitor;
import org.eclipse.epp.usagedata.internal.gathering.services.UsageDataService;

/*
 * Monitor for Usage Data Collector
 */
@SuppressWarnings("restriction")
public class PomImportMonitor implements UsageMonitor {

	private UsageDataService usageDataService;

	public PomImportMonitor() {
		Activator.getDefault().setMonitor(this);
	}


	public void startMonitoring(UsageDataService usageDataService) {
		this.usageDataService = usageDataService;
	}

	public void stopMonitoring() {
		usageDataService = null;
	}

	void recordEvent(String what, String kind, String description, String bundleId, String bundleVersion) {
		usageDataService.recordEvent(what, kind, description, bundleId, bundleVersion);
	}
}
