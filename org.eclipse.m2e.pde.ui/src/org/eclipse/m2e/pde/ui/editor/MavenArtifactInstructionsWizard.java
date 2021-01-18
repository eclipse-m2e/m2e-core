/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich
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

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.osgi.Analyzer;

public class MavenArtifactInstructionsWizard extends Wizard {

	private static final String BND_PAGE = Messages.MavenArtifactInstructionsWizard_0;
	private String instructions;
	private boolean usedefaults;

	public MavenArtifactInstructionsWizard(BNDInstructions bndInstructions) {
		this.instructions = bndInstructions == null ? null : bndInstructions.getInstructions();
		this.usedefaults = instructions == null || instructions.isBlank();
		setWindowTitle(Messages.MavenArtifactInstructionsWizard_1);
		WizardPage page = new WizardPage(Messages.MavenArtifactInstructionsWizard_2) {

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout(1, true));
				Button buttonInherit = new Button(composite, SWT.CHECK);
				buttonInherit.setText(Messages.MavenArtifactInstructionsWizard_3);
				Text textField = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
				textField.setFont(JFaceResources.getTextFont());
				GridData layoutData = new GridData(GridData.FILL_BOTH);
				textField.setLayoutData(layoutData);
				layoutData.heightHint = 100;
				Link link = new Link(composite, SWT.NONE);
				link.setText(String.format(Messages.MavenArtifactInstructionsWizard_4, BND_PAGE));
				link.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						Program.launch(e.text);
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				if (usedefaults) {
					textField.setText(BNDInstructions.getDefaultInstructions().getInstructions());
				} else {
					textField.setText(instructions);
				}
				buttonInherit.addSelectionListener(new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						boolean selection = buttonInherit.getSelection();
						usedefaults = selection;
						textField.setEnabled(!selection);
						link.setEnabled(!selection);
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				textField.addModifyListener(new ModifyListener() {

					@Override
					public void modifyText(ModifyEvent e) {
						instructions = textField.getText();
					}
				});
				buttonInherit.setSelection(usedefaults);
				textField.setEnabled(!buttonInherit.getSelection());
				link.setEnabled(!buttonInherit.getSelection());
				setControl(composite);
			}

			@Override
			public void performHelp() {
				IWizardContainer container = getContainer();
				if (container instanceof TrayDialog) {
					TrayDialog dialog = (TrayDialog) container;
					DialogTray tray = dialog.getTray();
					if (tray != null) {
						dialog.closeTray();
					} else {
						dialog.openTray(new BrowserTray(BND_PAGE));
					}
				}
			}
		};
		addPage(page);
		page.setImageDescriptor(ImageDescriptor.createFromURL(Analyzer.class.getResource("/img/bnd-64.png"))); //$NON-NLS-1$
		page.setTitle(page.getName());
		page.setDescription(Messages.MavenArtifactInstructionsWizard_6);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	/**
	 * Open a wizard to edit the given instructions
	 * 
	 * @param shell        parent shell for the wizard dialog
	 * @param instructions the instructions to edit
	 * @return the new instructions instance or <code>null</code> if the user
	 *         canceled the wizard
	 */
	public static BNDInstructions openWizard(Shell shell, BNDInstructions instructions) {
		MavenArtifactInstructionsWizard wizard = new MavenArtifactInstructionsWizard(instructions);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(800, 600);
		if (dialog.open() == Window.OK) {
			if (wizard.usedefaults) {
				return new BNDInstructions(instructions.getKey(), null);
			}
			return new BNDInstructions(instructions.getKey(), wizard.instructions);
		}
		return null;
	}

	private static final class BrowserTray extends DialogTray {

		private final String url;

		public BrowserTray(String url) {

			this.url = url;
		}

		@Override
		protected Control createContents(Composite parent) {

			Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(new GridLayout());
			GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
			container.setLayoutData(layoutData);
			Browser browser = new Browser(container, SWT.NONE);
			browser.setUrl(url);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.minimumWidth = 600;
			data.widthHint = 800;
			browser.setLayoutData(data);
			return container;
		}
	}

}
