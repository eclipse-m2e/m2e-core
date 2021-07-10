/*******************************************************************************
 * Copyright (c) 2020, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public record BNDInstructions(String key, String instructions) {

	public static final String DEFAULT_INSTRUCTIONS = """
			Bundle-Name:           Bundle derived from maven artifact ${mvnGroupId}:${mvnArtifactId}:${mvnVersion}
			version:               ${version_cleanup;${mvnVersion}}
			Bundle-SymbolicName:   wrapped.${mvnGroupId}.${mvnArtifactId}
			Bundle-Version:        ${version}
			Import-Package:        *;resolution:=optional
			Export-Package:        *;version="${version}";-noimport:=true
			DynamicImport-Package: *
			""";

	public static final BNDInstructions EMPTY = new BNDInstructions("", null);

	public BNDInstructions(String key, String instructions) {
		this.key = key;
		this.instructions = instructions == null ? null : instructions.strip();
	}

	public static Properties getDefaultInstructionProperties() {
		return asProperties(DEFAULT_INSTRUCTIONS);
	}

	public Properties asProperties() {
		return asProperties(instructions == null || instructions.isBlank() ? DEFAULT_INSTRUCTIONS : instructions);
	}

	private static Properties asProperties(String instructions) {
		Properties properties = new Properties();
		try {
			properties.load(new StringReader(instructions));
		} catch (IOException e) { // Cannot happen
			throw new AssertionError("conversion to properties failed", e);
		}
		return properties;
	}

	public boolean isEmpty() {
		return instructions == null || instructions.isBlank();
	}
}
