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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.project.configurator.ILifecycleMapping;
import org.eclipse.m2e.core.ui.internal.lifecycle.ILifecyclePropertyPage;
import org.eclipse.m2e.core.ui.internal.lifecycle.LifecycleMappingPropertyPageFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Maven project preference page
 *
 * @author Dan Yocum
 */
public class MavenProjectLifecycleMappingPage extends PropertyPage{

  private ILifecyclePropertyPage currentPage;
  
  public MavenProjectLifecycleMappingPage() {
    
    setTitle(""); //$NON-NLS-1$
  }

  protected Control createContents(Composite parent) {
    currentPage = loadCurrentPage((IProject)getElement());
    setMessage(currentPage.getName());
    return currentPage.createContents(parent);
  }
  
  private ILifecyclePropertyPage getErrorPage(String msg){
    SimpleLifecycleMappingPropertyPage p = new SimpleLifecycleMappingPropertyPage(msg);
    return p;
  }
  
  private ILifecyclePropertyPage getPage(ILifecycleMapping lifecycleMapping){
    ILifecyclePropertyPage page = LifecycleMappingPropertyPageFactory.getFactory().getPageForId(lifecycleMapping.getId(), getProject(), this.getShell());
    if(page == null){
      page = getErrorPage(Messages.MavenProjectLifecycleMappingPage_error_no_page);
      page.setName(lifecycleMapping.getName());
    }
    return page;
  }

  private ILifecyclePropertyPage loadCurrentPage(IProject project){
    ILifecyclePropertyPage page = null;
    try{
      ILifecycleMapping lifecycleMapping = LifecycleMappingPropertyPageFactory.getLifecycleMapping(project);
      if(lifecycleMapping == null){
        return getErrorPage(Messages.MavenProjectLifecycleMappingPage_error_no_strategy);
      }
      page = getPage(lifecycleMapping);
      return page;
    } catch(CoreException ce){
      MavenLogger.log(ce);
      SimpleLifecycleMappingPropertyPage p = new SimpleLifecycleMappingPropertyPage(Messages.MavenProjectLifecycleMappingPage_error_page_error);
      return p;
    }
  }

  protected void performDefaults() {
    currentPage.performDefaults();
  }

  protected IProject getProject() {
    return (IProject) getElement();
  }

  public boolean performOk() {
    return currentPage.performOk();
  }

  public void setElement(IAdaptable element){
    if(currentPage != null && element instanceof IProject){
      currentPage.setProject((IProject)element);
    }
    super.setElement(element);
  }

}

