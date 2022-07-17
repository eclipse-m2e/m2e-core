/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.mavenarchiver.internal;

/**
 * Defines plugin-wide constants for the mavenarchiver plugin.
 * 
 * @author Fred Bricon
 */
public class MavenArchiverConstants {

	@SuppressWarnings("restriction")
	public static final String MAVENARCHIVER_MARKER_ID = org.eclipse.m2e.core.internal.IMavenConstants.MARKER_ID
			+ ".mavenarchiver";

	public static final String MAVENARCHIVER_MARKER_ERROR = MAVENARCHIVER_MARKER_ID + ".error";

	private MavenArchiverConstants() {
		// prevent instantiation.
	}
}
