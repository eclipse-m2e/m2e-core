/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.apt.ui.preferences.xpl;

/**
 * Change listener used by <code>DialogField</code>
 */
public interface IDialogFieldListener {

	/**
	 * The dialog field has changed.
	 *
	 * @param field the dialog field that changed
	 */
	void dialogFieldChanged(DialogField field);

}
