/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *   Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *   Martin Karpisek <martin.karpisek@gmail.com> - Bug 247265
 *   Christoph Läubrich - adjust for m2e-pde usage
 *******************************************************************************/
package org.eclipse.m2e.pde.ui.target.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTableViewer;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.feature.BasePluginListPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

//derived from org.eclipse.pde.internal.ui.wizards.feature.PluginListPage
@SuppressWarnings("restriction")
public class PluginListPage extends BasePluginListPage {

	class PluginContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return getModels();
		}

		private IPluginModelBase[] getModels() {
			return PluginRegistry.getActiveModels();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// If the PDE models are not initialized, initialize with option to cancel
			if (newInput != null && !PDECore.getDefault().areModelsInitialized()) {
				try {
					getContainer().run(true, false, monitor -> {
						// Target reloaded method clears existing models (which don't exist currently)
						// and inits them with a progress monitor
						PDECore.getDefault().getModelManager().targetReloaded(monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		}
	}

	private CheckboxTableViewer pluginViewer;
	private MavenTargetLocation targetLocation;
	private Map<String, String> id2version = new HashMap<>();

	public PluginListPage(MavenTargetLocation targetLocation) {
		super("pluginListPage"); //$NON-NLS-1$
		this.targetLocation = targetLocation;
		setTitle(Messages.NewFeatureWizard_PlugPage_title);
		setDescription(Messages.NewFeatureWizard_PlugPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		GridData gd;

		tablePart.createControl(container, 4, true);
		pluginViewer = tablePart.getTableViewer();
		PluginContentProvider provider = new PluginContentProvider();
		pluginViewer.setContentProvider(provider);
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		gd = (GridData) tablePart.getControl().getLayoutData();
		gd.horizontalIndent = 0;
		gd.heightHint = 250;
		gd.widthHint = 300;
		pluginViewer.setInput(PDECore.getDefault().getModelManager());
		tablePart.setSelection(new Object[0]);
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_FEATURE_REFERENCED_PLUGINS);
		pluginViewer.addDoubleClickListener(event -> {
			TableItem firstTI = pluginViewer.getTable().getSelection()[0];
			tablePart.getTableViewer().setChecked(firstTI.getData(), !firstTI.getChecked());
			tablePart.updateCounterLabel();
		});
		if (targetLocation != null) {
			IFeature featureTemplate = targetLocation.getFeatureTemplate();
			if (featureTemplate != null) {
				Map<String, List<IFeaturePlugin>> map = Arrays.stream(featureTemplate.getPlugins())
						.collect(Collectors.groupingBy(IFeaturePlugin::getId));
				TableItem[] items = pluginViewer.getTable().getItems();
				CachedCheckboxTableViewer treeViewer = tablePart.getTableViewer();
				for (TableItem item : items) {
					IPluginModelBase model = (IPluginModelBase) item.getData();
					String id = model.getPluginBase().getId();
					List<IFeaturePlugin> list = map.getOrDefault(id, Collections.emptyList());
					if (list.size() > 0) {
						treeViewer.setChecked(model, true);
						String definedVersions = list.stream().map(fp -> fp.getVersion())
								.filter(v -> !ICoreConstants.DEFAULT_VERSION.equals(v)).findAny().orElse(null);
						if (definedVersions != null) {
							id2version.put(id, definedVersions);
						}
					}
				}
			}
		}
	}

	// derived from
	// org.eclipse.pde.internal.ui.wizards.feature.CreateFeatureProjectOperation.configureFeature(IFeature,
	// WorkspaceFeatureModel)
	public void update(IFeatureModel featureModel) throws CoreException {
		Object[] selected = tablePart.getTableViewer().getAllCheckedElements();
		IFeaturePlugin[] added = new IFeaturePlugin[selected.length];
		for (int i = 0; i < selected.length; i++) {
			IPluginBase plugin = ((IPluginModelBase) selected[i]).getPluginBase();
			FeaturePlugin fplugin = (FeaturePlugin) featureModel.getFactory().createPlugin();
			fplugin.loadFrom(plugin);
			fplugin.setVersion(id2version.getOrDefault(plugin.getId(), ICoreConstants.DEFAULT_VERSION));
			added[i] = fplugin;
		}
		IFeature feature = featureModel.getFeature();
		feature.removePlugins(feature.getPlugins());
		feature.addPlugins(added);
	}

}
