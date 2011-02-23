/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ILifecycleMappingElementKey;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDiscoveryProposal;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.IMavenDisovery;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.MojoExecutionMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.PackagingTypeMappingConfiguration;
import org.eclipse.m2e.core.internal.lifecyclemapping.discovery.ProjectLifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.MojoExecutionMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.PackagingTypeMappingLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;


/**
 * LifecycleMappingPage
 * 
 * @author igor
 */
@SuppressWarnings("restriction")
public class LifecycleMappingPage extends WizardPage {

  private static final int MAVEN_INFO_IDX = 0;

  private static final int ECLIPSE_INFO_IDX = 1;

  private LifecycleMappingConfiguration mappingConfiguration;

  private CheckboxTreeViewer treeViewer;

  private IAdapterManager adapterManager;

  /**
   * Create the wizard.
   */
  public LifecycleMappingPage() {
    super("wizardPage");
    setTitle("Wizard Page title");
    setDescription("Wizard Page description");
    adapterManager = Platform.getAdapterManager();
  }

  /**
   * Create contents of the wizard.
   * 
   * @param parent
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);

    setControl(container);
    container.setLayout(new GridLayout(2, false));

    treeViewer = new CheckboxTreeViewer(container, SWT.BORDER);
    treeViewer.setUseHashlookup(true);

    Tree tree = treeViewer.getTree();
    tree.setLinesVisible(true);
    tree.setHeaderVisible(true);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TreeViewerColumn treeViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn trclmnNewColumn = treeViewerColumn.getColumn();
    trclmnNewColumn.setWidth(222);
    trclmnNewColumn.setText("Maven");

    TreeViewerColumn treeViewerColumn_1 = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn trclmnNewColumn_1 = treeViewerColumn_1.getColumn();
    trclmnNewColumn_1.setWidth(100);
    trclmnNewColumn_1.setText("Eclipse mapping");
    treeViewer.setContentProvider(new ITreeContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof LifecycleMappingConfiguration) {
          return ((LifecycleMappingConfiguration) inputElement).getProjects().toArray();
        }
        return null;
      }

      public Object[] getChildren(Object parentElement) {
        ArrayList<Object> children = new ArrayList<Object>();
        if(parentElement instanceof ProjectLifecycleMappingConfiguration) {
          children.add(((ProjectLifecycleMappingConfiguration) parentElement).getPackagingTypeMappingConfiguration());
          for(MojoExecutionMappingConfiguration mojoExecution : ((ProjectLifecycleMappingConfiguration) parentElement)
              .getMojoExecutionConfigurations()) {
            children.add(mojoExecution);
          }
        } else if(parentElement instanceof MojoExecutionMappingConfiguration) {
          children.addAll(mappingConfiguration.getProposals(((MojoExecutionMappingConfiguration) parentElement)
              .getLifecycleMappingElementKey()));
        } else if(parentElement instanceof PackagingTypeMappingConfiguration) {
          children.addAll(mappingConfiguration.getProposals(((PackagingTypeMappingConfiguration) parentElement)
              .getLifecycleMappingElementKey()));
        }
        return children.toArray();
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

      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof ProjectLifecycleMappingConfiguration) {
          return getProjectColumnText((ProjectLifecycleMappingConfiguration) element, columnIndex);
        } else if(element instanceof IMavenDiscoveryProposal) {
          return getProposalColumnText((IMavenDiscoveryProposal) element, columnIndex);
        } else if(element instanceof MojoExecutionMappingConfiguration) {
          return getMojoExecutionColumnText((MojoExecutionMappingConfiguration) element, columnIndex);
        } else if(element instanceof PackagingTypeMappingConfiguration) {
          return getPackagingTypeMappingColumnText((PackagingTypeMappingConfiguration) element, columnIndex);
        }
        return null;
      }

      public Image getColumnImage(Object element, int columnIndex) {
        return null;
      }
    });

    treeViewer.setCheckStateProvider(new ICheckStateProvider() {

      public boolean isGrayed(Object element) {
        return false;
      }

      public boolean isChecked(Object element) {
        if(element instanceof IMavenDiscoveryProposal) {
          return mappingConfiguration.getSelectedProposals().contains(element);
        }
        return false;
      }
    });

    treeViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        if(!(event.getElement() instanceof IMavenDiscoveryProposal)) {
          event.getCheckable().setChecked(event.getElement(), false);
          return;
        }
        IMavenDiscoveryProposal proposal = (IMavenDiscoveryProposal) event.getElement();
        if(event.getChecked()) {
          mappingConfiguration.addSelectedProposal(proposal);
        } else {
          mappingConfiguration.removeSelectedProposal(proposal);
        }

        discoverProposals();
      }
    });

    Button btnNewButton = new Button(container, SWT.NONE);
    btnNewButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        mappingConfiguration.clearSelectedProposals();
        discoverProposals();
      }
    });
    btnNewButton.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false, 1, 1));
    btnNewButton.setText("Search marketplace");
  }

  protected void discoverProposals() {
    final IMavenDisovery discovery = ((AbstractMavenProjectWizard) getWizard()).getDiscovery();

    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          List<ProjectLifecycleMappingConfiguration> projects = mappingConfiguration.getProjects();
          monitor.beginTask("Searching m2e marketplace", projects.size());

          Map<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>> proposals = new LinkedHashMap<ILifecycleMappingElementKey, List<IMavenDiscoveryProposal>>();

          for(ProjectLifecycleMappingConfiguration project : projects) {
            if(monitor.isCanceled()) {
              throw new OperationCanceledException();
            }
            MavenProject mavenProject = project.getMavenProject();
            List<MojoExecution> mojoExecutions = project.getMojoExecutions();
            try {
              proposals.putAll(discovery.discover(mavenProject, mojoExecutions,
                  mappingConfiguration.getSelectedProposals(),
                  SubMonitor.convert(monitor, "Analysing " + project.getRelpath(), 1)));
            } catch(CoreException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            monitor.worked(1);
          }

          mappingConfiguration.setProposals(proposals);
        }
      });
    } catch(InvocationTargetException e) {
      setErrorMessage(e.getMessage());
    } catch(InterruptedException e) {
      setErrorMessage(e.getMessage());
    }

    treeViewer.refresh();
    treeViewer.expandAll();

    //setPageComplete(mappingConfiguration.isMappingComplete());
  }

  protected String getMojoExecutionColumnText(MojoExecutionMappingConfiguration execution, int columnIndex) {
    MojoExecutionMappingLabelProvider provider = new MojoExecutionMappingLabelProvider(execution);
    switch(columnIndex) {
      case MAVEN_INFO_IDX:
        return provider.getMavenText();
      case ECLIPSE_INFO_IDX:
        String text = provider.getEclipseMappingText();
        if(!execution.isOK()
            && mappingConfiguration.getSelectedProposal(execution.getLifecycleMappingElementKey()) != null) {
          text = "OK (WAS: " + text + ")";
        }
        return text;
    }
    return null;
  }

  protected String getPackagingTypeMappingColumnText(PackagingTypeMappingConfiguration packagingType, int columnIndex) {
    PackagingTypeMappingLabelProvider provider = new PackagingTypeMappingLabelProvider(packagingType);
    switch(columnIndex) {
      case MAVEN_INFO_IDX:
        return provider.getMavenText();
      case ECLIPSE_INFO_IDX:
        String text = provider.getEclipseMappingText();
        if(!packagingType.isOK()
            && mappingConfiguration.getSelectedProposal(packagingType.getLifecycleMappingElementKey()) != null) {
          text = "OK (WAS: " + text + ")";
        }
        return text;
    }
    return null;
  }

  protected String getProjectColumnText(ProjectLifecycleMappingConfiguration configuration, int columnIndex) {
    switch(columnIndex) {
      case MAVEN_INFO_IDX:
        return configuration.getMavenText();
    }
    return null;
  }

  protected String getProposalColumnText(IMavenDiscoveryProposal proposal, int columnIndex) {
    ILifecycleMappingLabelProvider provider = (ILifecycleMappingLabelProvider) adapterManager.getAdapter(proposal,
        ILifecycleMappingLabelProvider.class);
    if(provider == null) {
      return null;
    }
    switch(columnIndex) {
      case MAVEN_INFO_IDX:
        return provider.getMavenText();
      case ECLIPSE_INFO_IDX:
        return provider.getEclipseMappingText();
    }
    return null;
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      try {
        final ProjectImportConfiguration importConfiguration = getProjectImportConfiguration();
        final Collection<MavenProjectInfo> projects = getProjects();
        getContainer().run(true, true, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
              mappingConfiguration = LifecycleMappingConfiguration.calculate(projects, importConfiguration, monitor);
            } catch(CoreException e) {
              throw new InvocationTargetException(e);
            }
          }
        });
        treeViewer.setInput(mappingConfiguration);
        treeViewer.expandAll();
      } catch(InvocationTargetException e) {
        setErrorMessage(e.getMessage());
      } catch(InterruptedException e) {
        setErrorMessage(e.getMessage());
      }
    }
  }

  protected Collection<MavenProjectInfo> getProjects() {
    return ((MavenImportWizard) getWizard()).getProjects();
  }

  protected ProjectImportConfiguration getProjectImportConfiguration() {
    return ((MavenImportWizard) getWizard()).getProjectImportConfiguration();
  }

  public List<IMavenDiscoveryProposal> getSelectedDiscoveryProposals() {
    if(mappingConfiguration == null) {
      return Collections.emptyList();
    }
    return mappingConfiguration.getSelectedProposals();
  }
}
