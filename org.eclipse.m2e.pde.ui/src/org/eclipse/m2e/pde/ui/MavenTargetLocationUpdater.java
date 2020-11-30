/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich
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
package org.eclipse.m2e.pde.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

public class MavenTargetLocationUpdater implements ITargetLocationUpdater {

	@Override
	public boolean canUpdate(ITargetDefinition target, ITargetLocation targetLocation) {
		return targetLocation instanceof MavenTargetLocation;
	}

	@Override
	public IStatus update(ITargetDefinition target, ITargetLocation targetLocation, IProgressMonitor monitor) {
		MavenTargetLocation location = (MavenTargetLocation) targetLocation;
		try {
			MavenTargetLocation update = location.update(monitor);
			if (update == null) {
				return new Status(IStatus.OK, Activator.ID, ITargetLocationUpdater.STATUS_CODE_NO_CHANGE, "", null);
			}
			ITargetLocation[] targetLocations = target.getTargetLocations();
			if (targetLocation == null) {
				targetLocations = new ITargetLocation[] { update };
			} else {
				for (int i = 0; i < targetLocations.length; i++) {
					if (targetLocations[i] == targetLocation) {
						targetLocations[i] = update;
						break;
					}
				}
			}
			target.setTargetLocations(targetLocations);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

}
