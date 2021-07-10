/*******************************************************************************
 * Copyright (c) 2020, 2023 Christoph Läubrich and others
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

import java.util.Objects;

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.pde.target.BNDInstructions;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
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
	private BNDInstructions bndInstructions;

	public MavenArtifactInstructionsWizard(BNDInstructions bndInstructions) {
		this.bndInstructions = bndInstructions;
		this.instructions = bndInstructions.instructions();
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
				link.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> Program.launch(e.text)));
				if (usedefaults) {
					textField.setText(BNDInstructions.DEFAULT_INSTRUCTIONS);
				} else {
					textField.setText(instructions);
				}
				buttonInherit.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
						boolean selection = buttonInherit.getSelection();
						usedefaults = selection;
						textField.setEnabled(!selection);
						link.setEnabled(!selection);
				}));
				textField.addModifyListener(e -> instructions = textField.getText());
				buttonInherit.setSelection(usedefaults);
				textField.setEnabled(!buttonInherit.getSelection());
				link.setEnabled(!buttonInherit.getSelection());
				setControl(composite);
			}

			@Override
			public void performHelp() {
				IWizardContainer container = getContainer();
				if (container instanceof TrayDialog dialog) {
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

	protected BNDInstructions getInstructions() {
		return new BNDInstructions(bndInstructions.key(), usedefaults ? null : instructions);
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
		Objects.requireNonNull(instructions, "BNDInstructions can't be null");
		MavenArtifactInstructionsWizard wizard = new MavenArtifactInstructionsWizard(instructions);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		dialog.setMinimumPageSize(800, 600);
		if (dialog.open() == Window.OK) {
			return wizard.getInstructions();
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
