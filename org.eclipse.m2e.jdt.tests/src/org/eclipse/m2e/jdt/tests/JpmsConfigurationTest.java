/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.preferences.MavenConfigurationImpl;
import org.eclipse.m2e.jdt.IClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.internal.BuildPathManager;
import org.eclipse.m2e.jdt.internal.ClasspathEntryDescriptor;
import org.eclipse.m2e.jdt.internal.ModuleSupport;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;


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
    File argsSet1FS= new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/argsSet1.xml")).getFile());
    File argsSet2FS = new File(FileLocator.toFileURL(getClass().getResource("/projects/compilerJpmsSettings/argsSet2.xml")).getFile());
    
    IProject project = importProject(pomFileFS.getAbsolutePath());
    waitForJobsToComplete();
    
    IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
    Map<String, String> jreAttributes = getJreContainerAttributes(javaProject);
    Map<String, String> m2eAttributes = getM2eContainerAttributes(javaProject);
    
    assertEquals("11", javaProject.getOption(JavaCore.COMPILER_SOURCE, false));
    
    assertTrue(!jreAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertTrue(!jreAttributes.containsKey(ADD_OPENS_ATTR));
    assertTrue(!jreAttributes.containsKey(ADD_READS_ATTR));
    assertTrue(!jreAttributes.containsKey(PATCH_MODULE_ATTR));
    
    assertTrue(!m2eAttributes.containsKey(ADD_EXPORTS_ATTR));
    assertTrue(!m2eAttributes.containsKey(ADD_OPENS_ATTR));
    assertTrue(!m2eAttributes.containsKey(ADD_READS_ATTR));
    assertTrue(!m2eAttributes.containsKey(PATCH_MODULE_ATTR));
    
    
    
    String contents = read(project, pomFileFS);
    String argsSet1 = read(project, argsSet1FS);
    String argsSet2 = read(project, argsSet2FS);
    
    IFile pomFileWS = project.getFile(pomFileFS.getName());
    
    String argsSet1Content = contents.replace(REPLACED_POM_STRING, argsSet1);
    pomFileWS.setContents(new ByteArrayInputStream(argsSet1Content.getBytes()), true, false, null);
    waitForJobsToComplete();
    
    jreAttributes = getJreContainerAttributes(javaProject);
    m2eAttributes = getM2eContainerAttributes(javaProject);
    
    assertTrue(jreAttributes.get(ADD_EXPORTS_ATTR).equals(JRE_ADD_EXPORTS_VALUE1));
    assertTrue(jreAttributes.get(ADD_OPENS_ATTR).equals(JRE_ADD_OPENS_VALUE1));
    assertTrue(jreAttributes.get(ADD_READS_ATTR).equals(JRE_ADD_READS_VALUE1));
    assertTrue(jreAttributes.get(PATCH_MODULE_ATTR).equals(JRE_PATCH_MODULE_VALUE1));
    
    assertTrue(m2eAttributes.get(ADD_EXPORTS_ATTR).equals(M2E_ADD_EXPORTS_VALUE1));
    assertTrue(m2eAttributes.get(ADD_OPENS_ATTR).equals(M2E_ADD_OPENS_VALUE1));
    assertTrue(m2eAttributes.get(ADD_READS_ATTR).equals(M2E_ADD_READS_VALUE1));
    assertTrue(m2eAttributes.get(PATCH_MODULE_ATTR).equals(M2E_PATCH_MODULE_VALUE1));
    
    String argsSet2Content = contents.replace(REPLACED_POM_STRING, argsSet2);
    pomFileWS.setContents(new ByteArrayInputStream(argsSet2Content.getBytes()), true, false, null);
    waitForJobsToComplete();
    
    jreAttributes = getJreContainerAttributes(javaProject);
    m2eAttributes = getM2eContainerAttributes(javaProject);
    
    assertTrue(jreAttributes.get(ADD_EXPORTS_ATTR).equals(JRE_ADD_EXPORTS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_EXPORTS_VALUE2));
    assertTrue(jreAttributes.get(ADD_OPENS_ATTR).equals(JRE_ADD_OPENS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_OPENS_VALUE2));
    assertTrue(jreAttributes.get(ADD_READS_ATTR).equals(JRE_ADD_READS_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_ADD_READS_VALUE2));
    assertTrue(jreAttributes.get(PATCH_MODULE_ATTR).equals(JRE_PATCH_MODULE_VALUE1 + ATTR_VALUE_SEPARATOR + JRE_PATCH_MODULE_VALUE2));
    
    assertTrue(m2eAttributes.get(ADD_EXPORTS_ATTR).equals(M2E_ADD_EXPORTS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_EXPORTS_VALUE2));
    assertTrue(m2eAttributes.get(ADD_OPENS_ATTR).equals(M2E_ADD_OPENS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_OPENS_VALUE2));
    assertTrue(m2eAttributes.get(ADD_READS_ATTR).equals(M2E_ADD_READS_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_ADD_READS_VALUE2));
    assertTrue(m2eAttributes.get(PATCH_MODULE_ATTR).equals(M2E_PATCH_MODULE_VALUE1 + ATTR_VALUE_SEPARATOR + M2E_PATCH_MODULE_VALUE2));
  }
  
  private static String read(IProject project, File fileFS) throws IOException, CoreException {
    IFile pomFileWS = project.getFile(fileFS.getName());
    byte[] bytes = new byte[(int) fileFS.length()];
    try (InputStream stream = pomFileWS.getContents()) {
      stream.read(bytes);
    }
    return new String(bytes);
  }
  
  private static Map<String, String> getJreContainerAttributes(IJavaProject javaProject) {
    IClasspathEntry jreEntry = BuildPathManager.getJREContainerEntry(javaProject);
    IClasspathEntryDescriptor jreEntryDescriptor = new ClasspathEntryDescriptor(jreEntry);
    return jreEntryDescriptor.getClasspathAttributes();
  }
  
  private static Map<String, String> getM2eContainerAttributes(IJavaProject javaProject) {
    IClasspathEntry m2eEntry = BuildPathManager.getMavenContainerEntry(javaProject);
    IClasspathEntryDescriptor m2eEntryDescriptor = new ClasspathEntryDescriptor(m2eEntry);
    return m2eEntryDescriptor.getClasspathAttributes();
  }
  
  
}
