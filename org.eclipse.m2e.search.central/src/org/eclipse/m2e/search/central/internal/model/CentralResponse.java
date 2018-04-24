/*******************************************************************************
 * Copyright (c) 2018 Sonatype Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.search.central.internal.model;

/**
 * @author Matthew Piggott
 * @since 1.16.0
 */
public class CentralResponse {

  private Doc[] docs;

  private int numFound;

  private int start;

  public Doc[] getDocs() {
    return docs;
  }

  public int getNumFound() {
    return numFound;
  }

  public int getStart() {
    return start;
  }

  public void setDocs(Doc[] docs) {
    this.docs = docs;
  }

  public void setNumFound(int numFound) {
    this.numFound = numFound;
  }

  public void setStart(int start) {
    this.start = start;
  }
}
