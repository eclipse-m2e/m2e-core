/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.LocalCatalogFactory;
import org.eclipse.m2e.core.ui.internal.archetype.ArchetypeCatalogFactory.RemoteCatalogFactory;


/**
 * Archetype catalogs writer
 *
 * @author Eugene Kuleshov
 */
public class ArchetypeCatalogsWriter {
  private static final Logger log = LoggerFactory.getLogger(ArchetypeCatalogsWriter.class);

  private static final String ELEMENT_CATALOGS = "archetypeCatalogs"; //$NON-NLS-1$

  private static final String ELEMENT_CATALOG = "catalog"; //$NON-NLS-1$

  private static final String ATT_CATALOG_TYPE = "type"; //$NON-NLS-1$

  private static final String ATT_CATALOG_LOCATION = "location"; //$NON-NLS-1$

  public static final String ATT_CATALOG_DESCRIPTION = "description"; //$NON-NLS-1$

  private static final String TYPE_LOCAL = "local"; //$NON-NLS-1$

  private static final String TYPE_REMOTE = "remote"; //$NON-NLS-1$

  private static final String TYPE_SYSTEM = "system"; //$NON-NLS-1$

  public static final String ATT_CATALOG_ID = "id";

  public static final String ATT_CATALOG_ENABLED = "enabled";

  Collection<ArchetypeCatalogFactory> readArchetypeCatalogs(InputStream is,
      Map<String, ArchetypeCatalogFactory> existingCatalogs, ArchetypePlugin plugin) throws IOException {
    Collection<ArchetypeCatalogFactory> catalogs = new ArrayList<>();
    try {
      SAXParserFactory parserFactory = SAXParserFactory.newInstance();
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(is, new ArchetypeCatalogsContentHandler(catalogs, existingCatalogs, plugin));
    } catch(SAXException ex) {
      String msg = Messages.ArchetypeCatalogsWriter_error_parse;
      log.error(msg, ex);
      throw new IOException(NLS.bind(msg, ex.getMessage()));
    } catch(ParserConfigurationException ex) {
      String msg = Messages.ArchetypeCatalogsWriter_error_parse;
      log.error(msg, ex);
      throw new IOException(NLS.bind(msg, ex.getMessage()));
    }
    return catalogs;
  }

  public void writeArchetypeCatalogs(final Collection<ArchetypeCatalogFactory> catalogs, OutputStream os)
      throws IOException {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform(new SAXSource(new XMLArchetypeCatalogsWriter(catalogs), new InputSource()),
          new StreamResult(os));

    } catch(TransformerFactoryConfigurationError ex) {
      throw new IOException(NLS.bind(Messages.ArchetypeCatalogsWriter_error_write, ex.getMessage()));

    } catch(TransformerException ex) {
      throw new IOException(NLS.bind(Messages.ArchetypeCatalogsWriter_error_write, ex.getMessage()));

    }
  }

  static class XMLArchetypeCatalogsWriter extends XMLFilterImpl {

    private final Collection<ArchetypeCatalogFactory> catalogs;

    public XMLArchetypeCatalogsWriter(Collection<ArchetypeCatalogFactory> catalogs) {
      this.catalogs = catalogs;
    }

    @Override
    public void parse(InputSource input) throws SAXException {
      ContentHandler handler = getContentHandler();
      handler.startDocument();
      handler.startElement(null, ELEMENT_CATALOGS, ELEMENT_CATALOGS, new AttributesImpl());

      for(ArchetypeCatalogFactory factory : this.catalogs) {
        AttributesImpl attrs = new AttributesImpl();
        if(factory.isEditable()) {
          if(factory instanceof LocalCatalogFactory) {
            attrs.addAttribute(null, ATT_CATALOG_TYPE, ATT_CATALOG_TYPE, null, TYPE_LOCAL);
            attrs.addAttribute(null, ATT_CATALOG_LOCATION, ATT_CATALOG_LOCATION, null, factory.getId());
            attrs.addAttribute(null, ATT_CATALOG_DESCRIPTION, ATT_CATALOG_DESCRIPTION, null, factory.getDescription());
          } else if(factory instanceof RemoteCatalogFactory) {
            attrs.addAttribute(null, ATT_CATALOG_TYPE, ATT_CATALOG_TYPE, null, TYPE_REMOTE);
            attrs.addAttribute(null, ATT_CATALOG_LOCATION, ATT_CATALOG_LOCATION, null, factory.getId());
            attrs.addAttribute(null, ATT_CATALOG_DESCRIPTION, ATT_CATALOG_DESCRIPTION, null, factory.getDescription());
          }
        } else {
          attrs.addAttribute(null, ATT_CATALOG_TYPE, ATT_CATALOG_TYPE, null, TYPE_SYSTEM);
          attrs.addAttribute(null, ATT_CATALOG_ID, ATT_CATALOG_ID, null, factory.getId());
        }
        attrs.addAttribute(null, ATT_CATALOG_ENABLED, ATT_CATALOG_ENABLED, null, Boolean.toString(factory.isEnabled()));
        handler.startElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG, attrs);
        handler.endElement(null, ELEMENT_CATALOG, ELEMENT_CATALOG);
      }

      handler.endElement(null, ELEMENT_CATALOGS, ELEMENT_CATALOGS);
      handler.endDocument();
    }
  }

  static class ArchetypeCatalogsContentHandler extends DefaultHandler {

    private final Collection<ArchetypeCatalogFactory> catalogs;

    private final Map<String, ArchetypeCatalogFactory> existingCatalogs;

    private ArchetypePlugin plugin;

    public ArchetypeCatalogsContentHandler(Collection<ArchetypeCatalogFactory> catalogs,
        Map<String, ArchetypeCatalogFactory> existingCatalogs, ArchetypePlugin plugin) {
      this.catalogs = catalogs;
      this.plugin = plugin;
      this.existingCatalogs = existingCatalogs == null ? Collections.emptyMap() : existingCatalogs;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
      if(ELEMENT_CATALOG.equals(qName) && attributes != null) {
        String type = attributes.getValue(ATT_CATALOG_TYPE);
        String enabledStr = attributes.getValue(ATT_CATALOG_ENABLED);
        boolean enabled = enabledStr==null||Boolean.parseBoolean(enabledStr);
        if(TYPE_LOCAL.equals(type)) {
          String path = attributes.getValue(ATT_CATALOG_LOCATION);
          if(path != null) {
            String description = attributes.getValue(ATT_CATALOG_DESCRIPTION);
            catalogs.add(plugin.newLocalCatalogFactory(path,
                description, true, enabled));
          }
        } else if(TYPE_REMOTE.equals(type)) {
          String url = attributes.getValue(ATT_CATALOG_LOCATION);
          if(url != null) {
            String description = attributes.getValue(ATT_CATALOG_DESCRIPTION);
            catalogs.add(plugin.newRemoteCatalogFactory(url,
                description, true, enabled));
          }
        } else {
          String id = attributes.getValue(ATT_CATALOG_ID);
          if(id != null && !id.isEmpty()) {
            ArchetypeCatalogFactory catalog = existingCatalogs.get(id);
            if(catalog != null) {
              catalog.setEnabled(enabled);
            }
          }
        }
      }
    }

  }

}
