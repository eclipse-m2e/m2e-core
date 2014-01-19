/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.jdt.ui.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


/**
 * DownloadSourcesActionDelegate
 * 
 * @author Anton Kraev
 */

@SuppressWarnings("restriction")
public class DownloadSourcesActionDelegate implements IEditorActionDelegate {
  private static final Logger log = LoggerFactory.getLogger(DownloadSourcesActionDelegate.class);

  public void setActiveEditor(IAction action, IEditorPart part) {

    if(part != null) {
      try {
        IClasspathManager buildpathManager = MavenJdtPlugin.getDefault().getBuildpathManager();

        IClassFileEditorInput input = (IClassFileEditorInput) part.getEditorInput();
        IJavaElement element = input.getClassFile();
        while(element.getParent() != null) {
          element = element.getParent();
          if(element instanceof IPackageFragmentRoot) {
            IPackageFragmentRoot root = (IPackageFragmentRoot) element;

            if(root.getSourceAttachmentPath() != null) {
              // do nothing if sources attached already
              break;
            }

            buildpathManager.scheduleDownload(root, true/*sources*/, false/*javadoc*/);
          }
        }
      } catch(Exception ex) {
        log.error("Could not schedule source download", ex); //$NON-NLS-1$
      }
    }
  }

  public void run(IAction action) {
    // no need to do anything
  }

  public void selectionChanged(IAction action, ISelection selection) {
    // no need to do anything
  }

}
