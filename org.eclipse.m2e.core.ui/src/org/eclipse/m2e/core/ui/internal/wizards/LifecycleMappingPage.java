/*******************************************************************************
 * Copyright (c) 2011-2013 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Red Hat, Inc. - refactored proposals discovery
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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
import org.eclipse.m2e.core.project.IMavenProjectFacade;
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
@SuppressWarnings("restriction")
public class LifecycleMappingPage extends WizardPage {

  private static final Logger LOG = LoggerFactory.getLogger(LifecycleMappingPage.class);

  private static final String EMPTY_STRING = ""; //$NON-NLS-1$

  private static final int MAVEN_INFO_IDX = 0;

  private static final int ACTION_INFO_IDX = 1;

  private static final int NO_ACTION_IDX = 0;

  private static final int IGNORE_IDX = 1;

  private static final int IGNORE_PARENT_IDX = 2;

  private LifecycleMappingDiscoveryRequest mappingConfiguration;

  private TreeViewer treeViewer;

  private Button autoSelectButton;

  private boolean loading;

  private Text details;

  private Text license;

  private Set<ILifecycleMappingLabelProvider> ignore = new HashSet<ILifecycleMappingLabelProvider>();

  private Set<ILifecycleMappingLabelProvider> ignoreAtDefinition = new HashSet<ILifecycleMappingLabelProvider>();

  private Label errorCountLabel;

  /**
   * Create the wizard.
   */
  public LifecycleMappingPage() {
    super("LifecycleMappingPage"); //$NON-NLS-1$
    setTitle(Messages.LifecycleMappingPage_title);
    setDescription(Messages.LifecycleMappingPage_description);
    setPageComplete(true); // always allow to leave mapping page, even when there are mapping problems
  }

  /**
   * Create contents of the wizard.
   * 
   * @param parent
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    setControl(container);
    container.setLayout(new GridLayout(1, false));

    Composite treeViewerContainer = new Composite(container, SWT.NULL);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(treeViewerContainer);
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
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          int intVal = ((Integer) value).intValue();
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());

          if(ignore.contains(element)) {
            ignore.remove(element);
          } else if(ignoreAtDefinition.contains(element)) {
            ignoreAtDefinition.remove(element);
          } else if(intVal == all.size() + NO_ACTION_IDX || shouldDeslectProposal(prov)) {
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
            }
          }
          getViewer().refresh(true);
          updateErrorCount();
          getContainer().updateButtons();
        }
      }

      @SuppressWarnings("synthetic-access")
      @Override
      protected Object getValue(Object element) {
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          IMavenDiscoveryProposal prop = mappingConfiguration.getSelectedProposal(prov.getKey());
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          if(ignore.contains(element)) {
            return Integer.valueOf(all.size() + IGNORE_IDX);
          } else if(ignoreAtDefinition.contains(element)) {
            return Integer.valueOf(all.size() + IGNORE_PARENT_IDX);
          } else {
            int index = all.indexOf(prop);
            return index >= 0 ? Integer.valueOf(index) : Integer.valueOf(all.size() + NO_ACTION_IDX);
          }
        }
        return Integer.valueOf(0);
      }

      @SuppressWarnings("synthetic-access")
      @Override
      protected CellEditor getCellEditor(Object element) {
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          List<String> values = new ArrayList<String>();
          for(IMavenDiscoveryProposal prop : all) {
            values.add(NLS.bind(Messages.LifecycleMappingPage_installDescription, prop.toString()));
          }
          if(prov.isError(mappingConfiguration)) {
            values.add(Messages.LifecycleMappingPage_resolveLaterDescription);
          } else {
            values.add(EMPTY_STRING);
          }
          addIgnoreProposals(values, prov);
          ComboBoxCellEditor edit = new ComboBoxCellEditor(treeViewer.getTree(), values.toArray(new String[values
              .size()]));
          Control cont = edit.getControl();
          //this attempts to disable text edits in the combo..
          if(cont instanceof CCombo) {
            CCombo combo = (CCombo) cont;
            combo.setEditable(false);
          }
          return edit;
        }
        throw new IllegalStateException();
      }

      @SuppressWarnings("synthetic-access")
      @Override
      protected boolean canEdit(Object element) {
        if(element instanceof AggregateMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
          return all != null && !all.isEmpty() || prov.getKey() instanceof MojoExecutionMappingRequirement;
        }
        return false;
      }
    });

    treeViewer.setContentProvider(new ITreeContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof LifecycleMappingDiscoveryRequest) {
          Map<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> packagings = new HashMap<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>>();
          Map<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> mojos = new HashMap<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>>();
          Map<IMavenProjectFacade, List<ILifecycleMappingRequirement>> projects = ((LifecycleMappingDiscoveryRequest) inputElement)
              .getProjects();
          for(final Entry<IMavenProjectFacade, List<ILifecycleMappingRequirement>> entry : projects.entrySet()) {
            final String relPath = entry.getKey().getProject().getFile(IMavenConstants.POM_FILE_NAME).getFullPath()
                .toPortableString();
            for(final ILifecycleMappingRequirement requirement : entry.getValue()) {
              // include mojo execution if it has available proposals or interesting phase not mapped locally
              if(requirement != null) {
                List<ILifecycleMappingLabelProvider> val = mojos.get(requirement);
                if(val == null) {
                  val = new ArrayList<ILifecycleMappingLabelProvider>();
                  mojos.put(requirement, val);
                }
                val.add(new ILifecycleMappingLabelProvider() {

                  public String getMavenText() {
                    String executionId = null;
                    if(requirement instanceof MojoExecutionMappingRequirement) {
                      executionId = ((MojoExecutionMappingRequirement) requirement).getExecutionId();
                    } else if(requirement instanceof ProjectConfiguratorMappingRequirement) {
                      executionId = ((ProjectConfiguratorMappingRequirement) requirement).getExecution()
                          .getExecutionId();
                    }

                    if(executionId != null) {
                      if("default".equals(executionId)) {
                        return NLS.bind("{0}", relPath);
                      }
                      return NLS.bind("Execution {0}, in {1}", executionId, relPath);
                    }

                    return null;
                  }

                  public boolean isError(LifecycleMappingDiscoveryRequest mappingConfiguration) {
                    return !mappingConfiguration.isRequirementSatisfied(getKey());
                  }

                  public ILifecycleMappingRequirement getKey() {
                    return requirement;
                  }

                  @SuppressWarnings("synthetic-access")
                  public Collection<MavenProject> getProjects() {
                    MavenProject mavenProject;
                    try {
                      mavenProject = entry.getKey().getMavenProject(new NullProgressMonitor());
                      return Collections.singleton(mavenProject);
                    } catch(CoreException e) {
                      LOG.error(e.getMessage(), e);
                      throw new RuntimeException(e.getMessage(), e);
                    }
                  }

                });
              }
            }
          }
          List<ILifecycleMappingLabelProvider> toRet = new ArrayList<ILifecycleMappingLabelProvider>();
          for(Map.Entry<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> ent : packagings.entrySet()) {
            toRet.add(new AggregateMappingLabelProvider(ent.getKey(), ent.getValue()));
          }
          for(Map.Entry<ILifecycleMappingRequirement, List<ILifecycleMappingLabelProvider>> ent : mojos.entrySet()) {
            toRet.add(new AggregateMappingLabelProvider(ent.getKey(), ent.getValue()));
          }
          return toRet.toArray();
        }
        return null;
      }

      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof AggregateMappingLabelProvider) {
          return ((AggregateMappingLabelProvider) parentElement).getChildren();
        }
        return new Object[0];
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object element) {
        Object[] children = getChildren(element);
        return children != null && children.length > 0;
      }

    });
    treeViewer.setLabelProvider(new ITableLabelProvider() {

      public void removeListener(ILabelProviderListener listener) {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void dispose() {
      }

      public void addListener(ILabelProviderListener listener) {
      }

      @SuppressWarnings("synthetic-access")
      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          if(columnIndex == MAVEN_INFO_IDX) {
            String text = prov.getMavenText();
            if(prov instanceof AggregateMappingLabelProvider && !isHandled(prov)) {
              text = NLS.bind(Messages.LifecycleMappingPage_errorMavenBuild,
                  new String[] {text, String.valueOf(((AggregateMappingLabelProvider) prov).getChildren().length)});
            }
            return text;
          } else if(columnIndex == ACTION_INFO_IDX && element instanceof AggregateMappingLabelProvider) {
            IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
            if(ignore.contains(element)) {
              return Messages.LifecycleMappingPage_doNotExecutePom;
            } else if(ignoreAtDefinition.contains(element)) {
              return Messages.LifecycleMappingPage_doNotExecuteParent;
            } else if(proposal != null) {
              return NLS.bind(Messages.LifecycleMappingPage_installDescription, proposal.toString()); //not really feeling well here. 
            } else if(loading || !prov.isError(mappingConfiguration)) {
              return EMPTY_STRING;
            } else {
              return Messages.LifecycleMappingPage_resolveLaterDescription;
            }
          }
        }
        return null;
      }

      @SuppressWarnings("synthetic-access")
      public Image getColumnImage(Object element, int columnIndex) {
        if(columnIndex != 0) {
          return null;
        }
        if(element instanceof AggregateMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          if(prov.isError(mappingConfiguration)) {
            if(!isHandled(prov)) {
              return MavenImages.IMG_ERROR;
            }
          }
          return MavenImages.IMG_PASSED;
        }
        return MavenImages.IMG_POM;
      }
    });

    treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      @SuppressWarnings("synthetic-access")
      public void selectionChanged(SelectionChangedEvent event) {
        if(event.getSelection() instanceof IStructuredSelection
            && ((IStructuredSelection) event.getSelection()).size() == 1) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) ((IStructuredSelection) event
              .getSelection()).getFirstElement();
          if(ignore.contains(prov)) {
            details.setText(Messages.LifecycleMappingPage_doNotExecutePomDescription);
            license.setText(EMPTY_STRING);
          } else if(ignoreAtDefinition.contains(prov)) {
            details.setText(Messages.LifecycleMappingPage_doNotExecuteParentDescription);
            license.setText(EMPTY_STRING);
          } else {
            IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
            details.setText(proposal != null ? proposal.getDescription() : mappingConfiguration.getProposals(
                prov.getKey()).isEmpty() ? NLS.bind(Messages.LifecycleMappingPage_noMarketplaceEntryDescription,
                prov.getMavenText()) : EMPTY_STRING);
            license.setText(proposal == null ? EMPTY_STRING : proposal.getLicense());
          }
        } else {
          resetDetails();
        }
      }
    });

    treeViewer.setComparator(new ViewerComparator() {
      public int compare(Viewer viewer, Object e1, Object e2) {
        if(!(e1 instanceof ILifecycleMappingLabelProvider && e2 instanceof ILifecycleMappingLabelProvider)) {
          return super.compare(viewer, e1, e2);
        }
        int cat1 = category(e1);
        int cat2 = category(e2);

        if(cat1 != cat2) {
          return cat1 - cat2;
        }
        return ((ILifecycleMappingLabelProvider) e1).getMavenText().compareTo(
            ((ILifecycleMappingLabelProvider) e2).getMavenText());
      }
    });

    Composite composite = new Composite(container, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    composite.setLayout(new GridLayout(3, false));

    errorCountLabel = new Label(composite, SWT.NONE);
    errorCountLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    Button btnNewButton_1 = new Button(composite, SWT.NONE);
    btnNewButton_1.addSelectionListener(new SelectionAdapter() {
      @Override
      @SuppressWarnings("synthetic-access")
      public void widgetSelected(SelectionEvent e) {
        mappingConfiguration.clearSelectedProposals();
        ignore.clear();
        ignoreAtDefinition.clear();
        treeViewer.refresh();
        getWizard().getContainer().updateButtons(); // needed to enable/disable Finish button
        updateErrorCount();
      }
    });
    btnNewButton_1.setText(Messages.LifecycleMappingPage_deselectAllButton);

    autoSelectButton = new Button(composite, SWT.NONE);
    autoSelectButton.addSelectionListener(new SelectionAdapter() {
      @Override
      @SuppressWarnings("synthetic-access")
      public void widgetSelected(SelectionEvent e) {
        resetDetails();
        ignore.clear();
        ignoreAtDefinition.clear();
        discoverProposals();
      }
    });
    autoSelectButton.setText(Messages.LifecycleMappingPage_autoSelectButton);

    // Provide a reasonable height for the details box 
    GC gc = new GC(container);
    gc.setFont(JFaceResources.getDialogFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    gc.dispose();

    Group grpDetails = new Group(container, SWT.NONE);
    grpDetails.setLayout(new GridLayout(1, false));
    grpDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    grpDetails.setText(Messages.LifecycleMappingPage_descriptionLabel);

    details = new Text(grpDetails, SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 3);
    gd.minimumHeight = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
    details.setLayoutData(gd);

    Group grpLicense = new Group(container, SWT.NONE);
    grpLicense.setLayout(new GridLayout(1, false));
    grpLicense.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
    grpLicense.setText(Messages.LifecycleMappingPage_licenseLabel);

    license = new Text(grpLicense, SWT.READ_ONLY);
    gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.heightHint = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
    gd.minimumHeight = Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
    license.setLayoutData(gd);
  }

  /**
   * @param key
   * @return
   */
  protected boolean shouldDeslectProposal(ILifecycleMappingLabelProvider prov) {
    IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
    if(proposal != null) {
      TreeItem[] items = treeViewer.getTree().getItems();
      for(TreeItem item : items) {
        if(item.getData() instanceof ILifecycleMappingLabelProvider && item.getData() != prov) {
          if(proposal.equals(mappingConfiguration.getSelectedProposal(((ILifecycleMappingLabelProvider) item.getData())
              .getKey()))) {
            return false;
          }
        }
      }
    }
    return true;
  }

  protected void discoverProposals() {
    loading = true;
    treeViewer.refresh();
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        @SuppressWarnings("synthetic-access")
        public void run(IProgressMonitor monitor) throws InvocationTargetException {
          mappingConfiguration.clearSelectedProposals();
          try {
            LifecycleMappingDiscoveryHelper.discoverProposals(mappingConfiguration, monitor);
          } catch(CoreException ex) {
            throw new InvocationTargetException(ex);
          }
          mappingConfiguration.autoCompleteMapping();
        }
      });
    } catch(InvocationTargetException e) {
      setErrorMessage(e.getMessage());
    } catch(InterruptedException e) {
      setErrorMessage(e.getMessage());
    }
    loading = false;
    treeViewer.refresh();
    getWizard().getContainer().updateButtons(); // needed to enable/disable Finish button
    updateErrorCount();
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      PlatformUI.getWorkbench().getHelpSystem()
          .setHelp(getWizard().getContainer().getShell(), M2EUIPluginActivator.PLUGIN_ID + ".LifecycleMappingPage"); //$NON-NLS-1$
      mappingConfiguration = ((MavenDiscoveryProposalWizard) getWizard()).getLifecycleMappingDiscoveryRequest();
      if(!mappingConfiguration.isMappingComplete()) {
        // try to solve problems only if there are any
        mappingConfiguration.autoCompleteMapping();
      }
      treeViewer.setInput(mappingConfiguration);
      updateErrorCount();
    }
  }

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
   * Update the error summary
   */
  private void updateErrorCount() {
    int count = 0;
    for(TreeItem item : treeViewer.getTree().getItems()) {
      ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) item.getData();
      if(!isHandled(prov)) {
        if(prov instanceof AggregateMappingLabelProvider) {
          count += ((AggregateMappingLabelProvider) prov).getChildren().length;
        } else {
          ++count;
        }
      }
    }
    errorCountLabel.setText(NLS.bind(Messages.LifecycleMappingPage_numErrors, String.valueOf(count)));
  }

  /*
   * Only applicable for top level elements. Provider is considered handled if it is ignored, a proposal has been selected, or if it is an uninteresting phase.
   */
  private boolean isHandled(ILifecycleMappingLabelProvider prov) {
    return ignore.contains(prov) || ignoreAtDefinition.contains(prov)
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
