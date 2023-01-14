/*******************************************************************************
 * Copyright (c) 2011-2023 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.sourcelookup.ui.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.sourcelookup.advanced.AdvancedSourceLookup;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.m2e.core.internal.MetaInfMavenScanner;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.actions.StaticMavenStorageEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

@SuppressWarnings("restriction")
public class OpenPomCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return null;
		}

		try {
			final File location = AdvancedSourceLookup
					.getClassesLocation(((IStructuredSelection) selection).getFirstElement());

			if (location == null) {
				return null;
			}

			final String name = location.getName();

			List<IEditorInput> inputs = new MetaInfMavenScanner<IEditorInput>() {
				@Override
				protected IEditorInput visitFile(Path file) throws IOException {
					try (InputStream stream = Files.newInputStream(file)) {
						return toEditorInput(name, stream);
					}
				}

				@Override
				protected IEditorInput visitJarEntry(JarFile jar, JarEntry entry) throws IOException {
					return toEditorInput(name, jar.getInputStream(entry));
				}

			}.scan(location.toPath(), "pom.xml");

			if (!inputs.isEmpty()) {
				OpenPomAction.openEditor(inputs.get(0), "pom.xml");
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static StaticMavenStorageEditorInput toEditorInput(String name, InputStream is) throws IOException {
		return new StaticMavenStorageEditorInput(name, name, null, is.readAllBytes());
	}
}
