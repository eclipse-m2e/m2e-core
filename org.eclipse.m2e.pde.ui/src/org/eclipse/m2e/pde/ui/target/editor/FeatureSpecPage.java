/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *   Christoph Läubrich - adjust for m2e-pde usage
 *******************************************************************************/

package org.eclipse.m2e.pde.ui.target.editor;

import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.BundleProviderHistoryUtil;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

// derived from org.eclipse.pde.internal.ui.wizards.feature.FeatureSpecPage
@SuppressWarnings("restriction")
public class FeatureSpecPage extends AbstractFeatureSpecPage {

	private Combo fFeatureProviderCombo;
	private Text fFeatureIdText;
	private MavenTargetLocation targetLocation;

	public FeatureSpecPage(MavenTargetLocation targetLocation) {
		super();
		this.targetLocation = targetLocation;
		setTitle(PDEUIMessages.NewFeatureWizard_SpecPage_title);
		setDescription(PDEUIMessages.NewFeatureWizard_SpecPage_desc);
	}

	@Override
	protected void initialize() {
		fFeatureVersionText.setText("1.0.0.qualifier"); //$NON-NLS-1$
		setMessage(PDEUIMessages.NewFeatureWizard_MainPage_desc);
	}

	@Override
	public FeatureData getFeatureData() {
		FeatureData data = new FeatureData();
		data.id = fFeatureIdText.getText();
		data.version = fFeatureVersionText.getText();
		data.provider = fFeatureProviderCombo.getText();
		data.name = fFeatureNameText.getText();
		data.library = getInstallHandlerLibrary();
		return data;
	}

	@Override
	protected String validateContent() {
		setMessage(null);
		return null;
	}

	@Override
	protected String getHelpId() {
		return IHelpContextIds.NEW_FEATURE_DATA;
	}

	@Override
	protected void createContents(Composite container) {
		Composite group = new Composite(container, SWT.NULL);
		group.setLayout(new GridLayout(2, false));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.verticalIndent = 10;
		group.setLayoutData(gd);

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_id);
		fFeatureIdText = new Text(group, SWT.BORDER);
		fFeatureIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createCommonInput(group);

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.NewFeatureWizard_SpecPage_provider);
		fFeatureProviderCombo = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
		fFeatureProviderCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		BundleProviderHistoryUtil.loadHistory(fFeatureProviderCombo, getDialogSettings());

		createInstallHandlerText(group);
		if (targetLocation != null) {
			IFeature template = targetLocation.getFeatureTemplate();
			if (template != null) {
				fFeatureIdText.setText(Objects.requireNonNullElse(template.getId(), ""));
				fFeatureNameText.setText(Objects.requireNonNullElse(template.getLabel(), ""));
				fFeatureVersionText.setText(Objects.requireNonNullElse(template.getVersion(), ""));
				fFeatureProviderCombo.setText(Objects.requireNonNullElse(template.getProviderName(), ""));
				IFeatureInstallHandler handler = template.getInstallHandler();
				if (handler != null) {
					fLibraryText.setText(handler.getHandlerName());
				}
			}
		}
	}

	@Override
	protected void attachListeners(ModifyListener listener) {
		fFeatureProviderCombo.addModifyListener(listener);
		fFeatureIdText.addModifyListener(listener);
	}

	@Override
	protected String getFeatureId() {
		return fFeatureIdText.getText();
	}

	// derived from
	// org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation.createFeature()
	public void update(IFeatureModel model, boolean createFeatureInfos) throws CoreException {
		FeatureData featureData = getFeatureData();
		IFeature feature = model.getFeature();
		feature.setLabel(featureData.name);
		feature.setId(featureData.id);
		feature.setVersion(featureData.version);
		feature.setProviderName(featureData.provider);
		if (featureData.hasCustomHandler())
			feature.setInstallHandler(model.getFactory().createInstallHandler());

		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler != null)
			handler.setLibrary(featureData.library);
		if (createFeatureInfos) {
			IFeatureInfo info = model.getFactory().createInfo(IFeature.INFO_COPYRIGHT);
			feature.setFeatureInfo(info, IFeature.INFO_COPYRIGHT);
			info.setURL("http://www.example.com/copyright"); //$NON-NLS-1$
			info.setDescription(PDEUIMessages.NewFeatureWizard_sampleCopyrightDesc);

			info = model.getFactory().createInfo(IFeature.INFO_LICENSE);
			feature.setFeatureInfo(info, IFeature.INFO_LICENSE);
			info.setURL("http://www.example.com/license"); //$NON-NLS-1$
			info.setDescription(PDEUIMessages.NewFeatureWizard_sampleLicenseDesc);

			info = model.getFactory().createInfo(IFeature.INFO_DESCRIPTION);
			feature.setFeatureInfo(info, IFeature.INFO_DESCRIPTION);
			info.setURL("http://www.example.com/description"); //$NON-NLS-1$
			info.setDescription(PDEUIMessages.NewFeatureWizard_sampleDescriptionDesc);
		}
	}

	@Override
	protected void saveSettings(IDialogSettings settings) {
		BundleProviderHistoryUtil.saveHistory(fFeatureProviderCombo, settings);
	}
}
