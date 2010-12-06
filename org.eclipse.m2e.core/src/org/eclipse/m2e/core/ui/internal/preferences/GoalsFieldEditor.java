/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;


/**
 * A field editor for a combo box that allows the drop-down selection of one of
 * a list of items.
 * 
 * Adapted from org.eclipse.jface.preference.ComboFieldEditor
 */
public class GoalsFieldEditor extends FieldEditor {

	/**
	 * The <code>Combo</code> widget.
	 */
	Text goalsText;
	
	/**
	 * The value (not the name) of the currently selected item in the Combo widget.
	 */
	String value;

  private Button goialsSelectButton;

  private final String buttonText;
	
	/**
   * Create the combo box field editor.
   * 
   * @param name the name of the preference this field editor works on
   * @param labelText the label text of the field editor
	 * @param buttonText 
   * @param entryValues the entry values
   * @param parent the parent composite
   */
	public GoalsFieldEditor(String name, String labelText, String buttonText, Composite parent) {
    init(name, labelText);
    this.buttonText = buttonText;
		createControl(parent);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
	 */
	protected void adjustForNumColumns(int numColumns) {
    if(numColumns > 1) {
      Control control = getLabelControl();
      ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
      ((GridData) goalsText.getLayoutData()).horizontalSpan = numColumns - 1;
    } else {
      Control control = getLabelControl();
      ((GridData) control.getLayoutData()).horizontalSpan = 2;
      ((GridData) goalsText.getLayoutData()).horizontalSpan = 1;
    }
  }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
	 */
	protected void doFillIntoGrid(Composite parent, int numColumns) {
    Control labelControl = getLabelControl(parent);
    GridData gd = new GridData();
    gd.horizontalSpan = numColumns;
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    labelControl.setLayoutData(gd);

    Text goalsText = getTextControl(parent);
    gd = new GridData();
    gd.horizontalSpan = numColumns - 1;
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    goalsText.setLayoutData(gd);
    goalsText.setFont(parent.getFont());
    
    goialsSelectButton = new Button(parent, SWT.NONE);
    goialsSelectButton.setText(buttonText);
    goialsSelectButton.addSelectionListener(new MavenGoalSelectionAdapter(goalsText, parent.getShell()));
    gd = new GridData();
    gd.horizontalSpan = 1;
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    goalsText.setLayoutData(gd);
  }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoad()
	 */
	protected void doLoad() {
		updateComboForValue(getPreferenceStore().getString(getPreferenceName()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
	 */
	protected void doLoadDefault() {
		updateComboForValue(getPreferenceStore().getDefaultString(getPreferenceName()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#doStore()
	 */
	protected void doStore() {
		if (value == null) {
			getPreferenceStore().setToDefault(getPreferenceName());
		} else {
		  getPreferenceStore().setValue(getPreferenceName(), value);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
	 */
	public int getNumberOfControls() {
		return 2;
	}

	/*
	 * Lazily create and return the Combo control.
	 */
	private Text getTextControl(Composite parent) {
		if (goalsText == null) {
			goalsText = new Text(parent, SWT.BORDER);
			goalsText.setFont(parent.getFont());
//			for (int i = 0; i < entryValues.length; i++) {
//				goalsCombo.add(entryValues[i], i);
//			}
//			goalsCombo.addSelectionListener(new SelectionAdapter() {
//				public void widgetSelected(SelectionEvent evt) {
//					String oldValue = value;
//					value = goalsCombo.getText();
//					setPresentsDefaultValue(false);
//					fireValueChanged(VALUE, oldValue, value);					
//				}
//			});
			goalsText.addModifyListener(new ModifyListener() {
			  public void modifyText(ModifyEvent modifyevent) {
			    String oldValue = value;
			    value = goalsText.getText();
			    setPresentsDefaultValue(false);
			    fireValueChanged(VALUE, oldValue, value);					
			  }
			});
		}
		return goalsText;
	}
	
	protected void setPresentsDefaultValue(boolean booleanValue) {
	  super.setPresentsDefaultValue(booleanValue);
	}
	
	protected void fireValueChanged(String property, Object oldValue, Object newValue) {
	  super.fireValueChanged(property, oldValue, newValue);
	}
	
//	/*
//	 * Given the name (label) of an entry, return the corresponding value.
//	 */
//	String getValueForName(String name) {
//		for (int i = 0; i < fEntryValues.length; i++) {
//			String[] entry = fEntryValues[i];
//			if (name.equals(entry[0])) {
//				return entry[1];
//			}
//		}
//		return fEntryValues[0][0];
//	}
	
	/*
	 * Set the name in the combo widget to match the specified value.
	 */
	private void updateComboForValue(String value) {
		this.value = value;
		goalsText.setText(value);
	}
}
