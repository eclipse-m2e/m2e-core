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

package org.eclipse.m2e.editor.pom;

import static org.eclipse.m2e.editor.pom.FormUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
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
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.core.ui.internal.actions.OpenPomAction;
import org.eclipse.m2e.core.ui.internal.dialogs.InputHistory;
import static org.eclipse.m2e.core.ui.internal.editing.PomEdits.*;
import org.eclipse.m2e.editor.MavenEditorImages;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.m2e.editor.xml.internal.FormHoverProvider;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Parent;
import org.eclipse.m2e.model.edit.pom.PomPackage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/**
 * This class provides basic page editor functionality (event listeners, readonly, etc)
 * 
 * @author Anton Kraev
 * @author Eugene Kuleshov
 */
public abstract class MavenPomEditorPage extends FormPage implements Adapter {
  private static final String MODIFY_LISTENER = "MODIFY_LISTENER";

  private static final String VALUE_PROVIDER = "VALUE_PROVIDER";

  private static final Logger LOG = LoggerFactory.getLogger(MavenPomEditorPage.class);

  // parent editor
  protected final MavenPomEditor pomEditor;

  // model
  protected Model model;

  // Notifier target
  protected Notifier target;

  // are we already updating model
  protected boolean updatingModel;

  // have we loaded data?
  private boolean dataLoaded;

  private InputHistory inputHistory;
  
  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  private Action selectParentAction;

  public MavenPomEditorPage(MavenPomEditor pomEditor, String id, String title) {
    super(pomEditor, id, title);
    this.pomEditor = pomEditor;
    this.inputHistory = new InputHistory(id);
  }
  
  public MavenPomEditor getPomEditor() {
    return pomEditor;
  }

  @Override
  protected void createFormContent(IManagedForm managedForm) {
    ScrolledForm form = managedForm.getForm();
    IToolBarManager toolBarManager = form.getToolBarManager();

//    toolBarManager.add(pomEditor.showAdvancedTabsAction);
    
    selectParentAction = new Action(Messages.MavenPomEditorPage_action_open, MavenEditorImages.PARENT_POM) {
      public void run() {
        // XXX listen to parent modification and accordingly enable/disable action
        final Parent parent = model.getParent();
        if(parent!=null && !isEmpty(parent.getGroupId()) && !isEmpty(parent.getArtifactId()) && !isEmpty(parent.getVersion())) {
          new Job(Messages.MavenPomEditorPage_job_opening) {
            protected IStatus run(IProgressMonitor monitor) {
              OpenPomAction.openEditor(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), monitor);
              return Status.OK_STATUS;
            }
          }.schedule();
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
    
    //MNGECLIPSE-2674 checkreadonly is only calculated once, no need
    // to update everytime this page gets active
    boolean readOnly = pomEditor.checkReadOnly();
    if (readOnly) {
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
//      new Job("Loading pom.xml") {
//        protected IStatus run(IProgressMonitor monitor) {
        model = pomEditor.readProjectDocument();
        if(model != null) {
          if(getPartControl() != null) {
            getPartControl().getDisplay().asyncExec(new Runnable() {
              public void run() {
                updatingModel = true;
                try {
                  loadData();
                  updateParentAction();
                  registerListeners();
                } catch(Throwable e) {
                  LOG.error("Error loading data", e); //$NON-NLS-1$
                } finally {
                  updatingModel = false;
                }
              }
            });
          }
        }

      }
      
      //error markers have to be always updated..
      IFile pomFile = pomEditor.getPomFile();
      if(pomFile != null) {
        String text = "";  //$NON-NLS-1$
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
            if (toAdd != null) {
              //errors get prepended while warnings get appended.
              if (toAdd.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
                text = NLS.bind(Messages.MavenPomEditorPage_error_add, toAdd.getAttribute(IMarker.MESSAGE, "")) + text; //$NON-NLS-2$
              } else {
                text = text + NLS.bind(Messages.MavenPomEditorPage_warning_add, toAdd.getAttribute(IMarker.MESSAGE, ""));  //$NON-NLS-2$
              }
            }
          }
        }
        if(max != null) {
          String head; 
          String maxText = max.getAttribute(IMarker.MESSAGE, Messages.MavenPomEditorPage_error_unknown);
          if (text.length() > 0) {
            //if we have multiple errors
            text = NLS.bind(Messages.MavenPomEditorPage_add_desc,
                maxText, text);
            if (markers != null) {
              String number = new Integer(markers.length - 1).toString();
              head = NLS.bind(Messages.FormUtils_click_for_details2, maxText.length() > FormUtils.MAX_MSG_LENGTH ? maxText.substring(0, FormUtils.MAX_MSG_LENGTH) : maxText, number);
            } else {
              head = maxText;
              if (head.length() > FormUtils.MAX_MSG_LENGTH) {
                head = NLS.bind(Messages.FormUtils_click_for_details, head.substring(0, FormUtils.MAX_MSG_LENGTH));
              }
            }
          } else {
            //only this one
            text = maxText;
            head = maxText;
            if (head.length() > FormUtils.MAX_MSG_LENGTH) {
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
  
  private void setErrorMessageForMarkers(final String msg, final String tip, final int severity, final IMarker[] markers) {
    if (getPartControl() != null && !getPartControl().isDisposed()) {
      getPartControl().getDisplay().asyncExec(new Runnable() {
        public void run() {
          FormHoverProvider.Execute runnable = FormHoverProvider.createHoverRunnable(getManagedForm().getForm().getShell(), markers, getPomEditor().getSourcePage().getTextViewer());
          if (!getManagedForm().getForm().isDisposed()) {
            if (runnable != null) {
              FormUtils.setMessageWithPerformer(getManagedForm().getForm(), msg, severity, runnable);
            } else {
              FormUtils.setMessageAndTTip(getManagedForm().getForm(), msg, tip, severity);
            }
          }
        }
      });
    }
  }

  public void setErrorMessage(final String msg, final int severity) {
    if(getPartControl()!=null && !getPartControl().isDisposed()) {
      getPartControl().getDisplay().asyncExec(new Runnable() {
        public void run() {
          if (!getManagedForm().getForm().isDisposed()) {
            FormUtils.setMessage(getManagedForm().getForm(), msg, severity);
          }
        }
      });
    }
  }

  public Notifier getTarget() {
    return target;
  }

  public boolean isAdapterForType(Object type) {
    return false;
  }
  
  public void reload() {
    deRegisterListeners();
    boolean oldDataLoaded = dataLoaded;
    dataLoaded = false;
    doLoadData(oldDataLoaded);
  }

  public synchronized void notifyChanged(Notification notification) {
    if(updatingModel) {
      return;
    }
    
    updatingModel = true;
    try {
      switch(notification.getEventType()) {
        //TODO: fine-grained notification?
        case Notification.ADD:
        case Notification.MOVE:
        case Notification.REMOVE:
        case Notification.UNSET:
        case Notification.ADD_MANY: //this is for properties (clear/addAll is used for any properties update)
        case Notification.REMOVE_MANY:  
          if (getManagedForm() != null) {
            updateView(notification);
            updateParentAction();
          }
          break;
        case Notification.SET: {
          Object newValue = notification.getNewValue();
          Object oldValue = notification.getOldValue();
          if (newValue instanceof String && oldValue instanceof String && newValue.equals(oldValue)) {
            //the idea here is that triggering a view update for something that didn't change is not useful.
            //still there are other notifications that are similar (File>Revert or SCM team update seem to trigger
            // a complete reload of the model, which triggers a cloud of notification events..
            break;
          }
          if (getManagedForm() != null) {
            updateView(notification);
            updateParentAction();
          }
          break;
        }

        default:
          break;
          
        // case Notification.ADD_MANY:
        // case Notification.REMOVE_MANY:
      }

    } catch(Exception ex) {
      LOG.error("Can't update view", ex); //$NON-NLS-1$
    } finally {
      updatingModel = false;
    }
    
    registerListeners();
  }

  private void updateParentAction() {
    if (selectParentAction != null && model != null) {
      Parent par = model.getParent();
      if (par != null && par.getGroupId() != null && par.getArtifactId() != null && par.getVersion() != null) {
        selectParentAction.setEnabled(true);
      } else {
        selectParentAction.setEnabled(false);
      }
    }
  }
  
  /**
   * creates a text field/Ccombo decoration that shows the evaluated value 
   * @param control
   */
  public final void createEvaluatorInfo(final Control control) {
    if (!(control instanceof Text || control instanceof CCombo)) {
      throw new IllegalArgumentException("Not a Text or CCombo");
    }
    FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
        FieldDecorationRegistry.DEC_INFORMATION);
    final ControlDecoration decoration = new ControlDecoration(control, SWT.RIGHT | SWT.TOP) {

      /* (non-Javadoc)
       * @see org.eclipse.jface.fieldassist.ControlDecoration#getDescriptionText()
       */
      @Override
      public String getDescriptionText() {
        MavenProject mp = getPomEditor().getMavenProject();
        if (mp != null) {
          return FormUtils.simpleInterpolate(mp, control instanceof Text ? ((Text)control).getText() : ((CCombo)control).getText());
        }
        return "Cannot interpolate expressions, not resolvable file.";
      }
      
    };
    decoration.setShowOnlyOnFocus(false);
    decoration.setImage(fieldDecoration.getImage());
    decoration.setShowHover(true);
    decoration.hide(); //hide and wait for the value to be set.
    decoration.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        decoration.showHoverText(decoration.getDescriptionText());
      }
    });
    ModifyListener listener = new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        String text = control instanceof Text ? ((Text)control).getText() : ((CCombo)control).getText();
        if (text.indexOf("${") != -1 && text.indexOf("}") != -1) {
          decoration.show();
        } else {
          decoration.hide();
        }
      }
    };
    if (control instanceof Text) {
      ((Text)control).addModifyListener(listener);
    } else {
      ((CCombo)control).addModifyListener(listener);
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
    
    deRegisterListeners();
    
    super.dispose();
  }

  public void setTarget(Notifier newTarget) {
    this.target = newTarget;
  }

  public Model getModel() {
    return model;
  }

  /**
   * @deprecated to be removed soon
   * @return
   */
  public EditingDomain getEditingDomain() {
    return pomEditor.getEditingDomain();
  }
  
  public abstract void loadData();

  public abstract void updateView(Notification notification);

  public void registerListeners() {
    if(model!=null) {
      doRegister(model);
      
      for(Iterator<?> it = model.eAllContents(); it.hasNext();) {
        Object next = it.next();
        if (next instanceof EObject)
          doRegister((EObject) next);
      }
    }
  }

  private void doRegister(EObject object) {
    if (!object.eAdapters().contains(this)) {
      object.eAdapters().add(this);
    }
  }

  public void deRegisterListeners() {
    if(model!=null) {
      model.eAdapters().remove(this);
      for(Iterator<?> it = model.eAllContents(); it.hasNext(); ) {
        Object next = it.next();
        if(next instanceof EObject) {
          EObject object = (EObject) next;
          object.eAdapters().remove(this);
        }
      }
    }
  }
  
  public void setElementValueProvider(Control control, ElementValueProvider provider) {
    control.setData(VALUE_PROVIDER, provider);
  }
  
  public void setModifyListener(final Control control) {
    Assert.isTrue(control instanceof CCombo || control instanceof Text || control instanceof Combo);
    
    ModifyListener ml = new ModifyListener() {
      String getText(Control control) {
        if (control instanceof Text) {
          return ((Text)control).getText(); 
        }
        if (control instanceof Combo) {
          return ((Combo)control).getText();
        }
        if (control instanceof CCombo) {
          return ((CCombo)control).getText();
        }
        throw new IllegalStateException();
      }
      
      public void modifyText(ModifyEvent e) {
        final ElementValueProvider provider = (ElementValueProvider) control.getData(VALUE_PROVIDER);
        if (provider == null) {
          throw new IllegalStateException("no value provider for " + control);
        }
        try {
          performOnDOMDocument(new OperationTuple(getPomEditor().getDocument(), new Operation() {
            public void process(Document document) {
              String text = getText(control);
              if (isEmpty(text) || text.equals(provider.getDefaultValue())) {
                //remove value
                Element el = provider.find(document);
                if (el != null) {
                  Node parent = el.getParentNode();
                  if (parent instanceof Element) {
                    removeChild((Element)parent, el);
                    removeIfNoChildElement((Element)parent);
                  }
                }
              } else {
                //set value and any parents..
                Element el = provider.get(document);
                setText(el, text);
              }
            }
          }));
        } catch(Exception e1) {
          LOG.error("Error updating document", e1);
        }
      }
    };
    control.setData(MODIFY_LISTENER, ml);
  }


  public void removeNotifyListener(Text control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }
  
  public void addNotifyListener(Text control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
        control.addModifyListener(listener);
      }
    }
  }
    

  public void removeNotifyListener(CCombo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }
  
  public void addNotifyListener(CCombo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
        control.addModifyListener(listener);
      }
    }
  }

  public void removeNotifyListener(Combo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
        control.removeModifyListener(listener);
      }
    }
  }
  
  public void addNotifyListener(Combo control) {
    if(!control.isDisposed()) {
      ModifyListener listener = (ModifyListener) control.getData(MODIFY_LISTENER);
      if (listener != null) {
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
    if (modulePath == null) return null;
    modulePath = modulePath.append(module);
    //it's possible to have the pom file name in the module path..
    if (!modulePath.lastSegment().endsWith("pom.xml")) { //$NON-NLS-1$
      modulePath = modulePath.append("pom.xml"); //$NON-NLS-1$
    }
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    IMavenProjectFacade[] facades = projectManager.getProjects();
    for(int i = 0; i < facades.length; i++ ) {
      if(facades[i].getPom().getLocation().equals(modulePath)) {
        return facades[i];
      }
    }
    return null;
  }
  
  public IFile findModuleFile(String moduleName) {
    IFile pomFile = pomEditor.getPomFile();
    if(pomFile!=null) {
      IPath modulePath = pomFile.getParent().getLocation();
      if (modulePath == null) return null;
      modulePath = modulePath.append(moduleName);
      //it's possible to have the pom file name in the module path..
      if (!modulePath.lastSegment().endsWith("pom.xml")) { //$NON-NLS-1$
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

  /*
   * returns added/removed/updated EObject from notification (convenience method for detail forms)
   */
  public static Object getFromNotification(Notification notification) {
    if(notification.getFeature() != null && !(notification.getFeature() instanceof EAttribute)) {
      // for structuralFeatures, return new value (for insert/delete)
      return notification.getNewValue();
    } else {
      // for attributes, return the notifier as it contains all new attributes (attribute modified)
      return notification.getNotifier();
    }
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
    return pomFile != null? pomFile.getProject(): null;
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
