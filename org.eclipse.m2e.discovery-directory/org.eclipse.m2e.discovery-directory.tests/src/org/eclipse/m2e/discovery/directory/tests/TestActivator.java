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

package org.eclipse.m2e.discovery.directory.tests;

import org.osgi.framework.BundleContext;

import org.eclipse.core.runtime.Plugin;


public class TestActivator extends Plugin {
  private static TestActivator instance;

  public void start(BundleContext context) throws Exception {
    super.start(context);
    instance = this;
  }

  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    instance = null;
  }

  /**
   * @return Returns the instance.
   */
  public static TestActivator getDefault() {
    return instance;
  }
}
