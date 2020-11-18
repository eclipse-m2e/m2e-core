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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.BNDInstructions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import aQute.bnd.osgi.Analyzer;

public class MavenArtifactInstructionsWizard extends Wizard {

	private String instructions;
	private boolean usedefaults;

	public MavenArtifactInstructionsWizard(BNDInstructions bndInstructions) {
		this.instructions = bndInstructions.getInstructions();
		this.usedefaults = instructions == null || instructions.isBlank();
		setWindowTitle("Maven Artifact Instructions");
		WizardPage page = new WizardPage("Edit Instructions") {

			@Override
			public void createControl(Composite parent) {
				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout(1, true));
				Button buttonInherit = new Button(composite, SWT.CHECK);
				buttonInherit.setText("Use defaults");
				Text textField = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
				textField.setFont(JFaceResources.getTextFont());
				GridData layoutData = new GridData(GridData.FILL_BOTH);
				textField.setLayoutData(layoutData);
				layoutData.heightHint = 100;
				Link link = new Link(composite, SWT.NONE);
				link.setText(
						"Go to <a href=\"https://bnd.bndtools.org\">bnd.bndtools.org</a> to learn more about BND instructions and syntax.");
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
		};
		addPage(page);
		page.setImageDescriptor(ImageDescriptor.createFromURL(Analyzer.class.getResource("/img/bnd-64.png")));
		page.setTitle(page.getName());
		page.setDescription("Edit the BND Instructions to be used when wrapping this artifact is necessary");
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

}
