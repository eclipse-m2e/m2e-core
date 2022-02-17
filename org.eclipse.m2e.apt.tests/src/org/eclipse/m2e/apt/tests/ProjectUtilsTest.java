/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat, Inc. - Initial implementation.
 ************************************************************************************/

package org.eclipse.m2e.apt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.m2e.apt.internal.utils.ProjectUtils;
import org.junit.Test;

/**
 * @author Fred Bricon
 *
 */
public class ProjectUtilsTest {

	@Test
	public void testParseCompilerArgs() {
		// @formatter:off
		List<String> compilerArgs = Arrays.asList(	"-Afoo=bar", 
													"-Abracadabra", 
													"Xman", 
													"-A", 
													"-A=",
													"-Akey=space and&#9;tab", 
													"-A bar=foo", 
													"-Atoto =titi");
		// @formatter:on

		Map<String, String> result = ProjectUtils.parseProcessorOptions(compilerArgs);
		assertNotNull(result);
		assertEquals(result.toString(), 3, result.size());
		assertEquals("bar", result.get("foo"));
		assertEquals(null, result.get("bracadabra"));
		assertTrue(result.containsKey("bracadabra"));
		assertEquals("space and&#9;tab", result.get("key"));
	}
}
