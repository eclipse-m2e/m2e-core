/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich
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
package org.eclipse.m2e.pde;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class BNDInstructions {

	private static final String BND_DEFAULT_PROPERTIES_PATH = "bnd-default.properties";
	public static final BNDInstructions EMPTY = new BNDInstructions("", null);
	private String key;
	private String instructions;

	public BNDInstructions(String key, String instructions) {
		this.key = key;
		this.instructions = instructions;
	}

	public String getKey() {
		return key;
	}

	public String getInstructions() {
		return instructions;
	}

	public static BNDInstructions getDefaultInstructions() {
		InputStream input = MavenTargetLocation.class.getResourceAsStream(BND_DEFAULT_PROPERTIES_PATH);
		try {
			return new BNDInstructions("", IOUtils.toString(input, StandardCharsets.ISO_8859_1));
		} catch (IOException e) {
			throw new RuntimeException("load default properties failed", e);
		}
	}

	public Properties asProperties() {
		Reader reader;
		if (instructions == null || instructions.isBlank()) {
			reader = new InputStreamReader(MavenTargetLocation.class.getResourceAsStream(BND_DEFAULT_PROPERTIES_PATH),
					StandardCharsets.ISO_8859_1);
		} else {
			reader = new StringReader(instructions);
		}
		Properties properties = new Properties();
		try {
			properties.load(reader);
		} catch (IOException e) {
			throw new RuntimeException("conversion to properties failed", e);
		}
		return properties;
	}

	public boolean isEmpty() {
		return key.isBlank() && instructions == null;
	}
}
