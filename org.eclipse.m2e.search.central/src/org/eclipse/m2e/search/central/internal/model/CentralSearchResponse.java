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
 * @since 1.17.0
 */
public class CentralSearchResponse {

  private CentralResponse response;

  public CentralResponse getResponse() {
    return response;
  }

  public void setResponse(CentralResponse response) {
    this.response = response;
  }
}
