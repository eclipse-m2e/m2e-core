/*******************************************************************************
 * Copyright (c) 2008-2026 Sonatype, Inc. and others.
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

package org.eclipse.m2e.core.ui.internal.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.model.edit.pom.util.NodeOperation;


/**
 * @author mkleint
 */
@SuppressWarnings("restriction")
public class XmlUtils {
  private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);

  public static Element findChild(Element parent, String name) {
    return PomEdits.findChild(parent, name);
  }

  public static List<Element> findChilds(Element parent, String name) {
    return PomEdits.findChilds(parent, name);
  }

  public static String getTextValue(Node element) {
    return PomEdits.getTextValue(element);
  }

  /**
   * finds exactly one (first) occurence of child element with the given name (eg. dependency) that fulfills conditions
   * expressed by the Matchers (eg. groupId/artifactId match)
   *
   * @param parent
   * @param name
   * @param matchers
   * @return
   */
  public static Element findChild(Element parent, String name, PomEdits.Matcher... matchers) {
    return PomEdits.findChild(parent, name, matchers);
  }

  /**
   * converts an InputLocation to a file path on the local disk, null if not available. still the input source's model
   * value can be used further..
   *
   * @param location
   * @return
   */
  public static File fileForInputLocation(InputLocation location, MavenProject origin) {
    InputSource source = location.getSource();
    if(source != null) {
      //MNGECLIPSE-2539 apparently if maven can't resolve the model from local storage,
      //the location will be empty. not only applicable to local repo models but
      //apparently also to models in workspace not reachable by relativePath
      String loc = source.getLocation();
      File file = null;
      if(loc != null) {
        file = new File(loc);
      } else {
        //try to find pom by coordinates..
        String modelId = source.getModelId();
        if(origin.getModel().getId().equals(modelId) && origin.getFile() != null) {
          return origin.getFile();
        }
        String[] splitStrings = modelId.split(":");
        assert splitStrings.length == 3;
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getMavenProject(splitStrings[0],
            splitStrings[1], splitStrings[2]);
        if(facade != null) {
          file = facade.getPomFile();
        } else {
          //if not in the workspace, try looking into the local repository.
          IMaven maven = MavenPlugin.getMaven();
          try {
            String path = maven.getArtifactPath(maven.getLocalRepository(), splitStrings[0], splitStrings[1],
                splitStrings[2], "pom", null);
            if(path != null) {
              file = new File(maven.getLocalRepositoryPath(), path);
            }
          } catch(CoreException e) {
            log.error("Failed to calculate local repository path of artifact", e);
          }
        }
      }
      return file;
    }
    return null;
  }

  /**
   * this method grabs the IDOMModel for the IDocument, performs the passed operation on the root element of the
   * document and then releases the IDOMModel root Element value is also an instance of IndexedRegion
   *
   * @param doc
   * @param operation
   */
  public static void performOnRootElement(IDocument doc, NodeOperation<Element> operation) {
    assert doc != null;
    assert operation != null;
    IDOMModel domModel = null;
    try {
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(doc);
      if(domModel == null) {
        throw new IllegalArgumentException("Document is not structured: " + doc);
      }
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();
      operation.process(root, document);
    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

  public static void performOnRootElement(IFile resource, NodeOperation<Element> operation)
      throws IOException, CoreException {
    performOnRootElement(resource, operation, false);
  }

  public static void performOnRootElement(IFile resource, NodeOperation<Element> operation, boolean autoSave)
      throws IOException, CoreException {
    assert resource != null;
    assert operation != null;
    IDOMModel domModel = null;
    try {
      domModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(resource);
      if(domModel == null) {
        throw new IllegalArgumentException("Document is not structured: " + resource);
      }
      IStructuredDocument document = domModel.getStructuredDocument();
      Element root = domModel.getDocument().getDocumentElement();
      operation.process(root, document);

      if(autoSave && domModel.getReferenceCountForEdit() == 0) {
        domModel.save();
      }

    } finally {
      if(domModel != null) {
        domModel.releaseFromRead();
      }
    }
  }

}
