/*******************************************************************************
 * Copyright (c) 2011, 2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *      Andrew Eisenberg - adapted for workspace preferences
 *      Christoph Läubrich -  #563  Make the life-cycle-mapping available as a view in eclipse
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.lifecyclemapping.DefaultPluginExecutionMetadata;
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
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;


@SuppressWarnings("restriction")
public class LifecycleMappingsViewer {
  /*package*/TreeViewer mappingsTreeViewer;

  /*package*/boolean showPhases = false;

  /*package*/boolean showIgnoredExecutions = true;

  /*package*/final AtomicReference<Map<MojoExecutionKey, List<IPluginExecutionMetadata>>> mappings = new AtomicReference<>(
      Collections.emptyMap());

  /*package*/final AtomicReference<Map<String, List<MojoExecutionKey>>> phases = new AtomicReference<>(
      Collections.emptyMap());

  private Shell shell;

  private IProject project;

  void updateMappingsTreeViewer() {
    if(mappingsTreeViewer == null) {
      return;
    }
    mappingsTreeViewer.refresh();
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> map = mappings.get();
    if(showPhases) {
      // reveal non-empty mappings
      mappingsTreeViewer.collapseAll();
      for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : map.entrySet()) {
        boolean expand = false;
        if(isErrorMapping(entry.getKey())) {
          expand = true;
        } else {
          expand = !isIgnoreMapping(entry.getKey(), entry.getValue());
        }
        if(expand) {
          mappingsTreeViewer.expandToLevel(entry.getKey().lifecyclePhase(), AbstractTreeViewer.ALL_LEVELS);
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

    Composite optionsComposit = new Composite(container, SWT.NONE);
    GridLayout gl_optionsComposit = new GridLayout(3, false);
    gl_optionsComposit.marginWidth = 0;
    gl_optionsComposit.marginHeight = 0;
    optionsComposit.setLayout(gl_optionsComposit);
    optionsComposit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

    final Button btnShowPhases = new Button(optionsComposit, SWT.CHECK);
    btnShowPhases.setSelection(showPhases);
    btnShowPhases.setText(Messages.LifecycleMappingPropertyPage_showLIfecyclePhases);

    final Button btnShowIgnored = new Button(optionsComposit, SWT.CHECK);
    btnShowIgnored.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      showIgnoredExecutions = btnShowIgnored.getSelection();
      updateMappingsTreeViewer();
    }));
    btnShowIgnored.setSelection(showIgnoredExecutions);
    btnShowIgnored.setText(Messages.LifecycleMappingPropertyPage_mntmShowIgnoredExecutions_text);
    final Action actExpandAll = new Action(Messages.LifecycleMappingPropertyPage_mntmExpandAll_text,
        MavenImages.EXPANDALL) {
      @Override
      public void run() {
        mappingsTreeViewer.expandAll();
      }
    };
    actExpandAll.setEnabled(showPhases);
    final Action actCollapseAll = new Action(Messages.LifecycleMappingPropertyPage_mntmCollapseAll_text,
        MavenImages.COLLAPSEALL) {
      @Override
      public void run() {
        mappingsTreeViewer.collapseAll();
      }
    };
    actCollapseAll.setEnabled(showPhases);

    Composite toolbarComposite = new Composite(optionsComposit, SWT.NONE);
    toolbarComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
    GridLayout gl_toolbarComposite = new GridLayout(1, false);
    gl_toolbarComposite.marginWidth = 0;
    gl_toolbarComposite.marginHeight = 0;
    toolbarComposite.setLayout(gl_toolbarComposite);

    ToolBar toolBar = new ToolBar(toolbarComposite, SWT.FLAT | SWT.RIGHT);
    ToolBarManager toolBarManager = new ToolBarManager(toolBar);
    toolBarManager.add(actExpandAll);
    toolBarManager.add(actCollapseAll);
    toolBarManager.update(true);

    btnShowPhases.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      showPhases = btnShowPhases.getSelection();
      actExpandAll.setEnabled(showPhases);
      actCollapseAll.setEnabled(showPhases);
      updateMappingsTreeViewer();
    }));

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

      @Override
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      @Override
      public void dispose() {
      }

      @Override
      public boolean hasChildren(Object element) {
        return getChildren(element) != null;
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public Object[] getElements(Object inputElement) {
        if(showPhases) {
          return phases.get().keySet().toArray();
        }
        Set<MojoExecutionKey> executions;
        if(showIgnoredExecutions) {
          executions = mappings.get().keySet();
        } else {
          executions = new LinkedHashSet<>();
          for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : mappings.get().entrySet()) {
            if(!isIgnoreMapping(entry.getKey(), entry.getValue())) {
              executions.add(entry.getKey());
            }
          }
        }
        return executions.toArray();
      }

      @Override
      public Object[] getChildren(Object parentElement) {
        List<MojoExecutionKey> executions = phases.get().get(parentElement);
        if(executions == null || executions.isEmpty()) {
          return null;
        }
        if(showIgnoredExecutions) {
          return executions.toArray();
        }
        // filter out ignored executions
        executions = new ArrayList<>(executions); // clone
        Iterator<MojoExecutionKey> iter = executions.iterator();
        while(iter.hasNext()) {
          MojoExecutionKey execution = iter.next();
          if(isIgnoreMapping(execution, mappings.get().get(execution))) {
            iter.remove();
          }
        }
        return !executions.isEmpty() ? executions.toArray() : null;
      }
    });

    mappingsTreeViewer.setLabelProvider(new ITableLabelProvider() {

      @Override
      public void addListener(ILabelProviderListener listener) {
      }

      @Override
      public void dispose() {
      }

      @Override
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }

      @Override
      public void removeListener(ILabelProviderListener listener) {
      }

      @Override
      public Image getColumnImage(Object element, int columnIndex) {
        if(columnIndex == 0 && element instanceof MojoExecutionKey key) {
          return isErrorMapping(key) ? MavenImages.IMG_ERROR : MavenImages.IMG_PASSED;
        }
        return null;
      }

      @Override
      public String getColumnText(Object element, int columnIndex) {
        if(element instanceof MojoExecutionKey execution) {
          switch(columnIndex) {
            case 0:
              return LifecycleMappingsViewer.this.toString(execution);
            case 1:
              return LifecycleMappingsViewer.this.toString(execution, mappings.get().get(execution));
            case 2:
              return getSourcelabel(execution, mappings.get().get(execution), false);
          }
        }
        return columnIndex == 0 ? element.toString() : null;
      }
    });

    Composite actionsComposite = new Composite(container, SWT.NONE);
    actionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    actionsComposite.setLayout(new GridLayout(2, false));
    Label infoLabel = new Label(actionsComposite, SWT.NONE);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    Button btnCopyToClipboard = new Button(actionsComposite, SWT.NONE);
    btnCopyToClipboard.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> copyToClipboard()));
    btnCopyToClipboard.setText(Messages.LifecycleMappingPropertyPage_copyToClipboard);

    mappingsTreeViewer.setInput(phases);
    mappingsTreeViewer.addSelectionChangedListener(e -> {
      Object element = e.getStructuredSelection().getFirstElement();
      if(element instanceof MojoExecutionKey execution) {
        infoLabel.setText(getSourcelabel(execution, mappings.get().get(execution), true));
      } else {
        infoLabel.setText("");
      }
    });

    updateMappingsTreeViewer();
    return container;
  }

  void copyToClipboard() {
    Map<MojoExecutionKey, List<IPluginExecutionMetadata>> map = mappings.get();
    if(map.isEmpty()) {
      return;
    }

    LifecycleMappingMetadata meta = new LifecycleMappingMetadata();

    for(Map.Entry<MojoExecutionKey, List<IPluginExecutionMetadata>> entry : this.mappings.get().entrySet()) {
      MojoExecutionKey execution = entry.getKey();
      List<IPluginExecutionMetadata> mappings = entry.getValue();
      if(mappings != null && !mappings.isEmpty()) {
        for(IPluginExecutionMetadata mapping : mappings) {
          PluginExecutionMetadata clone = ((PluginExecutionMetadata) mapping).clone();
          setMappingSource(execution, mappings, clone);
          meta.addPluginExecution(clone);
        }
      } else {
        PluginExecutionFilter filter = new PluginExecutionFilter(execution.groupId(), execution.artifactId(),
            execution.version(), execution.goal());

        PluginExecutionMetadata mapping = new PluginExecutionMetadata();
        mapping.setFilter(filter);

        Xpp3Dom actionDom;
        if(LifecycleMappingFactory.isInterestingPhase(entry.getKey().lifecyclePhase())) {
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
    List<IPluginExecutionMetadata> mappings = this.mappings.get().get(execution);
    if(mappings == null || mappings.isEmpty()) {
      return LifecycleMappingFactory.isInterestingPhase(execution.lifecyclePhase());
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
      return !LifecycleMappingFactory.isInterestingPhase(execution.lifecyclePhase());
    }
    for(IPluginExecutionMetadata mapping : mappings) {
      if(PluginExecutionAction.ignore != mapping.getAction()) {
        return false;
      }
    }
    return true;
  }

  String toString(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings) {
    if(mappings != null && !mappings.isEmpty()) {
      return mappings.stream().map(IPluginExecutionMetadata::getAction).map(PluginExecutionAction::toString).distinct()
          .collect(Collectors.joining(", ")); //$NON-NLS-1$
    }
    if(LifecycleMappingFactory.isInterestingPhase(execution.lifecyclePhase())) {
      return PluginExecutionAction.error.toString();
    }
    return PluginExecutionAction.ignore.toString();
  }

  String getSourcelabel(MojoExecutionKey execution, List<IPluginExecutionMetadata> mappings, boolean detailed) {
    LinkedHashSet<String> sources = new LinkedHashSet<>();
    if(mappings != null && !mappings.isEmpty()) {
      for(IPluginExecutionMetadata mapping : mappings) {
        if(mapping instanceof LifecycleMappingMetadataSource metadata) {
          Object source = metadata.getSource();
          if(source instanceof String s) {
            sources.add(s);
          } else if(source instanceof Artifact artifact) {
            sources.add(getSourceLabel(artifact, detailed));
          } else if(source instanceof MavenProject mavenProject) {
            sources.add(getSourceLabel(mavenProject, detailed));
          } else if(source instanceof Bundle bundle) {
            sources.add(getSourceLabel(bundle, detailed));
          } else {
            sources.add("unknown"); //$NON-NLS-1$
          }
        } else if(mapping instanceof DefaultPluginExecutionMetadata) {
          sources.add("default"); //$NON-NLS-1$
        } else {
          sources.add("unknown"); //$NON-NLS-1$
        }
      }
    } else {
      if(!LifecycleMappingFactory.isInterestingPhase(execution.lifecyclePhase())) {
        sources.add("uninteresting"); //$NON-NLS-1$
      }
    }
    return String.join(", ", sources);
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
    String artifactId = execution.artifactId();
    if(artifactId.endsWith("-maven-plugin")) { //$NON-NLS-1$
      artifactId = artifactId.substring(0, artifactId.length() - "-maven-plugin".length()); //$NON-NLS-1$
    } else if(artifactId.startsWith("maven-") && artifactId.endsWith("-plugin")) { //$NON-NLS-1$ //$NON-NLS-2$
      artifactId = artifactId.substring("maven-".length(), artifactId.length() - "-plugin".length()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    sb.append(artifactId).append(':').append(execution.goal());

    // only show execution id if necessary
    int count = 0;
    for(MojoExecutionKey other : mappings.get().keySet()) {
      if(eq(execution.groupId(), other.groupId()) && eq(execution.artifactId(), other.artifactId())
          && eq(execution.goal(), other.goal())) {
        count++ ;
      }
    }
    if(count > 1) {
      sb.append(" (").append(execution.executionId()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return sb.toString();
  }

  static <T> boolean eq(T a, T b) {
    return a != null ? a.equals(b) : b == null;
  }

  public void setTarget(final IProject project) {
    if(this.project == project) {
      return;
    }
    this.project = project;

    phases.set(Collections.emptyMap());
    mappings.set(Collections.emptyMap());
    updateMappingsTreeViewer();
    if(project != null) {
      Job job = Job.create("Compute Mappings for project " + project.getName(), monitor -> {
        final IMavenProjectRegistry projectRegistry = MavenPlugin.getMavenProjectRegistry();
        final IMavenProjectFacade facade = projectRegistry.getProject(project);
        if(facade == null) {
          return;
        }
        projectRegistry.execute(facade, (context, monitor1) -> {
          MavenProject mavenProject = facade.getMavenProject(monitor1);
          List<MojoExecution> mojoExecutions = ((MavenProjectFacade) facade).getMojoExecutions(monitor1);
          LifecycleMappingResult mappingResult = LifecycleMappingFactory.calculateLifecycleMapping(mavenProject,
              mojoExecutions, facade.getConfiguration().getLifecycleMappingId(), monitor1);
          Map<MojoExecutionKey, List<IPluginExecutionMetadata>> result = mappingResult.getMojoExecutionMapping();
          Map<String, List<MojoExecutionKey>> phases = new LinkedHashMap<>();
          if(result != null) {
            for(MojoExecutionKey execution : result.keySet()) {
              List<MojoExecutionKey> executions = phases.get(execution.lifecyclePhase());
              if(executions == null) {
                executions = new ArrayList<>();
                phases.put(execution.lifecyclePhase(), executions);
              }
              executions.add(execution);
            }
            this.phases.set(phases);
            this.mappings.set(result);
          }
          Display.getDefault().asyncExec(this::updateMappingsTreeViewer);
          return null;
        }, monitor);
      });
      job.schedule();
    }
  }

  /**
   * @param shell
   */
  public void setShell(Shell shell) {
    this.shell = shell;
  }

}
