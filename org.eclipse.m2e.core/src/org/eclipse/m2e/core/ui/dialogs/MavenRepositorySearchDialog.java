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

package org.eclipse.m2e.core.ui.dialogs;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.index.IIndex;
import org.eclipse.m2e.core.index.IndexedArtifact;
import org.eclipse.m2e.core.index.IndexedArtifactFile;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.wizards.MavenPomSelectionComponent;


/**
 * Maven POM Search dialog
 * 
 * @author Eugene Kuleshov
 */
public class MavenRepositorySearchDialog extends AbstractMavenDialog {
  private static final String DIALOG_SETTINGS = MavenRepositorySearchDialog.class.getName();

  private final boolean showScope;
  
  private final Set<ArtifactKey> artifacts;

  private final Set<ArtifactKey> managed;

  /**
   * One of 
   *   {@link IIndex#SEARCH_ARTIFACT}, 
   *   {@link IIndex#SEARCH_CLASS_NAME}, 
   */
  private final String queryType;

  private String queryText;

  MavenPomSelectionComponent pomSelectionComponent;

  private IndexedArtifact selectedIndexedArtifact;

  private IndexedArtifactFile selectedIndexedArtifactFile;

  private String selectedScope;

  private Combo scopeCombo;


  /**
   * Create repository search dialog
   * 
   * @param parent parent shell
   * @param title dialog title
   * @param queryType one of 
   *   {@link IIndex#SEARCH_ARTIFACT}, 
   *   {@link IIndex#SEARCH_CLASS_NAME}, 
   * @param artifacts Set&lt;Artifact&gt;
   * @deprecated
   */
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts) {
    this(parent, title, queryType, artifacts, Collections.<ArtifactKey>emptySet(), false);
  }
  
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, Set<ArtifactKey> managed) {
    this(parent, title, queryType, artifacts, managed, false);
  }
  /**
   * @deprecated
   */
  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, boolean showScope) {
    this(parent, title, queryType, artifacts, Collections.<ArtifactKey>emptySet(), showScope);
  }

  public MavenRepositorySearchDialog(Shell parent, String title, String queryType, Set<ArtifactKey> artifacts, Set<ArtifactKey> managed, boolean showScope) {
    super(parent, DIALOG_SETTINGS);
    this.artifacts = artifacts;
    this.managed = managed;
    this.queryType = queryType;
    this.showScope = showScope;

    setShellStyle(getShellStyle() | SWT.RESIZE);
    setStatusLineAboveButtons(true);
    setTitle(title);
  }

  public void setQuery(String query) {
    this.queryText = query;
  }

  protected Control createDialogArea(Composite parent) {
    readSettings();
    
    Composite composite = (Composite) super.createDialogArea(parent);

    pomSelectionComponent = new MavenPomSelectionComponent(composite, SWT.NONE);
    pomSelectionComponent.init(queryText, queryType, artifacts, managed);
    
    pomSelectionComponent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    
    pomSelectionComponent.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        if (!pomSelectionComponent.getStatus().matches(IStatus.ERROR)) {
          okPressedDelegate();
        }
      }
    });
    pomSelectionComponent.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateStatusDelegate(pomSelectionComponent.getStatus());
      }
    });
    
    return composite;
  }
  
  protected void createButtonsForButtonBar(Composite parent) {
    if(showScope) {
      ((GridLayout) parent.getLayout()).numColumns += 2;
      
      Label scopeLabel = new Label(parent, SWT.NONE);
      scopeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
      scopeLabel.setText(Messages.MavenRepositorySearchDialog_lblScope);
  
      scopeCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
      scopeCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
      scopeCombo.setItems(new String[] {"compile", "test", "runtime", "provided", "system", "import"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
      scopeCombo.setText(Messages.MavenRepositorySearchDialog_7);
    }
    
    super.createButtonsForButtonBar(parent);
  }
  
  void okPressedDelegate() {
    okPressed();
  }
  
  void updateStatusDelegate(IStatus status) {
    updateStatus(status);
  }

  protected void computeResult() {
    selectedIndexedArtifact = pomSelectionComponent.getIndexedArtifact();
    selectedIndexedArtifactFile = pomSelectionComponent.getIndexedArtifactFile();
    selectedScope = scopeCombo == null ? null : scopeCombo.getText();
    setResult(Collections.singletonList(selectedIndexedArtifactFile));
  }
  
  public IndexedArtifact getSelectedIndexedArtifact() {
    return this.selectedIndexedArtifact;
  }
  
  public IndexedArtifactFile getSelectedIndexedArtifactFile() {
    return this.selectedIndexedArtifactFile;
  }
  
  public String getSelectedScope() {
    return this.selectedScope;
  } 
  
}
