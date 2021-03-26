/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.equinox;

/**
 * Facade that provides Equinox development classpath information.
 *
 * @since 1.5
 */
@SuppressWarnings("restriction")
public class DevClassPathHelper {

  public static String[] getDevClassPath(String bundleSymbolicName) {
    return org.eclipse.core.internal.runtime.DevClassPathHelper.getDevClassPath(bundleSymbolicName);
  }

  public static boolean inDevelopmentMode() {
    return org.eclipse.core.internal.runtime.DevClassPathHelper.inDevelopmentMode();
  }
}
