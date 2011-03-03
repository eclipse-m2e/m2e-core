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
import java.util.HashMap;
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
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.AggregateMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.ILifecycleMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.LifecycleMappingConfiguration;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.MojoExecutionMappingLabelProvider;
import org.eclipse.m2e.core.ui.internal.lifecyclemapping.PackagingTypeMappingLabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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

  private TreeViewer treeViewer;

  private IAdapterManager adapterManager;

  private Button btnNewButton;

  private boolean loading = true;

  /**
   * Create the wizard.
   */
  public LifecycleMappingPage() {
    super("LifecycleMappingPage");
    setTitle("Setup Maven plugin connectors");
    setDescription("Discover and map Eclipse plugins to Maven plugin goal executions.");
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
    container.setLayout(new GridLayout(1, false));

    treeViewer = new TreeViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
//    treeViewer.setUseHashlookup(true);

    Tree tree = treeViewer.getTree();
    tree.setLinesVisible(true);
    tree.setHeaderVisible(true);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TreeViewerColumn treeViewerColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn trclmnNewColumn = treeViewerColumn.getColumn();
    trclmnNewColumn.setText("Maven build");
    
    TreeViewerColumn columnViewerMapping = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn columnMapping = columnViewerMapping.getColumn();
    columnMapping.setText("Eclipse build");

    TreeViewerColumn columnViewerAction = new TreeViewerColumn(treeViewer, SWT.NONE);
    TreeColumn columnAction = columnViewerAction.getColumn();
    columnAction.setText("Action");
//    columnViewerAction.setEditingSupport(new EditingSupport(treeViewer) {
//      
//      @Override
//      protected void setValue(Object element, Object value) {
//        if (element instanceof ILifecycleMappingLabelProvider) { 
//          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider)element;
//          Integer val = (Integer)value;
//          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
//          IMavenDiscoveryProposal sel = all.get(val.intValue());
//          IMavenDiscoveryProposal prop = mappingConfiguration.getSelectedProposal(prov.getKey());
//          if (prop != null) {
//            mappingConfiguration.removeSelectedProposal(prop);
//          }
//          if (sel != null) {
//            mappingConfiguration.addSelectedProposal(sel);
//          }
//        }
//      }
//      
//      @Override
//      protected Object getValue(Object element) {
//        if (element instanceof ILifecycleMappingLabelProvider) { 
//          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider)element;
//          IMavenDiscoveryProposal prop = mappingConfiguration.getSelectedProposal(prov.getKey());
//          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
//          return new Integer(all.indexOf(prop));
//        }
//        return new Integer(0);
//      }
//      
//      @Override
//      protected CellEditor getCellEditor(Object element) {
//        if (element instanceof ILifecycleMappingLabelProvider) { 
//          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider)element;
//          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
//          List<String> values = new ArrayList<String>();
//          for (IMavenDiscoveryProposal prop : all) {
//            values.add(NLS.bind("Install {0}", prop.toString()));
//          }
//          ComboBoxCellEditor edit = new ComboBoxCellEditor(treeViewer.getTree(), values.toArray(new String[0]));
//          return edit;
//        }
//        throw new IllegalStateException();
//      }
//      
//      @Override
//      protected boolean canEdit(Object element) {
//        if (element instanceof ILifecycleMappingLabelProvider) { 
//          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider)element;
//          List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
//          return all != null && all.size() > 1;
//        }
//        return false;
//      }
//    });
    
    treeViewer.setContentProvider(new ITreeContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        if(inputElement instanceof LifecycleMappingConfiguration) {
          Map<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>> packagings = new HashMap<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>>();
          Map<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>> mojos = new HashMap<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>>();
          Collection<ProjectLifecycleMappingConfiguration> projects = ((LifecycleMappingConfiguration) inputElement).getProjects();
          for (ProjectLifecycleMappingConfiguration prjconf : projects) {
            PackagingTypeMappingConfiguration pack = prjconf.getPackagingTypeMappingConfiguration();
            if (pack != null && !pack.isOK()) {
              List<ILifecycleMappingLabelProvider> val = packagings.get(pack.getLifecycleMappingElementKey());
              if (val == null) {
                val = new ArrayList<ILifecycleMappingLabelProvider>();
                packagings.put(pack.getLifecycleMappingElementKey(), val);
              }
              val.add(new PackagingTypeMappingLabelProvider(prjconf, pack));
            }
            List<MojoExecutionMappingConfiguration> mojoExecs = prjconf.getMojoExecutionConfigurations();
            if (mojoExecs != null) {
              for (MojoExecutionMappingConfiguration mojoMap : mojoExecs) {
                if (!mojoMap.isOK()) {
                  List<ILifecycleMappingLabelProvider> val = mojos.get(mojoMap.getLifecycleMappingElementKey());
                  if (val == null) {
                    val = new ArrayList<ILifecycleMappingLabelProvider>();
                    mojos.put(mojoMap.getLifecycleMappingElementKey(), val);
                  }
                  val.add(new MojoExecutionMappingLabelProvider(prjconf, mojoMap));
                }
              }
            }
          }
          List<ILifecycleMappingLabelProvider> toRet = new ArrayList<ILifecycleMappingLabelProvider>();
          for (Map.Entry<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>> ent : packagings.entrySet()) {
            toRet.add(new AggregateMappingLabelProvider(ent.getKey(), ent.getValue()));
          }
          for (Map.Entry<ILifecycleMappingElementKey, List<ILifecycleMappingLabelProvider>> ent : mojos.entrySet()) {
            toRet.add(new AggregateMappingLabelProvider(ent.getKey(), ent.getValue()));
          }
          return toRet.toArray();
        }
        return null;
      }

      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof AggregateMappingLabelProvider) {
          return ((AggregateMappingLabelProvider)parentElement).getChildren();
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

      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider) element;
          if(columnIndex == 0) {
            return prov.getMavenText();
          }
          if (columnIndex == 1) {
            return prov.getEclipseMappingText(mappingConfiguration);
          }
          if (columnIndex == 2) {
            IMavenDiscoveryProposal proposal = mappingConfiguration.getSelectedProposal(prov.getKey());
            if (proposal != null) {
              return NLS.bind("Install {0}", proposal.toString()); //not really feeling well here.
            }
            if (loading) {
              return "";
            } else {
              return "";//"Nothing discovered";
            }
//            List<IMavenDiscoveryProposal> all = mappingConfiguration.getProposals(prov.getKey());
//            if (all.size() > 0) {
//              return "<Please select>";
//            } else {
//              return "Nothing discovered";
//            }
          }
        }
        return null;
      }

      public Image getColumnImage(Object element, int columnIndex) {
        if(element instanceof ILifecycleMappingLabelProvider) {
          ILifecycleMappingLabelProvider prov = (ILifecycleMappingLabelProvider)element;
          if (columnIndex == 0 && prov.isError()) {
            if (mappingConfiguration.getSelectedProposal(prov.getKey()) == null) {
              return MavenImages.IMG_ERROR;
            } else {
              return MavenImages.IMG_MSG_INFO;
            }
          }
        }
        return null;
      }
    });

//    treeViewer.setCheckStateProvider(new ICheckStateProvider() {
//
//      public boolean isGrayed(Object element) {
//        return false;
//      }
//
//      public boolean isChecked(Object element) {
//        if(element instanceof IMavenDiscoveryProposal) {
//          return mappingConfiguration.getSelectedProposals().contains(element);
//        }
//        return false;
//      }
//    });
//
//    treeViewer.addCheckStateListener(new ICheckStateListener() {
//      public void checkStateChanged(CheckStateChangedEvent event) {
//        if(!(event.getElement() instanceof IMavenDiscoveryProposal)) {
//          event.getCheckable().setChecked(event.getElement(), false);
//          return;
//        }
//        IMavenDiscoveryProposal proposal = (IMavenDiscoveryProposal) event.getElement();
//        if(event.getChecked()) {
//          mappingConfiguration.addSelectedProposal(proposal);
//        } else {
//          mappingConfiguration.removeSelectedProposal(proposal);
//        }
//
//        discoverProposals();
//      }
//    });

    btnNewButton = new Button(container, SWT.NONE);
    btnNewButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        mappingConfiguration.clearSelectedProposals();
        discoverProposals();
      }
    });
    btnNewButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
    btnNewButton.setText("Check m2e marketplace");
  }

  protected void discoverProposals() {
    loading = true;
    treeViewer.refresh();
    final IMavenDisovery discovery = ((AbstractMavenProjectWizard) getWizard()).getDiscovery();
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          Collection<ProjectLifecycleMappingConfiguration> projects = mappingConfiguration.getProjects();
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
                  SubMonitor.convert(monitor, NLS.bind("Analysing {0}", project.getRelpath()), 1)));
            } catch(CoreException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            monitor.worked(1);
          }

          mappingConfiguration.setProposals(proposals);
          mappingConfiguration.autoCompleteMapping();
        }
      });
    } catch(InvocationTargetException e) {
      setErrorMessage(e.getMessage());
    } catch(InterruptedException e) {
      setErrorMessage(e.getMessage());
    }
    loading  = false;
    treeViewer.refresh();

    setPageComplete(mappingConfiguration.isMappingComplete());
  }

//  protected String getMojoExecutionColumnText(MojoExecutionMappingConfiguration execution, int columnIndex) {
//    MojoExecutionMappingLabelProvider provider = new MojoExecutionMappingLabelProvider(execution);
//    switch(columnIndex) {
//      case MAVEN_INFO_IDX:
//        return provider.getMavenText();
//      case ECLIPSE_INFO_IDX:
//        String text = provider.getEclipseMappingText();
//        if(!execution.isOK()
//            && mappingConfiguration.getSelectedProposal(execution.getLifecycleMappingElementKey()) != null) {
//          text = "OK (WAS: " + text + ")";
//        }
//        return text;
//    }
//    return null;
//  }
//
//  protected String getPackagingTypeMappingColumnText(PackagingTypeMappingConfiguration packagingType, int columnIndex) {
//    PackagingTypeMappingLabelProvider provider = new PackagingTypeMappingLabelProvider(packagingType);
//    switch(columnIndex) {
//      case MAVEN_INFO_IDX:
//        return provider.getMavenText();
//      case ECLIPSE_INFO_IDX:
//        String text = provider.getEclipseMappingText();
//        if(!packagingType.isOK()
//            && mappingConfiguration.getSelectedProposal(packagingType.getLifecycleMappingElementKey()) != null) {
//          text = "OK (WAS: " + text + ")";
//        }
//        return text;
//    }
//    return null;
//  }
//
//  protected String getProjectColumnText(ProjectLifecycleMappingConfiguration configuration, int columnIndex) {
//    switch(columnIndex) {
//      case MAVEN_INFO_IDX:
//        return configuration.getMavenText();
//    }
//    return null;
//  }

//  protected String getProposalColumnText(IMavenDiscoveryProposal proposal, int columnIndex) {
//    ILifecycleMappingLabelProvider provider = (ILifecycleMappingLabelProvider) adapterManager.getAdapter(proposal,
//        ILifecycleMappingLabelProvider.class);
//    if(provider == null) {
//      return null;
//    }
//    switch(columnIndex) {
//      case MAVEN_INFO_IDX:
//        return provider.getMavenText();
//      case ECLIPSE_INFO_IDX:
//        return provider.getEclipseMappingText();
//    }
//    return null;
//  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if(visible) {
      mappingConfiguration = getMappingConfiguration();
      treeViewer.setInput(mappingConfiguration);
      
      //set initial column sizes
      TreeColumn[] columns = treeViewer.getTree().getColumns();
      for (int i = 0; i < columns.length; i++) {
        int ratio = i == 0 ? 5 :  i == 1 ? 3 : 2;
        columns[i].setWidth(treeViewer.getTree().getClientArea().width / 10 * ratio);        
      }
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          discoverProposals();
        }
      });
    }
  }


  protected Collection<MavenProjectInfo> getProjects() {
    return ((MavenImportWizard) getWizard()).getProjects();
  }

  protected ProjectImportConfiguration getProjectImportConfiguration() {
    return ((MavenImportWizard) getWizard()).getProjectImportConfiguration();
  }
  
  protected LifecycleMappingConfiguration getMappingConfiguration() {
    return ((MavenImportWizard) getWizard()).getMappingConfiguration();
  }

  public List<IMavenDiscoveryProposal> getSelectedDiscoveryProposals() {
    if(mappingConfiguration == null) {
      return Collections.emptyList();
    }
    return mappingConfiguration.getSelectedProposals();
  }
  

}
