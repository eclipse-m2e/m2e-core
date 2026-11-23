/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.apt.ui.internal;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {

  private static final String BUNDLE_NAME = "org.eclipse.m2e.apt.ui.internal.messages"; //$NON-NLS-1$

  public static String WorkspaceProcessorQuickFix_description;

  public static String WorkspaceProcessorQuickFix_failure;

  public static String WorkspaceProcessorQuickFix_job;

  public static String WorkspaceProcessorQuickFix_label;

  public static String WorkspaceProcessorQuickFix_missing_project;

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
