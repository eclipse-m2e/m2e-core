/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
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
package org.eclipse.m2e.apt.ui.preferences;

import org.eclipse.osgi.util.NLS;

public class PreferenceMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.apt.ui.preferences.preferenceMessages"; //$NON-NLS-1$
	
	public static String AnnotationProcessingSettingsPage_Use_Project_Settings_Label;
	public static String AnnotationProcessingSettingsPage_Use_Workspace_Settings_Label;
	public static String AnnotationProcessingSettingsPage_Title;
	
	public static String AnnotationProcessingSettingsPage_Disabled_Mode_Label;
	public static String AnnotationProcessingSettingsPage_Jdt_Apt_Mode_Label;

  public static String AnnotationProcessingSettingsPage_Maven_Execution_Mode;
	public static String AnnotationProcessingSettingsPage_Select_Annotation_Processing_Mode;
	
	
  public static String AnnotationProcessingSettingsPage_Other_Options;

  public static String AnnotationProcessingSettingsPage_Disable_APT_Processing;

  public static String AnnotationProcessingSettingsPage_Disable_APT_Processing_Tooltip;
  
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, PreferenceMessages.class);
	}

	private PreferenceMessages() {
	}
}
