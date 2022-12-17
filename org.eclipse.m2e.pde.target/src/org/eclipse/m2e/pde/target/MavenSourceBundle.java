/*******************************************************************************
 * Copyright (c) 2020, 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.core.target.TargetBundle;

public class MavenSourceBundle extends TargetBundle {

	public MavenSourceBundle(BundleInfo sourceTarget, Artifact artifact) throws Exception {
		this.fSourceTarget = sourceTarget;
		fInfo.setSymbolicName(sourceTarget.getSymbolicName() + ".source");
		fInfo.setVersion(sourceTarget.getVersion());
		File sourceFile = artifact.getFile();
		fInfo.setLocation(sourceFile.toURI());
	}

}
