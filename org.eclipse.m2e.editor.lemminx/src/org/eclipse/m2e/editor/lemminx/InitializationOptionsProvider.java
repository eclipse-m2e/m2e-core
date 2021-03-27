/*******************************************************************************
 * Copyright (c) 2019-2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.editor.lemminx;

import java.util.Map;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;

public class InitializationOptionsProvider implements org.eclipse.wildwebdeveloper.xml.InitializationOptionsProvider {

	@Override
	public Map<String, Object> get() {
		return toLemMinXOptions(MavenPlugin.getMavenConfiguration());
	}

	static Map<String, Object> toLemMinXOptions(IMavenConfiguration config) {
		return Map.of("maven", Map.of(
				"globalSettings", config.getGlobalSettingsFile(), //
				"userSettings", config.getUserSettingsFile()));
	}
}
