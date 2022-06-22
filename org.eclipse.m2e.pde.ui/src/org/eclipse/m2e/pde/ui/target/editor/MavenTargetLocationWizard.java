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
package org.eclipse.m2e.pde.ui.target.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.target.BNDInstructions;
import org.eclipse.m2e.pde.target.DependencyDepth;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.m2e.pde.target.MavenTargetRepository;
import org.eclipse.m2e.pde.target.MissingMetadataMode;
import org.eclipse.m2e.pde.target.TemplateFeatureModel;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class MavenTargetLocationWizard extends Wizard implements ITargetLocationWizard {

	private static final List<String> MAVEN_SCOPES = List.of(Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED,
			Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_IMPORT);

	private MavenTargetLocation targetLocation;
	private Button[] scopes;
	private ComboViewer metadata;
	private ComboViewer include;
	private ITargetDefinition targetDefinition;
	private BNDInstructions bndInstructions;
	private Button includeSource;
	private MavenTargetDependencyEditor dependencyEditor;
	private List<MavenTargetRepository> repositoryList = new ArrayList<>();
	private Button createFeature;
	private WizardPage page;
	private FeatureSpecPage featureSpecPage;
	private PluginListPage pluginListPage;
	private Text locationLabel;

	private MavenTargetDependency selectedRoot;

	public MavenTargetLocationWizard() {
		this(null);
	}

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

			private Label includeLabel;
			private Label scopeLabel;

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				setControl(composite);
				composite.setLayout(new GridLayout(2, false));
				createRepositoryLink(composite);
				dependencyEditor = new MavenTargetDependencyEditor(composite,
						targetLocation == null ? Collections.emptyList() : targetLocation.getRoots());
				dependencyEditor.setSelected(selectedRoot);
				GridData gd_dep = new GridData(GridData.FILL_BOTH);
				gd_dep.horizontalSpan = 2;
				gd_dep.heightHint = 200;
				dependencyEditor.getControl().setLayoutData(gd_dep);

				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_14);
				locationLabel = new Text(composite, SWT.BORDER);
				ModifyListener modifyListener = new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						String text = locationLabel.getText();
						if (text.isBlank()) {
							setWindowTitle(Messages.MavenTargetLocationWizard_0);
						} else {
							setWindowTitle(Messages.MavenTargetLocationWizard_0 + " - " + text);
						}

					}
				};
				locationLabel.addModifyListener(modifyListener);
				GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(locationLabel);
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_9);
				createMetadataCombo(composite);
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_17);
				createIncludeCombo(composite);
				scopeLabel = new Label(composite, SWT.NONE);
				scopeLabel.setText(Messages.MavenTargetLocationWizard_10);
				createScopes(composite);
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
					Collection<String> dependencyScopes = targetLocation.getDependencyScopes();
					for (int i = 0; i < scopes.length; i++) {
						if (dependencyScopes.contains(MAVEN_SCOPES.get(i))) {
							scopes[i].setSelection(true);
						}
					}
					metadata.setSelection(new StructuredSelection(targetLocation.getMetadataMode()));
					include.setSelection(new StructuredSelection(targetLocation.getDependencyDepth()));
					bndInstructions = targetLocation.getInstructions(null);
					includeSource.setSelection(targetLocation.isIncludeSource());
					var template = targetLocation.getFeatureTemplate();
					createFeature.setSelection(template != null);
					locationLabel.setText(Objects.requireNonNullElse(targetLocation.getLabel(), ""));
					modifyListener.modifyText(null);
				} else {
					metadata.setSelection(new StructuredSelection(MavenTargetLocation.DEFAULT_METADATA_MODE));
					include.setSelection(new StructuredSelection(MavenTargetLocation.DEFAULT_INCLUDE_MODE));
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

			private void createIncludeCombo(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(2, false);
				layout.horizontalSpacing = 20;
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				composite.setLayout(layout);
				include = new ComboViewer(combo(new CCombo(composite, SWT.READ_ONLY | SWT.BORDER | SWT.FLAT)));
				includeLabel = new Label(composite, SWT.NONE);
				includeLabel.setText(Messages.MavenTargetLocationWizard_16);
				include.setContentProvider(ArrayContentProvider.getInstance());
				include.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DependencyDepth) {
							return ((DependencyDepth) element).name().toLowerCase();
						}
						return super.getText(element);
					}

				});
				include.setInput(DependencyDepth.values());
				include.addSelectionChangedListener(e -> updateUI());
			}

			private void createScopes(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(MAVEN_SCOPES.size() + 1, false);
				layout.horizontalSpacing = 10;
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				composite.setLayout(layout);
				scopes = new Button[MAVEN_SCOPES.size()];
				for (int i = 0; i < scopes.length; i++) {
					scopes[i] = new Button(composite, SWT.CHECK);
					scopes[i].setText(MAVEN_SCOPES.get(i));
					scopes[i].addSelectionListener(new SelectionListener() {

						@Override
						public void widgetSelected(SelectionEvent e) {
							updateUI();
						}

						@Override
						public void widgetDefaultSelected(SelectionEvent e) {

						}
					});
				}
			}

			private void createMetadataCombo(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout layout = new GridLayout(2, false);
				layout.horizontalSpacing = 20;
				layout.marginWidth = 0;
				layout.marginHeight = 0;
				composite.setLayout(layout);
				metadata = new ComboViewer(combo(new CCombo(composite, SWT.READ_ONLY | SWT.BORDER | SWT.FLAT)));
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
				if (include.getStructuredSelection().getFirstElement() == DependencyDepth.NONE) {
					for (Button button : scopes) {
						button.setEnabled(false);
					}
					scopeLabel.setEnabled(false);
				} else {
					scopeLabel.setEnabled(true);
					for (Button button : scopes) {
						button.setEnabled(true);
					}
				}
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
			return super.canFinish();
		}
		return page.isPageComplete();
	}

	private boolean isCreateFeature() {
		return createFeature != null && createFeature.getSelection();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (isCreateFeature()) {
			return super.getNextPage(page);
		}
		return null;
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
			var featureTemplate = targetLocation.getFeatureTemplate();
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
				featureSpecPage.update(featureModel, iscreate || targetLocation.getFeatureTemplate() == null);
				pluginListPage.update(featureModel);
			} catch (CoreException e) {
				Platform.getLog(MavenTargetLocationWizard.class).log(e.getStatus());
			}
			featureModel.makeReadOnly();
		}
		var f = createFeature ? featureModel.getFeature() : null;
		Collection<String> selectedScopes = new LinkedHashSet<>();
		for (int i = 0; i < scopes.length; i++) {
			if (scopes[i].getSelection()) {
				selectedScopes.add(MAVEN_SCOPES.get(i));
			}
		}
		DependencyDepth depth = (DependencyDepth) include.getStructuredSelection().getFirstElement();
		MavenTargetLocation location = new MavenTargetLocation(locationLabel.getText(), dependencyEditor.getRoots(),
				repositoryList, (MissingMetadataMode) metadata.getStructuredSelection().getFirstElement(), depth,
				selectedScopes, includeSource.getSelection(), list, excludes, f);
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

	public void setSelectedRoot(MavenTargetDependency selectedRoot) {
		this.selectedRoot = selectedRoot;
	}

}
