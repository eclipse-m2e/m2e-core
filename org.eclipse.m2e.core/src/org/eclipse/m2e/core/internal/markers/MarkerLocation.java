/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.internal.markers;

public class MarkerLocation {
  /**
   * Must be null if the location applies to the resource that owns the marker.
   */
  private final String resourcePath;

  /**
   * Line number marker attribute. This attribute is 1-relative.
   */
  private final int lineNumber;

  /**
   * Character start marker attribute. This attribute is zero-relative and inclusive.
   */
  private final int charStart;

  /**
   * Character end marker attribute. This attribute is zero-relative and exclusive.
   */
  private final int charEnd;

  /**
   * The location of the cause for this marker. Can be null.
   */
  private MarkerLocation causeLocation;

  public MarkerLocation(int lineNumber, int charStart, int charEnd) {
    this(null /*resourcePath*/, lineNumber, charStart, charEnd);
  }

  public MarkerLocation(int lineNumber, int charStart, int charEnd, MarkerLocation causeLocation) {
    this(null /*resourcePath*/, lineNumber, charStart, charEnd);
    this.causeLocation = causeLocation;
  }

  public MarkerLocation(String resourcePath, int lineNumber, int charStart, int charEnd) {
    this.resourcePath = resourcePath;
    this.lineNumber = lineNumber;
    this.charStart = charStart;
    this.charEnd = charEnd;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getCharStart() {
    return charStart;
  }

  public int getCharEnd() {
    return charEnd;
  }

  public MarkerLocation getCauseLocation() {
    return causeLocation;
  }
//
//  public void setCauseLocation(MarkerLocation causeLocation) {
//    this.causeLocation = causeLocation;
//  }
}
