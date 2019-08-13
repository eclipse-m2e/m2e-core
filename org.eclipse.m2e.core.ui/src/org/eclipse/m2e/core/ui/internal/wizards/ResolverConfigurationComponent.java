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

package org.eclipse.m2e.core.ui.internal.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.ui.internal.Messages;


/**
 * A foldable resolver configuration panel
 */
public class ResolverConfigurationComponent extends ExpandableComposite {

  private static final String[] DEFAULT_NAME_TEMPLATES = {"[artifactId]", // //$NON-NLS-1$
      "[artifactId]-TRUNK", // //$NON-NLS-1$
      "[artifactId]-[version]", // //$NON-NLS-1$
      "[groupId].[artifactId]", "[groupId].[artifactId]-[version]", //$NON-NLS-1$ //$NON-NLS-2$
      "[name]"}; //$NON-NLS-1$

  /** The resolver configuration */
  protected final ResolverConfiguration resolverConfiguration;

  /** project import configuration */
  private final ProjectImportConfiguration projectImportConfiguration;

  private ModifyListener modifyListener;

  Button resolveWorkspaceProjects;

  Text profiles;

  Combo template;

  /** Creates a new component. */
  public ResolverConfigurationComponent(final Composite parent,
      final ProjectImportConfiguration propectImportConfiguration, final boolean enableProjectNameTemplate) {
    super(parent, ExpandableComposite.COMPACT | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
    this.projectImportConfiguration = propectImportConfiguration;
    this.resolverConfiguration = propectImportConfiguration.getResolverConfiguration();

    setText(Messages.resolverConfigurationAdvanced);

    final Composite advancedComposite = new Composite(this, SWT.NONE);
    setClient(advancedComposite);
    addExpansionListener(new ExpansionAdapter() {
      public void expansionStateChanged(ExpansionEvent e) {
        Shell shell = parent.getShell();
        Point minSize = shell.getMinimumSize();
        shell.setMinimumSize(shell.getSize().x, minSize.y);
        shell.pack();
        parent.layout();
        shell.setMinimumSize(minSize);
      }
    });

    GridLayout gridLayout = new GridLayout();
    gridLayout.marginLeft = 11;
    gridLayout.numColumns = 2;
    advancedComposite.setLayout(gridLayout);

    resolveWorkspaceProjects = new Button(advancedComposite, SWT.CHECK);
    resolveWorkspaceProjects.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    resolveWorkspaceProjects.setText(Messages.resolverConfigurationResolveWorkspaceProjects);
    resolveWorkspaceProjects.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        resolverConfiguration.setResolveWorkspaceProjects(resolveWorkspaceProjects.getSelection());
      }
    });

    Label profilesLabel = new Label(advancedComposite, SWT.NONE);
    profilesLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    profilesLabel.setText(Messages.resolverConfigurationProfiles);

    profiles = new Text(advancedComposite, SWT.BORDER);
    profiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    profiles.addModifyListener(e -> resolverConfiguration.setSelectedProfiles(profiles.getText()));

    if(enableProjectNameTemplate) {
      Label templateLabel = new Label(advancedComposite, SWT.NONE);
      templateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
      templateLabel.setText(Messages.resolverConfigurationTemplate);

      template = new Combo(advancedComposite, SWT.BORDER);
      template.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      template.setToolTipText(Messages.resolverConfigurationTemplateDescription);
      template.setItems(DEFAULT_NAME_TEMPLATES);
      template.addModifyListener(e -> propectImportConfiguration.setProjectNameTemplate(template.getText()));
    }

    loadData();
  }

  public void loadData() {
    resolveWorkspaceProjects.setSelection(resolverConfiguration.shouldResolveWorkspaceProjects());
    profiles.setText(resolverConfiguration.getSelectedProfiles());
    if(template != null) {
      template.setText(projectImportConfiguration.getProjectNameTemplate());
    }
  }

  public ResolverConfiguration getResolverConfiguration() {
    return this.resolverConfiguration;
  }

  public void setModifyListener(ModifyListener modifyListener) {
    this.modifyListener = modifyListener;

    if(template != null) {
      template.addModifyListener(modifyListener);
    }
  }

  public void dispose() {
    super.dispose();

    if(modifyListener != null) {
      template.removeModifyListener(modifyListener);
    }
  }

}
