/*******************************************************************************
 * Copyright (c) 2018, 2021 Christoph Läubrich
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.m2e.pde.MavenTargetRepository;
import org.eclipse.m2e.pde.MissingMetadataMode;
import org.eclipse.m2e.pde.TemplateFeatureModel;
import org.eclipse.m2e.pde.ui.Activator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.ui.target.ITargetLocationWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class MavenTargetLocationWizard extends Wizard implements ITargetLocationWizard {

	private MavenTargetLocation targetLocation;
	private CCombo scope;
	private ComboViewer metadata;
	private ITargetDefinition targetDefinition;
	private BNDInstructions bndInstructions;
	private Button includeSource;
	private MavenTargetDependencyEditor dependencyEditor;
	private List<MavenTargetRepository> repositoryList = new ArrayList<>();
	private Button createFeature;
	private WizardPage page;
	private FeatureSpecPage featureSpecPage;
	private PluginListPage pluginListPage;

	public MavenTargetLocationWizard() {
		this(null);
	}

	@SuppressWarnings("restriction")
	public MavenTargetLocationWizard(MavenTargetLocation targetLocation) {
		this.targetLocation = targetLocation;
		setWindowTitle(Messages.MavenTargetLocationWizard_0);
		if (targetLocation != null) {
			for (MavenTargetRepository mavenTargetRepository : targetLocation.getExtraRepositories()) {
				repositoryList.add(mavenTargetRepository.copy());
			}
		}

		page = new WizardPage(
				targetLocation == null ? Messages.MavenTargetLocationWizard_1 : Messages.MavenTargetLocationWizard_2) {

			private Link editInstructionsButton;
			private Label scopeLabel;

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				setControl(composite);
				composite.setLayout(new GridLayout(2, false));
				createRepositoryLink(composite);
				dependencyEditor = new MavenTargetDependencyEditor(composite,
						targetLocation == null ? Collections.emptyList() : targetLocation.getRoots());
				GridData gd_dep = new GridData(GridData.FILL_BOTH);
				gd_dep.horizontalSpan = 2;
				gd_dep.heightHint = 200;
				dependencyEditor.getControl().setLayoutData(gd_dep);

				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_9);
				createMetadataCombo(composite);
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_10);
				createScopeCombo(composite);
				includeSource = createCheckBox(composite, Messages.MavenTargetLocationWizard_8);
				createFeature = createCheckBox(composite, Messages.MavenTargetLocationWizard_13);
				createFeature.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						updateUI();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				if (targetLocation != null) {
					scope.setText(targetLocation.getDependencyScope());
					metadata.setSelection(new StructuredSelection(targetLocation.getMetadataMode()));
					bndInstructions = targetLocation.getInstructions(null);
					includeSource.setSelection(targetLocation.isIncludeSource());
					IFeature template = targetLocation.getFeatureTemplate();
					createFeature.setSelection(template != null);
				} else {
					metadata.setSelection(new StructuredSelection(MavenTargetLocation.DEFAULT_METADATA_MODE));
					bndInstructions = new BNDInstructions("", null); //$NON-NLS-1$
					includeSource.setSelection(true);
				}

				updateUI();
			}

			private Button createCheckBox(Composite composite, String text) {
				new Label(composite, SWT.NONE);
				Button box = new Button(composite, SWT.CHECK);
				box.setText(text);
				return box;
			}

			private void createScopeCombo(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(2, false);
				layout.horizontalSpacing = 20;
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				composite.setLayout(layout);
				scope = combo(new CCombo(composite, SWT.BORDER));
				scopeLabel = new Label(composite, SWT.NONE);
				scopeLabel.setText(Messages.MavenTargetLocationWizard_15);
				scope.add(""); //$NON-NLS-1$
				scope.add(Artifact.SCOPE_COMPILE);
				scope.add(Artifact.SCOPE_PROVIDED);
				scope.add(Artifact.SCOPE_TEST);
				scope.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						updateUI();
					}
				});
			}

			private void createMetadataCombo(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(2, false);
				layout.horizontalSpacing = 20;
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				composite.setLayout(layout);
				metadata = new ComboViewer(combo(new CCombo(composite, SWT.READ_ONLY | SWT.BORDER)));
				metadata.setContentProvider(ArrayContentProvider.getInstance());
				metadata.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof MissingMetadataMode) {
							return ((MissingMetadataMode) element).name().toLowerCase();
						}
						return super.getText(element);
					}

				});
				editInstructionsButton = new Link(composite, SWT.PUSH);
				editInstructionsButton.setText(Messages.MavenTargetLocationWizard_20);
				editInstructionsButton.setToolTipText(Messages.MavenTargetLocationWizard_21);
				editInstructionsButton.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						BNDInstructions edited = MavenArtifactInstructionsWizard.openWizard(getShell(),
								Objects.requireNonNullElse(bndInstructions, BNDInstructions.EMPTY));
						if (edited != null) {
							bndInstructions = edited;
						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				metadata.setInput(MissingMetadataMode.values());
				metadata.addSelectionChangedListener(e -> {
					updateUI();
				});
			}

			private void updateUI() {
				editInstructionsButton.setVisible(
						metadata.getStructuredSelection().getFirstElement() == MissingMetadataMode.GENERATE);
				scopeLabel.setVisible(scope.getText().isBlank());
				getContainer().updateButtons();
			}

			private CCombo combo(CCombo combo) {
				GridData data = new GridData();
				data.widthHint = 120;
				combo.setLayoutData(data);
				return combo;
			}

		};
		page.setImageDescriptor(ImageDescriptor
				.createFromURL(MavenTargetLocationWizard.class.getResource("/icons/new_m2_project_wizard.gif"))); //$NON-NLS-1$
		page.setTitle(page.getName());
		page.setDescription(Messages.MavenTargetLocationWizard_23);
		addPage(page);
		featureSpecPage = new FeatureSpecPage(targetLocation);
		addPage(featureSpecPage);
		pluginListPage = new PluginListPage(targetLocation);
		addPage(pluginListPage);
	}

	@Override
	public boolean canFinish() {
		if (isCreateFeature()) {
			return page.isPageComplete();
		}
		return super.canFinish();
	}

	private boolean isCreateFeature() {
		return createFeature == null || !createFeature.getSelection();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (isCreateFeature()) {
			return null;
		}
		return super.getNextPage(page);
	}

	@Override
	public void setTarget(ITargetDefinition target) {
		this.targetDefinition = target;
	}

	@Override
	public ITargetLocation[] getLocations() {
		return new ITargetLocation[] { targetLocation };
	}

	@Override
	public boolean performFinish() {
		List<BNDInstructions> list;
		Collection<String> excludes;
		boolean iscreate = targetLocation == null;
		boolean createFeature = this.createFeature.getSelection();
		TemplateFeatureModel featureModel = null;
		if (iscreate) {
			excludes = Collections.emptyList();
			if (bndInstructions == null) {
				list = Collections.emptyList();
			} else {
				list = Collections.singletonList(bndInstructions);
			}
		} else {
			excludes = targetLocation.getExcludes();
			list = new ArrayList<>();
			for (BNDInstructions instruction : targetLocation.getInstructions()) {
				if (instruction.getKey().isBlank()) {
					continue;
				}
				list.add(instruction);
			}
			if (bndInstructions != null) {
				list.add(bndInstructions);
			}
			IFeature featureTemplate = targetLocation.getFeatureTemplate();
			if (featureTemplate != null) {
				try {
					featureModel = new TemplateFeatureModel(featureTemplate);
					featureModel.load();
				} catch (CoreException e) {
					Platform.getLog(MavenTargetLocationWizard.class).log(e.getStatus());
				}
			}
		}
		if (createFeature) {
			if (featureModel == null) {
				featureModel = new TemplateFeatureModel(null);
			}
			try {
				featureSpecPage.update(featureModel, iscreate);
				pluginListPage.update(featureModel);
			} catch (CoreException e) {
				Platform.getLog(Activator.class).log(e.getStatus());
				e.printStackTrace();
			}
			featureModel.makeReadOnly();
		}
		IFeature f = createFeature ? featureModel.getFeature() : null;
		MavenTargetLocation location = new MavenTargetLocation(dependencyEditor.getRoots(), repositoryList,
				(MissingMetadataMode) metadata.getStructuredSelection().getFirstElement(), scope.getText(),
				includeSource.getSelection(), list, excludes, f);
		if (iscreate) {
			targetLocation = location;
		} else {
			ITargetLocation[] locations = targetDefinition.getTargetLocations().clone();
			for (int i = 0; i < locations.length; i++) {
				if (locations[i] == targetLocation) {
					locations[i] = location;
					break;
				}
			}
			targetDefinition.setTargetLocations(locations);
		}
		return true;
	}

	private void createRepositoryLink(Composite composite) {
		GridData gd_link = new GridData();
		gd_link.horizontalSpan = 2;
		Link link = new Link(composite, SWT.NONE);
		link.setText(Messages.MavenTargetLocationWizard_12);
		link.setLayoutData(gd_link);
		link.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if ("#maven".equals(e.text)) {
					PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(composite.getShell(),
							"org.eclipse.m2e.core.preferences.MavenSettingsPreferencePage",
							new String[] { "org.eclipse.m2e.core.preferences.MavenSettingsPreferencePage",
									"org.eclipse.m2e.core.preferences.MavenInstallationsPreferencePage",
									"org.eclipse.m2e.core.preferences.MavenArchetypesPreferencePage",
									"org.eclipse.m2e.core.ui.preferences.UserInterfacePreferencePage",
									"org.eclipse.m2e.core.ui.preferences.WarningsPreferencePage",
									"org.eclipse.m2e.core.preferences.LifecycleMappingPreferencePag" },
							null);
					dialog.open();
				} else if ("#configure".equals(e.text)) {
					MavenTargetRepositoryEditor editor = new MavenTargetRepositoryEditor(composite.getShell(),
							repositoryList);
					editor.open();
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
	}

}
