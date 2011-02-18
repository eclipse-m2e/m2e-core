/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal.editing;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.childEquals;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;

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

  public static final String DEPENDENCIES = "dependencies"; //$NON-NLS-1$

  public static final String GROUP_ID = "groupId";//$NON-NLS-1$

  public static final String ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

  public static final String DEPENDENCY = "dependency"; //$NON-NLS-1$

  public static final String EXCLUSIONS = "exclusions"; //$NON-NLS-1$

  public static final String EXCLUSION = "exclusion"; //$NON-NLS-1$

  public static final String VERSION = "version"; //$NON-NLS-1$

  /*
   * Return the Element matching the dependency or null.
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
}
