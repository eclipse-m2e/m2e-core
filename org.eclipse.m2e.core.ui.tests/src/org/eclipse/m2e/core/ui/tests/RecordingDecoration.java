/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.core.ui.tests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;


/**
 * {@link IDecoration} recording the prefixes and suffixes added by a lightweight decorator.
 */
class RecordingDecoration implements IDecoration {

  final List<String> prefixes = new ArrayList<>();

  final List<String> suffixes = new ArrayList<>();

  @Override
  public void addPrefix(String prefix) {
    prefixes.add(prefix);
  }

  @Override
  public void addSuffix(String suffix) {
    suffixes.add(suffix);
  }

  @Override
  public void addOverlay(ImageDescriptor overlay) {
    // not recorded
  }

  @Override
  public void addOverlay(ImageDescriptor overlay, int quadrant) {
    // not recorded
  }

  @Override
  public void setForegroundColor(Color color) {
    // not recorded
  }

  @Override
  public void setBackgroundColor(Color color) {
    // not recorded
  }

  @Override
  public void setFont(Font font) {
    // not recorded
  }

  @Override
  public IDecorationContext getDecorationContext() {
    return DecorationContext.DEFAULT_CONTEXT;
  }
}
