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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.eclipse.epp.usagedata.internal.gathering.services.UsageDataService;


@SuppressWarnings("restriction")
public class MavenUsageDataCollectorActivator implements BundleActivator {

  private static MavenUsageDataCollectorActivator instance;

  private UsageDataService usageDataService;

  public void start(BundleContext context) throws Exception {
    instance = this;
  }

  public void stop(BundleContext context) throws Exception {
    instance = null;
  }

  public static MavenUsageDataCollectorActivator getDefault() {
    return instance;
  }

  public synchronized void recordEvent(String what, String kind, String description, String bundleId, String bundleVersion) {
    if(usageDataService != null) {
      usageDataService.recordEvent(what, kind, description, bundleId, bundleVersion);
    }
  }

  public synchronized void setUsageDataService(UsageDataService usageDataService) {
    this.usageDataService = usageDataService;
  }
}
