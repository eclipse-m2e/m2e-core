/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.editor.composites.DependenciesComposite;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class DependenciesPage extends MavenPomEditorPage {

  private DependenciesComposite dependenciesComposite;

  private SearchControl searchControl;

  public DependenciesPage(MavenPomEditor pomEditor) {
    super(pomEditor, IMavenConstants.PLUGIN_ID + ".pom.dependencies", Messages.DependenciesPage_title); //$NON-NLS-1$
  }

  public void dispose() {
    if(dependenciesComposite != null) {
      dependenciesComposite.dispose();
    }
    super.dispose();
  }

  public void setActive(boolean active) {
    super.setActive(active);
    if(active) {
      dependenciesComposite.setSearchControl(searchControl);
      searchControl.getSearchText().setEditable(true);
    }
  }

  protected void createFormContent(IManagedForm managedForm) {
    FormToolkit toolkit = managedForm.getToolkit();

    ScrolledForm form = managedForm.getForm();
    form.setText(Messages.DependenciesPage_form);

    form.getBody().setLayout(new GridLayout(1, true));

    dependenciesComposite = new DependenciesComposite(form.getBody(), this, SWT.NONE, pomEditor);
    dependenciesComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    toolkit.adapt(dependenciesComposite);
    Link link = new Link(managedForm.getForm().getBody(), SWT.NONE);
    toolkit.adapt(link, true, true);
    link.setText(Messages.DependenciesPage_exclusions_link);
    link.addSelectionListener(SelectionListener
        .widgetSelectedAdapter(e -> pomEditor.setActivePage(IMavenConstants.PLUGIN_ID + ".pom.dependencyTree")));

    searchControl = new SearchControl(Messages.DependenciesPage_find, managedForm);

    IToolBarManager pageToolBarManager = form.getForm().getToolBarManager();
    pageToolBarManager.add(searchControl);
    pageToolBarManager.add(new Separator());

    form.updateToolBar();

//    form.pack();

    super.createFormContent(managedForm);
  }

  public void loadData() {
    dependenciesComposite.loadData();
  }

  public void updateView(final Notification notification) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.m2e.editor.pom.MavenPomEditorPage#mavenProjectHasChanged()
   */
  @Override
  public void mavenProjectHasChanged() {
    if(dependenciesComposite != null) {
      dependenciesComposite.mavenProjectHasChanged();
    }
  }

}
