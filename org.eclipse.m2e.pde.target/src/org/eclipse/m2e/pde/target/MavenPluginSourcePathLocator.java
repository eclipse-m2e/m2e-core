/*******************************************************************************
 * Copyright (c) 2022, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenArtifactIdentifier;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;

/**
 * Look up sources of plugins in maven
 *
 */
public class MavenPluginSourcePathLocator implements IPluginSourcePathLocator {

	@Override
	public IPath locateSource(IPluginBase plugin) {
		String installLocation = plugin.getModel().getInstallLocation();
		if (installLocation != null) {
			File file = new File(installLocation);
			if (file.isFile()) {
				String ext = FilenameUtils.getExtension(installLocation);
				String baseName = FilenameUtils.getBaseName(installLocation);
				File localFile = new File(file.getParentFile(), baseName + "-sources." + ext);
				if (localFile.isFile()) {
					return IPath.fromOSString(localFile.getAbsolutePath());
				}
				ArtifactKey artifact = aquireFromTargetState(file);
				if (artifact == null) {
					Collection<ArtifactKey> identify = MavenArtifactIdentifier.identify(file);
					if (identify.size() == 1) {
						artifact = identify.iterator().next();
					}
				}
				java.nio.file.Path location = MavenArtifactIdentifier.resolveSourceLocation(artifact, null);
				if (location != null) {
					return IPath.fromOSString(location.toFile().getAbsolutePath());
				}
			}
		}
		return null;
	}

	private ArtifactKey aquireFromTargetState(File file) {
		@SuppressWarnings("restriction")
		ITargetPlatformService platformService = org.eclipse.pde.internal.core.target.TargetPlatformService
				.getDefault();
		try {
			ITargetDefinition targetDefinition = platformService.getWorkspaceTargetDefinition();
			if (targetDefinition != null) {
				for (ITargetLocation location : targetDefinition.getTargetLocations()) {
					if (location instanceof MavenTargetLocation targetLocation) {
						Artifact lookupArtifact = targetLocation.lookupArtifact(file);
						if (lookupArtifact != null) {
							return new ArtifactKey(lookupArtifact);
						}
					}
				}
			}
		} catch (CoreException e) {
			// target might not be ready...
		}
		return null;
	}

}
