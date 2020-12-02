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

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.eclipse.m2e.pde.MissingMetadataMode;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
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

public class MavenTargetLocationWizard extends Wizard implements ITargetLocationWizard {

	private Text artifactId;
	private Text groupId;
	private Text version;
	private Text classifier;
	private CCombo type;
	private MavenTargetLocation targetLocation;
	private CCombo scope;
	private ComboViewer metadata;
	private ITargetDefinition targetDefinition;
	private BNDInstructions bndInstructions;
	private Button includeSource;

	public MavenTargetLocationWizard() {
		this(null);
	}

	public MavenTargetLocationWizard(MavenTargetLocation targetLocation) {
		this.targetLocation = targetLocation;
		setWindowTitle(Messages.MavenTargetLocationWizard_0);
		WizardPage page = new WizardPage(
				targetLocation == null ? Messages.MavenTargetLocationWizard_1 : Messages.MavenTargetLocationWizard_2) {

			private Link editInstructionsButton;
			private Label scopeLabel;

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				setControl(composite);
				composite.setLayout(new GridLayout(2, false));
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_3);
				groupId = fill(new Text(composite, SWT.BORDER));
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_4);
				artifactId = fill(new Text(composite, SWT.BORDER));
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_5);
				version = fill(new Text(composite, SWT.BORDER));
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_7);
				classifier = fill(new Text(composite, SWT.BORDER));
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_6);
				type = combo(new CCombo(composite, SWT.BORDER));
				type.add("jar"); //$NON-NLS-1$
				type.add("bundle"); //$NON-NLS-1$
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_9);
				createMetadataCombo(composite);
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_10);
				createScopeCombo(composite);
				new Label(composite, SWT.NONE).setText(Messages.MavenTargetLocationWizard_8);
				includeSource = new Button(composite, SWT.CHECK);
				if (targetLocation != null) {
					artifactId.setText(targetLocation.getArtifactId());
					groupId.setText(targetLocation.getGroupId());
					version.setText(targetLocation.getVersion());
					classifier.setText(targetLocation.getClassifier());
					type.setText(targetLocation.getArtifactType());
					scope.setText(targetLocation.getDependencyScope());
					metadata.setSelection(new StructuredSelection(targetLocation.getMetadataMode()));
					bndInstructions = targetLocation.getInstructions(null);
					includeSource.setSelection(targetLocation.isIncludeSource());
				} else {
					Clipboard clipboard = new Clipboard(composite.getDisplay());
					String text = (String) clipboard.getContents(TextTransfer.getInstance());
					clipboard.dispose();
					ClipboardParser clipboardParser = new ClipboardParser(text);
					groupId.setText(clipboardParser.getGroupId());
					artifactId.setText(clipboardParser.getArtifactId());
					version.setText(clipboardParser.getVersion());
					classifier.setText(clipboardParser.getClassifier());
					type.setText(MavenTargetLocation.DEFAULT_PACKAGE_TYPE);
					scope.setText(MavenTargetLocation.DEFAULT_DEPENDENCY_SCOPE);
					metadata.setSelection(new StructuredSelection(MavenTargetLocation.DEFAULT_METADATA_MODE));
					bndInstructions = new BNDInstructions("", null); //$NON-NLS-1$
					includeSource.setSelection(true);
				}
				updateUI();
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
				scope.add("compile"); //$NON-NLS-1$
				scope.add("test"); //$NON-NLS-1$
				scope.add("provided"); //$NON-NLS-1$
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
								bndInstructions);
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
			}

			private Text fill(Text text) {
				GridData data = new GridData(GridData.FILL_HORIZONTAL);
				data.grabExcessHorizontalSpace = true;
				text.setLayoutData(data);
				return text;
			}

			private CCombo combo(CCombo combo) {
				GridData data = new GridData();
				data.widthHint = 100;
				combo.setLayoutData(data);
				return combo;
			}
		};
		page.setImageDescriptor(ImageDescriptor
				.createFromURL(MavenTargetLocationWizard.class.getResource("/icons/new_m2_project_wizard.gif"))); //$NON-NLS-1$
		page.setTitle(page.getName());
		page.setDescription(Messages.MavenTargetLocationWizard_23);
		addPage(page);

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
		if (bndInstructions == null) {
			list = Collections.emptyList();
		} else {
			list = Collections.singletonList(bndInstructions);
		}
		MavenTargetLocation location = new MavenTargetLocation(groupId.getText(), artifactId.getText(),
				version.getText(), type.getText(), classifier.getText(),
				(MissingMetadataMode) metadata.getStructuredSelection().getFirstElement(), scope.getText(), list,
				includeSource.getSelection());
		if (targetLocation == null) {
			targetLocation = location;
		} else {
			ITargetLocation[] locations = targetDefinition.getTargetLocations().clone();
			for (int i = 0; i < locations.length; i++) {
				if (locations[i] == targetLocation) {
					locations[i] = location;
				}
			}
			targetDefinition.setTargetLocations(locations);
		}
		return true;
	}

}
