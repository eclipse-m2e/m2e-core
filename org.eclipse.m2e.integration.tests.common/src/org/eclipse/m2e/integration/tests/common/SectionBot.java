/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.integration.tests.common;

import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.ReferenceBy;
import org.eclipse.swtbot.swt.finder.SWTBotWidget;
import org.eclipse.swtbot.swt.finder.Style;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBotControl;
import org.eclipse.ui.forms.widgets.Section;
import org.hamcrest.SelfDescribing;

@SWTBotWidget(clasz = Section.class, style = @Style(name = "SWT.NONE", value = SWT.NONE), preferredName = "section", referenceBy = { ReferenceBy.LABEL, ReferenceBy.MNEMONIC })//$NON-NLS-1$
public class SectionBot extends AbstractSWTBotControl<Section> {

	public SectionBot(Section s) throws WidgetNotFoundException {
		this(s, null);
	}

	public SectionBot(Section s, SelfDescribing selfDescribing) {
		super(s, selfDescribing);
	}

	public boolean isExpanded() {
		return syncExec(new BoolResult() {
			public Boolean run() {
				return widget.isExpanded();
			}
		});
	}

	public void setExpanded(final boolean expand) {
		waitForEnabled();
		asyncExec(new VoidResult() {
			public void run() {
				log
						.debug(MessageFormat
								.format(
										"Expanding section {0}. Setting state to {1}", widget, (expand ? "expanded" //$NON-NLS-1$ //$NON-NLS-2$
												: "collapsed"))); //$NON-NLS-1$
				widget.setExpanded(expand);
			}
		});
	}

	public void expand() {
		waitForEnabled();
		asyncExec(new VoidResult() {
			public void run() {
				if (!widget.isExpanded()) {
					log.debug(MessageFormat.format("Expanding section {0}.",
							widget));
					widget.setExpanded(true);
				}
			}
		});
	}

	public void collapse() {
		waitForEnabled();
		asyncExec(new VoidResult() {
			public void run() {
				if (widget.isExpanded()) {
					log.debug(MessageFormat.format("Collapsing section {0}.",
							widget));
					widget.setExpanded(false);
				}
			}
		});
	}
}
