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
 *	 Patrick Ziegler - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.pde.ui.target.preferences;

import org.codehaus.mojo.versions.model.IgnoreVersion;
import org.codehaus.mojo.versions.model.Rule;
import org.codehaus.mojo.versions.model.RuleSet;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * Read-only viewer for showing the ignored versions and the rules for a given
 * rule-set. The viewers contained by this class are updated by the
 * {@link #setInput(RuleSet)} method.
 */
public class RuleSetViewer {
	private static final Object[] NO_CHILDREN = new Object[0];
	private RulesetTreeViewer ignoreVersionsViewer;
	private RulesetTreeViewer rulesViewer;

	public RuleSetViewer(Composite parent) {
		final TabFolder tabFolder = new TabFolder(parent, SWT.NONE);
		tabFolder.setLayout(new FillLayout());
		createIgnoreVersionContents(tabFolder);
		createRuleContents(tabFolder);
		final TabItem tabItem1 = new TabItem(tabFolder, SWT.NONE);
		tabItem1.setText(Messages.RuleSetViewer_GlobalRules);
		tabItem1.setControl(ignoreVersionsViewer);
		final TabItem tabItem2 = new TabItem(tabFolder, SWT.NONE);
		tabItem2.setText(Messages.RuleSetViewer_ArtifactRules);
		tabItem2.setControl(rulesViewer);
		tabFolder.setSelection(0);
	}

	public void setInput(RuleSet ruleSet) {
		ignoreVersionsViewer.getViewer().setInput(ruleSet.getIgnoreVersions());
		rulesViewer.getViewer().setInput(ruleSet.getRules());
	}

	private void createIgnoreVersionContents(Composite parent) {
		ignoreVersionsViewer = new RulesetTreeViewer(parent, true);
		ignoreVersionsViewer.getViewer().getTree().setHeaderVisible(true);
		ignoreVersionsViewer.getViewer().setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return ArrayContentProvider.getInstance().getElements(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				return NO_CHILDREN;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
			
		});
		TreeViewerColumn viewerColumn1 = new TreeViewerColumn(ignoreVersionsViewer.getViewer(), SWT.NONE);
		viewerColumn1.getColumn().setText(Messages.RuleSetViewer_Type);
		viewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				return ((IgnoreVersion) o).getType();
			}
		});
		TreeViewerColumn viewerColumn2 = new TreeViewerColumn(ignoreVersionsViewer.getViewer(), SWT.NONE);
		viewerColumn2.getColumn().setText(Messages.RuleSetViewer_Value);
		viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				return ((IgnoreVersion) o).getVersion();
			}
		});

		ignoreVersionsViewer.getTreeLayout().setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(20));
		ignoreVersionsViewer.getTreeLayout().setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(80));
	}

	private void createRuleContents(Composite parent) {
		rulesViewer = new RulesetTreeViewer(parent, false);
		rulesViewer.getViewer().getTree().setHeaderVisible(true);
		rulesViewer.getViewer().setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return ArrayContentProvider.getInstance().getElements(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (hasChildren(parentElement)) {
					return ((Rule) parentElement).getIgnoreVersions().toArray();
				}
				return NO_CHILDREN;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return element instanceof Rule rule && rule.getIgnoreVersions().size() > 1;
			}

		});

		TreeViewerColumn viewerColumn1 = new TreeViewerColumn(rulesViewer.getViewer(), SWT.NONE);
		viewerColumn1.getColumn().setText(Messages.RuleSetViewer_Artifact);
		viewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				if (o instanceof Rule rule) {
					return rule.getGroupId() + ':' + rule.getArtifactId();
				}
				return null;
			}
		});
		TreeViewerColumn viewerColumn2 = new TreeViewerColumn(rulesViewer.getViewer(), SWT.NONE);
		viewerColumn2.getColumn().setText(Messages.RuleSetViewer_Type);
		viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				if (o instanceof Rule rule && rule.getIgnoreVersions().size() == 1) {
					return rule.getIgnoreVersions().get(0).getType();
				}
				if (o instanceof IgnoreVersion version) {
					return version.getType();
				}
				return null;
			}
		});
		TreeViewerColumn viewerColumn3 = new TreeViewerColumn(rulesViewer.getViewer(), SWT.NONE);
		viewerColumn3.getColumn().setText(Messages.RuleSetViewer_Value);
		viewerColumn3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				if (o instanceof Rule rule && rule.getIgnoreVersions().size() == 1) {
					return rule.getIgnoreVersions().get(0).getVersion();
				}
				if (o instanceof IgnoreVersion version) {
					return version.getVersion();
				}
				return null;
			}
		});

		rulesViewer.getTreeLayout().setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(60));
		rulesViewer.getTreeLayout().setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(20));
		rulesViewer.getTreeLayout().setColumnData(viewerColumn3.getColumn(), new ColumnWeightData(20));
	}
	
	private static class RulesetTreeViewer extends FilteredTree {
		private final TreeColumnLayout layout;
		
		public RulesetTreeViewer(Composite parent, boolean useHashLookup) {
			super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.SINGLE, new PatternFilter(), true, useHashLookup);
			this.layout = new TreeColumnLayout();
			this.treeComposite.setLayout(layout);
		}
		
		public TreeColumnLayout getTreeLayout() {
			return layout;
		}
	}
}
