/*******************************************************************************
 * Copyright (c) 2018, 2021 Christoph Läubrich
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
package org.eclipse.m2e.pde;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenTargetLocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder
					.parse(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			Element location = document.getDocumentElement();
			MissingMetadataMode mode;
			try {
				mode = MissingMetadataMode
						.valueOf(location.getAttribute(MavenTargetLocation.ATTRIBUTE_MISSING_META_DATA).toUpperCase());
			} catch (IllegalArgumentException e) {
				// fall back to safe default
				mode = MissingMetadataMode.ERROR;
			}
			String dependencyScope = location.getAttribute(MavenTargetLocation.ATTRIBUTE_DEPENDENCY_SCOPE);
			String artifactId = getText(MavenTargetLocation.ELEMENT_ARTIFACT_ID, location);
			String groupId = getText(MavenTargetLocation.ELEMENT_GROUP_ID, location);
			String version = getText(MavenTargetLocation.ELEMENT_VERSION, location);
			String artifactType = getText(MavenTargetLocation.ELEMENT_TYPE, location);
			String classifier = getText(MavenTargetLocation.ELEMENT_CLASSIFIER, location);
			NodeList instructionsNodeList = location.getElementsByTagName(MavenTargetLocation.ELEMENT_INSTRUCTIONS);
			NodeList excludesNodeList = location.getElementsByTagName(MavenTargetLocation.ELEMENT_EXCLUDED);
			List<BNDInstructions> instructions = new ArrayList<>();
			List<String> excludes = new ArrayList<>();
			int instructionsLength = instructionsNodeList.getLength();
			for (int i = 0; i < instructionsLength; i++) {
				Node item = instructionsNodeList.item(i);
				if (item instanceof Element) {
					Element instructionElement = (Element) item;
					instructions.add(new BNDInstructions(
							instructionElement.getAttribute(MavenTargetLocation.ATTRIBUTE_INSTRUCTIONS_REFERENCE),
							instructionElement.getTextContent()));
				}
			}
			int excludesLength = excludesNodeList.getLength();
			for (int i = 0; i < excludesLength; i++) {
				Node item = excludesNodeList.item(i);
				if (item instanceof Element) {
					excludes.add(((Element) item).getTextContent());
				}
			}
			return new MavenTargetLocation(groupId, artifactId, version, artifactType, classifier, mode,
					dependencyScope, Boolean.parseBoolean(location.getAttribute(MavenTargetLocation.ATTRIBUTE_INCLUDE_SOURCE)),
					instructions,
					excludes);
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, MavenTargetLocationFactory.class.getPackage().getName(),
					e.getMessage(), e));
		}

	}

	private String getText(String tagName, Element location) {
		NodeList nodeList = location.getElementsByTagName(tagName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			String textContent = nodeList.item(i).getTextContent();
			if (textContent != null) {
				return textContent;
			}
		}
		return "";
	}

}
