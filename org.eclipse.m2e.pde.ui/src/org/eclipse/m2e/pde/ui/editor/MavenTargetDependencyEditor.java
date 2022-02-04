/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui.editor;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffectFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetSideEffects;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.m2e.pde.MavenTargetDependency;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MavenTargetDependencyEditor {

	private static final String EDITOR_KEY = "MavenTargetDependencyEditor.editor";
	private static final String DEPENDENCY_KEY = "MavenTargetDependencyEditor.dependency";
	private static final ImageDescriptor ADD_IMAGE_DESCRIPTOR = ImageDescriptor
			.createFromURL(MavenTargetLocationWizard.class.getResource("/icons/add_obj.png"));
	private CTabFolder tabFolder;

	public MavenTargetDependencyEditor(Composite parent, Collection<MavenTargetDependency> initialItems) {
		tabFolder = new CTabFolder(parent, SWT.FLAT);
		CTabItem addItem = new CTabItem(tabFolder, SWT.NONE);
		addItem.setToolTipText("Add a new item");
		Image image = ADD_IMAGE_DESCRIPTOR.createImage();
		tabFolder.addDisposeListener(e -> image.dispose());
		addItem.setImage(image);
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {

			@Override
			public void close(CTabFolderEvent event) {
				event.doit = tabFolder.getItemCount() > 2;

			}
		});
		if (initialItems.isEmpty()) {
			addNewItems(parent.getDisplay());
		} else {
			for (MavenTargetDependency dependency : initialItems) {
				add(dependency.copy());
			}
		}
		tabFolder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.item == addItem) {
					addNewItems(e.display);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

	}

	private void addNewItems(Display display) {
		Clipboard clipboard = new Clipboard(display);
		String text = (String) clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();
		ClipboardParser clipboardParser = new ClipboardParser(text);
		List<MavenTargetDependency> dependencies = clipboardParser.getDependencies();
		if (dependencies.isEmpty()) {
			add(new MavenTargetDependency("", "", "", "", ""));
		} else {
			for (MavenTargetDependency mavenTargetDependency : dependencies) {
				add(mavenTargetDependency);
			}
		}
		Exception clipboardError = clipboardParser.getError();
		if (clipboardError != null) {
			Platform.getLog(MavenTargetLocationWizard.class)
					.warn(MessageFormat.format(Messages.ClipboardParser_1, clipboardError.getMessage()));
		}
	}

	private void add(MavenTargetDependency dependency) {
		CTabItem newItem = new CTabItem(tabFolder, SWT.CLOSE);
		newItem.setData(EDITOR_KEY, new DependencyEditor(tabFolder, dependency, newItem));
		newItem.setData(DEPENDENCY_KEY, dependency);
		tabFolder.setSelection(newItem);
	}

	public Control getControl() {
		return tabFolder;
	}

	private static final class DependencyEditor {

		DependencyEditor(Composite parent, MavenTargetDependency dependency, CTabItem item) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			ISideEffectFactory factory = WidgetSideEffects.createFactory(composite);
			new Label(composite, SWT.NONE).setText(Messages.MavenTargetDependencyEditor_1);
			ISWTObservableValue<String> groupId = WidgetProperties.text(SWT.Modify)
					.observe(fill(new Text(composite, SWT.BORDER)));
			new Label(composite, SWT.NONE).setText(Messages.MavenTargetDependencyEditor_2);
			ISWTObservableValue<String> artifactId = WidgetProperties.text(SWT.Modify)
					.observe(fill(new Text(composite, SWT.BORDER)));
			new Label(composite, SWT.NONE).setText(Messages.MavenTargetDependencyEditor_3);
			ISWTObservableValue<String> version = WidgetProperties.text(SWT.Modify)
					.observe(fill(new Text(composite, SWT.BORDER)));
			new Label(composite, SWT.NONE).setText(Messages.MavenTargetDependencyEditor_4);
			ISWTObservableValue<String> classifier = WidgetProperties.text(SWT.Modify)
					.observe(fill(new Text(composite, SWT.BORDER)));
			new Label(composite, SWT.NONE).setText(Messages.MavenTargetDependencyEditor_5);
			CCombo combo = combo(new CCombo(composite, SWT.BORDER));
			combo.add("jar"); //$NON-NLS-1$
			combo.add("bundle"); //$NON-NLS-1$
			combo.add("pom"); //$NON-NLS-1$
			ISWTObservableValue<String> type = WidgetProperties.ccomboSelection().observe(combo);
			groupId.setValue(dependency.getGroupId());
			artifactId.setValue(dependency.getArtifactId());
			version.setValue(dependency.getVersion());
			classifier.setValue(dependency.getClassifier());
			type.setValue(dependency.getType());
			factory.create(() -> {
				dependency.setArtifactId(artifactId.getValue());
				dependency.setGroupId(groupId.getValue());
				dependency.setVersion(version.getValue());
				dependency.setClassifier(classifier.getValue());
				dependency.setType(type.getValue());
				String key = dependency.getKey();
				if (key.equals("::jar:")) {
					item.setText(Messages.MavenTargetDependencyEditor_6);
				} else {
					item.setText(key);
				}
			});
			item.setControl(composite);
			parent.getDisplay().asyncExec(() -> {
				((Control) groupId.getWidget()).forceFocus();
			});
		}

	}

	private static Text fill(Text text) {
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		text.setLayoutData(data);
		return text;
	}

	private static CCombo combo(CCombo combo) {
		GridData data = new GridData();
		data.widthHint = 100;
		combo.setLayoutData(data);
		return combo;
	}

	public Collection<MavenTargetDependency> getRoots() {
		return Arrays.stream(tabFolder.getItems()).map(item -> item.getData(DEPENDENCY_KEY)).filter(Objects::nonNull)
				.map(MavenTargetDependency.class::cast).collect(Collectors.toList());
	}

	public void setSelected(MavenTargetDependency selected) {
		if (selected == null) {
			tabFolder.setSelection(1);
			return;
		}
		for (CTabItem item : tabFolder.getItems()) {
			Object data = item.getData(DEPENDENCY_KEY);
			if (data != null && selected.matches((Dependency) data)) {
				tabFolder.setSelection(item);
				return;
			}
		}
	}
}
