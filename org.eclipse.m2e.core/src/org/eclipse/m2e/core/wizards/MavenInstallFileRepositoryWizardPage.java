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

package org.eclipse.m2e.core.wizards;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.m2e.model.edit.pom.Repository;


/**
 * MavenInstallFileRepositoryWizardPage used to chose to repositories for installing artifacts.
 * 
 * @author Mike Haller
 */
public class MavenInstallFileRepositoryWizardPage extends WizardPage {

  private final IFile pomFile;

  public MavenInstallFileRepositoryWizardPage(IFile pomFile) {
    super("mavenInstallFileRepositorySelectionPage");
    setTitle("Repository Selection Page");
    setDescription("Select the repositories where to deploy the artifact");
    this.pomFile = pomFile;
  }

  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Label repositoriesLabel = new Label(container, SWT.NONE);
    repositoriesLabel.setData("name", "repositoriesLabel");
    repositoriesLabel.setText("&Repositories:");

    CheckboxTreeViewer repositoryViewer = new CheckboxTreeViewer(container, SWT.BORDER);
    repositoryViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    // repositoryViewer.setInput(pomFile);
    repositoryViewer.setLabelProvider(new RepositoryLabelProvider());
    repositoryViewer.setContentProvider(new RepositoriesContentProvider());
    repositoryViewer.setCheckedElements(new Object[] {RepositoriesContentProvider.LOCAL_REPOSITORY});

    initRepositories();
    
    setControl(container);
  }

  private void initRepositories() {
    // TODO Auto-generated method initRepositories
    
  }

  /**
   * RepositoryLabelProvider prints the name of a repository, and if available, its URL.
   */
  public class RepositoryLabelProvider extends LabelProvider {

    public String getText(Object element) {
      if(element instanceof Repository) {
        Repository repository = (Repository) element;
        if(repository.getUrl() != null && repository.getUrl().trim().length() > 0) {
          return repository.getName() + "(" + repository.getUrl() + ")";
        }
        return repository.getName();
      }
      return super.getText(element);
    }

  }
  
  /**
   * LocalRepositoriesContentProvider provides a list of local and remote repositories.
   * <p>
   * If no repositories could be found in the POM Maven Model, a default local repository is shown.
   */
  public static class RepositoriesContentProvider implements ITreeContentProvider {

    public static final Repository LOCAL_REPOSITORY = createLocalRepository();

    private static Repository createLocalRepository() {
      Repository repository = PomFactory.eINSTANCE.createRepository();
      repository.setId("local");
      repository.setName("Local Repository");
      try {
        File localRepositoryDir = new File(MavenPlugin.getDefault().getMaven().getLocalRepository().getBasedir());
        repository.setUrl(localRepositoryDir.toURI().toString());
      } catch(CoreException ex) {
        MavenLogger.log("Unable to determine local repository URL, using default", ex);
      }
      return repository;
    }

    public Object[] getElements(Object arg0) {
      return repositories.toArray();
    }
    
    private EList<Repository> repositories;

    public Object[] getChildren(Object arg0) {
      return null;
    }

    public Object getParent(Object arg0) {
      return null;
    }

    public boolean hasChildren(Object arg0) {
      return false;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer arg0, Object arg1, Object newInput) {
      if(newInput instanceof Model) {
        Model model = (Model) newInput;
        repositories = model.getRepositories();
      }
    }

  }
  
}
