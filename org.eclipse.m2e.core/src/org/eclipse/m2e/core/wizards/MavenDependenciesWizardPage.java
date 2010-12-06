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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.MavenImages;
import org.eclipse.m2e.core.core.Messages;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.dialogs.MavenRepositorySearchDialog;


/**
 * Wizard page for gathering information about Maven artifacts. Allows to select 
 * artifacts from the repository index.
 */
public class MavenDependenciesWizardPage extends AbstractMavenWizardPage {

  /** 
   * Viewer containing dependencies
   */
  TableViewer dependencyViewer;
  
  private Dependency[] dependencies;
  
  /**
   * Listeners notified about all changes
   */
  private List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

  boolean showScope = false;

  public MavenDependenciesWizardPage() {
    this(null, Messages.getString("wizard.project.page.dependencies.title"), // //$NON-NLS-1$
        Messages.getString("wizard.project.page.dependencies.description")); //$NON-NLS-1$
  }
  
  public MavenDependenciesWizardPage(ProjectImportConfiguration projectImportConfiguration, String title, String description) {
    super("MavenDependenciesWizardPage", projectImportConfiguration); //$NON-NLS-1$
    setTitle(title);
    setDescription(description);
    setPageComplete(true);
  }

  public void setShowScope(boolean showScope) {
    this.showScope = showScope;
  }
  
  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }
  
  /**
   * {@inheritDoc} This wizard page contains a <code>TableViewer</code> to display the currently included Maven2
   * directories and a button area with buttons to add further dependencies or remove existing ones.
   */
  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(3, false);
    composite.setLayout(layout);

    if(dependencies!=null) {
      createArtifacts(composite);
    }
    
    createAdvancedSettings(composite, new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

    setControl(composite);

    updatePage();
  }

  private void createArtifacts(Composite composite) {
    Label mavenArtifactsLabel = new Label(composite, SWT.NONE);
    mavenArtifactsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
    mavenArtifactsLabel.setText(org.eclipse.m2e.core.internal.Messages.MavenDependenciesWizardPage_lblArtifacts);
    
    dependencyViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    dependencyViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
    dependencyViewer.setUseHashlookup(true);
    dependencyViewer.setLabelProvider(new ArtifactLabelProvider());
    dependencyViewer.setComparator(new DependencySorter());
    dependencyViewer.add(dependencies);

    Button addDependencyButton = new Button(composite, SWT.PUSH);
    GridData gd_addDependencyButton = new GridData(SWT.FILL, SWT.TOP, false, false);
    addDependencyButton.setLayoutData(gd_addDependencyButton);
    addDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.add")); //$NON-NLS-1$

    addDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRepositorySearchDialog dialog = new MavenRepositorySearchDialog(getShell(), //
            org.eclipse.m2e.core.internal.Messages.MavenDependenciesWizardPage_searchDialog_title, IIndex.SEARCH_ARTIFACT, Collections.<ArtifactKey>emptySet(), showScope);
        if(dialog.open() == Window.OK) {
          Object result = dialog.getFirstResult();
          if(result instanceof IndexedArtifactFile) {
            Dependency dependency = ((IndexedArtifactFile) result).getDependency();
            dependency.setScope(dialog.getSelectedScope());
            dependencyViewer.add(dependency);
            notifyListeners();
          } else if(result instanceof IndexedArtifact) {
            // If we have an ArtifactInfo, we add the first FileInfo it contains
            // which corresponds to the latest version of the artifact.
            Set<IndexedArtifactFile> files = ((IndexedArtifact) result).getFiles();
            if(files != null && !files.isEmpty()) {
              dependencyViewer.add(files.iterator().next().getDependency());
              notifyListeners();
            }
          }
        }
      }
    });

    final Button removeDependencyButton = new Button(composite, SWT.PUSH);
    removeDependencyButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeDependencyButton.setText(Messages.getString("wizard.project.page.dependencies.remove")); //$NON-NLS-1$
    removeDependencyButton.setEnabled(false);
    
    removeDependencyButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IStructuredSelection selection = (IStructuredSelection) dependencyViewer.getSelection();
        if(selection != null) {
          dependencyViewer.remove(selection.toArray());
          notifyListeners();
        }
      }
    });

    dependencyViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        removeDependencyButton.setEnabled(selection.size() > 0);
      }
    });
  }
  
  public IWizardContainer getContainer() {
    return super.getContainer();
  }

  void updatePage() {
    setPageComplete(isPageValid());
  }

  private boolean isPageValid() {
    setErrorMessage(null);
    return true;
  }
  
  /**
   * Notify listeners about changes
   */
  protected void notifyListeners() {
    SelectionChangedEvent event = new SelectionChangedEvent(dependencyViewer, dependencyViewer.getSelection());
    for(ISelectionChangedListener listener : listeners) {
      listener.selectionChanged(event);
    }
  }

  public void addListener(ISelectionChangedListener listener) {
    listeners.add(listener);
  }
  
  /**
   * Returns dependencies currently chosen by the user.
   * 
   * @return dependencies currently chosen by the user. Neither the array nor any of its elements is
   *         <code>null</code>.
   */
  public Dependency[] getDependencies() {
    List<Dependency> dependencies = new ArrayList<Dependency>();
    for(int i = 0; i < dependencyViewer.getTable().getItemCount(); i++ ) {
      Object element = dependencyViewer.getElementAt(i);
      if(element instanceof Dependency) {
        dependencies.add((Dependency) element);
      }
    }
    return dependencies.toArray(new Dependency[dependencies.size()]);
  }
  

  /**
   * Simple <code>LabelProvider</code> attached to the dependency viewer.
   * <p>
   * The information displayed for objects of type <code>Dependency</code> inside the dependency viewer is the
   * following:
   * </p>
   * <p>
   * {groupId} - {artifactId} - {version} - {type}
   * </p>
   */
  public static class ArtifactLabelProvider extends LabelProvider {

    /** The image to show for all objects of type <code>Dependency</code>. */
    private static final Image DEPENDENCY_IMAGE = MavenImages.IMG_JAR;

    /**
     * {@inheritDoc}
     * <p>
     * The text returned for objects of type <code>Dependency</code> contains the following information about the
     * dependency:
     * </p>
     * <p>
     * {groupId} - {artifactId} - {version} - {type}
     * </p>
     */
    public String getText(Object element) {
      if(element instanceof Dependency) {
        Dependency d = (Dependency) element;
        return d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion() + (d.getClassifier() == null ? "" : ":" + d.getClassifier()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      }
      return super.getText(element);
    }

    public Image getImage(Object element) {
      if(element instanceof Dependency) {
        return DEPENDENCY_IMAGE;
      }
      return super.getImage(element);
    }
  }

  /**
   * Simple <code>ViewerComparator</code> attached to the dependency viewer. Objects of type <code>Dependency</code> are
   * sorted by (1) their groupId and (2) their artifactId.
   */
  public static class DependencySorter extends ViewerComparator {

    /**
     * Two objects of type <code>Dependency</code> are sorted by (1) their groupId and (2) their artifactId.
     */
    public int compare(Viewer viewer, Object e1, Object e2) {
      if(!(e1 instanceof Dependency) || !(e2 instanceof Dependency)) {
        return super.compare(viewer, e1, e2);
      }

      // First of all, compare the group IDs of the two dependencies.
      String group1 = ((Dependency) e1).getGroupId();
      String group2 = ((Dependency) e2).getGroupId();

      int result = (group1 == null) ? -1 : group1.compareToIgnoreCase(group2);

      // If the group IDs match, we sort by the artifact IDs.
      if(result == 0) {
        String artifact1 = ((Dependency) e1).getArtifactId();
        String artifact2 = ((Dependency) e2).getArtifactId();
        result = artifact1 == null ? -1 : artifact1.compareToIgnoreCase(artifact2);
      }

      return result;
    }
  }

}
