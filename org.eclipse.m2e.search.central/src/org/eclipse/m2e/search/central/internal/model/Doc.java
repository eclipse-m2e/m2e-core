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
public class Doc {

  private String id;

  private String g;

  private String a;

  private String latestVersion;

  private String v;

  private long timestamp;

  private String[] ec;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getG() {
    return g;
  }

  public void setG(String g) {
    this.g = g;
  }

  public String getA() {
    return a;
  }

  public void setA(String a) {
    this.a = a;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public void setLatestVersion(String latestVersion) {
    this.latestVersion = latestVersion;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String[] getEc() {
    return ec;
  }

  public void setEc(String[] ec) {
    this.ec = ec;
  }

  public String getV() {
    return v;
  }

  public void setV(String version) {
    this.v = version;
  }

}
