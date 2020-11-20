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
package org.eclipse.m2e.pde.ui.editor;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClipboardParser {

	private String groupId;
	private String artifactId;
	private String version;

	public ClipboardParser(Clipboard clipboard) {
		String text = (String) clipboard.getContents(TextTransfer.getInstance());
		if (text != null) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes("UTF-8"));
				Document doc = builder.parse(input);
				groupId = getTextFor("groupId", doc);
				artifactId = getTextFor("artifactId", doc);
				version = getTextFor("version", doc);
			} catch (Exception e) {
				// we can't use the clipboard content then...
			}
		}
	}

	private String getTextFor(String element, Document doc) {
		NodeList nl = doc.getElementsByTagName(element);
		Node item = nl.item(0);
		if (item != null) {
			return item.getTextContent();
		}
		return null;
	}

	public String getGroupId() {
		return Objects.requireNonNullElse(groupId, "");
	}

	public String getArtifactId() {
		return Objects.requireNonNullElse(artifactId, "");
	}

	public String getVersion() {
		return Objects.requireNonNullElse(version, "");
	}
}
