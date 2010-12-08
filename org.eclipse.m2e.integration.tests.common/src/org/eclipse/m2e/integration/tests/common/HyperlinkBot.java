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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swtbot.swt.finder.ReferenceBy;
import org.eclipse.swtbot.swt.finder.SWTBotWidget;
import org.eclipse.swtbot.swt.finder.Style;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBotControl;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hamcrest.SelfDescribing;

@SWTBotWidget( clasz = Hyperlink.class, style = @Style( name = "SWT.NONE", value = SWT.NONE ), preferredName = "hyperlink", referenceBy = { ReferenceBy.LABEL, ReferenceBy.MNEMONIC } )//$NON-NLS-1$
public class HyperlinkBot
    extends AbstractSWTBotControl<Hyperlink>
{

    public HyperlinkBot( Hyperlink h )
        throws WidgetNotFoundException
    {
        this( h, null );
    }

    public HyperlinkBot( Hyperlink h, SelfDescribing selfDescribing )
    {
        super( h, selfDescribing );
    }

    public HyperlinkBot click()
    {
        log.debug( MessageFormat.format( "Clicking on {0}", SWTUtils.getText( widget ) ) ); //$NON-NLS-1$
        waitForEnabled();
        Event e = createEvent();
        e.character = SWT.CR;
        notify( SWT.KeyDown, e );
        log.debug( MessageFormat.format( "Clicked on {0}", SWTUtils.getText( widget ) ) ); //$NON-NLS-1$
        return this;
    }
}
