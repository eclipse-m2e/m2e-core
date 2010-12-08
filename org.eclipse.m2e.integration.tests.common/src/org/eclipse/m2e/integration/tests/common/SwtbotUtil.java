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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

public class SwtbotUtil {

    private static final boolean OS_MAC = "carbon".equals( SWT.getPlatform() ) || "cocoa".equals( SWT.getPlatform() );

	public static boolean waitForClose(SWTBotShell shell) {
		for (int i = 0; i < 50; i++) {
			if (!shell.isOpen()) {
				return true;
			}
			sleep(200);
		}
		shell.close();
		return false;
	}

	private static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException ex) {
			// ;)
		}
	}

	public static ICondition waitForLoad(final SWTBotTable table) {
		return new DefaultCondition() {
			public boolean test() throws Exception {
				return table.rowCount() != 0;
			}

			public String getFailureMessage() {
				return "Table still empty";
			}
		};

	}

    public static KeyStroke[] getUndoShortcut() {
    	return Keystrokes.toKeys(OS_MAC ? SWT.COMMAND : SWT.CONTROL, 'z');
    }

    public static KeyStroke[] getRedoShortcut() {
    	return Keystrokes.toKeys(OS_MAC ? SWT.COMMAND : SWT.CONTROL, 'y');
    }

    public static KeyStroke[] getPasteShortcut() {
    	return Keystrokes.toKeys(OS_MAC ? SWT.COMMAND : SWT.CONTROL, 'v');
    }

    public static KeyStroke[] getMaximizeEditorShortcut() {
    	return Keystrokes.toKeys(OS_MAC ? SWT.COMMAND : SWT.CONTROL, 'm');
    }

    public static KeyStroke[] getCloseShortcut() {
    	return Keystrokes.toKeys(OS_MAC ? SWT.COMMAND : SWT.CONTROL, 'w');
    }

}
