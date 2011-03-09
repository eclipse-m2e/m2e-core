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

package org.eclipse.m2e.core.ui.internal.dialogs;

import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;


/**
 * A dialog superclass, featuring position and size settings.
 */
public abstract class AbstractMavenDialog extends SelectionStatusDialog {

  protected static final String KEY_WIDTH = "width"; //$NON-NLS-1$
  protected static final String KEY_HEIGHT = "height"; //$NON-NLS-1$
  private static final String KEY_X = "x"; //$NON-NLS-1$
  private static final String KEY_Y = "y"; //$NON-NLS-1$
  protected IDialogSettings settings;
  private Point location;
  private Point size;

  /**
   * @param parent
   */
  protected AbstractMavenDialog(Shell parent, String settingsSection) {
    super(parent);

    IDialogSettings pluginSettings = M2EUIPluginActivator.getDefault().getDialogSettings();
    IDialogSettings settings = pluginSettings.getSection(settingsSection);
    if(settings == null) {
      settings = new DialogSettings(settingsSection);
      settings.put(KEY_WIDTH, 480);
      settings.put(KEY_HEIGHT, 450);
      pluginSettings.addSection(settings);
    }
    this.settings = settings;
  }

  protected Point getInitialSize() {
    Point result = super.getInitialSize();
    if(size != null) {
      result.x = Math.max(result.x, size.x);
      result.y = Math.max(result.y, size.y);
      Rectangle display = getShell().getDisplay().getClientArea();
      result.x = Math.min(result.x, display.width);
      result.y = Math.min(result.y, display.height);
    }
    return result;
  }

  protected Point getInitialLocation(Point initialSize) {
    Point result = super.getInitialLocation(initialSize);
    if(location != null) {
      result.x = location.x;
      result.y = location.y;
      Rectangle display = getShell().getDisplay().getClientArea();
      int xe = result.x + initialSize.x;
      if(xe > display.width) {
        result.x -= xe - display.width;
      }
      int ye = result.y + initialSize.y;
      if(ye > display.height) {
        result.y -= ye - display.height;
      }
    }
    return result;
  }

  public boolean close() {
    writeSettings();
    return super.close();
  }

  /**
   * Initializes itself from the dialog settings with the same state as at the
   * previous invocation.
   */
  protected void readSettings() {
    try {
      int x = settings.getInt(KEY_X);
      int y = settings.getInt(KEY_Y);
      location = new Point(x, y);
    } catch(NumberFormatException e) {
      location = null;
    }
    try {
      int width = settings.getInt(KEY_WIDTH);
      int height = settings.getInt(KEY_HEIGHT);
      size = new Point(width, height);
  
    } catch(NumberFormatException e) {
      size = null;
    }
  }

  /**
   * Stores it current configuration in the dialog store.
   */
  private void writeSettings() {
    Point location = getShell().getLocation();
    settings.put(KEY_X, location.x);
    settings.put(KEY_Y, location.y);
  
    Point size = getShell().getSize();
    settings.put(KEY_WIDTH, size.x);
    settings.put(KEY_HEIGHT, size.y);
  }
}
