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

package org.eclipse.m2e.model.edit.pom.translators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;

import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PomFactory;


public class SSESyncResource extends ResourceImpl {
  private static final Set<String> NO_EVENT_MODELS = new HashSet<String>();

  private IDOMModel domModel;

  private Model pomModel;

  private Document doc;

  public SSESyncResource() {
    super();
  }

  public SSESyncResource(URI uri) {
    super(uri);
  }

  @Override
  public void load(Map<?, ?> options) throws IOException {
    if(isLoaded()) {
      return;
    }
    loadDOMModel();
    setProcessEvents(false);
    try {
      pomModel = PomFactory.eINSTANCE.createModel();
      doc = domModel.getDocument();
      DocumentAdapter da = new DocumentAdapter();
      if(doc.getDocumentElement() != null) {
        createAdapterForRootNode(domModel.getDocument().getDocumentElement()).load();
      } else {
        pomModel.eAdapters().add(da);
      }
      ((IDOMNode) doc).addAdapter(da);
      this.getContents().add(pomModel);
      this.setLoaded(true);
    } finally {
      setProcessEvents(true);
    }

  }

  @Override
  protected void doSave(OutputStream outputStream, Map<?, ?> options) throws IOException {
    try {
      domModel.save(outputStream);
    } catch(CoreException e) {
      IOException ioe = new IOException(e.getMessage());
      ioe.initCause(e);
      throw ioe;
    }
  }

  @Override
  protected void doUnload() {
    domModel.releaseFromEdit();
  }

  private void loadDOMModel() throws IOException {
    IFile ifile = null;
    if(uri.isPlatformResource()) {
      String localPath = uri.toPlatformString(true);
      ifile = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(localPath);
    } else if(uri.isFile()) {
      String filePath = uri.toFileString();
      File f = new File(filePath);
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(f.getAbsoluteFile().toURI());
      if(files.length > 0) {
        ifile = files[0];
      }
    }

    try {
      IModelManager modelManager = StructuredModelManager.getModelManager();
      if(ifile != null && ifile.exists()) {
        domModel = (IDOMModel) modelManager.getExistingModelForEdit(ifile);
      }
      if(null == domModel) {
        if(ifile != null && ifile.exists()) {
          domModel = (IDOMModel) modelManager.getModelForEdit(ifile);
        } else if(uri.isFile()) {
          File f = new File(uri.toFileString());
          FileInputStream is = new FileInputStream(f);
          try {
            domModel = (IDOMModel) modelManager.getModelForEdit(f.getAbsolutePath(), is, null);
          } finally {
            is.close();
          }
        }
        // Had to comment this out, ExtensibleURIConverterImpl isn't available in Eclipse 3.3
//        else {
//          ExtensibleURIConverterImpl converter = new ExtensibleURIConverterImpl();
//          InputStream is = converter.createInputStream(uri);
//          domModel = (IDOMModel) modelManager.getModelForEdit(uri.toString(), is, null);
//          is.close();
//        }
      }
    } catch(CoreException e) {
      // IOException can't wrap another exception before Java 6
      if(e.getCause() != null && e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw new IOException(e.getMessage());
    }
  }

  private ModelObjectAdapter createAdapterForRootNode(Element root) {
    ModelObjectAdapter adapter = new ModelObjectAdapter(this, pomModel, root);
    ((IDOMElement) root).addAdapter(adapter);
    pomModel.eAdapters().add(adapter);
    return adapter;
  }

  boolean isProcessEvents() {
    return !NO_EVENT_MODELS.contains(domModel.getId());
  }

  void setProcessEvents(boolean processEvents) {
    if(processEvents) {
      NO_EVENT_MODELS.remove(domModel.getId());
    } else {
      NO_EVENT_MODELS.add(domModel.getId());
    }
  }

  private class DocumentAdapter implements INodeAdapter, Adapter {
    private Notifier target;

    public boolean isAdapterForType(Object type) {
      return getClass().equals(type);
    }

    public void notifyChanged(Notification notification) {
      if(isProcessEvents()) {
        setProcessEvents(false);
        try {
          int type = notification.getEventType();
          if(Notification.ADD == type || Notification.ADD_MANY == type || Notification.SET == type) {
            if(null == doc.getDocumentElement()) {
              Element newRoot = doc.createElementNS("http://maven.apache.org/POM/4.0.0", //$NON-NLS-1$
                  "project"); //$NON-NLS-1$
              newRoot.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", //$NON-NLS-1$
                  "xsi:schemaLocation", //$NON-NLS-1$
                  "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"); //$NON-NLS-1$

              // I think this is just wrong...but can't find a
              // better way.
              newRoot.setAttribute("xmlns", //$NON-NLS-1$
                  "http://maven.apache.org/POM/4.0.0"); //$NON-NLS-1$
              newRoot.setAttribute("xmlns:xsi", //$NON-NLS-1$
                  "http://www.w3.org/2001/XMLSchema-instance"); //$NON-NLS-1$
              doc.appendChild(newRoot);
              pomModel.setModelVersion("4.0.0"); //$NON-NLS-1$
              createAdapterForRootNode(newRoot).save();
            } else {
              Element root = doc.getDocumentElement();
              createAdapterForRootNode(root).load();
            }
            DocumentAdapter existingDocAdapter = (DocumentAdapter) EcoreUtil.getExistingAdapter(pomModel,
                DocumentAdapter.class);
            if(null != existingDocAdapter) {
              pomModel.eAdapters().remove(existingDocAdapter);
            }
          }
        } finally {
          setProcessEvents(true);
        }
      }

    }

    public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue,
        Object newValue, int pos) {
      if(isProcessEvents()) {
        setProcessEvents(false);
        try {
          if(INodeNotifier.ADD == eventType) {
            if(newValue instanceof Element) {
              Element e = (Element) newValue;
              if(doc.getDocumentElement().equals(e)) {
                DocumentAdapter existingDocAdapter = (DocumentAdapter) EcoreUtil.getExistingAdapter(pomModel,
                    DocumentAdapter.class);
                if(null != existingDocAdapter) {
                  pomModel.eAdapters().remove(existingDocAdapter);
                }
                createAdapterForRootNode(e).load();
              }
            }
          } else if(INodeNotifier.REMOVE == eventType) {
            if(changedFeature instanceof Element) {
              ModelObjectAdapter existing = (ModelObjectAdapter) EcoreUtil.getExistingAdapter(pomModel,
                  ModelObjectAdapter.class);
              if(existing != null) {
                pomModel.eAdapters().remove(existing);
              }

              if(null == doc.getDocumentElement()) {
                for(EStructuralFeature feature : pomModel.eClass().getEStructuralFeatures()) {
                  pomModel.eUnset(feature);
                }
              }

              DocumentAdapter existingDocAdapter = (DocumentAdapter) EcoreUtil.getExistingAdapter(pomModel,
                  DocumentAdapter.class);
              if(null == existingDocAdapter) {
                pomModel.eAdapters().add(this);
              }

            }
          }
        } finally {
          setProcessEvents(true);
        }
      }
    }

    public Notifier getTarget() {
      return target;
    }

    public void setTarget(Notifier newTarget) {
      this.target = newTarget;
    }
  }
}
