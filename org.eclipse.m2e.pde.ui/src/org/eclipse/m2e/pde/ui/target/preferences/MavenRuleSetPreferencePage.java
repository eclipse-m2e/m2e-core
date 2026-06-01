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

import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.m2e.pde.target.versions.RuleSetMatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.BorderData;
import org.eclipse.swt.layout.BorderLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Preference page for configuring the rule set used by M2E. The user can specify comparison rules that are used when
 * updating Maven artifacts.
 * 
 * @see <a href="https://www.mojohaus.org/versions/versions-model/rule.html">Rule</a>
 */
public class MavenRuleSetPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	// Preferences have to be stored in "org.eclipse.m2e.pde.target"
	private static final Bundle BUNDLE = FrameworkUtil.getBundle(RuleSetMatcher.class);
	private static final ILog LOG = Platform.getLog(MavenRuleSetPreferencePage.class);
	private RuleSetViewer ruleSetViewer;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, BUNDLE.getSymbolicName()));
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), "org.eclipse.m2e.pde.ui.ruleset"); //$NON-NLS-1$
	}

	@Override
	protected void createFieldEditors() {
		Composite compositeParent = getFieldEditorParent();
		compositeParent.setLayout(new BorderLayout());
		compositeParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite fieldEditorComposite = new Composite(compositeParent, SWT.NONE);
		fieldEditorComposite.setLayoutData(new BorderData(SWT.TOP));
		FileFieldEditor ruleSetFieldEditor = new FileFieldEditor(RuleSetMatcher.P_MAVEN_VERSION_RULESET_FILEPATH,
				Messages.MavenRuleSetPreferencePage_RuleSet, fieldEditorComposite);
		ruleSetFieldEditor.setFileExtensions(new String[] { "*.xml" }); //$NON-NLS-1$
		ruleSetFieldEditor.setEmptyStringAllowed(true);
		ruleSetFieldEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
		addField(ruleSetFieldEditor);

		Composite viewerComposite = new Composite(compositeParent, SWT.NONE);
		viewerComposite.setLayoutData(new BorderData(SWT.CENTER));
		viewerComposite.setLayout(new FillLayout(SWT.VERTICAL));
		ruleSetViewer = new RuleSetViewer(viewerComposite);
		refreshViewer(Path.of(getPreferenceStore().getString(RuleSetMatcher.P_MAVEN_VERSION_RULESET_FILEPATH)));
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (ruleSetViewer != null && FieldEditor.VALUE.equals(event.getProperty())) {
			FieldEditor editor = (FieldEditor) event.getSource();
			if (RuleSetMatcher.P_MAVEN_VERSION_RULESET_FILEPATH.equals(editor.getPreferenceName())) {
				refreshViewer(Path.of((String) event.getNewValue()));
			}
		}
		super.propertyChange(event);
	}

	private void refreshViewer(Path path) {
		if (ruleSetViewer == null) {
			return;
		}

		try {
			RuleSetMatcher ruleSetMatcher = RuleSetMatcher.getMatcherFromPath(path);
			ruleSetViewer.setInput(ruleSetMatcher.getRuleSet());
		} catch (CoreException e) {
			LOG.error(e.getMessage(), e);
			setErrorMessage(e.getLocalizedMessage());
		}
	}
}
