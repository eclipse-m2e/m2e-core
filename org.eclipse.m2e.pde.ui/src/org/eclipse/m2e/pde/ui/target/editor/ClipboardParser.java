/*******************************************************************************
 * Copyright (c) 2020, 2021 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.ui.target.editor;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.pde.target.MavenTargetDependency;
import org.eclipse.m2e.pde.target.MavenTargetLocation;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
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
						parseElement((Element) item);
					}
				}
				if (this.dependencies.isEmpty()) {
					parseElement(doc.getDocumentElement());
				}
			} catch (Exception e) {
				// we can't use the clipboard content then...
				this.error = e;
			}
		}
	}

	private void parseElement(Element element) {
		String groupId = getTextFor("groupId", element, "");
		String artifactId = getTextFor("artifactId", element, "");
		String version = getTextFor("version", element, "");
		String classifier = getTextFor("classifier", element, "");
		String type = getTextFor("type", element, MavenTargetLocation.DEFAULT_PACKAGE_TYPE);
		this.dependencies
				.add(new MavenTargetDependency(groupId, artifactId, version, type, classifier));
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

	/**
	 * Attempts to retrieve all Maven dependencies from the clipboard. The
	 * dependencies are in the normal Maven format. Example:
	 * 
	 * <pre>
	 * <dependency>
	 *     <groupId>org.eclipse.jdt</groupId>
	 *     <artifactId>org.eclipse.jdt.annotation</artifactId>
	 *     <version>2.2.700</version>
	 * </dependency>
	 * </pre>
	 * 
	 * The clipboard may contain one or more of those entries. On an ill-formed
	 * content, an exception is logged and an empty list returned.
	 * 
	 * @param display The display on which to allocate the clipboard
	 * @return All dependencies which are stored in the clipboard. May be empty.
	 */
	public static List<MavenTargetDependency> getClipboardDependencies(Display display) {
		String text = getClipboardContent(display);

		ClipboardParser clipboardParser = new ClipboardParser(text);

		try {
			return clipboardParser.getDependencies();
		} finally {
			Exception clipboardError = clipboardParser.getError();

			if (clipboardError != null) {
				Platform.getLog(MavenTargetLocationWizard.class)
						.warn(MessageFormat.format(Messages.ClipboardParser_1, clipboardError.getMessage()));
			}
		}
	}

	private static String getClipboardContent(Display display) {
		Clipboard clipboard = new Clipboard(display);

		try {
			clipboard = new Clipboard(display);
			return (String) clipboard.getContents(TextTransfer.getInstance());
		} finally {
			clipboard.dispose();
		}
	}
}
