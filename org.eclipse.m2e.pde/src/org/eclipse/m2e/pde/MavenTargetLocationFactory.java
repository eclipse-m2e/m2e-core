/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MavenTargetLocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder
					.parse(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			Element location = document.getDocumentElement();
			boolean includeDependencies = Boolean.parseBoolean(location.getAttribute("includeDependencies"));
			MissingMetadataMode mode = MissingMetadataMode
					.valueOf(location.getAttribute("missingMetaData").toUpperCase());
			String dependencyScope = location.getAttribute("dependencyScope");
			String artifactId = getText("artifactId", location);
			String groupId = getText("groupId", location);
			String version = getText("version", location);
			String artifactType = getText("type", location);
			return new MavenTargetLocation(groupId, artifactId, version, artifactType, mode, includeDependencies,
					dependencyScope);
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
