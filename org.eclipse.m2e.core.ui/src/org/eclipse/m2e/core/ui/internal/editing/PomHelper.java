/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.Operation;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public final class PomHelper {


  private static final Logger LOG = LoggerFactory.getLogger(PomHelper.class);


  /*
   * Return the Element matching the dependency or null,
   * PLEASE NOTE: the dependency values are resolved, while the xml content is not, which makes the method
   * not as reliable as the signature suggests
   */
  public static Element findDependency(Document document, Dependency dependency) {
    Element dependenciesElement = findChild(document.getDocumentElement(), DEPENDENCIES);
    return findChild(dependenciesElement, DEPENDENCY, childEquals(GROUP_ID, dependency.getGroupId()),
        childEquals(ARTIFACT_ID, dependency.getArtifactId()));
  }

  @SuppressWarnings("restriction")
  public static TextFileChange createChange(IFile file, Operation operation, String label) throws CoreException {
    IStructuredModel model = null;
    try {
      model = StructuredModelManager.getModelManager().getModelForRead(file);
      IDocument document = model.getStructuredDocument();
      IStructuredModel tempModel = StructuredModelManager.getModelManager().createUnManagedStructuredModelFor(
          "org.eclipse.m2e.core.pomFile");
      tempModel.getStructuredDocument().setText(StructuredModelManager.getModelManager(), document.get());
      IDocument tempDocument = tempModel.getStructuredDocument();
      performOnDOMDocument(new OperationTuple((IDOMModel) tempModel, operation));

      return new ChangeCreator(file, document, tempDocument, label).createChange();
    } catch(Exception exc) {
      LOG.error("An error occurred creating change", exc);
      throw new CoreException(new Status(IStatus.ERROR, M2EUIPluginActivator.PLUGIN_ID,
          "An error occurred creating change", exc));
    } finally {
      if(model != null) {
        model.releaseFromRead();
      }
    }
  }

  /**
   * creates and adds new plugin to the parent. Formats the result.
   * @param parentList
   * @param groupId null or value
   * @param artifactId never null
   * @param version null or value
   * @return
   */
  public static Element createPlugin(Element parentList, String groupId, String artifactId, String version) {
    Document doc = parentList.getOwnerDocument();
    Element plug = doc.createElement(PLUGIN);
    parentList.appendChild(plug);
    
    if (groupId != null) {
      createElementWithText(plug, GROUP_ID, groupId);
    }
    createElementWithText(plug, ARTIFACT_ID, artifactId);
    if (version != null) {
      createElementWithText(plug, VERSION, version);
    }
    format(plug);
    return plug;
  }

  /**
   * creates and adds new dependency to the parent. formats the result.
   * @param parentList
   * @param groupId null or value
   * @param artifactId never null
   * @param version null or value
   * @return
   */
  public static Element createDependency(Element parentList, String groupId, String artifactId, String version) {
    Element dep = createElement(parentList, DEPENDENCY);
    
    if (groupId != null) {
      createElementWithText(dep, GROUP_ID, groupId);
    }
    createElementWithText(dep, ARTIFACT_ID, artifactId);
    if (version != null) {
      createElementWithText(dep, VERSION, version);
    }
    format(dep);
    return dep;
  }

  /**
   * node is expected to be the node containing <dependencies> node, so <project>, <dependencyManagement> etc..
   * @param node
   * @return
   */
  public static List<Element> findDependencies(Element node) {
    return findChilds(findChild(node, "dependencies"), "dependency");
  }
}
