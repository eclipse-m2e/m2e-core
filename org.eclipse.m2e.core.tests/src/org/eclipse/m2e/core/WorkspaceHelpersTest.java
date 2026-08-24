/*******************************************************************************
 * Copyright (c) 2026 Tyce Herrman and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.m2e.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.tests.common.WorkspaceHelpers;

@SuppressWarnings("restriction")
public class WorkspaceHelpersTest {

	private static final String MARKER_TYPE = "test.marker";

	private static final String MARKER_MESSAGE = "expected message";

	private static final int MARKER_LINE = 7;

	private static final String RESOURCE_PATH = "pom.xml";

	@Test
	public void testAssertMarkerReturnsStableMarker() throws Exception {
		IMarker marker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		IProject project = mock(IProject.class);
		when(project.findMarkers(null, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] {marker});

		IMarker result = WorkspaceHelpers.assertMarker(MARKER_TYPE, IMarker.SEVERITY_ERROR, MARKER_MESSAGE,
				MARKER_LINE, RESOURCE_PATH, project);

		assertSame(marker, result);
		verify(project).findMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	@Test
	public void testAssertMarkerRetriesWhenMarkerDisappears() throws Exception {
		CoreException staleMarkerException = new CoreException(Status.error("Marker id 1 not found"));
		IMarker staleMarker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		when(staleMarker.getType()).thenThrow(staleMarkerException);
		when(staleMarker.exists()).thenReturn(false);
		IMarker replacementMarker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		IProject project = mock(IProject.class);
		when(project.findMarkers(null, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] {staleMarker},
				new IMarker[] {replacementMarker});

		IMarker result = WorkspaceHelpers.assertMarker(MARKER_TYPE, IMarker.SEVERITY_ERROR, MARKER_MESSAGE,
				MARKER_LINE, RESOURCE_PATH, project);

		assertSame(replacementMarker, result);
		verify(project, times(2)).findMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	@Test
	public void testAssertMarkerDoesNotRetryUnrelatedCoreException() throws Exception {
		CoreException readException = new CoreException(Status.error("Marker read failed"));
		IMarker marker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		when(marker.getType()).thenThrow(readException);
		when(marker.exists()).thenReturn(true);
		IProject project = mock(IProject.class);
		when(project.findMarkers(null, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] {marker});

		try {
			WorkspaceHelpers.assertMarker(MARKER_TYPE, IMarker.SEVERITY_ERROR, MARKER_MESSAGE, MARKER_LINE,
					RESOURCE_PATH, project);
			fail("Expected CoreException");
		} catch(CoreException thrown) {
			assertSame(readException, thrown);
		}
		verify(project).findMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	@Test
	public void testAssertMarkerRetriesOnlyOnce() throws Exception {
		CoreException firstException = new CoreException(Status.error("Marker id 1 not found"));
		IMarker firstMarker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		when(firstMarker.getType()).thenThrow(firstException);
		when(firstMarker.exists()).thenReturn(false);
		CoreException secondException = new CoreException(Status.error("Marker id 2 not found"));
		IMarker secondMarker = marker(MARKER_TYPE, MARKER_MESSAGE, MARKER_LINE);
		when(secondMarker.getType()).thenThrow(secondException);
		when(secondMarker.exists()).thenReturn(false);
		IProject project = mock(IProject.class);
		when(project.findMarkers(null, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] {firstMarker},
				new IMarker[] {secondMarker});

		try {
			WorkspaceHelpers.assertMarker(MARKER_TYPE, IMarker.SEVERITY_ERROR, MARKER_MESSAGE, MARKER_LINE,
					RESOURCE_PATH, project);
			fail("Expected CoreException");
		} catch(CoreException thrown) {
			assertSame(secondException, thrown);
		}
		verify(project, times(2)).findMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	@Test
	public void testAssertMarkerPreservesMissingMarkerDiagnostics() throws Exception {
		IMarker marker = marker("other.marker", "other message", 11);
		IProject project = mock(IProject.class);
		when(project.findMarkers(null, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] {marker});

		AssertionError thrown = null;
		try {
			WorkspaceHelpers.assertMarker(MARKER_TYPE, IMarker.SEVERITY_ERROR, MARKER_MESSAGE, MARKER_LINE,
					RESOURCE_PATH, project);
		} catch(AssertionError ex) {
			thrown = ex;
		}

		assertNotNull("Expected AssertionError", thrown);
		assertTrue(thrown.getMessage(), thrown.getMessage()
				.contains("Expected marker not found. Found markers :Type=other.marker:Message=other message:LineNumber=11"));
		verify(project).findMarkers(null, true, IResource.DEPTH_INFINITE);
	}

	private static IMarker marker(String type, String message, int lineNumber) throws CoreException {
		IMarker marker = mock(IMarker.class);
		when(marker.getType()).thenReturn(type);
		when(marker.getAttribute(IMarker.SEVERITY, 0)).thenReturn(IMarker.SEVERITY_ERROR);
		when(marker.getAttribute(IMarker.MESSAGE, "")).thenReturn(message);
		when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(message);
		when(marker.getAttribute(IMarker.LINE_NUMBER, -1)).thenReturn(lineNumber);
		when(marker.getAttribute(IMarker.LINE_NUMBER)).thenReturn(lineNumber);
		IResource resource = mock(IResource.class);
		when(resource.getProjectRelativePath()).thenReturn(new Path(RESOURCE_PATH));
		when(marker.getResource()).thenReturn(resource);
		when(marker.isSubtypeOf(IMarker.PROBLEM)).thenReturn(true);
		return marker;
	}
}
