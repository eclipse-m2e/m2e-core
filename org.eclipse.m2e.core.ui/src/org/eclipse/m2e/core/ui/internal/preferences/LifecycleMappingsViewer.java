/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - adapted for workspace preferences
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PlatformUI;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingResult;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionFilter;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.PluginExecutionMetadata;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.io.xpp3.LifecycleMappingMetadataSourceXpp3Writer;
import org.eclipse.m2e.core.internal.project.registry.MavenProjectFacade;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.lifecyclemapping.model.PluginExecutionAction;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


@SuppressWarnings("restriction")
class LifecycleMappingsViewer {
  private static final Logger log = LoggerFactory.getLogger(LifecycleMappingsViewer.class);

  private TreeViewer mappingsTreeViewer;

  private boolean showPhases = false;

  private boolean showIgnoredExecutions = true;

  private Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mappings;

  private Map<String, List<MojoExecutionKey>> phases;

  private Shell shell;

  void updateMappingsTreeViewer() {
    mappingsTreeViewer.refresh();
    if(showPhases) {
      // reveal non-empty mappings
      mappingsTreeViewer.collapseAll();
      for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : mappings.entrySet()) {
        boolean expand = false;
        if(isErrorMapping(entry.getKey())) {
          expand = true;
        } else {
          expand = !isIgnoreMapping(entry.getKey(), entry.getValue());
        }
        if(expand) {
          mappingsTreeViewer.expandToLevel(entry.getKey().getLifecyclePhase(), TreeViewer.ALL_LEVELS);
        }
      }
    }
    // auto-size all columns
    for(TreeColumn column : mappingsTreeViewer.getTree().getColumns()) {
      column.pack();
    }
  }

  /**
   * @wbp.parser.entryPoint
   */
  public Composite createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout gl_container = new GridLayout(1, false);
    gl_container.marginWidth = 0;
    gl_container.marginHeight = 0;
    container.setLayout(gl_container);

    mappingsTreeViewer = new TreeViewer(container, SWT.BORDER);
    Tree tree = mappingsTreeViewer.getTree();
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);
    tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

    TreeViewerColumn treeViewerColumn = new TreeViewerColumn(mappingsTreeViewer, SWT.NONE);
    TreeColumn trclmnGoal = treeViewerColumn.getColumn();
    trclmnGoal.setWidth(100);
    trclmnGoal.setText(Messages.LifecycleMappingPropertyPage_pluginExecution);

    TreeViewerColumn treeViewerColumn_1 = new TreeViewerColumn(mappingsTreeViewer, SWT.NONE);
    TreeColumn trclmnNewColumn = treeViewerColumn_1.getColumn();
    trclmnNewColumn.setWidth(100);
    trclmnNewColumn.setText(Messages.LifecycleMappingPropertyPage_mapping);

    TreeViewerColumn treeViewerColumn_2 = new TreeViewerColumn(mappingsTreeViewer, SWT.NONE);
    TreeColumn trclmnSource = treeViewerColumn_2.getColumn();
    trclmnSource.setWidth(100);
    trclmnSource.setText(Messages.LifecycleMappingsViewer_trclmnSource_text);

    mappingsTreeViewer.setContentProvider(new ITreeContentProvider() {

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }

      public boolean hasChildren(Object element) {
        return getChildren(element) != null;
      }

      public Object getParent(Object element) {
        return null;
      }

      public Object[] getElements(Object inputElement) {
        if(showPhases) {
          return phases.keySet().toArray();
        } else {
          Set<MojoExecutionKey> executions;
          if(showIgnoredExecutions) {
            executions = mappings.keySet();
          } else {
            executions = new LinkedHashSet<MojoExecutionKey>();
            for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : mappings.entrySet()) {
              if(!isIgnoreMapping(entry.getKey(), entry.getValue())) {
                executions.add(entry.getKey());
              }
            }
          }
          return executions.toArray();
        }
      }

      public Object[] getChildren(Object parentElement) {
        List<MojoExecutionKey> executions = phases.get(parentElement);
        if(executions == null || executions.isEmpty()) {
          return null;
        }
        if(showIgnoredExecutions) {
          return executions.toArray();
        }
        // filter out ignored executions
        executions = new ArrayList<MojoExecutionKey>(executions); // clone
        Iterator<MojoExecutionKey> iter = executions.iterator();
        while(iter.hasNext()) {
          MojoExecutionKey execution = iter.next();
          if(isIgnoreMapping(execution, mappings.get(execution))) {
            iter.remove();
          }
        }
        return !executions.isEmpty() ? executions.toArray() : null;
      }
    });

    mappingsTreeViewer.setLabelProvider(new ITableLabelProvider() {

      public void addListener(ILabelProviderListener listener) {
      }

      public void dispose() {
      }

      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      public void removeListener(ILabelProviderListener listener) {
      }

      public Image getColumnImage(Object element, int columnIndex) {
        if(columnIndex == 0 && element instanceof MojoExecutionKey) {
          return isErrorMapping((MojoExecutionKey) element) ? MavenImages.IMG_ERROR : MavenImages.IMG_PASSED;
        }
        return null;
      }

      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof MojoExecutionKey) {
          MojoExecutionKey execution = (MojoExecutionKey) element;
          switch(columnIndex) {
            case 0:
              return LifecycleMappingsViewer.this.toString(execution);
            case 1:
              return LifecycleMappingsViewer.this.toString(execution, mappings.get(execution));
            case 2:
              return getSourcelabel(execution, mappings.get(execution), false);
          }
        }
        return columnIndex == 0 ? element.toString() : null;
      }
    });

    Menu menu = new Menu(tree);
    tree.setMenu(menu);

    final MenuItem mntmShowPhases = new MenuItem(menu, SWT.CHECK);
    mntmShowPhases.setSelection(showPhases);
    mntmShowPhases.setText(Messages.LifecycleMappingPropertyPage_showLIfecyclePhases);

    final MenuItem mntmShowIgnoredExecutions = new MenuItem(menu, SWT.CHECK);
    mntmShowIgnoredExecutions.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        showIgnoredExecutions = mntmShowIgnoredExecutions.getSelection();
        updateMappingsTreeViewer();
      }
    });
    mntmShowIgnoredExecutions.setSelection(showIgnoredExecutions);
    mntmShowIgnoredExecutions.setText(Messages.LifecycleMappingPropertyPage_mntmShowIgnoredExecutions_text);

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem mntmExpandAll = new MenuItem(menu, SWT.NONE);
    mntmExpandAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        mappingsTreeViewer.expandAll();
      }
    });
    mntmExpandAll.setEnabled(showPhases);
    mntmExpandAll.setText(Messages.LifecycleMappingPropertyPage_mntmExpandAll_text);

    final MenuItem mntmCollapseAll = new MenuItem(menu, SWT.NONE);
    mntmCollapseAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        mappingsTreeViewer.collapseAll();
      }
    });
    mntmCollapseAll.setEnabled(showPhases);
    mntmCollapseAll.setText(Messages.LifecycleMappingPropertyPage_mntmCollapseAll_text);

    mntmShowPhases.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        showPhases = mntmShowPhases.getSelection();
        mntmExpandAll.setEnabled(showPhases);
        mntmCollapseAll.setEnabled(showPhases);
        updateMappingsTreeViewer();
      }
    });

    Composite actionsComposite = new Composite(container, SWT.NONE);
    actionsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
    actionsComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

    Button btnCopyToClipboard = new Button(actionsComposite, SWT.NONE);
    btnCopyToClipboard.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        copyToClipboard();
      }
    });
    btnCopyToClipboard.setText(Messages.LifecycleMappingPropertyPage_copyToClipboard);

    mappingsTreeViewer.setInput(phases);

    updateMappingsTreeViewer();
    return container;
  }

  void copyToClipboard() {
    if(mappings == null) {
      return;
    }

    LifecycleMappingMetadata meta = new LifecycleMappingMetadata();

    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : this.mappings.entrySet()) {
      MojoExecutionKey execution = entry.getKey();
      List<IPluginExecutionMetadata> mappings = entry.getValue();
      if(mappings != null && !mappings.isEmpty()) {
        for(IPluginExecutionMetadata mapping : mappings) {
          PluginExecutionMetadata clone = ((PluginExecutionMetadata) mapping).clone();
          setMappingSource(execution, mappings, clone);
          meta.addPluginExecution(clone);
        }
      } else {
        PluginExecutionFilter filter = new PluginExecutionFilter(execution.getGroupId(), execution.getArtifactId(),
            execution.getVersion(), execution.getGoal());

        PluginExecutionMetadata mapping = new PluginExecutionMetadata();
        mapping.setFilter(filter);

        Xpp3Dom actionDom;
        if(LifecycleMappingFactory.isInterestingPhase(entry.getKey().getLifecyclePhase())) {
          actionDom = new Xpp3Dom(PluginExecutionAction.error.toString());
        } else {
          actionDom = new Xpp3Dom(PluginExecutionAction.ignore.toString());
        }
        mapping.setActionDom(actionDom);

        setMappingSource(execution, mappings, mapping);

        meta.addPluginExecution(mapping);
      }
    }

    LifecycleMappingMetadataSource xml = new LifecycleMappingMetadataSource();
    xml.addLifecycleMapping(meta);

    StringWriter buf = new StringWriter();
    try {
      new LifecycleMappingMetadataSourceXpp3Writer().write(buf, xml);

      Clipboard clipboard = new Clipboard(shell.getDisplay());

      Object[] data = new Object[] {buf.toString()};
      Transfer[] dataTypes = new Transfer[] {TextTransfer.getInstance()};

      clipboard.setContents(data, dataTypes);

      clipboard.dispose();
    } catch(IOException ex) {
      // TODO log
    }
  }

  private void setMappingSource(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings,
      PluginExecutionMetadata clone) {
    clone.setComment("source: " + getSourcelabel(execution, mappings, true)); //$NON-NLS-1$
  }

  boolean isErrorMapping(MojoExecutionKey execution) {
    List<IPluginExecutionMetadata> mappings = this.mappings.get(execution);
    if(mappings == null || mappings.isEmpty()) {
      return LifecycleMappingFactory.isInterestingPhase(execution.getLifecyclePhase());
    }
    for(IPluginExecutionMetadata mapping : mappings) {
      if(PluginExecutionAction.error == mapping.getAction()) {
        return true;
      }
    }
    return false;
  }

  boolean isIgnoreMapping(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings) {
    if(mappings == null || mappings.isEmpty()) {
      return !LifecycleMappingFactory.isInterestingPhase(execution.getLifecyclePhase());
    }
    for(IPluginExecutionMetadata mapping : mappings) {
      if(PluginExecutionAction.ignore != mapping.getAction()) {
        return false;
      }
    }
    return true;
  }

  String toString(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings) {
    StringBuilder sb = new StringBuilder();
    if(mappings != null && !mappings.isEmpty()) {
      for(IPluginExecutionMetadata mapping : mappings) {
        if(sb.length() > 0) {
          sb.append(',');
        }
        sb.append(mapping.getAction().toString());
      }
    } else {
      if(LifecycleMappingFactory.isInterestingPhase(execution.getLifecyclePhase())) {
        sb.append(PluginExecutionAction.error.toString());
      } else {
        sb.append(PluginExecutionAction.ignore.toString());
      }
    }
    return sb.toString();
  }

  String getSourcelabel(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings, boolean detailed) {
    LinkedHashSet<String> sources = new LinkedHashSet<String>();
    if(mappings != null && !mappings.isEmpty()) {
      for(IPluginExecutionMetadata mapping : mappings) {
        LifecycleMappingMetadataSource metadata = ((PluginExecutionMetadata) mapping).getSource();
        if(metadata != null) {
          Object source = metadata.getSource();
          if(source instanceof String) {
            sources.add((String) source);
          } else if(source instanceof Artifact) {
            sources.add(getSourceLabel((Artifact) source, detailed));
          } else if(source instanceof MavenProject) {
            sources.add(getSourceLabel((MavenProject) source, detailed));
          } else if(source instanceof Bundle) {
            sources.add(getSourceLabel((Bundle) source, detailed));
          } else {
            sources.add("unknown"); //$NON-NLS-1$
          }
        }
      }
    } else {
      if(!LifecycleMappingFactory.isInterestingPhase(execution.getLifecyclePhase())) {
        sources.add("uninteresting"); //$NON-NLS-1$
      }
    }
    StringBuilder sb = new StringBuilder();
    for(String source : sources) {
      if(sb.length() > 0) {
        sb.append(',');
      }
      sb.append(source);
    }
    return sb.toString();
  }

  private String getSourceLabel(Bundle bundle, boolean detailed) {
    StringBuilder sb = new StringBuilder("extension"); //$NON-NLS-1$
    if(detailed) {
      sb.append('(').append(bundle.getSymbolicName()).append('_').append(bundle.getVersion()).append(')');
    }
    return sb.toString();
  }

  private String getSourceLabel(MavenProject project, boolean detailed) {
    StringBuilder sb = new StringBuilder("pom"); //$NON-NLS-1$
    if(detailed) {
      sb.append('(').append(project.toString()).append(')');
    }
    return sb.toString();
  }

  private String getSourceLabel(Artifact plugin, boolean detailed) {
    StringBuilder sb = new StringBuilder("maven-plugin"); //$NON-NLS-1$
    if(detailed) {
      sb.append('(').append(plugin.toString()).append(')');
    }
    return sb.toString();
  }

  String toString(MojoExecutionKey execution) {
    // http://maven.apache.org/guides/plugin/guide-java-plugin-development.html#Shortening_the_Command_Line

    StringBuilder sb = new StringBuilder();

    // TODO show groupId, but only if not a known plugin groupId

    // shorten artifactId
    String artifactId = execution.getArtifactId();
    if(artifactId.endsWith("-maven-plugin")) { //$NON-NLS-1$
      artifactId = artifactId.substring(0, artifactId.length() - "-maven-plugin".length()); //$NON-NLS-1$
    } else if(artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) { //$NON-NLS-1$ //$NON-NLS-2$
      artifactId = artifactId.substring("maven-".length(), artifactId.length() - "-plugin".length()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    sb.append(artifactId).append(':').append(execution.getGoal());

    // only show execution id if necessary
    int count = 0;
    for(MojoExecutionKey other : mappings.keySet()) {
      if(eq(execution.getGroupId(), other.getGroupId()) && eq(execution.getArtifactId(), other.getArtifactId())
          && eq(execution.getGoal(), other.getGoal())) {
        count++ ;
      }
    }
    if(count > 1) {
      sb.append(" (").append(execution.getExecutionId()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return sb.toString();
  }

  static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }

  public void setTarget(final IProject project) {
    if(project == null) {
      // TODO FIXADE find the mojo execution mapping for the workspace...How do I do this?
    } else {
      try {
        PlatformUI.getWorkbench().getProgressService().run(false, false, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
            MavenExecutionRequest executionRequest;
            try {
              facade.getMavenProject(monitor); // make sure MavenProject is loaded
              executionRequest = MavenPlugin.getMaven().createExecutionRequest(monitor);
            } catch(CoreException ex) {
              throw new InvocationTargetException(ex);
            }
            LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(executionRequest,
                (MavenProjectFacade) facade, monitor);
            mappings = mappingResult.getMojoExecutionMapping();
          }
        });
      } catch(InvocationTargetException ex) {
        log.error(ex.getMessage(), ex);
      } catch(InterruptedException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    phases = new LinkedHashMap<String, List<MojoExecutionKey>>();
    for(MojoExecutionKey execution : mappings.keySet()) {
      List<MojoExecutionKey> executions = phases.get(execution.getLifecyclePhase());
      if(executions == null) {
        executions = new ArrayList<MojoExecutionKey>();
        phases.put(execution.getLifecyclePhase(), executions);
      }
      executions.add(execution);
    }
  }

  /**
   * @param shell
   */
  public void setShell(Shell shell) {
    this.shell = shell;
  }
}
