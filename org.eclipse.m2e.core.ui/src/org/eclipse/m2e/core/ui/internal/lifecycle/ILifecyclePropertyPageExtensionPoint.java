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

package org.eclipse.m2e.core.ui.internal.lifecycle;

/**
 * ILifecyclePropertyPageExtensionPoint
 *
 * @author dyocum
 */
public interface ILifecyclePropertyPageExtensionPoint {
  /**
   * The name of the page. This will be displayed in the title of the properties page.
   * @return
   */
  public String getName();
  
  /**
   * Name of the page, called when the extension point is read in
   */
  public void setName(String name);
  
  /**
   * Get the id of the property page as defined in the extension point
   * @return
   */
  public String getPageId();
  
  /**
   * Set the id of the property page, called when extension point is read
   */
  public void setPageId(String id);
  
  /**
   * Sets the id of the lifecycle mapping strategy that this property page is
   * associated with
   * @param lifecycleMappingId
   */
  public void setLifecycleMappingId(String lifecycleMappingId);
  
  /**
   * Gets the id of the lifecycle mapping strategy
   * @return
   */
  public String getLifecycleMappingId();
  
}
