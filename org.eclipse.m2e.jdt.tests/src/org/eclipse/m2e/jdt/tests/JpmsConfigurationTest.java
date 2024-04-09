/*******************************************************************************
 * Copyright (c) 2020 Pascal Treilhes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;


@SuppressWarnings("restriction")
public class JpmsConfigurationTest extends AbstractMavenProjectTestCase {
	
  private static final String REPLACED_POM_STRING = "<!-- compilerArgs: replacedByArgsSets -->";
  
  private static final String ADD_EXPORTS_ATTR = "add-exports";
  private static final String ADD_OPENS_ATTR = "add-opens";
  private static final String ADD_READS_ATTR = "add-reads";
  private static final String PATCH_MODULE_ATTR = "patch-module";
  
  private static final String ATTR_VALUE_SEPARATOR = ":";
  
  private static final String M2E_ADD_EXPORTS_VALUE1 = "javafx.graphics/com.sun.javafx.geometry=somemodule";
  private static final String JRE_ADD_EXPORTS_VALUE1 = "jdk.jartool/sun.tools.jar=somemodule";
  private static final String M2E_ADD_OPENS_VALUE1 = M2E_ADD_EXPORTS_VALUE1;
  private static final String JRE_ADD_OPENS_VALUE1 = JRE_ADD_EXPORTS_VALUE1;
  private static final String M2E_ADD_READS_VALUE1 = "javafx.graphics=somemodule";
  private static final String JRE_ADD_READS_VALUE1 = "jdk.jartool=jdk.javadoc";
  private static final String M2E_PATCH_MODULE_VALUE1 = "javafx.graphics=somepath/some.jar";
  private static final String JRE_PATCH_MODULE_VALUE1 = "jdk.jartool=somepath/some.jar";
  
  private static final String M2E_ADD_EXPORTS_VALUE2 = "javafx.graphics/com.sun.javafx.scene=somemodule";
  private static final String JRE_ADD_EXPORTS_VALUE2 = "jdk.jartool/sun.security.tools.jarsigner=somemodule";
  private static final String M2E_ADD_OPENS_VALUE2 = M2E_ADD_EXPORTS_VALUE2;
  private static final String JRE_ADD_OPENS_VALUE2 = JRE_ADD_EXPORTS_VALUE2;
  private static final String M2E_ADD_READS_VALUE2 = "javafx.graphics=somemodule2";
  private static final String JRE_ADD_READS_VALUE2 = "jdk.jartool=java.security.jgss";
  private static final String M2E_PATCH_MODULE_VALUE2 = "somemodule=somepath/some.jar";
  private static final String JRE_PATCH_MODULE_VALUE2 = "jdk.javadoc=somepath/some.jar";
  
  @Test
  public void testFileChangeUpdatesJPMSSettings() throws CoreException, IOException, InterruptedException {
    ((MavenConfigurationImpl) MavenPlugin.getMavenConfiguration()).setAutomaticallyUpdateConfiguration(true);
    setAutoBuilding(true);
    
    File pomFileFS = new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/pom.xml")).getFile());
    File argsSet1FS= new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/compilerArgsSet1.xml")).getFile());
    File argsSet2FS = new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/compilerArgsSet2.xml")).getFile());
	File argsSet3FS = new File(FileLocator
			.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/compilerArgsSet3.xml")).getFile());
    
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    
    // At start, check all attributes are empty
	IJavaProject javaProject = JavaCore.create(project);
    Map<String, String> jreAttributes = Utils.getJreContainerAttributes(javaProject);
    Map<String, String> m2eAttributes = Utils.getM2eContainerAttributes(javaProject);
    
    assertEquals("11", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
    
    assertFalse(jreAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertFalse(jreAttributes.containsKey(ADD_OPENS_ATTR));
    assertFalse(jreAttributes.containsKey(ADD_READS_ATTR));
    assertFalse(jreAttributes.containsKey(PATCH_MODULE_ATTR));
    
    assertFalse(m2eAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertFalse(m2eAttributes.containsKey(ADD_OPENS_ATTR));
    assertFalse(m2eAttributes.containsKey(ADD_READS_ATTR));
    assertFalse(m2eAttributes.containsKey(PATCH_MODULE_ATTR));
    
    String contents = Utils.read(project, pomFileFS);
    String argsSet1 = Utils.read(project, argsSet1FS);
    String argsSet2 = Utils.read(project, argsSet2FS);
	String argsSet3 = Utils.read(project, argsSet3FS);
    
    IFile pomFileWS = project.getFile(pomFileFS.getName());
    
    // then test attributes are created with one value each
    String argsSet1Content = contents.replace(REPLACED_POM_STRING, argsSet1);
    pomFileWS.setContents(new ByteArrayInputStream(argsSet1Content.getBytes()), true, false, null);
    waitForJobsToComplete();
    
    jreAttributes = Utils.getJreContainerAttributes(javaProject);
    m2eAttributes = Utils.getM2eContainerAttributes(javaProject);
    
    assertEquals(JRE_ADD_EXPORTS_VALUE1, jreAttributes.get(ADD_EXPORTS_ATTR));
    assertEquals(JRE_ADD_OPENS_VALUE1, jreAttributes.get(ADD_OPENS_ATTR));
    assertEquals(JRE_ADD_READS_VALUE1, jreAttributes.get(ADD_READS_ATTR));
    assertEquals(JRE_PATCH_MODULE_VALUE1, jreAttributes.get(PATCH_MODULE_ATTR));
    
    assertEquals(M2E_ADD_EXPORTS_VALUE1, m2eAttributes.get(ADD_EXPORTS_ATTR));
    assertEquals(M2E_ADD_OPENS_VALUE1, m2eAttributes.get(ADD_OPENS_ATTR));
    assertEquals(M2E_ADD_READS_VALUE1, m2eAttributes.get(ADD_READS_ATTR));
    assertEquals(M2E_PATCH_MODULE_VALUE1, m2eAttributes.get(PATCH_MODULE_ATTR));
    
    // then test attributes are updated with two values each
    String argsSet2Content = contents.replace(REPLACED_POM_STRING, argsSet2);
    pomFileWS.setContents(new ByteArrayInputStream(argsSet2Content.getBytes()), true, false, null);
    waitForJobsToComplete();
    
    jreAttributes = Utils.getJreContainerAttributes(javaProject);
    m2eAttributes = Utils.getM2eContainerAttributes(javaProject);
    
    assertEquals(JRE_ADD_EXPORTS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_EXPORTS_VALUE2, jreAttributes.get(ADD_EXPORTS_ATTR));
    assertEquals(JRE_ADD_OPENS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_OPENS_VALUE2, jreAttributes.get(ADD_OPENS_ATTR));
    assertEquals(JRE_ADD_READS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_READS_VALUE2, jreAttributes.get(ADD_READS_ATTR));
    assertEquals(JRE_PATCH_MODULE_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_PATCH_MODULE_VALUE2, jreAttributes.get(PATCH_MODULE_ATTR));
    
    assertEquals(M2E_ADD_EXPORTS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_EXPORTS_VALUE2, m2eAttributes.get(ADD_EXPORTS_ATTR));
    assertEquals(M2E_ADD_OPENS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_OPENS_VALUE2, m2eAttributes.get(ADD_OPENS_ATTR));
    assertEquals(M2E_ADD_READS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_READS_VALUE2, m2eAttributes.get(ADD_READS_ATTR));
    assertEquals(M2E_PATCH_MODULE_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_PATCH_MODULE_VALUE2, m2eAttributes.get(PATCH_MODULE_ATTR));
    
	// then test attributes are created with one value each
	String argsSet3Content = contents.replace(REPLACED_POM_STRING, argsSet3);
	pomFileWS.setContents(new ByteArrayInputStream(argsSet3Content.getBytes()), true, false, null);
	waitForJobsToComplete();

	jreAttributes = Utils.getJreContainerAttributes(javaProject);
	m2eAttributes = Utils.getM2eContainerAttributes(javaProject);

	assertEquals(JRE_ADD_EXPORTS_VALUE1, jreAttributes.get(ADD_EXPORTS_ATTR));
	assertEquals(JRE_ADD_OPENS_VALUE1, jreAttributes.get(ADD_OPENS_ATTR));
	assertEquals(JRE_ADD_READS_VALUE1, jreAttributes.get(ADD_READS_ATTR));
	assertEquals(JRE_PATCH_MODULE_VALUE1, jreAttributes.get(PATCH_MODULE_ATTR));

	assertEquals(M2E_ADD_EXPORTS_VALUE1, m2eAttributes.get(ADD_EXPORTS_ATTR));
	assertEquals(M2E_ADD_OPENS_VALUE1, m2eAttributes.get(ADD_OPENS_ATTR));
	assertEquals(M2E_ADD_READS_VALUE1, m2eAttributes.get(ADD_READS_ATTR));
	assertEquals(M2E_PATCH_MODULE_VALUE1, m2eAttributes.get(PATCH_MODULE_ATTR));

    // then test attributes are removed
    pomFileWS.setContents(new ByteArrayInputStream(contents.getBytes()), true, false, null);
    waitForJobsToComplete();
    
    jreAttributes = Utils.getJreContainerAttributes(javaProject);
    m2eAttributes = Utils.getM2eContainerAttributes(javaProject);
    
    assertFalse(jreAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertFalse(jreAttributes.containsKey(ADD_OPENS_ATTR));
    assertFalse(jreAttributes.containsKey(ADD_READS_ATTR));
    assertFalse(jreAttributes.containsKey(PATCH_MODULE_ATTR));
    
    assertFalse(m2eAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertFalse(m2eAttributes.containsKey(ADD_OPENS_ATTR));
    assertFalse(m2eAttributes.containsKey(ADD_READS_ATTR));
    assertFalse(m2eAttributes.containsKey(PATCH_MODULE_ATTR));
  }
  
  
}
