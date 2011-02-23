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

public class SourceLocation {
  /**
   * Absolute path of the resource to which this location applies. Can be null.
   */
  private final String resourcePath;

  /**
   * An id for the resource to which this location applies. For example, it can be the Maven GAV for a pom file. Can be
   * null.
   */
  private final String resourceId;

  /**
   * This attribute is 1-relative.
   */
  private final int lineNumber;

  /**
   * This attribute is 1-relative and inclusive.
   */
  private final int columnStart;

  /**
   * This attribute is 1-relative and inclusive.
   */
  private final int columnEnd;

  /**
   * A location linked to this location. Can be null.
   */
  private SourceLocation linkedLocation;

  public SourceLocation(int lineNumber, int columnStart, int columnEnd) {
    this(null /*resourcePath*/, null /*resourceId*/, lineNumber, columnStart, columnEnd);
  }

  public SourceLocation(int lineNumber, int columnStart, int columnEnd, SourceLocation linkedLocation) {
    this(null /*resourcePath*/, null /*resourceId*/, lineNumber, columnStart, columnEnd);
    this.linkedLocation = linkedLocation;
  }

  public SourceLocation(String resourcePath, String resourceId, int lineNumber, int columnStart, int columnEnd) {
    this.resourcePath = resourcePath;
    this.resourceId = resourceId;
    this.lineNumber = lineNumber;
    this.columnStart = columnStart;
    this.columnEnd = columnEnd;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public String getResourceId() {
    return resourceId;
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

  public SourceLocation getLinkedLocation() {
    return linkedLocation;
  }
}
