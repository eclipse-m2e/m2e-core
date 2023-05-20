/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others
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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class BNDInstructions {

	private static final String BND_DEFAULT_PROPERTIES_PATH = "bnd-default.properties";
	public static final BNDInstructions EMPTY = new BNDInstructions("", null);
	private final String key;
	private final String instructions;

	public BNDInstructions(String key, String instructions) {
		this.key = key;
		this.instructions = instructions == null ? null : instructions.strip();
	}

	public String getKey() {
		return key;
	}

	public String getInstructions() {
		return instructions;
	}

	public static BNDInstructions getDefaultInstructions() {
		try (Reader reader = getDefaultInstructionsReader()) {
			return new BNDInstructions("", IOUtils.toString(reader));
		} catch (IOException e) {
			throw new RuntimeException("load default properties failed", e);
		}
	}

	public Properties asProperties() {
		Reader reader;
		if (instructions == null || instructions.isBlank()) {
			reader = getDefaultInstructionsReader();
		} else {
			reader = new StringReader(instructions);
		}
		Properties properties = new Properties();
		try (reader) {
			properties.load(reader);
		} catch (IOException e) {
			throw new RuntimeException("conversion to properties failed", e);
		}
		return properties;
	}

	private static InputStreamReader getDefaultInstructionsReader() {
		return new InputStreamReader(MavenTargetLocation.class.getResourceAsStream(BND_DEFAULT_PROPERTIES_PATH),
				StandardCharsets.ISO_8859_1);
	}

	public boolean isEmpty() {
		return instructions == null || instructions.isBlank();
	}
}
