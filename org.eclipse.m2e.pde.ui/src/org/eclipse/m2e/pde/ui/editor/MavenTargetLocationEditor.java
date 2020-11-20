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
package org.eclipse.m2e.pde.ui.editor;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.m2e.pde.MavenTargetBundle;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.m2e.pde.MissingMetadataMode;
import org.eclipse.m2e.pde.ui.Activator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class MavenTargetLocationEditor implements ITargetLocationHandler {

	@Override
	public boolean canEdit(ITargetDefinition target, TreePath treePath) {
		Object root = treePath.getFirstSegment();
		if (root instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) root;
			if (treePath.getSegmentCount() == 1) {
				return true;
			}
			// it must use generate mode
			if (location.getMetadataMode() == MissingMetadataMode.GENERATE) {
				// and the selected child must be a dependency node
				Object lastSegment = treePath.getLastSegment();
				if (lastSegment instanceof DependencyNode) {
					DependencyNode node = (DependencyNode) lastSegment;
					MavenTargetBundle bundle = location.getTargetBundle(node.getArtifact());
					return bundle != null && bundle.isWrapped();
				}
			}
		}
		return false;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		Object root = treePath.getFirstSegment();
		if (root instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) root;
			if (treePath.getSegmentCount() == 1) {
				MavenTargetLocationWizard wizard = new MavenTargetLocationWizard(location);
				wizard.setTarget(target);
				return wizard;
			}
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode) {
				DependencyNode node = (DependencyNode) lastSegment;
				Artifact artifact = node.getArtifact();
				BNDInstructions bnd = location.getInstructions(artifact);
				MavenArtifactInstructionsWizard wizard = new MavenArtifactInstructionsWizard(bnd,
						() -> location.getDefaultInstructions().getInstructions()) {
					@Override
					public boolean performFinish() {
						if (usedefaults) {
							if (location.setInstructions(artifact, null)) {
								location.refresh();
								target.setTargetLocations(target.getTargetLocations());
							}
						} else {
							if (location.setInstructions(artifact, bnd.withInstructions(instructions))) {
								location.refresh();
								target.setTargetLocations(target.getTargetLocations());
							}
						}
						return true;
					}
				};
				wizard.setWindowTitle(wizard.getWindowTitle() + " [" + bnd.getKey() + "]");
				return wizard;
			}
		}
		return null;
	}

	@Override
	public boolean canUpdate(ITargetDefinition target, TreePath treePath) {
		return treePath.getFirstSegment() instanceof MavenTargetLocation;
	}

	@Override
	public IStatus update(ITargetDefinition target, TreePath treePath, IProgressMonitor monitor) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) segment;
			location.refresh();
			return Status.OK_STATUS;
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public void reload(ITargetDefinition target, ITargetLocation targetLocation) {
		if (targetLocation instanceof MavenTargetLocation) {
			((MavenTargetLocation) targetLocation).refresh();
		}
	}

	@Override
	public boolean canDisable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) segment;
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode) {
				DependencyNode node = (DependencyNode) lastSegment;
				return !location.isDisabled(node.getArtifact());
			}
		}
		return false;
	}

	@Override
	public boolean canEnable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) segment;
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode) {
				DependencyNode node = (DependencyNode) lastSegment;
				return location.isDisabled(node.getArtifact());
			}
		}
		return false;
	}

	@Override
	public IStatus disable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) segment;
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode) {
				DependencyNode node = (DependencyNode) lastSegment;
				location.setDisabled(node.getArtifact(), true);
				target.setTargetLocations(target.getTargetLocations());
				return new Status(IStatus.OK, Activator.class.getPackageName(), STATUS_FORCE_RELOAD, "", null);
			}
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus enable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			MavenTargetLocation location = (MavenTargetLocation) segment;
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode) {
				DependencyNode node = (DependencyNode) lastSegment;
				location.setDisabled(node.getArtifact(), false);
				target.setTargetLocations(target.getTargetLocations());
				return new Status(IStatus.OK, Activator.class.getPackageName(), STATUS_FORCE_RELOAD, "", null);
			}
		}
		return Status.CANCEL_STATUS;
	}

}
