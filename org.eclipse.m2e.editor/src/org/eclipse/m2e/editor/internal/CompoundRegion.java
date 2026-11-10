/*******************************************************************************
 * Copyright (c) 2008-2026 Sonatype, Inc.
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

package org.eclipse.m2e.editor.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;


/**
 * A region combining several regions, shown together in the pom editor's marker hover.
 */
public class CompoundRegion implements IRegion {

  private int length = Integer.MIN_VALUE;

  private int offset = Integer.MAX_VALUE;

  private final List<IRegion> regions = new ArrayList<>();

  public final ITextViewer textViewer;

  public final int textOffset;

  public CompoundRegion(ITextViewer textViewer, int textOffset) {
    this.textViewer = textViewer;
    this.textOffset = textOffset;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  public void addRegion(IRegion region) {
    regions.add(region);
    int start = Math.min(region.getOffset(), offset);
    int end = Math.max(region.getOffset() + region.getLength(), offset + length);
    offset = start;
    length = end - start;
  }

  public List<IRegion> getRegions() {
    return regions;
  }

}
