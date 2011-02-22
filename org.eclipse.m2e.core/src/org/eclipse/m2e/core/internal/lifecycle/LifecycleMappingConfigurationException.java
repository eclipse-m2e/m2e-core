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

package org.eclipse.m2e.core.internal.lifecycle;

import org.eclipse.m2e.core.internal.markers.SourceLocation;

public class LifecycleMappingConfigurationException extends RuntimeException {
  private static final long serialVersionUID = 713512516951833457L;

  private SourceLocation location;

  public LifecycleMappingConfigurationException(String message) {
    super(message);
  }

  public LifecycleMappingConfigurationException(String message, SourceLocation location) {
    super(message);
    this.location = location;
  }

  public LifecycleMappingConfigurationException(String message, Throwable cause) {
    super(message + " Cause: " + cause.getMessage(), cause);
  }

  public LifecycleMappingConfigurationException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  public SourceLocation getLocation() {
    return location;
  }

  public void setLocation(SourceLocation location) {
    this.location = location;
  }
}
