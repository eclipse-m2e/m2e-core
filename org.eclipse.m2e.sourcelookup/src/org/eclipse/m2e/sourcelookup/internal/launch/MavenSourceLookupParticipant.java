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
package org.eclipse.m2e.sourcelookup.internal.launch;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookupParticipant;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;

public class MavenSourceLookupParticipant extends AdvancedSourceLookupParticipant
		implements IMavenProjectChangedListener {

	@Override
	public void init(ISourceLookupDirector director) {
		super.init(director);
		MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener(this);
	}

	@Override
	public void dispose() {
		MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener(this);
		super.dispose();
	}

	@Override
	public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
		disposeContainers();
	}
}
