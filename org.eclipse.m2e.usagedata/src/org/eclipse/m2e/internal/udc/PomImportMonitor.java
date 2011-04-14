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

  public PomImportMonitor() {
  }

  public void startMonitoring(UsageDataService usageDataService) {
    MavenUsageDataCollectorActivator.getDefault().setUsageDataService(usageDataService);
  }

  public void stopMonitoring() {
    // Null check to avoid NPE in shutdown 
    MavenUsageDataCollectorActivator instance = MavenUsageDataCollectorActivator.getDefault();
    if(instance != null) {
      MavenUsageDataCollectorActivator.getDefault().setUsageDataService(null);
    }
  }
}
