/*******************************************************************************
 * Copyright (c) 2022, 2023 Hannes Wellmann and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Hannes Wellmann - initial API and implementation
 *      Konrad Windszus - Add tests for required java runtime version implied by enforcer rule 
 *******************************************************************************/

package org.eclipse.m2e.jdt.tests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.jdt.MavenExecutionJre;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@SuppressWarnings("restriction")
public class MavenExecutionJreTest extends AbstractMavenProjectTestCase {

	private static final String DEFAULT_VM = "defaultVM";
	private static final List<String> AVAILABLE_VM_VERSIONS = List.of("17.0.4", "11.0.7", "13.0.5", "11.0.1", "1.8.0");

	@Test
	public void testGetBestMatchingVM_majorOnly() {
		try (var mock = mockJavaRuntime()) {
			assertEquals("11.0.7", MavenExecutionJre.getBestMatchingVM("11").get().getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_rangeWithOnlyMajorLowerBound() {
		try (var mock = mockJavaRuntime()) {
			assertEquals("11.0.7", MavenExecutionJre.getBestMatchingVM("[11,)").get().getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_9versionRange() {
		try (var mock = mockJavaRuntime()) {
			assertEquals("17.0.4", MavenExecutionJre.getBestMatchingVM("[11,18)").get().getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_1XversionRange() {
		try (var mock = mockJavaRuntime()) {
			assertEquals("1.8.0", MavenExecutionJre.getBestMatchingVM("[1.8,9)").get().getId());
		}
	}

	@Test
	public void testGetBestMatchingVM_versionRangeWithNoMajorVersionMatch() {
		try (var mock = mockJavaRuntime()) {
			assertEquals("13.0.5", MavenExecutionJre.getBestMatchingVM("[12,)").get().getId());
		}
	}

	private static MockedStatic<JavaRuntime> mockJavaRuntime() {
		IVMInstall defaultVM = Mockito.mock(IVMInstall.class);
		Mockito.when(defaultVM.getId()).thenReturn(DEFAULT_VM);

		IVMInstallType standardVMType = Mockito.mock(StandardVMType.class, Mockito.CALLS_REAL_METHODS);
		IVMInstall[] installs = AVAILABLE_VM_VERSIONS.stream().map(version -> {
			AbstractVMInstall vm = Mockito.mock(AbstractVMInstall.class, Mockito.CALLS_REAL_METHODS);
			when(vm.getId()).thenReturn(version);
			when(vm.getJavaVersion()).thenReturn(version);
			when(vm.getVMInstallType()).thenReturn(standardVMType);
			when(vm.getName()).thenReturn("JDK " + version);
			return vm;
		}).toArray(IVMInstall[]::new);
		Mockito.doReturn(installs).when(standardVMType).getVMInstalls();

		MockedStatic<JavaRuntime> javaRuntimeMock = Mockito.mockStatic(JavaRuntime.class, Mockito.CALLS_REAL_METHODS);
		javaRuntimeMock.when(JavaRuntime::getVMInstallTypes).thenReturn(new IVMInstallType[] { standardVMType });
		javaRuntimeMock.when(() -> JavaRuntime.computeVMInstall(Mockito.any())).thenReturn(defaultVM);
		return javaRuntimeMock;
	}

}
