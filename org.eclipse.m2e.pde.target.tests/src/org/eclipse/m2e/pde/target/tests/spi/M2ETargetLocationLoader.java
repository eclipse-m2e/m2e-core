/*******************************************************************************
 * Copyright (c) 2023 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - extraction from AbstractMavenTargetTest
 *******************************************************************************/
package org.eclipse.m2e.pde.target.tests.spi;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.pde.target.MavenTargetLocationFactory;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;

public class M2ETargetLocationLoader implements TargetLocationLoader {

	@Override
	public int getPriority() {
		// we use default priority here...
		return 0;
	}

	@Override
	public ITargetLocation resolveMavenTarget(String targetXML, File tempDir) throws Exception {
		// temp dir is not used here, but we probably should configure m2e to use a
		// separate folder for each test to prevent items from one test to drip into
		// another (especially when wrapping is involved or custom repositories)
		@SuppressWarnings("restriction")
		ITargetPlatformService s = org.eclipse.pde.internal.core.PDECore.getDefault()
				.acquireService(ITargetPlatformService.class);
		ITargetDefinition target = s.newTarget();
		ITargetLocation targetLocation = new MavenTargetLocationFactory().getTargetLocation(MAVEN_LOCATION_TYPE,
				targetXML);
		target.setTargetLocations(new ITargetLocation[] { targetLocation });
		targetLocation.resolve(target, new NullProgressMonitor());
		return targetLocation;
	}

}
