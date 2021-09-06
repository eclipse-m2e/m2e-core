/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.util.HashSet;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;


/**
 * Group of controls with the same width
 *
 * @author Eugene Kuleshov
 */
public class WidthGroup extends ControlAdapter {

  private final HashSet<Control> controls = new HashSet<>();

  @Override
  public void controlResized(ControlEvent e) {
    int maxWidth = 0;
    for(Control c : this.controls) {
      int width = c.getSize().x;
      if(width > maxWidth) {
        maxWidth = width;
      }
    }
    if(maxWidth > 0) {
      for(Control c : this.controls) {
        GridData gd = (GridData) c.getLayoutData();
        gd.widthHint = maxWidth;
        c.getParent().layout();
      }
    }
  }

  public void addControl(Control control) {
    controls.add(control);
    control.getParent().layout();
  }

}
