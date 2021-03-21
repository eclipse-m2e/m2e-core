/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

package org.eclipse.m2e.scm.internal.wizards;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import org.apache.maven.model.Scm;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenProjectWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardLocationPage;
import org.eclipse.m2e.scm.MavenProjectScmInfo;
import org.eclipse.m2e.scm.ScmUrl;
import org.eclipse.m2e.scm.internal.Messages;


/**
 * Maven checkout wizard
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutWizard extends AbstractMavenProjectWizard implements IImportWizard, INewWizard {

  private ScmUrl[] urls;

  private String parentUrl;

  private MavenCheckoutLocationPage scheckoutPage;

  private MavenProjectWizardLocationPage locationPage;

  public MavenCheckoutWizard() {
    this(null);
    setNeedsProgressMonitor(true);
  }

  public MavenCheckoutWizard(ScmUrl[] urls) {
    setUrls(urls);
    setNeedsProgressMonitor(true);
    setWindowTitle(Messages.MavenCheckoutWizard_title);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
    super.init(workbench, selection);

    this.selection = selection;

    ArrayList<ScmUrl> urls = new ArrayList<ScmUrl>();
    IAdapterManager adapterManager = Platform.getAdapterManager();
    for(Object name : selection) {
      ScmUrl url = adapterManager.getAdapter(name, ScmUrl.class);
      if(url != null) {
        urls.add(url);
      }
    }
    setUrls(urls.toArray(new ScmUrl[urls.size()]));
  }

  private void setUrls(ScmUrl[] urls) {
    if(urls != null && urls.length > 0) {
      this.urls = urls;
      this.parentUrl = getParentUrl(urls);
    }
  }

  private String getParentUrl(ScmUrl[] urls) {
    if(urls.length == 1) {
      return urls[0].getUrl();
    }

    String parent = urls[0].getParentUrl();
    for(int i = 1; parent != null && i < urls.length; i++ ) {
      String url = urls[i].getParentUrl();
      if(!parent.equals(url)) {
        parent = null;
      }
    }
    return parent;
  }

  public void addPages() {
    scheckoutPage = new MavenCheckoutLocationPage(importConfiguration);
    scheckoutPage.setUrls(urls);
    scheckoutPage.setParent(parentUrl);

    locationPage = new MavenProjectWizardLocationPage(importConfiguration, //
        Messages.MavenCheckoutWizard_location1, Messages.MavenCheckoutWizard_location2, workingSets);
    locationPage.setLocationPath(SelectionUtil.getSelectedLocation(selection));

    addPage(scheckoutPage);
    addPage(locationPage);
  }

//  /** Adds the listeners after the page controls are created. */
//  public void createPageControls(Composite pageContainer) {
//    super.createPageControls(pageContainer);
//
//    locationPage.addListener(new SelectionAdapter() {
//      public void widgetSelected(SelectionEvent e) {
//        projectsPage.setScms(locationPage.getScms(new NullProgressMonitor()));
//      }
//    });
//    
//    projectsPage.setScms(locationPage.getScms(new NullProgressMonitor()));
//  }

  public boolean canFinish() {
    if(scheckoutPage.isCheckoutAllProjects() && scheckoutPage.isPageComplete()) {
      return true;
    }
    return super.canFinish();
  }

  public boolean performFinish() {
    if(!canFinish()) {
      return false;
    }

    final boolean checkoutAllProjects = scheckoutPage.isCheckoutAllProjects();

    Scm[] scms = scheckoutPage.getScms();

    final Collection<MavenProjectScmInfo> mavenProjects = new ArrayList<MavenProjectScmInfo>();
    for(Scm scm : scms) {
      String url = scm.getConnection();
      String revision = scm.getTag();

      if(url.endsWith("/")) { //$NON-NLS-1$
        url = url.substring(0, url.length() - 1);
      }

      int n = url.lastIndexOf("/"); //$NON-NLS-1$
      String label = (n == -1 ? url : url.substring(n)) + "/" + IMavenConstants.POM_FILE_NAME; //$NON-NLS-1$
      MavenProjectScmInfo projectInfo = new MavenProjectScmInfo(label, null, //
          null, revision, url, url);
      mavenProjects.add(projectInfo);
    }

    MavenProjectCheckoutJob job = new MavenProjectCheckoutJob(importConfiguration, checkoutAllProjects, workingSets) {
      protected Collection<MavenProjectScmInfo> getProjects(IProgressMonitor monitor) {
        return mavenProjects;
      }
    };

    if(!locationPage.isInWorkspace()) {
      job.setLocation(locationPage.getLocationPath().toFile());
    }

    job.schedule();

    return true;
  }

}
