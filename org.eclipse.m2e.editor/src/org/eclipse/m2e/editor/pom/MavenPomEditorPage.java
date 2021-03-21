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

package org.eclipse.m2e.editor.pom;

import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.ARTIFACT_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.GROUP_ID;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.PARENT;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.VERSION;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.findChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.getTextValue;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.performOnDOMDocument;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeChild;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.removeIfNoChildElement;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.setText;
import static org.eclipse.m2e.editor.pom.FormUtils.isEmpty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.wst.sse.core.internal.provisional.IModelStateListener;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;

import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.dialogs.InputHistory;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.FormHoverProvider;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * This class provides basic page editor functionality (event listeners, readonly, etc)
 * 
 * @author Anton Kraev
 * @author Eugene Kuleshov
 */
public abstract class MavenPomEditorPage extends FormPage {
  private static final String MODIFY_LISTENER = "MODIFY_LISTENER";

  private static final String VALUE_PROVIDER = "VALUE_PROVIDER";

  private static final Logger LOG = LoggerFactory.getLogger(MavenPomEditorPage.class);

  // parent editor
  protected final MavenPomEditor pomEditor;

  private boolean updatingModel2 = false;

  // have we loaded data?
  private boolean dataLoaded;

  private InputHistory inputHistory;

  private Action selectParentAction;

  private IModelStateListener listener;

  private boolean alreadyShown = false;

  public MavenPomEditorPage(MavenPomEditor pomEditor, String id, String title) {
    super(pomEditor, id, title);
    this.pomEditor = pomEditor;
    this.inputHistory = new InputHistory(id);
    listener = new IModelStateListener() {
      public void modelResourceMoved(IStructuredModel oldModel, IStructuredModel newModel) {
      }

      public void modelResourceDeleted(IStructuredModel model) {
      }

      public void modelReinitialized(IStructuredModel structuredModel) {
      }

      public void modelDirtyStateChanged(IStructuredModel model, boolean isDirty) {
      }

      public void modelChanged(IStructuredModel model) {
        if(!updatingModel2) {
          loadData();
        }
      }

      public void modelAboutToBeReinitialized(IStructuredModel structuredModel) {
      }

      public void modelAboutToBeChanged(IStructuredModel model) {
      }
    };

  }

  public MavenPomEditor getPomEditor() {
    return pomEditor;
  }

  /**
   * all edits in the editor to be channeled through this method..
   * 
   * @param operation
   * @param logger
   * @param logMessage
   */
  public final void performEditOperation(PomEdits.Operation operation, Logger logger, String logMessage) {
    try {
      updatingModel2 = true;
      PomEdits.performOnDOMDocument(new PomEdits.OperationTuple(getPomEditor().getDocument(), operation));
    } catch(Exception e) {
      logger.error(logMessage, e);
    } finally {
      updatingModel2 = false;
    }
  }

  @Override
  protected void createFormContent(IManagedForm managedForm) {
    ScrolledForm form = managedForm.getForm();
    IToolBarManager toolBarManager = form.getToolBarManager();

//    toolBarManager.add(pomEditor.showAdvancedTabsAction);

    selectParentAction = new Action(Messages.MavenPomEditorPage_action_open, MavenEditorImages.PARENT_POM) {
      public void run() {
        final String[] ret = new String[3];
        try {
          performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), document -> {
            Element parent = findChild(document.getDocumentElement(), PARENT);
            ret[0] = getTextValue(findChild(parent, GROUP_ID));
            ret[1] = getTextValue(findChild(parent, ARTIFACT_ID));
            ret[2] = getTextValue(findChild(parent, VERSION));
          }, true));
          // XXX listen to parent modification and accordingly enable/disable action
          if(!isEmpty(ret[0]) && !isEmpty(ret[1]) && !isEmpty(ret[2])) {
            new Job(Messages.MavenPomEditorPage_job_opening) {
              protected IStatus run(IProgressMonitor monitor) {
                OpenPomAction.openEditor(ret[0], ret[1], ret[2], getPomEditor().getMavenProject(), monitor);
                return Status.OK_STATUS;
              }
            }.schedule();
          }
        } catch(Exception e) {
          LOG.error("Error finding parent element", e);
        }

      }
    };
    toolBarManager.add(selectParentAction);
    updateParentAction();

    toolBarManager.add(new Action(Messages.MavenPomEditorPage_actio_refresh, MavenEditorImages.REFRESH) {
      public void run() {
        pomEditor.reload();
      }
    });

    form.updateToolBar();

    // compatibility proxy to support Eclipse 3.2
    FormUtils.decorateHeader(managedForm.getToolkit(), form.getForm());

    inputHistory.load();
  }

  public void setActive(boolean active) {
    super.setActive(active);

    doLoadData(active);
    if(active) {
      getPomEditor().getModel().addModelStateListener(listener);
    } else {
      getPomEditor().getModel().removeModelStateListener(listener);
    }
    if(active && alreadyShown) {
      loadData();
      updateParentAction();
    }
    alreadyShown = true;

    //MNGECLIPSE-2674 checkreadonly is only calculated once, no need
    // to update everytime this page gets active
    boolean readOnly = pomEditor.checkReadOnly();
    if(readOnly) {
      // only perform when readonly==true, to prevent enabling all buttons on the page.
      FormUtils.setReadonly((Composite) getPartControl(), readOnly);
    }
  }

  public boolean isReadOnly() {
    return pomEditor.checkReadOnly();
  }

  private void doLoadData(boolean active) {
    try {
      if(active && !dataLoaded) {
        dataLoaded = true;
        if(getPartControl() != null) {
          getPartControl().getDisplay().asyncExec(() -> {
            try {
              loadData();
              updateParentAction();
            } catch(Throwable e) {
              LOG.error("Error loading data", e); //$NON-NLS-1$
            }
          });
        }

      }

      //error markers have to be always updated..
      IFile pomFile = pomEditor.getPomFile();
      if(pomFile != null) {
        String text = ""; //$NON-NLS-1$
        IMarker[] markers = pomFile.findMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_ZERO);
        IMarker max = null;
        int maxSev = -1;
        if(markers != null) {
          for(IMarker mark : markers) {
            IMarker toAdd = max;
            int sev = mark.getAttribute(IMarker.SEVERITY, -1);
            if(sev > maxSev) {
              max = mark;
              maxSev = sev;
            } else {
              toAdd = mark;
            }
            if(toAdd != null) {
              //errors get prepended while warnings get appended.
              if(toAdd.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
                text = NLS.bind(Messages.MavenPomEditorPage_error_add, toAdd.getAttribute(IMarker.MESSAGE, "")) + text; //$NON-NLS-2$
              } else {
                text = text
                    + NLS.bind(Messages.MavenPomEditorPage_warning_add, toAdd.getAttribute(IMarker.MESSAGE, "")); //$NON-NLS-2$
              }
            }
          }
        }
        if(max != null) {
          String head;
          String maxText = max.getAttribute(IMarker.MESSAGE, Messages.MavenPomEditorPage_error_unknown);
          if(text.length() > 0) {
            //if we have multiple errors
            text = NLS.bind(Messages.MavenPomEditorPage_add_desc, maxText, text);
            if(markers != null) {
              String number = Integer.toString(markers.length - 1);
              head = NLS.bind(Messages.FormUtils_click_for_details2,
                  maxText.length() > FormUtils.MAX_MSG_LENGTH ? maxText.substring(0, FormUtils.MAX_MSG_LENGTH)
                      : maxText,
                  number);
            } else {
              head = maxText;
              if(head.length() > FormUtils.MAX_MSG_LENGTH) {
                head = NLS.bind(Messages.FormUtils_click_for_details, head.substring(0, FormUtils.MAX_MSG_LENGTH));
              }
            }
          } else {
            //only this one
            text = maxText;
            head = maxText;
            if(head.length() > FormUtils.MAX_MSG_LENGTH) {
              head = NLS.bind(Messages.FormUtils_click_for_details, head.substring(0, FormUtils.MAX_MSG_LENGTH));
            }
          }
          int severity;
          switch(max.getAttribute(IMarker.SEVERITY, -1)) {
            case IMarker.SEVERITY_ERROR: {
              severity = IMessageProvider.ERROR;
              break;
            }
            case IMarker.SEVERITY_WARNING: {
              severity = IMessageProvider.WARNING;
              break;
            }
            case IMarker.SEVERITY_INFO: {
              severity = IMessageProvider.INFORMATION;
              break;
            }
            default: {
              severity = IMessageProvider.NONE;
            }
          }
          setErrorMessageForMarkers(head, text, severity, markers);
        } else {
          setErrorMessageForMarkers(null, null, IMessageProvider.NONE, new IMarker[0]);
        }
      }
    } catch(final CoreException ex) {
      LOG.error(ex.getMessage(), ex);
      final String msg = ex.getMessage();
      setErrorMessageForMarkers(msg, msg, IMessageProvider.ERROR, new IMarker[0]);
    }

  }

  private void setErrorMessageForMarkers(final String msg, final String tip, final int severity,
      final IMarker[] markers) {
    if(getPartControl() != null && !getPartControl().isDisposed()) {
      getPartControl().getDisplay().asyncExec(() -> {
        if(!getManagedForm().getForm().isDisposed()) {
          ISourceViewer sourceViewer = null;
          try {
            Method getSourceViewer = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
            getSourceViewer.setAccessible(true);
            sourceViewer = (ISourceViewer) getSourceViewer.invoke(getPomEditor().getSourcePage());
          } catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
              | InvocationTargetException ex) {
            // TODO Auto-generated catch block
            //log.error(ex.getMessage(), ex);
            ex.printStackTrace();
          }

          Consumer<Point> runnable = FormHoverProvider.createHoverRunnable(getManagedForm().getForm().getShell(),
              markers, sourceViewer);
          if(runnable != null) {
            FormUtils.setMessageWithPerformer(getManagedForm().getForm(), msg, severity, runnable);
          } else {
            FormUtils.setMessageAndTTip(getManagedForm().getForm(), msg, tip, severity);
          }
        }
      });
    }
  }

  public void setErrorMessage(final String msg, final int severity) {
    if(getPartControl() != null && !getPartControl().isDisposed()) {
      getPartControl().getDisplay().asyncExec(() -> {
        if(!getManagedForm().getForm().isDisposed()) {
          FormUtils.setMessage(getManagedForm().getForm(), msg, severity);
        }
      });
    }
  }

  public boolean isAdapterForType(Object type) {
    return false;
  }

  private void updateParentAction() {
    if(selectParentAction != null) {
      final boolean[] ret = new boolean[1];
      try {
        performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), document -> {
          Element parent = findChild(document.getDocumentElement(), PARENT);
          String g = getTextValue(findChild(parent, GROUP_ID));
          String a = getTextValue(findChild(parent, ARTIFACT_ID));
          String v = getTextValue(findChild(parent, VERSION));
          ret[0] = g != null && a != null && v != null;
        }, true));
      } catch(Exception e) {
        ret[0] = false;
      }
      if(ret[0]) {
        selectParentAction.setEnabled(true);
      } else {
        selectParentAction.setEnabled(false);
      }
    }
  }

  /**
   * creates a text field/Ccombo decoration that shows the evaluated value
   * 
   * @param control
   */
  public final void createEvaluatorInfo(final Control control) {
    if(!(control instanceof Text || control instanceof CCombo)) {
      throw new IllegalArgumentException("Not a Text or CCombo");
    }
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
        .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
    final ControlDecoration decoration = new ControlDecoration(control, SWT.RIGHT | SWT.TOP) {

      /* (non-Javadoc)
       * @see org.eclipse.jface.fieldassist.ControlDecoration#getDescriptionText()
       */
      @Override
      public String getDescriptionText() {
        MavenProject mp = getPomEditor().getMavenProject();
        if(mp != null) {
          return FormUtils.simpleInterpolate(mp,
              control instanceof Text ? ((Text) control).getText() : ((CCombo) control).getText());
        }
        return "Cannot interpolate expressions, not resolvable file.";
      }

    };
    decoration.setShowOnlyOnFocus(false);
    decoration.setImage(fieldDecoration.getImage());
    decoration.setShowHover(true);
    decoration.hide(); //hide and wait for the value to be set.
    decoration.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> decoration.showHoverText(decoration.getDescriptionText())));
    ModifyListener listener = e -> {
      String text = control instanceof Text ? ((Text) control).getText() : ((CCombo) control).getText();
      if(text.indexOf("${") != -1 && text.indexOf("}") != -1) {
        decoration.show();
      } else {
        decoration.hide();
      }
    };
    if(control instanceof Text) {
      ((Text) control).addModifyListener(listener);
    } else {
      ((CCombo) control).addModifyListener(listener);
    }
    control.addMouseTrackListener(new MouseTrackListener() {
      public void mouseHover(MouseEvent e) {
        decoration.showHoverText(decoration.getDescriptionText());
      }

      public void mouseExit(MouseEvent e) {
        decoration.hideHover();
      }

      public void mouseEnter(MouseEvent e) {
      }
    });
  }

  public void dispose() {
    inputHistory.save();
    MavenPomEditor pe = getPomEditor();
    if(pe != null && pe.getModel() != null) {
      pe.getModel().removeModelStateListener(listener);
    }

    super.dispose();
  }

  public abstract void loadData();

  public void setElementValueProvider(Control control, ElementValueProvider provider) {
    control.setData(VALUE_PROVIDER, provider);
  }

  public void setModifyListener(final Control control) {
    Assert.isTrue(control instanceof CCombo || control instanceof Text || control instanceof Combo);

    ModifyListener ml = new ModifyListener() {
      String getText(Control control) {
        if(control instanceof Text) {
          return ((Text) control).getText();
        }
        if(control instanceof Combo) {
          return ((Combo) control).getText();
        }
        if(control instanceof CCombo) {
          return ((CCombo) control).getText();
        }
        throw new IllegalStateException();
      }

      public void modifyText(ModifyEvent e) {
        final ElementValueProvider provider = (ElementValueProvider) control.getData(VALUE_PROVIDER);
        if(provider == null) {
          throw new IllegalStateException("no value provider for " + control);
        }
        performEditOperation(document -> {
          String text = getText(control);
          if(isEmpty(text) || text.equals(provider.getDefaultValue())) {
            //remove value
            Element el1 = provider.find(document);
            if(el1 != null) {
              Node parent = el1.getParentNode();
              if(parent instanceof Element) {
                removeChild((Element) parent, el1);
                removeIfNoChildElement((Element) parent);
              }
            }
          } else {
            //set value and any parents..
            Element el2 = provider.get(document);
            setText(el2, text);
          }
        }, LOG, "Error updating document");
      }
    };
    control.setData(MODIFY_LISTENER, ml);
  }

  public void removeNotifyListener(Text control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }

  public void addNotifyListener(Text control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.addModifyListener(listener);
      }
    }
  }

  public void removeNotifyListener(CCombo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }

  public void addNotifyListener(CCombo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.addModifyListener(listener);
      }
    }
  }

  public void removeNotifyListener(Combo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }

  public void addNotifyListener(Combo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if(listener != null) {
        control.addModifyListener(listener);
      }
    }
  }

  public IMavenProjectFacade findModuleProject(String moduleName) {
    IFile pomFile = pomEditor.getPomFile();
    if(pomFile != null) {
      return findModuleProject(pomFile, moduleName);
    }
    return null;
  }

  private IMavenProjectFacade findModuleProject(IFile pomFile, String module) {
    IPath modulePath = pomFile.getParent().getLocation();
    if(modulePath == null)
      return null;
    modulePath = modulePath.append(module);
    //it's possible to have the pom file name in the module path..
    if(!modulePath.lastSegment().endsWith("pom.xml")) { //$NON-NLS-1$
      modulePath = modulePath.append("pom.xml"); //$NON-NLS-1$
    }
    IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
    IMavenProjectFacade[] facades = projectManager.getProjects();
    for(IMavenProjectFacade facade : facades) {
      if(modulePath.equals(facade.getPom().getLocation())) {
        return facade;
      }
    }
    return null;
  }

  public IFile findModuleFile(String moduleName) {
    IFile pomFile = pomEditor.getPomFile();
    if(pomFile != null) {
      IPath modulePath = pomFile.getParent().getLocation();
      if(modulePath == null)
        return null;
      modulePath = modulePath.append(moduleName);
      //it's possible to have the pom file name in the module path..
      if(!modulePath.lastSegment().endsWith("pom.xml")) { //$NON-NLS-1$
        modulePath = modulePath.append("pom.xml"); //$NON-NLS-1$
      }
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(modulePath);
      return file;
    }
    return null;
  }

  public void initPopupMenu(Viewer viewer, String id) {
    MenuManager menuMgr = new MenuManager("#PopupMenu-" + id); //$NON-NLS-1$
    menuMgr.setRemoveAllWhenShown(true);

    Menu menu = menuMgr.createContextMenu(viewer.getControl());

    viewer.getControl().setMenu(menu);

    getEditorSite().registerContextMenu(MavenPomEditor.EDITOR_ID + id, menuMgr, viewer, false);
  }

  /**
   * Adapter for Text, Combo and CCombo widgets
   */
  public interface TextAdapter {
    String getText();

    void addModifyListener(ModifyListener listener);
  }

  public IProject getProject() {
    IFile pomFile = pomEditor.getPomFile();
    return pomFile != null ? pomFile.getProject() : null;
  }

  protected void addToHistory(Control control) {
    inputHistory.add(control);
  }

  /**
   * pages gets notified when cached effective model has changed.
   */
  public void mavenProjectHasChanged() {
  }
}
