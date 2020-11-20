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
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;

public class BNDInstructions {

	public static final BNDInstructions EMPTY = new BNDInstructions("", null, null);
	private static final String BND_DEFAULT_PROPERTIES_PATH = "bnd-default.properties";
	private String key;
	private String instructions;
	private Supplier<BNDInstructions> parent;

	public BNDInstructions(String key, String instructions, Supplier<BNDInstructions> parent) {
		this.key = key;
		this.instructions = instructions;
		this.parent = parent;
	}

	public String getKey() {
		return key;
	}

	public BNDInstructions getParent() {
		return parent != null ? parent.get() : null;
	}

	public String getInstructions() {
		return instructions;
	}

	public boolean isEmpty() {
		return instructions == null || instructions.isBlank();
	}

	public BNDInstructions withInstructions(String instructions) {
		return new BNDInstructions(this.key, instructions, this.parent);
	}

	public BNDInstructions withParent(Supplier<BNDInstructions> parent) {
		return new BNDInstructions(this.key, this.instructions, parent);
	}

	public static BNDInstructions getDefaultInstructions() {
		InputStream input = MavenTargetLocation.class.getResourceAsStream(BND_DEFAULT_PROPERTIES_PATH);
		try {
			return new BNDInstructions("", IOUtils.toString(input, StandardCharsets.ISO_8859_1), null);
		} catch (IOException e) {
			throw new RuntimeException("load default properties failed", e);
		}
	}

	public Properties asProperties() {
		Reader reader;
		BNDInstructions parentInstructions = getParent();
		if (isEmpty()) {
			if (parentInstructions != null) {
				return parentInstructions.asProperties();
			}
			reader = new InputStreamReader(MavenTargetLocation.class.getResourceAsStream(BND_DEFAULT_PROPERTIES_PATH),
					StandardCharsets.ISO_8859_1);
		} else {
			reader = new StringReader(instructions);
		}
		Properties properties;
		if (parentInstructions == null) {
			properties = new Properties();
		} else {
			properties = new Properties(parentInstructions.asProperties());
		}
		try {
			properties.load(reader);
		} catch (IOException e) {
			throw new RuntimeException("conversion to properties failed", e);
		}
		return properties;
	}

}
