/********************************************************************************
 * Copyright (c) 2026 Patrick Ziegler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *	 Patrick Ziegler - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.pde.ui.target.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String MavenRuleSetPreferencePage_RuleSet;
	public static String RuleSetViewer_Artifact;
	public static String RuleSetViewer_IgnoredVersions;
	public static String RuleSetViewer_Rules;
	public static String RuleSetViewer_Type;
	public static String RuleSetViewer_Value;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
