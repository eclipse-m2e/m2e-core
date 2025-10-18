/*******************************************************************************
 * Copyright (c) 2024 Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Eclipse Foundation - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for Maven launch shortcuts that re-runs the most recent Maven launch configuration
 * or opens the launch dialog if no previous launch exists.
 */
public class MavenLaunchShortcutHandler extends AbstractHandler implements IExecutableExtension {

	private String goalName;

	public MavenLaunchShortcutHandler() {
		this(null);
	}

	public MavenLaunchShortcutHandler(String goalName) {
		this.goalName = goalName;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if (data instanceof String) {
			this.goalName = (String) data;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String mode = ILaunchManager.RUN_MODE;
		
		// If a specific goal is set, try to find a matching configuration or create one
		if (goalName != null && !goalName.isEmpty()) {
			// Delegate to ExecutePomAction for goal-specific launches
			ExecutePomAction action = new ExecutePomAction();
			try {
				action.setInitializationData(null, null, goalName);
			} catch (Exception e) {
				throw new ExecutionException("Failed to initialize action", e);
			}
			
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			action.launch(selection, mode);
		} else {
			// For general Maven build, try to re-run the last launch or open dialog
			ILaunchConfiguration config = getLastLaunchedMavenConfiguration();
			
			if (config != null) {
				// Re-run the last Maven launch
				DebugUITools.launch(config, mode);
			} else {
				// No previous launch - delegate to ExecutePomAction which will show dialog
				ExecutePomAction action = new ExecutePomAction();
				try {
					action.setInitializationData(null, null, "WITH_DIALOG");
				} catch (Exception e) {
					throw new ExecutionException("Failed to initialize action", e);
				}
				
				ISelection selection = HandlerUtil.getCurrentSelection(event);
				action.launch(selection, mode);
			}
		}
		
		return null;
	}

	/**
	 * Gets the most recently launched Maven configuration
	 */
	private ILaunchConfiguration getLastLaunchedMavenConfiguration() {
		try {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType mavenType = launchManager
					.getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
			
			if (mavenType == null) {
				return null;
			}
			
			// Get the launch history for the run mode
			ILaunchConfiguration[] history = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
			
			// Find the most recent Maven launch configuration
			for (ILaunchConfiguration config : history) {
				if (config.getType().equals(mavenType)) {
					return config;
				}
			}
		} catch (Exception e) {
			// If there's any error, return null and let the action handle it
		}
		
		return null;
	}
}
