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
