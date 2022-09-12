/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.apt.internal;

import org.eclipse.osgi.util.NLS;


/**
 * Messages
 *
 * @author patrick
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.apt.internal.messages"; //$NON-NLS-1$

  public static String ProjectUtils_error_invalid_option_name;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
