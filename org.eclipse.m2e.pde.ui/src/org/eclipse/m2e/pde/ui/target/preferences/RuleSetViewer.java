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
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

/**
 * Read-only viewer for showing the ignored versions and the rules for a given
 * rule-set. The viewers contained by this class are updated by the
 * {@link #setInput(RuleSet)} method.
 */
public class RuleSetViewer {
	private static final Object[] NO_CHILDREN = new Object[0];
	private TableViewer ignoreVersionsViewer;
	private TreeViewer rulesViewer;

	public RuleSetViewer(Composite parent) {
		createIgnoreVersionContents(parent);
		createRuleContents(parent);
	}

	public void setInput(RuleSet ruleSet) {
		ignoreVersionsViewer.setInput(ruleSet.getIgnoreVersions());
		rulesViewer.setInput(ruleSet.getRules());
	}

	private Control createIgnoreVersionContents(Composite parent) {
		TableColumnLayout layout = new TableColumnLayout();

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.RuleSetViewer_IgnoredVersions);
		group.setLayout(layout);

		ignoreVersionsViewer = new TableViewer(group);
		ignoreVersionsViewer.setUseHashlookup(true);
		ignoreVersionsViewer.setContentProvider(ArrayContentProvider.getInstance());
		ignoreVersionsViewer.getTable().setHeaderVisible(true);
		TableViewerColumn viewerColumn1 = new TableViewerColumn(ignoreVersionsViewer, SWT.NONE);
		viewerColumn1.getColumn().setText(Messages.RuleSetViewer_Type);
		viewerColumn1.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				return ((IgnoreVersion) o).getType();
			}
		});
		TableViewerColumn viewerColumn2 = new TableViewerColumn(ignoreVersionsViewer, SWT.NONE);
		viewerColumn2.getColumn().setText(Messages.RuleSetViewer_Value);
		viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				return ((IgnoreVersion) o).getVersion();
			}
		});

		layout.setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(20));
		layout.setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(80));

		return group;
	}

	private Control createRuleContents(Composite parent) {
		TreeColumnLayout layout = new TreeColumnLayout();

		Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.RuleSetViewer_Rules);
		group.setLayout(layout);

		rulesViewer = new TreeViewer(group);
		rulesViewer.getTree().setHeaderVisible(true);
		rulesViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				return ArrayContentProvider.getInstance().getElements(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Rule rule) {
					return rule.getIgnoreVersions().toArray();
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

		TreeViewerColumn viewerColumn1 = new TreeViewerColumn(rulesViewer, SWT.NONE);
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
		TreeViewerColumn viewerColumn2 = new TreeViewerColumn(rulesViewer, SWT.NONE);
		viewerColumn2.getColumn().setText(Messages.RuleSetViewer_Type);
		viewerColumn2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				if (o instanceof IgnoreVersion version) {
					return version.getType();
				}
				return null;
			}
		});
		TreeViewerColumn viewerColumn3 = new TreeViewerColumn(rulesViewer, SWT.NONE);
		viewerColumn3.getColumn().setText(Messages.RuleSetViewer_Value);
		viewerColumn3.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object o) {
				if (o instanceof IgnoreVersion version) {
					return version.getVersion();
				}
				return null;
			}
		});

		layout.setColumnData(viewerColumn1.getColumn(), new ColumnWeightData(60));
		layout.setColumnData(viewerColumn2.getColumn(), new ColumnWeightData(20));
		layout.setColumnData(viewerColumn3.getColumn(), new ColumnWeightData(20));

		return group;
	}
}
