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

import java.util.List;

import org.eclipse.m2e.integration.tests.common.matchers.ContainsMnemonic;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.MenuFinder;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.hamcrest.Matcher;


public class SWTBotMenuEx extends SWTBotMenu {

  public SWTBotMenuEx(SWTBotMenu bot) {
    super(bot.widget);
  }

  public SWTBotMenu menuContains(final String menuName) throws WidgetNotFoundException {
    final Matcher<MenuItem> matcher = ContainsMnemonic.containsMnemonic(menuName);
    MenuItem menuItem = syncExec(new WidgetResult<MenuItem>() {
      public MenuItem run() {
        Menu bar = widget.getMenu();
        List<MenuItem> menus = new MenuFinder().findMenus(bar, matcher, true);
        if(!menus.isEmpty()) {
          return menus.get(0);
        }
        return null;
      }
    });
    return new SWTBotMenu(menuItem, matcher);
  }

}
