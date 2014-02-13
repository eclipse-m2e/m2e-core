/*************************************************************************************
 * Copyright (c) 2011-2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fred Bricon / JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.eclipse.m2e.profiles.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author Fred Bricon 
 * @since 1.5.0
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.m2e.profiles.ui.internal.messages"; //$NON-NLS-1$

	public static String ProfileManager_Updating_maven_profiles;
	public static String ProfileSelectionHandler_Loading_maven_profiles;
	public static String ProfileSelectionHandler_Maven_Builder_still_processing;
	public static String ProfileSelectionHandler_multiple_definitions;
	public static String ProfileSelectionHandler_Select_some_maven_projects;
	public static String ProfileSelectionHandler_Unable_to_open_profile_dialog;
	public static String SelectProfilesDialog_autoactivated;
	public static String SelectProfilesDialog_Activate_menu;
	public static String SelectProfilesDialog_Active_Profiles_for_Project;
	public static String SelectProfilesDialog_Available_profiles;
	public static String SelectProfilesDialog_Warning_Common_profiles;
	public static String SelectProfilesDialog_deactivated;
	public static String SelectProfilesDialog_Deactivate_menu;
	public static String SelectProfilesDialog_DeselectAll;
	public static String SelectProfilesDialog_Force_update;
	public static String SelectProfilesDialog_Maven_profile_selection;
	public static String SelectProfilesDialog_Move_Down;
	public static String SelectProfilesDialog_Move_Up;
	public static String SelectProfilesDialog_No_Common_Profiles;
	public static String SelectProfilesDialog_Offline;
	public static String SelectProfilesDialog_Profile_id_header;
	public static String SelectProfilesDialog_Profile_source_header;
	public static String SelectProfilesDialog_Project_has_no_available_profiles;
	public static String SelectProfilesDialog_Read_Only_profiles;
	public static String SelectProfilesDialog_Select_active_profiles_for_selected_projects;
	public static String SelectProfilesDialog_Select_Maven_profiles;
	public static String SelectProfilesDialog_Select_the_active_Maven_profiles;
	public static String SelectProfilesDialog_SelectAll;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
