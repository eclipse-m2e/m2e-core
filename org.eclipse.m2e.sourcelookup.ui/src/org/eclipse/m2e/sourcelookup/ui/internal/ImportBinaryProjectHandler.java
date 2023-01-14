/*******************************************************************************
 * Copyright (c) 2011-2023 Igor Fedorenko
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
package org.eclipse.m2e.sourcelookup.ui.internal;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookupParticipant;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.binaryproject.internal.AbstractBinaryProjectsImportJob;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.MavenArtifactIdentifier;
import org.eclipse.ui.handlers.HandlerUtil;

public class ImportBinaryProjectHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

		if (selection instanceof IStructuredSelection structuredSelection && !selection.isEmpty()) {
			try {
				importBinaryProjects(structuredSelection.getFirstElement());
			} catch (DebugException e) {
				throw new ExecutionException("Could not import binary project", e);
			}
		}

		return null;
	}

	public static void importBinaryProjects(final Object debugElement) throws DebugException {

		File location = AdvancedSourceLookup.getClassesLocation(debugElement);
		if (location == null) {
			return;
		}
		Job job = new AbstractBinaryProjectsImportJob() {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = super.run(monitor);

				if (status.isOK()) {
					AdvancedSourceLookupParticipant sourceLookup = AdvancedSourceLookupParticipant
							.getSourceLookup(debugElement);
					try {
						sourceLookup.getSourceContainer(debugElement, true, monitor);
					} catch (CoreException e) {
						status = e.getStatus();
					}
				}
				return status;
			}

			@Override
			protected Collection<ArtifactKey> getArtifactKeys(IProgressMonitor monitor) {
				return MavenArtifactIdentifier.identify(location);
			}
		};
		job.setUser(true);
		job.schedule();
	}
}
