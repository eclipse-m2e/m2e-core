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

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.ui.internal.lifecycle.LifecycleMappingPropertyPageFactory;

public class MissingLifecycleMappingPropertyPage extends SimpleLifecycleMappingPropertyPage {

  public MissingLifecycleMappingPropertyPage() {
    super(Messages.MissingLifecycleMappingPropertyPage_title);
  }

  protected String getMessage() {
    try {
      ILifecycleMapping lifecycleMapping = LifecycleMappingPropertyPageFactory.getLifecycleMapping(getProject());
      if(lifecycleMapping == null) {
        return Messages.MissingLifecycleMappingPropertyPage_error;
      }
    } catch(CoreException ex) {
      // this is odd, but lets ignore it anyways
    }
    return super.getMessage();
  }
}
