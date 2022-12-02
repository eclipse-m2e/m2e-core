/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.binaryproject.internal;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.jobs.MavenJob;

public abstract class AbstractBinaryProjectsImportJob extends MavenJob {

	protected AbstractBinaryProjectsImportJob() {
		super("Import binary projects");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Collection<ArtifactKey> artifacts = getArtifactKeys(monitor);
		for (ArtifactKey a : artifacts) {
			try {
				BinaryProjectPlugin.create(a.groupId(), a.artifactId(), a.version(), null, monitor);
			} catch (CoreException e) {
			}
		}
		return Status.OK_STATUS;
	}

	protected abstract Collection<ArtifactKey> getArtifactKeys(IProgressMonitor monitor);

}
