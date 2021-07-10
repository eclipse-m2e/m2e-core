/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
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
package org.eclipse.m2e.pde.ui.target.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.m2e.pde.target.BNDInstructions;
import org.eclipse.m2e.pde.target.MavenTargetBundle;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.target.MissingMetadataMode;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

public class MavenTargetLocationEditor implements ITargetLocationHandler {

	@Override
	public boolean canEdit(ITargetDefinition target, TreePath treePath) {
		Object root = treePath.getFirstSegment();
		if (root instanceof MavenTargetLocation location) {
			if (treePath.getSegmentCount() == 1) {
				return true;
			}
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof MavenTargetDependency) {
				return true;
			}
			// it must use generate mode
			if (location.getMetadataMode() == MissingMetadataMode.GENERATE) {
				// and the selected child must be a dependency node
				if (lastSegment instanceof DependencyNode node) {
					MavenTargetBundle bundle = location.getMavenTargetBundle(node.getArtifact());
					return bundle != null && bundle.isWrapped();
				}
				// or a target dependency
				if (lastSegment instanceof MavenTargetDependency dependency) {
					MavenTargetBundle bundle = location.getMavenTargetBundle(dependency);
					return bundle != null && bundle.isWrapped();
				}
			}
		}
		return false;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		Object root = treePath.getFirstSegment();
		if (root instanceof MavenTargetLocation location) {
			if (treePath.getSegmentCount() == 1) {
				MavenTargetLocationWizard wizard = new MavenTargetLocationWizard(location);
				wizard.setTarget(target);
				return wizard;
			}
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof MavenTargetDependency selectedRoot) {
				MavenTargetLocationWizard wizard = new MavenTargetLocationWizard(location);
				wizard.setTarget(target);
				wizard.setSelectedRoot(selectedRoot);
				return wizard;
			}
			if (lastSegment instanceof DependencyNode node) {
				return edit(target, location, node.getArtifact());
			}
			if (lastSegment instanceof MavenTargetDependency dependency) {
				MavenTargetBundle bundle = location.getMavenTargetBundle(dependency);
				if (bundle != null) {
					return edit(target, location, bundle.getArtifact());
				}

			}
		}
		return null;
	}

	private IWizard edit(ITargetDefinition target, MavenTargetLocation location, Artifact artifact) {
		BNDInstructions instructions = location.getInstructions(artifact);
		MavenArtifactInstructionsWizard wizard = new MavenArtifactInstructionsWizard(instructions) {
			@Override
			public boolean performFinish() {
				boolean finish = super.performFinish();
				if (finish) {
					BNDInstructions instructions = getInstructions();
					List<BNDInstructions> updatedInstructions = new ArrayList<>();
					updatedInstructions.add(instructions);
					for (BNDInstructions existing : location.getInstructions()) {
						if (!existing.key().equals(instructions.key())) {
							updatedInstructions.add(instructions);
						}
					}
					MavenTargetLocation update = location.withInstructions(updatedInstructions);
					ITargetLocation[] locations = target.getTargetLocations();
					for (int i = 0; i < locations.length; i++) {
						if (locations[i] == location) {
							locations[i] = update;
							break;
						}
					}
					target.setTargetLocations(locations);
				}
				return finish;
			}
		};
		wizard.setWindowTitle(wizard.getWindowTitle() + " [" + artifact + "]");
		return wizard;
	}

	@Override
	public boolean canUpdate(ITargetDefinition target, TreePath treePath) {
		return treePath.getFirstSegment() instanceof MavenTargetLocation;
	}

	@Override
	public IStatus update(ITargetDefinition target, TreePath[] treePaths, IProgressMonitor monitor) {
		ITargetLocation[] targetLocations = target.getTargetLocations();
		IStatus status = new Status(IStatus.OK, "org.eclipse.m2e.pde.ui", ITargetLocationHandler.STATUS_CODE_NO_CHANGE,
				"", null);
		for (TreePath treePath : treePaths) {
			Object segment = treePath.getFirstSegment();
			if (segment instanceof MavenTargetLocation targetLocation) {
				try {
					MavenTargetLocation update = targetLocation.update(monitor);
					if (update == null) {
						continue;
					}
					for (int i = 0; i < targetLocations.length; i++) {
						if (targetLocations[i] == targetLocation) {
							targetLocations[i] = update;
							break;
						}
					}
					target.setTargetLocations(targetLocations);
					status = Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
		}
		return status;
	}

	@Override
	public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
		for (ITargetLocation location : targetLocations) {
			if (location instanceof MavenTargetLocation mavenLocation) {
				mavenLocation.refresh();
			}

		}
		return Status.OK_STATUS;
	}

	@Override
	public boolean canDisable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation location) {
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode node) {
				return !location.isExcluded(node.getArtifact());
			}
		}
		return false;
	}

	@Override
	public boolean canEnable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation location) {
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof DependencyNode node) {
				return location.isExcluded(node.getArtifact());
			}
		}
		return false;
	}

	@Override
	public boolean canRemove(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getFirstSegment();
		if (segment instanceof MavenTargetLocation) {
			Object lastSegment = treePath.getLastSegment();
			if (lastSegment instanceof MavenTargetDependency) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IStatus remove(ITargetDefinition target, TreePath[] treePaths) {
		ITargetLocation[] targetLocations = target.getTargetLocations();
		for (TreePath treePath : treePaths) {
			Object segment = treePath.getFirstSegment();
			if (segment instanceof MavenTargetLocation mavenTargetLocation) {
				Object lastSegment = treePath.getLastSegment();
				if (lastSegment instanceof MavenTargetDependency mavenTargetDependency) {
					for (int i = 0; i < targetLocations.length; i++) {
						ITargetLocation location = targetLocations[i];
						if (location == mavenTargetLocation) {
							targetLocations[i] = mavenTargetLocation.withoutRoot(mavenTargetDependency);
						}
					}
				}
			}
		}
		target.setTargetLocations(targetLocations);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus toggle(ITargetDefinition target, TreePath[] treePaths) {
		int toggled = 0;
		for (TreePath treePath : treePaths) {
			Object segment = treePath.getFirstSegment();
			if (segment instanceof MavenTargetLocation location) {
				Object lastSegment = treePath.getLastSegment();
				if (lastSegment instanceof DependencyNode node) {
					location.setExcluded(node.getArtifact(), !location.isExcluded(node.getArtifact()));
					target.setTargetLocations(target.getTargetLocations());
					toggled++;
				}
			}
		}
		return toggled > 0 ? new Status(IStatus.OK, "org.eclipse.m2e.pde.ui", STATUS_FORCE_RELOAD, "", null)
				: Status.CANCEL_STATUS;
	}

}
