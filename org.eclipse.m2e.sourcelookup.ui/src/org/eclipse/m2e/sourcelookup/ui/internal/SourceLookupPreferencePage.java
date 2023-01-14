/*******************************************************************************
 * Copyright (c) 2014-2023 Igor Fedorenko
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
package org.eclipse.m2e.sourcelookup.ui.internal;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SourceLookupPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public SourceLookupPreferencePage() {
		setMessage("Manual configuration of dynamic source lookup");
		noDefaultAndApplyButton();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		disableScrollingFor(composite);

		GridLayoutFactory.swtDefaults().numColumns(1).equalWidth(false).margins(0, 0).applyTo(composite);

		createLabel("VM arguments:", composite);

		GridDataFactory textGridDataFactory = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.FILL).grab(true, false)
				.span(1, 1).hint(parent.getSize().x, SWT.DEFAULT);

		@SuppressWarnings("restriction")
		String javaagentString = org.eclipse.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport
				.getJavaagentString();
		createTextField(javaagentString, composite, textGridDataFactory);

		createLabel(".launch file VM arguments:", composite);

		createTextField("-javaagent:${sourcelookup_agent_path}", composite, textGridDataFactory);

		createLabel(".launch file attribute:", composite);

		createTextField(
				"<stringAttribute key=\"org.eclipse.debug.core.source_locator_id\" value=\"org.eclipse.m2e.sourcelookupDirector\"/>\n",
				composite, textGridDataFactory);

		return composite;
	}

	private void createLabel(String text, Composite composite) {
		new Label(composite, SWT.NONE).setText(text);
	}

	private void createTextField(String text, Composite composite, GridDataFactory gridDataFactory) {
		Text textField = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		gridDataFactory.applyTo(textField);
		textField.setText(text);
	}

	private void disableScrollingFor(Composite composite) {
		Composite temp = composite;
		while (temp != null && !(temp instanceof ScrolledComposite)) {
			temp = temp.getParent();
		}
		ScrolledComposite scrolledComposite = (ScrolledComposite) temp;
		if (scrolledComposite != null) {
			ControlListener resizeAndWrapRatherThanScroll = ControlListener.controlResizedAdapter(e -> {
				if (composite.isVisible()) {
					scrolledComposite.setMinWidth(scrolledComposite.getSize().x);
				}
			});
			scrolledComposite.addControlListener(resizeAndWrapRatherThanScroll);
			composite.addDisposeListener(e -> scrolledComposite.removeControlListener(resizeAndWrapRatherThanScroll));
		}
	}
}
