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

import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.results.ArrayResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;

public class SonatypeSWTBotTree extends SWTBotTree {

	public SonatypeSWTBotTree(Tree tree, SelfDescribing description)
			throws WidgetNotFoundException {
		super(tree, description);
	}

	public SonatypeSWTBotTree(Tree tree) throws WidgetNotFoundException {
		super(tree);
	}

	public SonatypeSWTBotTree(SWTBotTree tree) {
		this(tree.widget, getDescription(tree));
	}

	private static SelfDescribing getDescription(SWTBotTree tree) {
		try {
			Field f = AbstractSWTBot.class.getDeclaredField("description");
			return (SelfDescribing) f.get(tree);
		} catch (Exception e) {
			return null;
		}
	}

	public SWTBotTreeItem getTreeItem(final Matcher<Widget>... matchers) {
		try {
			new SWTBot().waitUntil(new DefaultCondition() {
				public String getFailureMessage() {
					return "Could not find node with text " + Arrays.toString(matchers) + "\nAvailable items: " + Arrays.toString(getItemsText()); //$NON-NLS-1$
				}

				public boolean test() throws Exception {
					return getItem(matchers) != null;
				}
			});
		} catch (TimeoutException e) {
			throw new WidgetNotFoundException(
					"Timed out waiting for tree item " + Arrays.toString(matchers), e); //$NON-NLS-1$
		}
		return new SWTBotTreeItem(getItem(matchers));
	}

	protected String[] getItemsText() {
		return syncExec(new ArrayResult<String>() {
			public String[] run() {
				TreeItem[] treeItems = widget.getItems();
				String[] names = new String[treeItems.length];

				for (int i = 0; i < treeItems.length; i++) {
					TreeItem treeItem = treeItems[i];
					names[i] = treeItem.getText();
				}

				return names;
			}
		});
	}

	protected TreeItem getItem(final Matcher<Widget>... matchers) {
		return syncExec(new WidgetResult<TreeItem>() {
			public TreeItem run() {
				TreeItem[] treeItems = widget.getItems();
				item: for (TreeItem treeItem : treeItems) {
					for (Matcher<Widget> matcher : matchers) {
						if (!matcher.matches(treeItem)) {
							continue item;
						}
					}

					return treeItem;
				}

				return null;
			}
		});
	}
}
