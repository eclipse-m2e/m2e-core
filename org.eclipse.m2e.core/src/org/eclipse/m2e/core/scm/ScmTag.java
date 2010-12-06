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

package org.eclipse.m2e.core.scm;

/**
 * An SCM wrapper for tags
 *
 * @author Eugene Kuleshov
 */
public class ScmTag {

  public static enum Type {
    HEAD, TAG, BRANCH, DATE;
  }
  
  private final String name;
  private final Type type;
  

  public ScmTag(String name) {
    this(name, null);
  }

  public ScmTag(String name, Type type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Returns tag name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Returns tag type
   */
  public Type getType() {
    return this.type;
  }
  
}

