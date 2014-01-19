/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import org.eclipse.osgi.util.NLS;


/**
 * Messages
 * 
 * @author mkleint
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.eclipse.m2e.jdt.ui.internal.messages"; //$NON-NLS-1$

  public static String MavenClasspathContainerPage_control_desc;

  public static String MavenClasspathContainerPage_control_title;

  public static String MavenClasspathContainerPage_link;

  public static String MavenClasspathContainerPage_title;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
