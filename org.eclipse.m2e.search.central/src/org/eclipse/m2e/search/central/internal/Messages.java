/*******************************************************************************
 * Copyright (c) 2018 Sonatype Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.search.central.internal;

import org.eclipse.osgi.util.NLS;

/**
 * @author Matthew Piggott
 * @since 1.16.0
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "com.sonatype.m2e.search.central.internal.messages"; //$NON-NLS-1$

  public static String MavenCentral_UnexpectedError;

  public static String MavenCentral_UnexpectedResponse;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
