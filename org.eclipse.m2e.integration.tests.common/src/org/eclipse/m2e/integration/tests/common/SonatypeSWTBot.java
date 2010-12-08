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

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.allOf;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withId;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withLabel;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withStyle;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withText;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.hamcrest.Matcher;

public class SonatypeSWTBot
    extends SWTWorkbenchBot
{

    public SWTBotText textWithName( String value )
    {
        return textWithName( value, 0 );
    }

    @SuppressWarnings( "unchecked" )
    public SWTBotText textWithName( String value, int index )
    {
        Matcher matcher = allOf( widgetOfType( Text.class ), withId( "name", value ) );
        return new SWTBotText( (Text) widget( matcher, index ), matcher );
    }

    public SWTBotCheckBox checkBoxWithName( String name )
    {
        return checkBoxWithName( name, 0 );
    }

    @SuppressWarnings( "unchecked" )
    public SWTBotCCombo ccomboBoxWithName( String value, int index )
    {
        Matcher matcher = allOf( widgetOfType( CCombo.class ), withId( "name", value ) );
        return new SWTBotCCombo( (CCombo) widget( matcher, index ), matcher );
    }

    public SWTBotCCombo ccomboBoxWithName( String value )
    {
        return ccomboBoxWithName( value, 0 );
    }

    @SuppressWarnings( "unchecked" )
    public SWTBotCheckBox checkBoxWithName( String name, int index )
    {
        Matcher matcher =
            allOf( widgetOfType( Button.class ), withId( "name", name ), withStyle( SWT.CHECK, "SWT.CHECK" ) );
        return new SWTBotCheckBox( (Button) widget( matcher, index ), matcher );
    }

    @SuppressWarnings( "unchecked" )
    public HyperlinkBot hyperlink( String text )
    {
        return new HyperlinkBot( (Hyperlink) widget( allOf( widgetOfType( Hyperlink.class ), withText( text ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public HyperlinkBot hyperlinkWithLabel( String label )
    {
        return new HyperlinkBot( (Hyperlink) widget( allOf( widgetOfType( Hyperlink.class ), withLabel( label ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public SectionBot section( String title )
    {
        return new SectionBot( (Section) widget( allOf( widgetOfType( Section.class ), withText( title ) ) ) );
    }

    @SuppressWarnings( "unchecked" )
    public SectionBot sectionWithName( String value )
    {
        return new SectionBot( (Section) widget( allOf( widgetOfType( Section.class ), withId( "name", value ) ) ) );
    }

    public boolean waitForShellToClose( String title )
    {
        SWTBotShell shell = activeShell();
        if ( title != null && title.equals( shell.getText() ) )
        {
            return waitForShellToClose( shell );
        }
        return false;
    }

    public boolean waitForShellToClose( SWTBotShell shell )
    {
        if ( shell != null )
        {
            for ( int i = 0; i < 50; i++ )
            {
                if ( !shell.isOpen() )
                {
                    return true;
                }
                sleep( 200 );
            }
            shell.close();
        }
        return false;
    }

    @Override
    public SonatypeSWTBotTree tree()
    {
        return new SonatypeSWTBotTree( super.tree() );
    }
}
