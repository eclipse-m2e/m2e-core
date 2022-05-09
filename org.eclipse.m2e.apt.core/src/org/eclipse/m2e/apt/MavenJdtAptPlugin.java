/*******************************************************************************
 * Copyright (c) 2011 Knowledge Computing Corp.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Karl M. Davis (Knowledge Computing Corp.) - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt;

import org.osgi.framework.BundleActivator;

import org.eclipse.m2e.apt.internal.AbstractAptProjectConfigurator;
import org.eclipse.m2e.apt.internal.IMavenAptConstants;
import org.eclipse.m2e.apt.internal.preferences.PreferencesManager;
import org.eclipse.m2e.apt.preferences.IPreferencesManager;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;


/**
 * This is the {@link BundleActivator} for the Eclipse plugin providing the {@link AbstractAptProjectConfigurator}
 * {@link AbstractProjectConfigurator} implementation.
 */
public class MavenJdtAptPlugin {

  public static final String PLUGIN_ID = IMavenAptConstants.PLUGIN_ID;


  private static IPreferencesManager preferencesManager = new PreferencesManager();

  public static IPreferencesManager getPreferencesManager() {
    return  preferencesManager;
  }
}
