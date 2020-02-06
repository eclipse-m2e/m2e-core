/*******************************************************************************
 * Copyright (c) 2008-2020 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.sonatype.m2e.mavenarchiver.internal;

import org.eclipse.m2e.core.internal.IMavenConstants;

/**
 * Defines plugin-wide constants for the mavenarchiver plugin.
 * 
 * @author Fred Bricon
 */
public class MavenArchiverConstants {

	public static final String MAVENARCHIVER_MARKER_ID = IMavenConstants.MARKER_ID + ".mavenarchiver"; //$NON-NLS-1$

	public static final String MAVENARCHIVER_MARKER_ERROR = MAVENARCHIVER_MARKER_ID + ".error"; //$NON-NLS-1$

	private MavenArchiverConstants() {
		// prevent instantiation.
	}
}
