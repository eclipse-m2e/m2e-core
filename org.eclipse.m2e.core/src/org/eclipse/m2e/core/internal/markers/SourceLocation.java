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
   * Column start marker attribute. This attribute is 1-relative and inclusive.
   */
  private final int columnStart;

  /**
   * Column end marker attribute. This attribute is 1-relative and inclusive.
   */
  private final int columnEnd;

  /**
   * The location of the cause for this marker. Can be null.
   */
  private MarkerLocation causeLocation;

  public MarkerLocation(int lineNumber, int columnStart, int columnEnd) {
    this(null /*resourcePath*/, lineNumber, columnStart, columnEnd);
  }

  public MarkerLocation(int lineNumber, int columnStart, int columnEnd, MarkerLocation causeLocation) {
    this(null /*resourcePath*/, lineNumber, columnStart, columnEnd);
    this.causeLocation = causeLocation;
  }

  public MarkerLocation(String resourcePath, int lineNumber, int columnStart, int columnEnd) {
    this.resourcePath = resourcePath;
    this.lineNumber = lineNumber;
    this.columnStart = columnStart;
    this.columnEnd = columnEnd;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getColumnStart() {
    return columnStart;
  }

  public int getColumnEnd() {
    return columnEnd;
  }

  public MarkerLocation getCauseLocation() {
    return causeLocation;
  }
//
//  public void setCauseLocation(MarkerLocation causeLocation) {
//    this.causeLocation = causeLocation;
//  }
}
