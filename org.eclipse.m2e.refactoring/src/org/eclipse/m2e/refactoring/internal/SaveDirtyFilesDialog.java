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

package org.eclipse.m2e.refactoring.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;

import org.eclipse.m2e.refactoring.Messages;


/**
 * Taken from org.eclipse.wst.common.ui.internal.dialogs A generic save files dialog. The bulk of the code for this
 * dialog was taken from the JDT refactoring support in org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper.
 * This class is a good candidate for reuse amoung components.
 */
public class SaveDirtyFilesDialog extends ListDialog {
  public static final String ALL_MODIFIED_RESOURCES_MUST_BE_SAVED_BEFORE_THIS_OPERATION = Messages.SaveDirtyFilesDialog_message_not_saved;

  public static boolean saveDirtyFiles(String mask) {
    boolean result = true;
    // TODO (cs) add support for save automatically
    Shell shell = Display.getCurrent().getActiveShell();
    IEditorPart[] dirtyEditors = getDirtyEditors(mask);
    if(dirtyEditors.length > 0) {
      result = false;
      SaveDirtyFilesDialog saveDirtyFilesDialog = new SaveDirtyFilesDialog(shell);
      saveDirtyFilesDialog.setInput(Arrays.asList(dirtyEditors));
      // Save all open editors.
      if(saveDirtyFilesDialog.open() == Window.OK) {
        result = true;
        int numDirtyEditors = dirtyEditors.length;
        for(int i = 0; i < numDirtyEditors; i++ ) {
          dirtyEditors[i].doSave(null);
        }
      } else {
        MessageDialog dlg = new MessageDialog(shell, Messages.SaveDirtyFilesDialog_title_error, null,
            ALL_MODIFIED_RESOURCES_MUST_BE_SAVED_BEFORE_THIS_OPERATION, MessageDialog.ERROR,
            new String[] {IDialogConstants.OK_LABEL}, 0);
        dlg.open();
      }
    }
    return result;
  }

  private static IEditorPart[] getDirtyEditors(String mask) {
    List<IEditorPart> result = new ArrayList<>(0);
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
    for(IWorkbenchWindow window : windows) {
      IWorkbenchPage[] pages = window.getPages();
      for(IWorkbenchPage page : pages) {
        IEditorPart[] editors = page.getDirtyEditors();
        for(IEditorPart ep : editors) {
          if(ep.getTitle().indexOf(mask) > 0) {
            result.add(ep);
          }
        }
      }
    }
    return result.toArray(new IEditorPart[result.size()]);
  }

  public SaveDirtyFilesDialog(Shell parent) {
    super(parent);
    setTitle(Messages.SaveDirtyFilesDialog_title);
    setAddCancelButton(true);
    setLabelProvider(createDialogLabelProvider());
    setMessage(ALL_MODIFIED_RESOURCES_MUST_BE_SAVED_BEFORE_THIS_OPERATION);
    setContentProvider(new ListContentProvider());
  }

  @Override
  protected Control createDialogArea(Composite container) {
    Composite result = (Composite) super.createDialogArea(container);
    // TODO... provide preference that supports 'always save'
    return result;
  }

  private ILabelProvider createDialogLabelProvider() {
    return new LabelProvider() {
      @Override
      public Image getImage(Object element) {
        return ((IEditorPart) element).getTitleImage();
      }

      @Override
      public String getText(Object element) {
        return ((IEditorPart) element).getTitle();
      }
    };
  }

  /**
   * A specialized content provider to show a list of editor parts. This class has been copied from
   * org.eclipse.jdt.internal.ui.viewsupport.ListContentProvider This class should be removed once a generic solution is
   * made available.
   */
  @SuppressWarnings("rawtypes")
  static class ListContentProvider implements IStructuredContentProvider {
    List fContents;

    @Override
    public Object[] getElements(Object input) {
      if(fContents != null && fContents == input)
        return fContents.toArray();
      return new Object[0];
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      if(newInput instanceof List) {
        fContents = (List) newInput;
      } else {
        fContents = null;
        // we use a fixed set.
      }
    }

    @Override
    public void dispose() {
    }

    public boolean isDeleted(Object o) {
      return fContents != null && !fContents.contains(o);
    }
  }
}
