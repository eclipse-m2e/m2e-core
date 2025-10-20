/********************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.maven.compat;

import java.io.File;

import org.apache.maven.cli.internal.ExtensionResolutionException;
import org.apache.maven.cli.internal.extension.model.CoreExtension;
import org.codehaus.plexus.PlexusContainerException;

/**
 * Facade for {@link ExtensionResolutionException} to avoid direct usage that
 * might change in Maven 4. This facade wraps the exception and provides a
 * method to throw it as a PlexusContainerException with appropriate context
 * information.
 */
public class ExtensionResolutionExceptionFacade {

	/**
	 * Throws a PlexusContainerException with information about the failed extension
	 * and the file where it was defined.
	 * 
	 * @param file the extensions file where the failed extension was defined
	 * @throws PlexusContainerException always thrown with details about the failed
	 *                                  extension
	 */
	public static void throwForFile(ExtensionResolutionException exception, File file) throws PlexusContainerException {
		CoreExtension extension = exception.getExtension();
		throw new PlexusContainerException("can't create plexus container because the extension "
				+ extension.getGroupId() + ":" + extension.getArtifactId() + ":" + extension.getVersion()
				+ " can't be loaded (defined in " + file.getAbsolutePath() + ").", exception);
	}
}
