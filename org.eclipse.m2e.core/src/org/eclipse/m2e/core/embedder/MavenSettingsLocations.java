/********************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.core.embedder;

import java.io.File;


/**
 * This recored encapsulates the combination of a global and a user settings files as defined in
 * https://maven.apache.org/settings.html
 */
public record MavenSettingsLocations(File globalSettings, File userSettings) {

}
