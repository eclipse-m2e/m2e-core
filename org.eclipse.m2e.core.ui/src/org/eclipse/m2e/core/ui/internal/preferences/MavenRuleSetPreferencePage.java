/********************************************************************************
 * Copyright (c) 2026 Patrick Ziegler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Ziegler - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.Rule;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.RuleSetParser;
import org.eclipse.m2e.core.ui.internal.util.M2EUIUtils;

/**
 * Preference page for configuring the rule set used by M2E. The user can specify comparison rules that are used when
 * updating Maven artifacts.
 * 
 * @see <a href="https://www.mojohaus.org/versions/versions-model/rule.html">Rule</a>
 */
public class MavenRuleSetPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private static final ILog LOG = Platform.getLog(MavenRuleSetPreferencePage.class);

  private static final String FILTER_EXTENSION = "*.xml"; //$NON-NLS-1$

  private static final Object[] NO_CHILDREN = new Object[0];

  private static final RuleSet NO_RULESET = new RuleSet();

  private IObservableValue<RuleSet> ruleSetObservable = new WritableValue<>();

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(M2EUIPluginActivator.getDefault().getPreferenceStore());
    try {
      ruleSetObservable.setValue(M2EUIUtils.getCurrentRuleSet());
    } catch(CoreException e) {
      LOG.log(Status.error(e.getMessage(), e));
      setErrorMessage(e.getLocalizedMessage());
      ruleSetObservable.setValue(NO_RULESET);
    }
  }

  @Override
  protected void performDefaults() {
    ruleSetObservable.setValue(NO_RULESET);
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    IPreferenceStore preferenceStore = getPreferenceStore();
    RuleSet ruleSet = ruleSetObservable.getValue();

    RuleSet.IgnoreVersions ignoreVersions = ruleSet.getIgnoreVersions();
    RuleSet.Rules rules = ruleSet.getRules();

    if(ignoreVersions == null && rules == null) {
      preferenceStore.setToDefault(MavenPreferenceConstants.P_MAVEN_VERSION_RULESET);
    } else {
      try {
        preferenceStore.setValue(MavenPreferenceConstants.P_MAVEN_VERSION_RULESET, RuleSetParser.toXMLString(ruleSet));
      } catch(CoreException e) {
        LOG.log(Status.error(e.getMessage(), e));
        setErrorMessage(e.getLocalizedMessage());
      }
    }
    return super.performOk();
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    createImportExportContents(composite).setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
    createIgnoreVersionContents(composite).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    createRuleContents(composite).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    return composite;
  }

  private Control createImportExportContents(Composite parent) {
    Action importAction = new Action(Messages.MavenRuleSetPreferencePage_Import, MavenImages.IMPORT) {
      @Override
      public void run() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFilterExtensions(FILTER_EXTENSION);
        dialog.openDialog().ifPresent(pathString -> {
          try {
            IPath ruleSetPath = Path.fromOSString(pathString);
            String ruleSetString = Files.readString(ruleSetPath.toPath());
            ruleSetObservable.setValue(RuleSetParser.fromXMLString(ruleSetString));
          } catch(CoreException | IOException e) {
            LOG.log(Status.error(e.getMessage(), e));
            setErrorMessage(e.getLocalizedMessage());
          }
        });
      }
    };
    Action exportAction = new Action(Messages.MavenRuleSetPreferencePage_Export, MavenImages.EXPORT) {
      @Override
      public void run() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(FILTER_EXTENSION);
        dialog.openDialog().ifPresent(pathString -> {
          try {
            RuleSet ruleSet = ruleSetObservable.getValue();
            String ruleSetString = RuleSetParser.toXMLString(ruleSet);
            IPath ruleSetPath = Path.fromOSString(pathString);
            Files.writeString(ruleSetPath.toPath(), ruleSetString);
          } catch(CoreException | IOException e) {
            LOG.log(Status.error(e.getMessage(), e));
            setErrorMessage(e.getLocalizedMessage());
          }
        });
      }
    };
    ToolBar toolBar = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
    ToolBarManager toolBarManager = new ToolBarManager(toolBar);
    toolBarManager.add(importAction);
    toolBarManager.add(exportAction);
    toolBarManager.update(true);
    return toolBar;
  }

  private Control createIgnoreVersionContents(Composite parent) {
    TableColumnLayout layout = new TableColumnLayout();

    Group group = new Group(parent, SWT.NONE);
    group.setText(Messages.MavenRuleSetPreferencePage_IgnoredVersions);
    group.setLayout(layout);

    TableViewer viewer = new TableViewer(group);
    viewer.setUseHashlookup(true);
    viewer.setContentProvider(ArrayContentProvider.getInstance());
    viewer.getTable().setHeaderVisible(true);
    TableViewerColumn viewerColumn1 = new TableViewerColumn(viewer, SWT.NONE);
    viewerColumn1.getColumn().setText(Messages.MavenRuleSetPreferencePage_Type);
    viewerColumn1.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object o) {
        return ((IgnoreVersion) o).getType();
      }
    });
    TableViewerColumn viewerColumn2 = new TableViewerColumn(viewer, SWT.NONE);
    viewerColumn2.getColumn().setText(Messages.MavenRuleSetPreferencePage_Value);
    viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object o) {
        return ((IgnoreVersion) o).getValue();
      }
    });

    layout.setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(20));
    layout.setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(80));

    viewer.setInput(getIgnoredVersions());
    ruleSetObservable.addChangeListener(event -> viewer.setInput(getIgnoredVersions()));

    return group;
  }

  private Control createRuleContents(Composite parent) {
    TreeColumnLayout layout = new TreeColumnLayout();

    Group group = new Group(parent, SWT.NONE);
    group.setText(Messages.MavenRuleSetPreferencePage_Rules);
    group.setLayout(layout);

    TreeViewer viewer = new TreeViewer(group);
    viewer.getTree().setHeaderVisible(true);
    viewer.setContentProvider(new ITreeContentProvider() {
      @Override
      public Object[] getElements(Object inputElement) {
        return ArrayContentProvider.getInstance().getElements(inputElement);
      }

      @Override
      public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof Rule rule) {
          return getIgnoredVersions(rule).toArray();
        }
        return NO_CHILDREN;
      }

      @Override
      public Object getParent(Object element) {
        return null;
      }

      @Override
      public boolean hasChildren(Object element) {
        return element instanceof Rule;
      }

    });

    TreeViewerColumn viewerColumn1 = new TreeViewerColumn(viewer, SWT.NONE);
    viewerColumn1.getColumn().setText(Messages.MavenRuleSetPreferencePage_Artifact);
    viewerColumn1.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object o) {
        if(o instanceof Rule rule) {
          return rule.getGroupId() + ':' + rule.getArtifactId();
        }
        return null;
      }
    });
    TreeViewerColumn viewerColumn2 = new TreeViewerColumn(viewer, SWT.NONE);
    viewerColumn2.getColumn().setText(Messages.MavenRuleSetPreferencePage_Type);
    viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object o) {
        if(o instanceof IgnoreVersion version) {
          return version.getType();
        }
        return null;
      }
    });
    TreeViewerColumn viewerColumn3 = new TreeViewerColumn(viewer, SWT.NONE);
    viewerColumn3.getColumn().setText(Messages.MavenRuleSetPreferencePage_Value);
    viewerColumn3.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object o) {
        if(o instanceof IgnoreVersion version) {
          return version.getValue();
        }
        return null;
      }
    });

    layout.setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(60));
    layout.setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(20));
    layout.setColumnData(viewerColumn3.getColumn(), new ColumnWeightData(20));

    viewer.setInput(getRules());
    ruleSetObservable.addChangeListener(event -> viewer.setInput(getRules()));

    return group;
  }

  private List<Rule> getRules() {
    RuleSet.Rules rules = ruleSetObservable.getValue().getRules();
    if(rules == null) {
      return Collections.emptyList();
    }
    return rules.getRule();
  }

  private List<IgnoreVersion> getIgnoredVersions() {
    RuleSet.IgnoreVersions ignoreVersions = ruleSetObservable.getValue().getIgnoreVersions();
    if(ignoreVersions == null) {
      return Collections.emptyList();
    }
    return ignoreVersions.getIgnoreVersion();
  }

  private static List<IgnoreVersion> getIgnoredVersions(Rule rule) {
    Rule.IgnoreVersions ignoreVersions = rule.getIgnoreVersions();
    if(ignoreVersions == null) {
      return Collections.emptyList();
    }
    return ignoreVersions.getIgnoreVersion();
  }
}
