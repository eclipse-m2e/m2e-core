/*******************************************************************************
 * Copyright (c) 2011-2015 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored proposals discovery
 *      Anton Tanasenko - Refactor marker resolutions and quick fixes (Bug #484359)
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.LifecycleMappingDiscoveryRequest;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.MojoExecutionMappingRequirement;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration.ProjectConfiguratorMappingRequirement;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.AggregateMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;


/**
 * LifecycleMappingPage
 *
 * @author igor
 */
public class LifecycleMappingPage extends WizardPage {

  private static final Logger LOG = LoggerFactory.getLogger(LifecycleMappingPage.class);

  private static final String EMPTY_STRING = ""; //$NON-NLS-1$

  private static final int MAVEN_INFO_IDX = 0;

  private static final int ACTION_INFO_IDX = 1;

  private static final int NO_ACTION_IDX = 0;

  private static final int IGNORE_IDX = 1;

  private static final int IGNORE_PARENT_IDX = 2;

  private static final int IGNORE_WSPACE_IDX = 3;

  private LifecycleMappingDiscoveryRequest mappingConfiguration;

  private TreeViewer treeViewer;

  private Button autoSelectButton;

  private boolean loading;

  private Text details;

  private Text license;

  private final Set<ILifecycleMappingLabelProvider> ignore = new HashSet<>();

  private final Set<ILifecycleMappingLabelProvider> ignoreAtDefinition = new HashSet<>();

  private final Set<ILifecycleMappingLabelProvider> ignoreWorkspace = new HashSet<>();

  /**
   * Create the wizard.
   */
  public LifecycleMappingPage() {
    super("LifecycleMappingPage"); //$NON-NLS-1$
    setTitle(Messages.LifecycleMappingPage_title);
    setDescription(Messages.LifecycleMappingPage_description);
    setPageComplete(true); // always allow to leave mapping page, even when there are mapping problems
    setImageDescriptor(ImageDescriptor
        .createFromURL(LifecycleMappingPage.class.getResource("/icons/banner_lifecycleMappingPage.png")));
  }

  /**
   * Create contents of the wizard.
   *
   * @param parent
   */
  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    setControl(container);
    container.setLayout(new BorderLayout());
    BorderData tableBorderData = new BorderData(SWT.CENTER);
    tableBorderData.hHint = 300;
    tableBorderData.wHint = 800;
    createMappingTree(container).setLayoutData(tableBorderData);
    createDescription(container).setLayoutData(new BorderData(SWT.BOTTOM));
  }

  private Control createDescription(Composite parent) {
    ExpandBar bar = new ExpandBar(parent, SWT.NONE);
    Composite container = new Composite(bar, SWT.NULL);
    container.setLayout(new BorderLayout());
    details = new Text(container, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
    details.setLayoutData(new BorderData(SWT.CENTER));
    Composite licenseContainer = new Composite(container, SWT.NULL);
    new Label(licenseContainer, SWT.NONE).setText(Messages.LifecycleMappingPage_licenseLabel);
    license = new Text(licenseContainer, SWT.READ_ONLY);
    license.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    licenseContainer.setLayout(new GridLayout(2, false));
    licenseContainer.setLayoutData(new BorderData(SWT.BOTTOM));

    ExpandItem expandItem = new ExpandItem(bar, SWT.NONE, 0);
    expandItem.setText(Messages.LifecycleMappingPage_descriptionLabel);
    expandItem.setHeight(120);
    expandItem.setControl(container);
    expandItem.setExpanded(true);
    //Workaround for https://github.com/eclipse-platform/eclipse.platform.swt/issues/551 
    bar.addExpandListener(new ExpandListener() {

      @Override
      public void itemExpanded(ExpandEvent e) {
        itemCollapsed(e);
      }

      @Override
      public void itemCollapsed(ExpandEvent e) {
        bar.requestLayout();
      }
    });
    return bar;
  }

  private Composite createMappingTree(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    container.setLayout(new BorderLayout());

    Composite treeViewerContainer = new Composite(container, SWT.NULL);
    treeViewerContainer.setLayoutData(new BorderData(SWT.CENTER));
    TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
    treeViewerContainer.setLayout(treeColumnLayout);

    treeViewer = new TreeViewer(treeViewerContainer, SWT.BORDER | SWT.FULL_SELECTION);

    Tree tree = treeViewer.getTree();
    tree.setLinesVisible(true);
    tree.setHeaderVisible(true);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TreeViewerColumn treeViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn trclmnNewColumn = treeViewerColumn.getColumn();
    trclmnNewColumn.setText(Messages.LifecycleMappingPage_mavenBuildColumnTitle);
    treeColumnLayout.setColumnData(trclmnNewColumn, new ColumnWeightData(65, 150, true));

    TreeViewerColumn columnViewerAction = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn columnAction = columnViewerAction.getColumn();
    treeColumnLayout.setColumnData(columnAction, new ColumnWeightData(35, true));
    columnAction.setText(Messages.LifecycleMappingPage_actionColumnTitle);
    columnViewerAction.setEditingSupport(new EditingSupport(treeViewer) {

      @SuppressWarnings("synthetic-access")
      @Override
      protected void setValue(Object element, Object value) {
        if(element instanceof ILifecycleMappingLabelProvider prov) {
          int intVal = ((Integer) value).intValue();
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());

          if(ignore.contains(element)) {
            ignore.remove(element);
          } else if(ignoreAtDefinition.contains(element)) {
            ignoreAtDefinition.remove(element);
          } else if(ignoreWorkspace.contains(element)) {
            ignoreWorkspace.remove(element);
          } else if(intVal >= all.size() + NO_ACTION_IDX || shouldDeslectProposal(prov)) {
            IMavenDiscoveryProposal prop = mappingConfiguration.getSelectedProposal(prov.getKey());
            mappingConfiguration.removeSelectedProposal(prop);
          }

          // Set new selection
          if(intVal < all.size()) {
            IMavenDiscoveryProposal sel = all.get(intVal);
            if(sel != null) {
              mappingConfiguration.addSelectedProposal(sel);
            }
          } else {
            switch(intVal - all.size()) {
              case IGNORE_IDX:
                ignore.add(prov);
                break;
              case IGNORE_PARENT_IDX:
                ignoreAtDefinition.add(prov);
                break;
              case IGNORE_WSPACE_IDX:
                ignoreWorkspace.add(prov);
            }
          }
          getViewer().refresh(true);
          getContainer().updateButtons();
        }
      }

      @Override
      protected Object getValue(Object element) {
        if(element instanceof ILifecycleMappingLabelProvider prov) {
          IMavenDiscoveryProposal prop = mappingConfiguration.getSelectedProposal(prov.getKey());
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          if(ignore.contains(element)) {
            return Integer.valueOf(all.size() + IGNORE_IDX);
          } else if(ignoreAtDefinition.contains(element)) {
            return Integer.valueOf(all.size() + IGNORE_PARENT_IDX);
          } else if(ignoreWorkspace.contains(element)) {
            return Integer.valueOf(all.size() + IGNORE_WSPACE_IDX);
          } else {
            int index = all.indexOf(prop);
            return index >= 0 ? Integer.valueOf(index) : Integer.valueOf(all.size() + NO_ACTION_IDX);
          }
        }
        return Integer.valueOf(0);
      }

      @Override
      protected CellEditor getCellEditor(Object element) {
        if(element instanceof ILifecycleMappingLabelProvider prov) {
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          List<String> values = new ArrayList<>();
          for(IMavenDiscoveryProposal prop : all) {
            values.add(NLS.bind(Messages.LifecycleMappingPage_installDescription, prop.toString()));
          }
          values.add(Messages.LifecycleMappingPage_useDefaultMapping);

          addIgnoreProposals(values, prov);
          ComboBoxCellEditor edit = new ComboBoxCellEditor(treeViewer.getTree(),
              values.toArray(new String[values.size()]));
          edit.setActivationStyle(
              ComboBoxCellEditor.DROP_DOWN_ON_KEY_ACTIVATION | ComboBoxCellEditor.DROP_DOWN_ON_MOUSE_ACTIVATION);

          Control cont = edit.getControl();
          //this attempts to disable text edits in the combo..
          if(cont instanceof CCombo combo) {
            combo.setEditable(false);
          }
          return edit;
        }
        throw new IllegalStateException();
      }

      @Override
      protected boolean canEdit(Object element) {
        if(element instanceof AggregateMappingLabelProvider prov) {
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          return all != null && !all.isEmpty() || prov.getKey() instanceof MojoExecutionMappingRequirement;
        }
        return false;
      }
    });

    treeViewer.setContentProvider(new ITreeContentProvider() {

      @Override
      public void dispose() {
      }

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      @Override
      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof LifecycleMappingDiscoveryRequest request) {
          Map<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> packagings = new HashMap<>();
          Map<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> mojos = new HashMap<>();

          request.getProjects().forEach((facade, requirements) -> {
            String relPath = facade.getProject().getFile(IMavenConstants.POM_FILE_NAME).getFullPath()
                .toPortableString();
            for(ILifecycleMappingRequirement requirement : requirements) {
              // include mojo execution if it has available proposals or interesting phase not mapped locally
              if(requirement != null) {
                List<ILifecycleMappingLabelProvider> val = mojos.computeIfAbsent(requirement, r -> new ArrayList<>());
                val.add(new ILifecycleMappingLabelProvider() {

                  @Override
                  public String getMavenText() {
                    String executionId = null;
                    if(requirement instanceof MojoExecutionMappingRequirement mojo) {
                      executionId = mojo.getExecutionId();
                    } else if(requirement instanceof ProjectConfiguratorMappingRequirement conf) {
                      executionId = conf.execution().executionId();
                    }

                    if(executionId != null) {
                      if("default".equals(executionId)) {
                        return NLS.bind("{0}", relPath);
                      }
                      return NLS.bind("Execution {0}, in {1}", executionId, relPath);
                    }

                    return null;
                  }

                  @Override
                  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration) {
                    return !mappingConfiguration.isRequirementSatisfied(getKey());
                  }

                  @Override
                  public ILifecycleMappingRequirement getKey() {
                    return requirement;
                  }

                  @Override
                  public Collection<MavenProject> getProjects() {
                    MavenProject mavenProject;
                    try {
                      mavenProject = facade.getMavenProject(new NullProgressMonitor());
                      return Collections.singleton(mavenProject);
                    } catch(CoreException e) {
                      LOG.error(e.getMessage(), e);
                      throw new RuntimeException(e.getMessage(), e);
                    }
                  }

                });
              }
            }
          });
          List<ILifecycleMappingLabelProvider> toRet = new ArrayList<>();
          packagings.forEach((req, providers) -> toRet.add(new AggregateMappingLabelProvider(req, providers)));
          mojos.forEach((req, providers) -> toRet.add(new AggregateMappingLabelProvider(req, providers)));
          return toRet.toArray();
        }
        return null;
      }

      @Override
      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof AggregateMappingLabelProvider prov) {
          return prov.getChildren();
        }
        return new Object[0];
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public boolean hasChildren(Object element) {
        Object[] children = getChildren(element);
        return children != null && children.length > 0;
      }

    });
    treeViewer.setLabelProvider(new ITableLabelProvider() {

      @Override
      public void removeListener(ILabelProviderListener listener) {
      }

      @Override
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      @Override
      public void dispose() {
      }

      @Override
      public void addListener(ILabelProviderListener listener) {
      }

      @Override
      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof ILifecycleMappingLabelProvider prov) {
          if(columnIndex == MAVEN_INFO_IDX) {
            return prov.getMavenText();
          } else if(columnIndex == ACTION_INFO_IDX && element instanceof AggregateMappingLabelProvider) {
            IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
            if(ignore.contains(element)) {
              return Messages.LifecycleMappingPage_doNotExecutePom;
            } else if(ignoreAtDefinition.contains(element)) {
              return Messages.LifecycleMappingPage_doNotExecuteParent;
            } else if(ignoreWorkspace.contains(element)) {
              return Messages.LifecycleMappingPage_doNotExecuteWorkspace;
            } else if(proposal != null) {
              return NLS.bind(Messages.LifecycleMappingPage_installDescription, proposal.toString()); //not really feeling well here.
            } else if(loading || !prov.isError(mappingConfiguration)) {
              return EMPTY_STRING;
            } else {
              return Messages.LifecycleMappingPage_useDefaultMapping;
            }
          }
        }
        return null;
      }

      @Override
      public Image getColumnImage(Object element, int columnIndex) {
        if(columnIndex != 0) {
          return null;
        }
        if(element instanceof AggregateMappingLabelProvider prov) {
          if(prov.isError(mappingConfiguration) && !isHandled(prov)) {
            //historically missing mappings where shows as ERROR (therefore the name isError)
            //but now m2e execute missing mappings according on user request so this is more an info that
            //it is handled automatic and the user can choose to change this
            return MavenImages.IMG_INFO_AUTO;
          }
          return MavenImages.IMG_PASSED;
        }
        return MavenImages.IMG_POM;
      }
    });

    treeViewer.addSelectionChangedListener(event -> {
      if(event.getSelection() instanceof IStructuredSelection structuredSelection && structuredSelection.size() == 1) {
        ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) structuredSelection.getFirstElement();
        if(ignore.contains(prov)) {
          details.setText(Messages.LifecycleMappingPage_doNotExecutePomDescription);
          license.setText(EMPTY_STRING);
        } else if(ignoreAtDefinition.contains(prov)) {
          details.setText(Messages.LifecycleMappingPage_doNotExecuteParentDescription);
          license.setText(EMPTY_STRING);
        } else if(ignoreWorkspace.contains(prov)) {
          details.setText(Messages.LifecycleMappingPage_doNotExecuteWorkspaceDescription);
          license.setText(EMPTY_STRING);
        } else {
          IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
          details.setText(proposal != null ? proposal.getDescription()
              : mappingConfiguration.getProposals(prov.getKey()).isEmpty()
                  ? NLS.bind(Messages.LifecycleMappingPage_noMarketplaceEntryDescription, prov.getMavenText())
                  : EMPTY_STRING);
          license.setText(proposal == null ? EMPTY_STRING : proposal.getLicense());
        }
      } else {
        resetDetails();
      }
    });

    treeViewer.setComparator(new ViewerComparator() {
      Comparator<ILifecycleMappingLabelProvider> providerComparator = Comparator
          .comparing(ILifecycleMappingLabelProvider::getMavenText);
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        if(e1 instanceof ILifecycleMappingLabelProvider p1 && e2 instanceof ILifecycleMappingLabelProvider p2) {
          return providerComparator.compare(p1, p2);
        }
        return super.compare(viewer, e1, e2);
      }
    });

    Composite buttons = new Composite(container, SWT.NONE);
    buttons.setLayoutData(new BorderData(SWT.BOTTOM));
    buttons.setLayout(new GridLayout(3, false));
    new Label(buttons, SWT.NONE).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    Button btnNewButton_1 = new Button(buttons, SWT.NONE);
    btnNewButton_1.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      mappingConfiguration.clearSelectedProposals();
      ignore.clear();
      ignoreAtDefinition.clear();
      ignoreWorkspace.clear();
      treeViewer.refresh();
      getWizard().getContainer().updateButtons(); // needed to enable/disable Finish button
    }));
    btnNewButton_1.setText(Messages.LifecycleMappingPage_deselectAllButton);

    autoSelectButton = new Button(buttons, SWT.NONE);
    autoSelectButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      resetDetails();
      ignore.clear();
      ignoreAtDefinition.clear();
      ignoreWorkspace.clear();
      discoverProposals();
    }));
    autoSelectButton.setText(Messages.LifecycleMappingPage_autoSelectButton);
    return container;
  }

  /**
   * @param key
   * @return
   */
  protected boolean shouldDeslectProposal(ILifecycleMappingLabelProvider prov) {
    IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
    if(proposal != null) {
      return Arrays.stream(treeViewer.getTree().getItems()).noneMatch(
          item -> item.getData() instanceof ILifecycleMappingLabelProvider itemProvider && item.getData() != prov
              && proposal.equals(mappingConfiguration.getSelectedProposal(itemProvider.getKey())));
    }
    return true;
  }

  protected void discoverProposals() {
    loading = true;
    treeViewer.refresh();
    try {
      getContainer().run(true, true, monitor -> {
        mappingConfiguration.clearSelectedProposals();
        try {
          LifecycleMappingDiscoveryHelper.discoverProposals(mappingConfiguration, monitor);
        } catch(CoreException ex) {
          throw new InvocationTargetException(ex);
        }
        mappingConfiguration.autoCompleteMapping();
      });
    } catch(InvocationTargetException e) {
      setErrorMessage(e.getMessage());
    } catch(InterruptedException e) {
      setErrorMessage(e.getMessage());
    }
    loading = false;
    treeViewer.refresh();
    getWizard().getContainer().updateButtons(); // needed to enable/disable Finish button
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      PlatformUI.getWorkbench().getHelpSystem().setHelp(getWizard().getContainer().getShell(),
          M2EUIPluginActivator.PLUGIN_ID + ".LifecycleMappingPage"); //$NON-NLS-1$
      mappingConfiguration = ((MavenDiscoveryProposalWizard) getWizard()).getLifecycleMappingDiscoveryRequest();
      if(!mappingConfiguration.isMappingComplete()) {
        // try to solve problems only if there are any
        mappingConfiguration.autoCompleteMapping();
      }
      treeViewer.setInput(mappingConfiguration);
    }
  }

  @Override
  public boolean canFlipToNextPage() {
    return true;//getNextPage() != null;
  }

  public List<IMavenDiscoveryProposal> getSelectedDiscoveryProposals() {
    if(mappingConfiguration == null) {
      return Collections.emptyList();
    }
    return mappingConfiguration.getSelectedProposals();
  }

  /*
   * Mapping is complete when mappings are handled (a proposal has been selected, or if it is an uninteresting phase).
   */
  public boolean isMappingComplete() {
    for(TreeItem item : treeViewer.getTree().getItems()) {
      if(!isHandled((ILifecycleMappingLabelProvider) item.getData())) {
        return false;
      }
    }
    return true;
  }

  /*
   * Populate list with ignore options (should be called last)
   */
  private void addIgnoreProposals(List<String> values, ILifecycleMappingLabelProvider provider) {
    if(provider.getKey() instanceof MojoExecutionMappingRequirement) {
      values.add(Messages.LifecycleMappingPage_doNotExecutePom);
      values.add(Messages.LifecycleMappingPage_doNotExecuteParent);
      values.add(Messages.LifecycleMappingPage_doNotExecuteWorkspace);
    }
  }

  /*
   * Get the list of mojos to ignore in executing pom
   */
  public Collection<ILifecycleMappingLabelProvider> getIgnore() {
    return ignore;
  }

  /*
   * Get the list of mojos to ignore in parents
   */
  public Collection<ILifecycleMappingLabelProvider> getIgnoreParent() {
    return ignoreAtDefinition;
  }

  /*
   * Get the list of mojos to ignore globally in the workspace
   */
  public Collection<ILifecycleMappingLabelProvider> getIgnoreWorkspace() {
    return ignoreWorkspace;
  }

  /*
   * Only applicable for top level elements. Provider is considered handled if it is ignored, a proposal has been selected, or if it is an uninteresting phase.
   */
  private boolean isHandled(ILifecycleMappingLabelProvider prov) {
    return ignore.contains(prov) || ignoreAtDefinition.contains(prov) || ignoreWorkspace.contains(prov)
        || mappingConfiguration.getSelectedProposal(prov.getKey()) != null || !prov.isError(mappingConfiguration);
  }

  private void resetDetails() {
    if(details != null) {
      details.setText(EMPTY_STRING);
    }
    if(license != null) {
      license.setText(EMPTY_STRING);
    }
  }

}
