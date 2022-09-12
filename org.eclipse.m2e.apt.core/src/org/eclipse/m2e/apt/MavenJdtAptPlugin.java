/*******************************************************************************
 * Copyright (c) 2011 Knowledge Computing Corp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
