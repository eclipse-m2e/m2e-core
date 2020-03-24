/*******************************************************************************
 * Copyright (c) 2020 RedHat and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.m2e.editor.xml.pomeditor;

import org.eclipse.m2e.editor.pom.MavenPomEditor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenStructuredPomEditorPageFactory extends org.eclipse.m2e.editor.pom.MavenPomEditorPageFactory {

	private static final Logger log = LoggerFactory.getLogger(MavenStructuredPomEditorPageFactory.class);

	@Override
	public void addPages(MavenPomEditor pomEditor) {

		ITextEditor sourcePage = new MavenStructuredSourceTextEditor(pomEditor);
		ITextEditor effectPomPage = new MavenStructuredSourceTextEditor(pomEditor);

		try {
			int dex = pomEditor.addPage(effectPomPage, pomEditor.getEffectivePomEditorInput());
			pomEditor.setPageText(dex, MavenPomEditor.EFFECTIVE_POM);
			pomEditor.setEffectivePomSourcePage(effectPomPage);

			int sourcePageIndex = pomEditor.addPage(sourcePage, pomEditor.getEditorInput());
			pomEditor.setPageText(sourcePageIndex, MavenPomEditor.POM_XML);
			pomEditor.setSourcePage(sourcePage);

		} catch (PartInitException ex) {
			log.error(ex.getMessage(), ex);
		}
	}

}
