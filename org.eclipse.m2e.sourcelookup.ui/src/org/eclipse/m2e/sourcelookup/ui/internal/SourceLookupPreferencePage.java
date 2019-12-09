/*******************************************************************************
 * Copyright (c) 2014 Igor Fedorenko
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

import org.eclipse.jdt.internal.launching.sourcelookup.advanced.AdvancedSourceLookupSupport;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


public class SourceLookupPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private Text vmArguments;

  private Text launchFileVMArguments;

  private Text launchFileAttribute;

  public SourceLookupPreferencePage() {
    setMessage("Manual configuration of dynamic source lookup");
    noDefaultAndApplyButton();
  }

  @Override
public void init(IWorkbench workbench) {}

  @Override
  protected Control createContents(Composite parent) {
	Composite composite = new Composite(parent, SWT.NONE);

    disableScrollingFor(composite);

    GridLayout gl_composite = new GridLayout(1, false);
    gl_composite.marginWidth = 0;
    gl_composite.marginHeight = 0;
    composite.setLayout(gl_composite);

    Label lblVMArguments = new Label(composite, SWT.NONE);
    lblVMArguments.setText("VM arguments:");

    GridDataFactory textGridDataFactory = GridDataFactory.createFrom(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1)).hint(parent.getSize().x, SWT.DEFAULT);

    vmArguments = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
	textGridDataFactory.applyTo(vmArguments);
    vmArguments.setText(AdvancedSourceLookupSupport.getJavaagentString());

    Label lblLaunchVMArguments = new Label(composite, SWT.NONE);
    lblLaunchVMArguments.setText(".launch file VM arguments:");

    launchFileVMArguments = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
    textGridDataFactory.applyTo(launchFileVMArguments);
    launchFileVMArguments.setText("-javaagent:${sourcelookup_agent_path}");

    Label lblLaunchFileAttribute = new Label(composite, SWT.NONE);
    lblLaunchFileAttribute.setText(".launch file attribute:");

    launchFileAttribute = new Text(composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
    launchFileAttribute.setText(
        "<stringAttribute key=\"org.eclipse.debug.core.source_locator_id\" value=\"org.eclipse.m2e.sourcelookupDirector\"/>\n");
    textGridDataFactory.applyTo(launchFileAttribute);

    return composite;
  }

  private void disableScrollingFor(Composite composite) {
    Composite temp = composite;
    while (temp != null && !(temp instanceof ScrolledComposite)) {
       temp = temp.getParent();
    }
    ScrolledComposite scrolledComposite = (ScrolledComposite)temp;
    if (scrolledComposite != null) {
        ControlAdapter resizeAndWrapRatherThanScroll = new ControlAdapter() {
            @Override public void controlResized(ControlEvent e) {
                if (composite.isVisible()) {
                    scrolledComposite.setMinWidth(scrolledComposite.getSize().x);
                }
            }
        };
        scrolledComposite.addControlListener(resizeAndWrapRatherThanScroll);
        composite.addDisposeListener(e -> scrolledComposite.removeControlListener(resizeAndWrapRatherThanScroll));
    }
  }
}
