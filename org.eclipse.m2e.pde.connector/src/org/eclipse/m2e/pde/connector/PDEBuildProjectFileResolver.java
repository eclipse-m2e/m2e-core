/********************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.pde.connector;

import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.IBuildProjectFileResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = IBuildProjectFileResolver.class)
public class PDEBuildProjectFileResolver implements IBuildProjectFileResolver {

	private static final Map<String, IPath> POM_NAME_2_PROJECT_FILE = Map.of( //
			".polyglot.META-INF", IPath.forPosix("META-INF/MANIFEST.MF"), //
			".polyglot.feature.xml", IPath.forPosix("feature.xml"));

	@Override
	public IPath resolveProjectFile(String pomFilename) {
		IPath realFile = POM_NAME_2_PROJECT_FILE.get(pomFilename);
		if (realFile != null) {
			return realFile;
		}
		if (pomFilename.startsWith(".polyglot.")
				&& (pomFilename.endsWith(".product") || pomFilename.endsWith(".target"))) {
			return IPath.forPosix(pomFilename.substring(".polyglot.".length()));
		}
		return null;
	}

}
