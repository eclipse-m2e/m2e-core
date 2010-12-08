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

package org.eclipse.m2e.integration.tests.common.matchers;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.utils.SWTUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class ContainsMnemonic<T extends Widget> extends AbstractMatcher<T> {
	private final String text;

	ContainsMnemonic(String text) {
		this.text = text;
	}

	String getText(Object obj) throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		String text = ((String) SWTUtils.invokeMethod(obj, "getText"))
				.replaceAll(Text.DELIMITER, "\n");
		return text.replaceAll("&", "").split("\t")[0];
	}

	public void describeTo(Description description) {
		description
				.appendText("contains mnemonic '").appendText(text).appendText("'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean doMatch(Object obj) {
		try {
			return getText(obj).contains(text);
		} catch (Exception e) {
			// do nothing
		}
		return false;
	}

	@Factory
	public static <T extends Widget> Matcher<T> containsMnemonic(String text) {
		return new ContainsMnemonic<T>(text);
	}

}
