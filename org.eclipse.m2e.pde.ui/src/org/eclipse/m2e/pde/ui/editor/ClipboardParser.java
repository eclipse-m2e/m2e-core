/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.m2e.pde.MavenTargetDependency;
import org.eclipse.m2e.pde.MavenTargetLocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ClipboardParser {

	private Exception error;

	private List<MavenTargetDependency> dependencies = new ArrayList<>();

	public ClipboardParser(String text) {
		if (text != null && text.trim().startsWith("<")) {
			text = "<dummy>" + text + "</dummy>";
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes("UTF-8"));
				Document doc = builder.parse(input);
				NodeList dependencies = doc.getElementsByTagName("dependency");

				for (int i = 0; i < dependencies.getLength(); i++) {
					Node item = dependencies.item(i);
					if (item instanceof Element) {
						Element element = (Element) item;
						String groupId = getTextFor("groupId", element, "");
						String artifactId = getTextFor("artifactId", element, "");
						String version = getTextFor("version", element, "");
						String classifier = getTextFor("classifier", element, "");
						String type = getTextFor("type", element, MavenTargetLocation.DEFAULT_DEPENDENCY_SCOPE);
						this.dependencies
								.add(new MavenTargetDependency(groupId, artifactId, version, type, classifier));
					}

				}
			} catch (Exception e) {
				// we can't use the clipboard content then...
				this.error = e;
			}
		}
	}

	private String getTextFor(String element, Element doc, String defaultValue) {
		NodeList nl = doc.getElementsByTagName(element);
		Node item = nl.item(0);
		if (item != null) {
			String v = Objects.requireNonNullElse(item.getTextContent(), defaultValue);
			if (!v.isBlank()) {
				return v;
			}
		}
		return defaultValue;
	}

	public Exception getError() {
		return error;
	}

	public List<MavenTargetDependency> getDependencies() {
		return dependencies;
	}

}
