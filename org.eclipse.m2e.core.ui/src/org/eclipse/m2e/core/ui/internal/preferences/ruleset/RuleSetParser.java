/********************************************************************************
 * Copyright (c) 2026 Patrick Ziegler and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Ziegler - initial API and implementation
 ********************************************************************************/
package org.eclipse.m2e.core.ui.internal.preferences.ruleset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.osgi.framework.FrameworkUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.IgnoreVersion;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.Rule;
import org.eclipse.m2e.core.ui.internal.preferences.ruleset.model.RuleSet;

/**
 * Simple parser for converting the XML rule set into the Java model and vice versa.
 */
public final class RuleSetParser {
  public static final String TYPE_EXACT = "exact"; //$NON-NLS-1$

  public static final String TYPE_REGEX = "regex"; //$NON-NLS-1$

  public static final String TYPE_RANGE = "range"; //$NON-NLS-1$

  private static final String NS = "https://www.mojohaus.org/VERSIONS/RULE/3.0.0"; //$NON-NLS-1$

  private static final String NODE_RULESET = "ruleset"; //$NON-NLS-1$

  private static final String NODE_IGNORE_VERSIONS = "ignoreVersions"; //$NON-NLS-1$

  private static final String NODE_IGNORE_VERSION = "ignoreVersion"; //$NON-NLS-1$

  private static final String NODE_RULES = "rules"; //$NON-NLS-1$

  private static final String NODE_RULE = "rule"; //$NON-NLS-1$

  private static final String ATTR_GROUPID = "groupId"; //$NON-NLS-1$

  private static final String ATTR_ARTIFACTID = "artifactId"; //$NON-NLS-1$

  private static final String ATTR_TYPE = "type"; //$NON-NLS-1$

  private static Schema SCHEMA;

  /**
   * Creates and returns {@link _RuleSet} instance from the given source string. This method is namespace-aware.
   * 
   * @param source The XML-encoded rule set.
   * @return The {@link _RuleSet} object created from the rule set.
   * @throws CoreException If the source is not a valid XML string or if an internal error occurred.
   */
  public static RuleSet fromXMLString(String source) throws CoreException {
    try {
      Validator validator = getSchema().newValidator();
      validator.validate(new StreamSource(new StringReader(source)));
    } catch(SAXException | IOException e) {
      throw new CoreException(Status.error(e.getMessage(), e));
    }

    try (InputStream is = new ByteArrayInputStream(source.getBytes())) {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(true);
      Document document = documentBuilderFactory.newDocumentBuilder().parse(is);
      Element root = getFirstElement(document.getChildNodes(), NODE_RULESET);

      RuleSet ruleSet = new RuleSet();

      Element ignoreVersionsNode = getFirstElement(root.getChildNodes(), NODE_IGNORE_VERSIONS);
      if(ignoreVersionsNode != null) {
        RuleSet.IgnoreVersions ignoreVersions = new RuleSet.IgnoreVersions();
        NodeList ignoreVersionNodes = ignoreVersionsNode.getElementsByTagNameNS(NS, NODE_IGNORE_VERSION);
        for(int i = 0; i < ignoreVersionNodes.getLength(); ++i) {
          Node ignoreVersionNode = ignoreVersionNodes.item(i);
          if(ignoreVersionNode.getNodeType() == Node.ELEMENT_NODE) {
            ignoreVersions.getIgnoreVersion().add(fromXMLString((Element) ignoreVersionNode, IgnoreVersion.class));
          }
        }
        ruleSet.setIgnoreVersions(ignoreVersions);
      }

      Element rulesNode = getFirstElement(root.getChildNodes(), NODE_RULES);
      if(rulesNode != null) {
        RuleSet.Rules rules = new RuleSet.Rules();
        NodeList rulesNodes = rulesNode.getElementsByTagNameNS(NS, NODE_RULE);
        for(int i = 0; i < rulesNodes.getLength(); ++i) {
          Node ruleNode = rulesNodes.item(i);
          if(ruleNode.getNodeType() == Node.ELEMENT_NODE) {
            rules.getRule().add(fromXMLString((Element) ruleNode, Rule.class));
          }
        }
        ruleSet.setRules(rules);
      }

      return ruleSet;
    } catch(ParserConfigurationException | SAXException | IOException e) {
      throw new CoreException(Status.error(e.getMessage(), e));
    }
  }

  private static <T> T fromXMLString(Element element, Class<T> clazz) {
    if(NODE_IGNORE_VERSION.equals(element.getNodeName())) {
      IgnoreVersion ignoreVersion = new IgnoreVersion();
      ignoreVersion.setType(element.getAttribute(ATTR_TYPE));
      ignoreVersion.setValue(element.getTextContent());
      return clazz.cast(ignoreVersion);
    }
    if(NODE_RULE.equals(element.getNodeName())) {
      Rule rule = new Rule();
      rule.setArtifactId(element.getAttribute(ATTR_ARTIFACTID));
      rule.setGroupId(element.getAttribute(ATTR_GROUPID));

      Element ignoreVersionsNode = getFirstElement(element.getChildNodes(), NODE_IGNORE_VERSIONS);
      if(ignoreVersionsNode != null) {
        Rule.IgnoreVersions ignoreVersions = new Rule.IgnoreVersions();
        NodeList ignoreVersionNodes = ignoreVersionsNode.getElementsByTagNameNS(NS, NODE_IGNORE_VERSION);
        for(int i = 0; i < ignoreVersionNodes.getLength(); ++i) {
          Node ignoreVersionNode = ignoreVersionNodes.item(i);
          if(ignoreVersionNode.getNodeType() == Node.ELEMENT_NODE) {
            ignoreVersions.getIgnoreVersion().add(fromXMLString((Element) ignoreVersionNode, IgnoreVersion.class));
          }
        }
        rule.setIgnoreVersions(ignoreVersions);
      }

      return clazz.cast(rule);
    }
    return clazz.cast(null);
  }

  /**
   * Creates and returns the XML representation for the given rule set. The resulting XML is namespace-aware.
   * 
   * @param source The rule set to encode.
   * @return The XML representation of the rule set.
   * @throws CoreException If an internal error occurred.
   */
  public static String toXMLString(RuleSet source) throws CoreException {
    try (StringWriter writer = new StringWriter()) {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

      Element ruleSet = document.createElementNS(NS, NODE_RULESET);
      ruleSet.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$ //$NON-NLS-2$
      ruleSet.setAttribute("xsi:schemaLocation", //$NON-NLS-1$
          "https://www.mojohaus.org/VERSIONS/RULE/3.0.0 https://www.mojohaus.org/versions/versions-model/xsd/rule-3.0.0.xsd"); //$NON-NLS-1$
      document.appendChild(ruleSet);

      RuleSet.IgnoreVersions ignoreVersions = source.getIgnoreVersions();
      if(ignoreVersions != null && !ignoreVersions.getIgnoreVersion().isEmpty()) {
        Node ignoreVersionsNode = document.createElementNS(NS, NODE_IGNORE_VERSIONS);
        for(IgnoreVersion ignoreVersion : ignoreVersions.getIgnoreVersion()) {
          ignoreVersionsNode.appendChild(toXMLNode(document, ignoreVersion));
        }
        ruleSet.appendChild(ignoreVersionsNode);
      }

      RuleSet.Rules rules = source.getRules();
      if(rules != null && !rules.getRule().isEmpty()) {
        Node rulesNode = document.createElementNS(NS, NODE_RULES);
        for(Rule rule : rules.getRule()) {
          rulesNode.appendChild(toXMLNode(document, rule));
        }
        ruleSet.appendChild(rulesNode);
      }

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      return writer.toString();
    } catch(ParserConfigurationException | TransformerException | IOException e) {
      throw new CoreException(Status.error(e.getMessage(), e));
    }
  }

  private static Node toXMLNode(Document document, Rule model) {
    Element element = document.createElementNS(RuleSetParser.NS, RuleSetParser.NODE_RULE);
    element.setAttribute(ATTR_GROUPID, model.getGroupId());
    element.setAttribute(ATTR_ARTIFACTID, model.getArtifactId());
    Rule.IgnoreVersions ignoreVersions = model.getIgnoreVersions();
    if(ignoreVersions != null && !ignoreVersions.getIgnoreVersion().isEmpty()) {
      Node ignoreVersionsNode = document.createElementNS(RuleSetParser.NS, RuleSetParser.NODE_IGNORE_VERSIONS);
      for(IgnoreVersion ignoreVersion : ignoreVersions.getIgnoreVersion()) {
        ignoreVersionsNode.appendChild(toXMLNode(document, ignoreVersion));
      }
      element.appendChild(ignoreVersionsNode);
    }
    return element;
  }

  private static Node toXMLNode(Document document, IgnoreVersion model) {
    Element element = document.createElementNS(NS, NODE_IGNORE_VERSION);
    element.setAttribute(ATTR_TYPE, model.getType());
    element.setTextContent(model.getValue());
    return element;
  }

  private static Element getFirstElement(NodeList nodeList, String tagName) {
    for(int i = 0; i < nodeList.getLength(); ++i) {
      Node node = nodeList.item(i);
      if(node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      if(!NS.equals(node.getNamespaceURI())) {
        continue;
      }
      if(!tagName.equals(node.getNodeName())) {
        continue;
      }
      return (Element) node;
    }
    return null;
  }

  private static Schema getSchema() throws SAXException {
    if(SCHEMA == null) {
      URL xsdFile = FrameworkUtil.getBundle(RuleSetParser.class).getEntry("xsd/rule-3.0.0.xsd"); //$NON-NLS-1$
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      SCHEMA = factory.newSchema(xsdFile);
    }
    return SCHEMA;
  }
}
