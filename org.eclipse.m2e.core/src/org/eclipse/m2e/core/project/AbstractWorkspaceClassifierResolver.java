/*******************************************************************************
 * Copyright (c) 2017 Walmartlabs
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Anton Tanasenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.core.project;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;


/**
 * AbstractWorkspaceClassifierResolver
 *
 * @author atanasenko
 * @since 1.9
 */
public abstract class AbstractWorkspaceClassifierResolver
    implements IWorkspaceClassifierResolver, IExecutableExtension {

  private static final String ATTR_ID = "id";

  private static final String ATTR_NAME = "name";

  private String id;

  private String name;

  @Override
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
  }

  public int getPriority() {
    return 0;
  }

  public String toString() {
    return getName();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

}
