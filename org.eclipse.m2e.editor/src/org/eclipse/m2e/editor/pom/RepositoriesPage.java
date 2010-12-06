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

package org.eclipse.m2e.editor.pom;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.editor.composites.RepositoriesComposite;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.DistributionManagement;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * @author Eugene Kuleshov
 */
public class RepositoriesPage extends MavenPomEditorPage {

  private RepositoriesComposite repositoriesComposite;
  
  public RepositoriesPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.repositories", Messages.RepositoriesPage_title); //$NON-NLS-1$
  }
  
  public void dispose() {
    if(repositoriesComposite!=null) {
      repositoriesComposite.dispose();
    }
    super.dispose();
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();

    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.RepositoriesPage_form);
    
    Composite body = form.getBody();
    body.setLayout(new GridLayout(1, true));

    repositoriesComposite = new RepositoriesComposite(body, SWT.NONE);
    repositoriesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(repositoriesComposite);
    
    // form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    ValueProvider<DistributionManagement> distributionManagementProvider = new ValueProvider<DistributionManagement>() {
      public DistributionManagement getValue() {
        return model.getDistributionManagement();
      }
      
      public DistributionManagement create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        DistributionManagement dm = PomFactory.eINSTANCE.createDistributionManagement();
        Command command = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_DistributionManagement(), dm);
        compoundCommand.append(command);
        return dm;
      }
    };
    
    repositoriesComposite.loadData(this, model, distributionManagementProvider);
  }
  
  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        repositoriesComposite.updateView(RepositoriesPage.this, notification);
      }
    });
    
  }
  

//  public static class PairNode {
//    final String label;
//    final Object value;
//  
//    public PairNode(String label, Object value) {
//      this.label = label;
//      this.value = value;
//    }
//  }
//
//
//  public static class ExclusionsNode {
//
//    final String label;
//    final Exclusion exclusions;
//
//    public ExclusionsNode(String label, Exclusion exclusions) {
//      this.label = label;
//      this.exclusions = exclusions;
//    }
//  
//  }

}
