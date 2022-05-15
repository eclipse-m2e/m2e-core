/*******************************************************************************
 * Copyright (c) 2012, 2022 Igor Fedorenko and others
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
package org.eclipse.m2e.binaryproject.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.m2e.binaryproject.internal.AbstractBinaryProjectsImportJob;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.wizards.MavenDependenciesWizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

@SuppressWarnings("restriction")
public class BinaryProjectImportWizard extends Wizard implements IImportWizard {

	private MavenDependenciesWizardPage artifactsPage;

	private List<Dependency> initialDependencies;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		List<Dependency> dependencies = new ArrayList<>();
		for (Object element : selection) {
			ArtifactKey artifactKey = SelectionUtil.getType(element, ArtifactKey.class);
			if (artifactKey != null) {
				Dependency d = new Dependency();
				d.setGroupId(artifactKey.getGroupId());
				d.setArtifactId(artifactKey.getArtifactId());
				d.setVersion(artifactKey.getVersion());
				d.setClassifier(artifactKey.getClassifier());
				dependencies.add(d);
			}
		}
		artifactsPage = new MavenDependenciesWizardPage(new ProjectImportConfiguration(), "Artifacts",
				"Select artifacts to import") {
			@Override
			protected void createAdvancedSettings(Composite composite, GridData gridData) {
				// TODO profile can theoretically be useful
			}
		};
		artifactsPage.setDependencies(dependencies.toArray(new Dependency[dependencies.size()]));
		artifactsPage.addListener(event -> getContainer().updateButtons());
		this.initialDependencies = Collections.unmodifiableList(dependencies);
	}

	@Override
	public boolean performFinish() {
		List<ArtifactKey> artifacts = artifactsPage.getDependencies().stream()
				.map(d -> new ArtifactKey(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getClassifier()))
				.collect(Collectors.toList());

		if (!artifacts.isEmpty()) {
			new AbstractBinaryProjectsImportJob() {
				@Override
				protected Collection<ArtifactKey> getArtifactKeys(IProgressMonitor monitor) {
					return artifacts;
				}
			}.schedule();
		}
		return !artifacts.isEmpty();
	}

	@Override
	public boolean canFinish() {
		return !artifactsPage.getDependencies().isEmpty();
	}

	@Override
	public void addPages() {
		addPage(artifactsPage);
	}

	public List<Dependency> getInitialDependencies() {
		return initialDependencies;
	}
}
