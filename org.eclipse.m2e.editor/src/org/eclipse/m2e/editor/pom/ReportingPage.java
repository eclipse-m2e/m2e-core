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
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.editor.composites.ReportingComposite;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.Reporting;
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
public class ReportingPage extends MavenPomEditorPage {

  private ReportingComposite reportingComposite;
  
  private SearchControl searchControl;

  public ReportingPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.reporting", Messages.ReportingPage_title); //$NON-NLS-1$
  }

  public void setActive(boolean active) {
    super.setActive(active);
    if(active) {
      reportingComposite.setSearchControl(searchControl);
      searchControl.getSearchText().setEditable(true);
    }
  }
  
  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();
    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.ReportingPage_form);

    Composite body = form.getBody();
    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginHeight = 0;
    body.setLayout(gridLayout);
    toolkit.paintBordersFor(body);

    reportingComposite = new ReportingComposite(body, this, SWT.NONE);
    reportingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(reportingComposite);

    searchControl = new SearchControl(Messages.ReportingPage_find, managedForm);
    
    IToolBarManager pageToolBarManager = form.getForm().getToolBarManager();
    pageToolBarManager.add(searchControl);
    pageToolBarManager.add(new Separator());
    
    form.updateToolBar();
    
//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    ValueProvider<Reporting> reportingProvider = new ValueProvider<Reporting>() {
      public Reporting getValue() {
        return model.getReporting();
      }

      public Reporting create(EditingDomain editingDomain, CompoundCommand compoundCommand) {
        Reporting reporting = PomFactory.eINSTANCE.createReporting();
        Command createReportingCommand = SetCommand.create(editingDomain, model, POM_PACKAGE.getModel_Reporting(),
            reporting);
        compoundCommand.append(createReportingCommand);
        return reporting;
      }
    };
    reportingComposite.loadData(this, reportingProvider);
  }

  public void updateView(final Notification notification) {
    Display.getDefault().asyncExec(new Runnable(){
      public void run(){
        reportingComposite.updateView(ReportingPage.this, notification);
      }
    });
  }

}
