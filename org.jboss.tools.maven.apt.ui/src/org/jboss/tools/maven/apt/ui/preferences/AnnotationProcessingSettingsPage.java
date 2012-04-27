/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.maven.apt.ui.preferences;

import org.jboss.tools.maven.apt.MavenJdtAptPlugin;
import org.jboss.tools.maven.apt.preferences.AnnotationProcessingMode;
import org.jboss.tools.maven.apt.preferences.IPreferencesManager;
import org.jboss.tools.maven.apt.ui.MavenJdtAptUIPlugin;
import org.jboss.tools.maven.apt.ui.preferences.xpl.PropertyAndPreferencePage;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class AnnotationProcessingSettingsPage extends PropertyAndPreferencePage {

	public static final String PREF_ID= "org.jboss.tools.maven.apt.ui.preferences.AnnotationProcessingPreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID= "org.jboss.tools.maven.apt.ui.propertyPages.AnnotationProcessingPropertyPage"; //$NON-NLS-1$

	private Button disableAptButton;
	private Button useJdtAptButton;
	private Button mavenExecutionButton; 

	private IPreferencesManager preferencesManager; 
	AnnotationProcessingMode annotationProcessingMode;

	public AnnotationProcessingSettingsPage() {
		setPreferenceStore(MavenJdtAptUIPlugin.getDefault().getPreferenceStore());
		setTitle(PreferenceMessages.AnnotationProcessingSettingsPage_Title);
		preferencesManager = MavenJdtAptPlugin.getDefault().getPreferencesManager();
	}
	
	@Override 
	protected Control createPreferenceContent(Composite parent) {
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		layout.marginHeight= 0;
		layout.marginWidth= 0;

		Composite composite= new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayout(layout);

		annotationProcessingMode = preferencesManager.getAnnotationProcessorMode(getProject());

		createModeGroup(composite);
		
		return composite;
	}

	private void createModeGroup(Composite composite) {

        Group modeGroup = new Group(composite, SWT.LEFT);
        GridLayout layout = new GridLayout();
        modeGroup.setLayout(layout);
        GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
        modeGroup.setLayoutData(data);
        modeGroup.setText(PreferenceMessages.AnnotationProcessingSettingsPage_Select_Annotation_Processing_Mode); 

        useJdtAptButton = createRadioButton(modeGroup, 
        									PreferenceMessages.AnnotationProcessingSettingsPage_Jdt_Apt_Mode_Label, 
        									AnnotationProcessingMode.jdt_apt);

        mavenExecutionButton = createRadioButton(modeGroup, 
                               PreferenceMessages.AnnotationProcessingSettingsPage_Maven_Execution_Mode, 
                               AnnotationProcessingMode.maven_execution);

        disableAptButton = createRadioButton(modeGroup, 
                           PreferenceMessages.AnnotationProcessingSettingsPage_Disabled_Mode_Label, 
                           AnnotationProcessingMode.disabled);
        
        resetModeButtons();
	}

	@Override
	protected boolean hasProjectSpecificOptions(IProject project) {
		return preferencesManager.hasSpecificProjectSettings(project); 
	}

	@Override
	protected String getPreferencePageID() {
		return PREF_ID;
	}

	@Override
	protected String getPropertyPageID() {
		return PROP_ID;
	}
	
    protected Button createRadioButton(Composite parent, String label, final AnnotationProcessingMode newMode) {
        Button button = new Button(parent, SWT.RADIO | SWT.LEFT);
        button.setText(label);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
            	annotationProcessingMode = newMode;
            	resetModeButtons();
            }
        });
        return button;
    }

    @Override
    public boolean performOk() {
    	IProject project = getProject();
    	boolean useProjectSettings = useProjectSettings();
    	if (!useProjectSettings) {
    		preferencesManager.clearSpecificSettings(project);
    		project = null;
    	} 
    	preferencesManager.setAnnotationProcessorMode(project, annotationProcessingMode);
    	
    	return super.performOk();
    }
    
	@Override
	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		super.enableProjectSpecificSettings(useProjectSpecificSettings);
		//reload
		if (!useProjectSpecificSettings) {
			annotationProcessingMode = preferencesManager.getAnnotationProcessorMode(null);
		}
		resetModeButtons();
	}
	
	private void resetModeButtons() {
        useJdtAptButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.jdt_apt);
        disableAptButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.disabled);
        mavenExecutionButton.setSelection(annotationProcessingMode == AnnotationProcessingMode.maven_execution);
	}
}
