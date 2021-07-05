/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc.
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

package org.eclipse.m2e.model.edit.pom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.sse.core.internal.provisional.IModelLoader;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelqueryimpl.ModelQueryImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.util.CMDocumentCache;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceTable;
import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;
import org.eclipse.wst.xml.core.internal.modelhandler.XMLModelLoader;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryAdapterFactoryForXML;
import org.eclipse.wst.xml.core.internal.modelquery.XMLModelQueryAssociationProvider;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapter;
import org.eclipse.wst.xml.core.internal.ssemodelquery.ModelQueryAdapterImpl;


@SuppressWarnings("restriction")
public class PomModelHandler extends ModelHandlerForXML {

  private static final String ASSOCIATED_CONTENT_TYPE_ID = "org.eclipse.m2e.pomFile"; //$NON-NLS-1$

  private static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0"; //$NON-NLS-1$

  private static final String POM_XSD = "http://maven.apache.org/xsd/maven-4.0.0.xsd"; //$NON-NLS-1$

  public PomModelHandler() {
    setAssociatedContentTypeId(ASSOCIATED_CONTENT_TYPE_ID);
  }

  @Override
  public IModelLoader getModelLoader() {
    return new PomModelLoader();
  }

  private static class PomModelLoader extends XMLModelLoader {

    @Override
    public List<?> getAdapterFactories() {
      List<Object> result = new ArrayList<>();
      INodeAdapterFactory factory = new ModelQueryAdapterFactoryForPom();
      result.add(factory);
      return result;
    }

  }

  static class ModelQueryAdapterFactoryForPom extends ModelQueryAdapterFactoryForXML {

    protected ModelQueryAdapterImpl modelQueryAdapterImpl;

    @Override
    protected INodeAdapter createAdapter(INodeNotifier target) {
      if(modelQueryAdapterImpl == null) {
        ModelQueryAdapter mqa = (ModelQueryAdapter) super.createAdapter(target);
        modelQueryAdapterImpl = new ModelQueryAdapterImpl(mqa.getCMDocumentCache(),
            new PomModelQueryImpl(mqa.getCMDocumentCache(), mqa.getIdResolver()), mqa.getIdResolver());
      }
      return modelQueryAdapterImpl;
    }

  }

  static class PomModelQueryImpl extends ModelQueryImpl {

    public PomModelQueryImpl(CMDocumentCache cache, URIResolver idResolver) {
      super(new PomModelQueryAssociationProvider(cache, idResolver));
    }

  }

  static class PomModelQueryAssociationProvider extends XMLModelQueryAssociationProvider {

    public PomModelQueryAssociationProvider(CMDocumentCache cache, URIResolver idResolver) {
      super(cache, idResolver);
    }

    @Override
    public CMDocument getCMDocument(String publicId, String systemId, String type) {
      if("".equals(publicId) && "".equals(systemId)) { //$NON-NLS-1$ //$NON-NLS-2$
        return null;
      }

      return super.getCMDocument(publicId, systemId, type);
    }

    @Override
    public CMElementDeclaration getCMElementDeclaration(Element element) {
      CMElementDeclaration result = super.getCMElementDeclaration(element);

      if(result == null) {
        NamespaceTable namespaceTable = new NamespaceTable(element.getOwnerDocument());
        List list = NamespaceTable.getElementLineage(element);
        Element rootElement = (Element) list.get(0);
        namespaceTable.addElement(rootElement);

        documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, false);
        documentManager.addCMDocumentReference(POM_NAMESPACE, POM_XSD, "XSD"); //$NON-NLS-1$
        namespaceTable.addNamespaceInfo("", POM_NAMESPACE, ""); //$NON-NLS-1$ //$NON-NLS-2$

        if(namespaceTable.isNamespaceEncountered()) {
          result = getCMElementDeclaration(element, list, namespaceTable);
        }
      }

      return result;
    }
  }

}
