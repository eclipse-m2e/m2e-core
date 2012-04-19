/*************************************************************************************
 * Copyright (c) 2012 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.tools.maven.apt.tests;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.jdt.apt.core.internal.util.FactoryContainer;
import org.eclipse.jdt.apt.core.internal.util.FactoryPath;
import org.eclipse.jdt.apt.core.util.AptConfig;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

@SuppressWarnings("restriction")
public class M2eAptProjectconfiguratorTest extends AbstractMavenProjectTestCase {

	public void testJdtAptSupport() throws Exception {

		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();

		// Import doesn't build, so we trigger it manually
		p.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete();

		IJavaProject javaProject = JavaCore.create(p);
		assertNotNull(javaProject);
		
		assertTrue(AptConfig.isEnabled(javaProject));
        IFolder annotationsFolder = p.getFolder("target/generated-sources/annotations/");
        assertTrue(annotationsFolder  + " was not generated", annotationsFolder.exists());
     
        FactoryPath factoryPath = (FactoryPath) AptConfig.getFactoryPath(javaProject);
        Iterator<FactoryContainer> ite = factoryPath.getEnabledContainers().keySet().iterator();
        FactoryContainer jpaModelGen = ite.next();
        assertEquals(FactoryContainer.FactoryType.VARJAR, jpaModelGen.getType());
        assertEquals("M2_REPO/org/hibernate/hibernate-jpamodelgen/1.1.1.Final/hibernate-jpamodelgen-1.1.1.Final.jar", jpaModelGen.getId());

        FactoryContainer jpaApi = ite.next();
        assertEquals(FactoryContainer.FactoryType.VARJAR, jpaApi.getType());
        assertEquals("M2_REPO/org/hibernate/javax/persistence/hibernate-jpa-2.0-api/1.0.0.Final/hibernate-jpa-2.0-api-1.0.0.Final.jar", jpaApi.getId());

		/*
		There's an ugly bug in Tycho which makes 
		JavaModelManager.getJavaModelManager().createAnnotationProcessorManager() return null
		as a consequence, no annotation processors are run during Tycho builds
		See http://dev.eclipse.org/mhonarc/lists/tycho-user/msg02344.html
		
		For the time being, only APT configuration can be tested, not APT build outcomes
		*/
        if (JavaModelManager.getJavaModelManager().createAnnotationProcessorManager() == null) {
        	return;
        }

        IFile generatedFile = p.getFile("target/generated-sources/annotations/foo/bar/Dummy_.java");
		assertTrue(generatedFile + " was not generated", generatedFile.exists());
		assertNoErrors(p);
	}
	
}
