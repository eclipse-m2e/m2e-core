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

package org.jboss.tools.maven.apt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.tools.maven.apt.internal.utils.ProjectUtils;
import org.junit.Test;

/**
 * @author Fred Bricon
 *
 */
public class ProjectUtilsTest {

  @Test
  public void testParseCompilerArgs() {
    List<String> compilerArgs = Arrays.asList("-Afoo=bar","-Abracadabra", "Xman");
    
    Map<String, String> result = ProjectUtils.parseProcessorOptions(compilerArgs);
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("bar", result.get("foo"));
    assertEquals(null, result.get("bracadabra"));
    assertTrue(result.containsKey("bracadabra"));
  }
}
