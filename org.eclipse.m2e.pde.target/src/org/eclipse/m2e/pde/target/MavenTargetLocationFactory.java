/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
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
package org.eclipse.m2e.pde.target;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.pde.target.shared.DependencyDepth;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class MavenTargetLocationFactory implements ITargetLocationFactory {
	// For backward compat
	private static final String ATTRIBUTE_DEPENDENCY_SCOPE = "includeDependencyScope";

	@Override
	public MavenTargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		try {
			Element location = parseXMLDocument(serializedXML).getDocumentElement();

			MissingMetadataMode mode = parseMissingMetadataMode(location);
			DependencyDepth dependencyDepth = parseDependencyDepth(location);
			Collection<String> locationScopes = parseDependencyScopes(location);

			List<MavenTargetDependency> dependencies = descendants(location, MavenTargetLocation.ELEMENT_DEPENDENCY)
					.map(MavenTargetLocationFactory::parseDependency).toList();
			if (dependencies.isEmpty()) {
				// backward compatibility for older formats
				dependencies = List.of(parseDependency(location));
			}

			List<MavenTargetRepository> repositories = descendants(location, MavenTargetLocation.ELEMENT_REPOSITORY)
					.map(element -> {
						String id = getText(MavenTargetLocation.ELEMENT_REPOSITORY_ID, element);
						String url = getText(MavenTargetLocation.ELEMENT_REPOSITORY_URL, element);
						return new MavenTargetRepository(id, url);
					}).toList();

			List<BNDInstructions> instructions = descendants(location, MavenTargetLocation.ELEMENT_INSTRUCTIONS)
					.map(element -> {
						String reference = element.getAttribute(MavenTargetLocation.ATTRIBUTE_INSTRUCTIONS_REFERENCE);
						return new BNDInstructions(reference, element.getTextContent());
					}).toList();

			List<String> excludes = descendants(location, MavenTargetLocation.ELEMENT_EXCLUDED)
					.map(Element::getTextContent).toList();

			IFeature templateFeature = descendants(location, MavenTargetLocation.ELEMENT_FEATURE)
					.map(DomXmlFeature::new).findFirst().orElse(null);

			String label = location.getAttribute(MavenTargetLocation.ATTRIBUTE_LABEL);
			boolean includeSource = Boolean
					.parseBoolean(location.getAttribute(MavenTargetLocation.ATTRIBUTE_INCLUDE_SOURCE));
			return new MavenTargetLocation(label, dependencies, repositories, mode, dependencyDepth, locationScopes,
					includeSource, instructions, excludes, templateFeature);
		} catch (Exception e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
	}

	private Collection<String> parseDependencyScopes(Element location) {
		Collection<String> locationScopesSet = new LinkedHashSet<>();
		if (location.hasAttribute(MavenTargetLocation.ATTRIBUTE_DEPENDENCY_SCOPES)) {
			String dependencyScopes = location.getAttribute(MavenTargetLocation.ATTRIBUTE_DEPENDENCY_SCOPES);
			for (String scope : dependencyScopes.split(",")) {
				locationScopesSet.add(scope.strip().toLowerCase(Locale.ENGLISH));
			}
		} else {
			// backward compatibility for older formats
			String dependencyScope = location.getAttribute(ATTRIBUTE_DEPENDENCY_SCOPE);
			locationScopesSet.addAll(MavenTargetDependencyFilter.expandScope(dependencyScope));
		}
		return locationScopesSet;
	}

	private DependencyDepth parseDependencyDepth(Element location) {
		if (location.hasAttribute(MavenTargetLocation.ATTRIBUTE_DEPENDENCY_DEPTH)) {
			return parseEnumAttribute(location, MavenTargetLocation.ATTRIBUTE_DEPENDENCY_DEPTH,
					DependencyDepth::valueOf, DependencyDepth.NONE);
		} else {
			// backward compatibility for older formats
			if (location.getAttribute(ATTRIBUTE_DEPENDENCY_SCOPE).isEmpty()) {
				return DependencyDepth.NONE;
			} else {
				return DependencyDepth.INFINITE;
			}
		}
	}

	private MissingMetadataMode parseMissingMetadataMode(Element location) {
		return parseEnumAttribute(location, MavenTargetLocation.ATTRIBUTE_MISSING_META_DATA,
				MissingMetadataMode::valueOf, MissingMetadataMode.ERROR);
	}

	private static MavenTargetDependency parseDependency(Element element) {
		String artifactId = getText(MavenTargetLocation.ELEMENT_ARTIFACT_ID, element);
		String groupId = getText(MavenTargetLocation.ELEMENT_GROUP_ID, element);
		String version = getText(MavenTargetLocation.ELEMENT_VERSION, element);
		String artifactType = getText(MavenTargetLocation.ELEMENT_TYPE, element);
		String classifier = getText(MavenTargetLocation.ELEMENT_CLASSIFIER, element);
		return new MavenTargetDependency(groupId, artifactId, version, artifactType, classifier);
	}

	private static String getText(String tagName, Element location) {
		return descendants(location, tagName).map(Element::getTextContent).filter(Objects::nonNull)//
				.findFirst().orElse("");
	}

	// --- utility XML parsing/processing methods ---

	public static Document parseXMLDocument(String serializedXML)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		return docBuilder.parse(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
	}

	private static Stream<Element> descendants(Element parent, String name) {
		return elements(parent.getElementsByTagName(name));
	}

	public static Stream<Element> elements(NodeList list) {
		return IntStream.range(0, list.getLength()).mapToObj(list::item) //
				.filter(Element.class::isInstance).map(Element.class::cast);
	}

	private static <T> T parseEnumAttribute(Element location, String attribute, Function<String, T> parser,
			T defaultValue) {
		try {
			return parser.apply(location.getAttribute(attribute).toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			// fall back to safe default
			return defaultValue;
		}
	}

}
