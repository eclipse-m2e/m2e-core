/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.search.ui.text.ISearchEditorAccess;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.util.DocumentInputStream;
import org.eclipse.wst.xml.core.internal.emf2xml.EMF2DOMSSEAdapter;
import org.eclipse.wst.xml.core.internal.provisional.contenttype.ContentTypeIdForXML;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;

import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.jobs.MavenJob;
import org.eclipse.m2e.core.internal.preferences.MavenPreferenceConstants;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.ui.internal.M2EUIPluginActivator;
import org.eclipse.m2e.core.ui.internal.actions.SelectionUtil;
import org.eclipse.m2e.core.ui.internal.actions.StaticMavenStorageEditorInput;
import org.eclipse.m2e.editor.MavenEditorPlugin;
import org.eclipse.m2e.editor.internal.Messages;


/**
 * Maven POM editor
 *
 * @author Eugene Kuleshov
 * @author Anton Kraev
 * @param <page>
 */
@SuppressWarnings("restriction")
public class MavenPomEditor extends FormEditor implements IResourceChangeListener, IShowEditorInput, IGotoMarker,
    ISearchEditorAccess, IMavenProjectChangedListener {
  private static final Logger log = LoggerFactory.getLogger(MavenPomEditor.class);

  public static final String POM_XML = "pom.xml";

  public static final String EDITOR_ID = "org.eclipse.m2e.editor.MavenPomEditor"; //$NON-NLS-1$

  private static final String EXTENSION_FACTORIES = MavenEditorPlugin.PLUGIN_ID + ".pageFactories"; //$NON-NLS-1$

  private static final String ELEMENT_PAGE = "factory"; //$NON-NLS-1$

  public static final String EFFECTIVE_POM = Messages.MavenPomEditor_effective_pom;

  OverviewPage overviewPage;

  DependenciesPage dependenciesPage;

  DependencyTreePage dependencyTreePage;

  ITextEditor sourcePage;

  ITextEditor effectivePomSourcePage;

  private final List<MavenPomEditorPage> mavenpomEditorPages = new ArrayList<>();

  private final Map<String, org.eclipse.aether.graph.DependencyNode> rootNodes = new HashMap<>();

  IStructuredModel structuredModel;

  private MavenProject mavenProject;

  private int sourcePageIndex;

  IModelManager modelManager;

  IFile pomFile;

  MavenPomActivationListener activationListener;

  List<IPomFileChangedListener> fileChangeListeners = new ArrayList<>();

  private boolean resourceChangeEventSkip = false;

  private StaticMavenStorageEditorInput effectivePomEditorInput;

  private boolean disposed = false;

  private IDocumentListener documentListener;

  private IDocument sourceDocument;

  public MavenPomEditor() {
    modelManager = StructuredModelManager.getModelManager();
  }

  /**
   * the pom document being edited..
   *
   * @return
   */
  public IDocument getDocument() {
    if(structuredModel == null)
      return null;
    return structuredModel.getStructuredDocument();
  }

  public IStructuredModel getModel() {
    return structuredModel;
  }

  // IResourceChangeListener

  /**
   * Closes all project files on project close.
   */
  @Override
  public void resourceChanged(final IResourceChangeEvent event) {
    if(pomFile == null) {
      return;
    }

    //handle project delete
    if(event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
      if(pomFile.getProject().equals(event.getResource())) {

      }
      return;
    }
    //handle pom delete
    class RemovedResourceDeltaVisitor implements IResourceDeltaVisitor {
      boolean removed = false;

      @Override
      public boolean visit(IResourceDelta delta) {
        if(delta.getResource() == pomFile //
            && (delta.getKind() & (IResourceDelta.REMOVED)) != 0) {
          removed = true;
          return false;
        }
        return true;
      }
    }

    try {
      RemovedResourceDeltaVisitor visitor = new RemovedResourceDeltaVisitor();
      event.getDelta().accept(visitor);
      if(visitor.removed) {
        Display.getDefault().asyncExec(() -> close(true));
      }
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }

    // Reload model if pom file was changed externally.
    // TODO implement generic merge scenario (when file is externally changed and is dirty)

    class ChangedResourceDeltaVisitor implements IResourceDeltaVisitor {

      @Override
      public boolean visit(IResourceDelta delta) {
        if(delta.getResource().equals(pomFile) && (delta.getKind() & IResourceDelta.CHANGED) != 0
            && delta.getResource().exists()) {
          int flags = delta.getFlags();
          if((flags & IResourceDelta.CONTENT) != 0 || (flags & IResourceDelta.REPLACED) != 0) {
            handleContentChanged();
            return false;
          }
          if((flags & IResourceDelta.MARKERS) != 0) {
            handleMarkersChanged();
            return false;
          }
        }
        return true;
      }

      /**
       * this method never got called with the current editor changes/saves the file. the doSave() method removed/added
       * the resource listener when saving it did get called however when external editor (txt/xml) saved the file..
       * I've changed that behaviour to only avoid the reload() call when current editor saves the file. we still want
       * to attempt to reload the mavenproject instance.. please read <code>mavenProjectChanged</code> javadoc for
       * details on when this works and when not.
       */
      private void handleContentChanged() {
        reloadMavenProjectCache();
        if(!resourceChangeEventSkip) {
          Display.getDefault().asyncExec(() -> reload());
        }

      }

      private void handleMarkersChanged() {
        try {
          IMarker[] markers = pomFile.findMarkers(IMavenConstants.MARKER_ID, true, IResource.DEPTH_ZERO);
          final String msg = markers != null && markers.length > 0 //
              ? markers[0].getAttribute(IMarker.MESSAGE, "Unknown error")
              : null;
          final int severity = markers != null && markers.length > 0
              ? (markers[0].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR) == IMarker.SEVERITY_WARNING
                  ? IMessageProvider.WARNING
                  : IMessageProvider.ERROR)
              : IMessageProvider.NONE;

          Display.getDefault().asyncExec(() -> {
            for(MavenPomEditorPage page : getMavenPomEditorPages()) {
              page.setErrorMessage(msg, msg == null ? IMessageProvider.NONE : severity);
            }
          });
        } catch(CoreException ex) {
          log.error("Error updating pom file markers.", ex); //$NON-NLS-1$
        }
      }
    }

    try {
      ChangedResourceDeltaVisitor visitor = new ChangedResourceDeltaVisitor();
      event.getDelta().accept(visitor);
    } catch(CoreException ex) {
      log.error(ex.getMessage(), ex);
    }

  }

  public void reload() {
    int active = getActivePage();
    //this code assumes the MavenPomEditorPages are the first ones in the list..
    //currenty the case, effective+xml editor are at the end..
    //if this constraint changes, we need to find the active page in the super.pages list first and check for instanceof
    if(active > -1 && active < getMavenPomEditorPages().size()) {
      MavenPomEditorPage page = getMavenPomEditorPages().get(active);
      page.loadData();
    }
    if(isEffectiveActive()) {
      loadEffectivePOM();
    }
  }

  private boolean isEffectiveActive() {
    int active = getActivePage();
    if(active < 0) {
      return false;
    }
    String name = getPageText(active);
    return EFFECTIVE_POM.equals(name);
  }

  @Override
  protected void addPages() {

    overviewPage = new OverviewPage(this);
    addPomPage(overviewPage);

    dependenciesPage = new DependenciesPage(this);
    addPomPage(dependenciesPage);

    dependencyTreePage = new DependencyTreePage(this);
    addPomPage(dependencyTreePage);

    addEditorPageExtensions();
    addFallbackEditors();
    setModel();
    addDocumentListener();

    selectActivePage();
  }

  private void addFallbackEditors() {
    if(effectivePomSourcePage == null) {
      effectivePomSourcePage = new ExtensionBasedTextEditor();
      int effectPomIndex;
      try {
        effectPomIndex = addPage(effectivePomSourcePage, getEffectivePomEditorInput());
        setPageText(effectPomIndex, MavenPomEditor.EFFECTIVE_POM);
        setEffectivePomSourcePage(effectivePomSourcePage);
      } catch(PartInitException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    if(sourcePage == null) {
      sourcePage = new ExtensionBasedTextEditor();
      int dex;
      try {
        dex = addPage(sourcePage, getEditorInput());
        setPageText(dex, MavenPomEditor.POM_XML);
        setSourcePage(sourcePage);
        initializeSourceDocument();
      } catch(PartInitException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
  }

  private void setModel() {
    if(sourceDocument == null) {
      initializeSourceDocument();
    }
    if(sourceDocument instanceof IStructuredDocument structuredSelection) {
      this.structuredModel = this.modelManager.getModelForEdit(structuredSelection);
    } else {
      // create a read-only copy of the document
      try (InputStream docStream = new DocumentInputStream(sourceDocument)) {
        this.structuredModel = this.modelManager.getModelForRead(getEditorInput().getName(), docStream, null);
      } catch(IOException ex) {
        // TODO Auto-generated catch block
        log.error(ex.getMessage(), ex);
      }
    }
  }

  private void initializeSourceDocument() {
    documentProvider = sourcePage.getDocumentProvider();
    if(documentProvider == null) {
      return;
    }
    sourceDocument = documentProvider.getDocument(this.getEditorInput());
    if(documentProvider instanceof TextFileDocumentProvider textDocumentProvider) {
      try {
        // not clear why, but some documents are sometimes not synchronized at this stage
        // and this causes some strange dirtiness behavior or non-working refactorings.
        // It's most probably some disposal issue but no better solution was found yet.
        // so let's force synchronization...
        textDocumentProvider.synchronize(getEditorInput());
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    closeEditorOnDocumentDeletion = new IElementStateListener() {
      @Override
      public void elementMoved(Object originalElement, Object movedElement) {
        // TODO handle move
      }

      @Override
      public void elementDirtyStateChanged(Object element, boolean isDirty) {
        // nothing to do
      }

      @Override
      public void elementDeleted(Object element) {
        if(Objects.equals(element, getEditorInput())) {
          Display.getDefault().asyncExec(() -> close(false));
        }
      }

      @Override
      public void elementContentReplaced(Object element) {
        // nothing to do
      }

      @Override
      public void elementContentAboutToBeReplaced(Object element) {
        // nothing to do
      }
    };
    documentProvider.addElementStateListener(closeEditorOnDocumentDeletion);
  }

  protected void selectActivePage() {
    boolean showXML = M2EUIPluginActivator.getDefault().getPreferenceStore()
        .getBoolean(MavenPreferenceConstants.P_DEFAULT_POM_EDITOR_PAGE);
    if(showXML) {
      setActivePage(sourcePageIndex);
    }
  }

  @Override
  protected void pageChange(int newPageIndex) {
    String name = getPageText(newPageIndex);
    if(EFFECTIVE_POM.equals(name)) {
      loadEffectivePOM();
    }
    //The editor occassionally doesn't get
    //closed if the project gets deleted. In this case, the editor
    //stays open and very bad things happen if you select it
    try {
      super.pageChange(newPageIndex);
    } catch(NullPointerException e) {
      MavenEditorPlugin.getDefault().getLog().log(Status.error("", e)); //$NON-NLS-1$
      this.close(false);
    }
    // a workaround for editor pages not returned
    IEditorActionBarContributor contributor = getEditorSite().getActionBarContributor();
    if(contributor instanceof MultiPageEditorActionBarContributor multiPageContributor) {
      IEditorPart activeEditor = getActivePageInstance();
      multiPageContributor.setActivePage(activeEditor);
    }
  }

  private void addEditorPageExtensions() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint indexesExtensionPoint = registry.getExtensionPoint(EXTENSION_FACTORIES);
    if(indexesExtensionPoint != null) {
      IExtension[] indexesExtensions = indexesExtensionPoint.getExtensions();
      for(IExtension extension : indexesExtensions) {
        for(IConfigurationElement element : extension.getConfigurationElements()) {
          if(ELEMENT_PAGE.equals(element.getName())) {
            try {
              MavenPomEditorPageFactory factory;
              factory = (MavenPomEditorPageFactory) element.createExecutableExtension("class"); //$NON-NLS-1$
              factory.addPages(this);
            } catch(CoreException ex) {
              log.error(ex.getMessage(), ex);
            }
          }
        }
      }
    }
  }

  private void addDocumentListener() {
    sourceDocument = this.getDocument();
    documentListener = new IDocumentListener() {

      @Override
      public void documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent event) {
      }

      @Override
      public void documentChanged(org.eclipse.jface.text.DocumentEvent event) {
        try {
          if(pomFile != null) {
            pomFile.refreshLocal(IResource.DEPTH_INFINITE, null);
          }
        } catch(CoreException e) {
          log.error(e.getMessage(), e);
        }

        // recheck the read-only status if the document changes (will happen when xml
        // page is edited)
        if(MavenPomEditor.this.isCheckedWritableStatus() && MavenPomEditor.this.isReadOnly()) {
          MavenPomEditor.this.setCheckedWritableStatus(false);
        }
      }
    };
    sourceDocument.addDocumentListener(documentListener);

  }

  @Override
  protected IEditorSite createSite(IEditorPart editor) {
    IEditorSite site = null;
    if(editor == sourcePage) {
      site = new MultiPageEditorSite(this, editor) {
        /**
         * @see org.eclipse.ui.part.MultiPageEditorSite#getActionBarContributor()
         */
        @Override
        public IEditorActionBarContributor getActionBarContributor() {
          IEditorActionBarContributor contributor = super.getActionBarContributor();
          IEditorActionBarContributor multiContributor = MavenPomEditor.this.getEditorSite().getActionBarContributor();
          if(multiContributor instanceof MavenPomEditorContributor pomEditorContributor) {
            contributor = pomEditorContributor.sourceViewerActionContributor;
          }
          return contributor;
        }

        @Override
        public String getId() {
          // sets this id so nested editor is considered xml source
          // page
          return ContentTypeIdForXML.ContentTypeID_XML + ".source"; //$NON-NLS-1$;
        }
      };
    } else {
      site = super.createSite(editor);
    }
    return site;
  }

  /**
   * Load the effective POM in a job and then update the effective pom page when its done
   *
   * @author dyocum
   */
  class LoadEffectivePomJob extends MavenJob {

    public LoadEffectivePomJob(String name) {
      super(name);
    }

    private void showEffectivePomError(final String name) {
      if(disposed) {
        return;
      }
      String error = Messages.MavenPomEditor_error_loading_effective_pom;
      IDocument doc = getEffectivePomSourcePage().getDocumentProvider().getDocument(getEffectivePomEditorInput());
      if(Display.getCurrent() != null) {
        doc.set(error);
      } else {
        getSite().getShell().getDisplay().asyncExec(() -> doc.set(error));
      }
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      try {
        StringWriter sw = new StringWriter();
        final String name = getPartName() + Messages.MavenPomEditor_effective;
        MavenProject mavenProject = SelectionUtil.getMavenProject(getEditorInput(), monitor);
        if(mavenProject == null) {
          showEffectivePomError(name);
          return Status.CANCEL_STATUS;
        }
        new MavenXpp3Writer().write(sw, mavenProject.getModel());
        final String content = sw.toString();
        if(disposed) {
          return Status.OK_STATUS;
        }
        IDocument doc = getEffectivePomSourcePage().getDocumentProvider().getDocument(getEffectivePomEditorInput());
        getSite().getShell().getDisplay().syncExec(() -> doc.set(content)); // run in UI Thread as some listeners require it
        return Status.OK_STATUS;
      } catch(CoreException ce) {
        return Status.error(Messages.MavenPomEditor_error_failed_effective, ce);
      } catch(IOException ie) {
        return Status.error(Messages.MavenPomEditor_error_failed_effective, ie);
      }
    }
  }

  /**
   * Load the effective POM. Should only happen when tab is brought to front or tab is in front when a reload happens.
   * This is NOT an API, it's only meant for usage in tests.
   */
  public void loadEffectivePOM() {
    if(disposed) {
      return;
    }
    String content = Messages.MavenPomEditor_loading;
    IDocument doc = getEffectivePomSourcePage().getDocumentProvider().getDocument(getEffectivePomEditorInput());
    doc.set(content);

    //then start the load
    LoadEffectivePomJob job = new LoadEffectivePomJob(Messages.MavenPomEditor_loading);
    job.schedule();
  }

  /**
   * @return
   */
  public IEditorInput getEffectivePomEditorInput() {
    //put a msg in the editor saying that the effective pom is loading, in case this is a long running job
    if(effectivePomEditorInput == null) {
      String content = Messages.MavenPomEditor_loading;
      String name = Messages.MavenPomEditor_effective + getPartName();
      effectivePomEditorInput = new StaticMavenStorageEditorInput(name, name, null,
          content.getBytes(StandardCharsets.UTF_8));
    }
    return effectivePomEditorInput;
  }

  public boolean isReadOnly() {
    return !(getEditorInput() instanceof IFileEditorInput);
  }

  private int addPomPage(IFormPage page) {
    try {
      if(page instanceof MavenPomEditorPage pomPage) {
        mavenpomEditorPages.add(pomPage);
      }
      if(page instanceof IPomFileChangedListener listener) {
        fileChangeListeners.add(listener);
      }
      return addPage(page);
    } catch(PartInitException ex) {
      log.error(ex.getMessage(), ex);
      return -1;
    }
  }

  public synchronized org.eclipse.aether.graph.DependencyNode readDependencyTree(boolean force, String classpath,
      IProgressMonitor monitor) throws CoreException {
    if(force || !rootNodes.containsKey(classpath)) {
      monitor.setTaskName(Messages.MavenPomEditor_task_reading);
      //mkleint: I'm wondering if the force parameter on dependencyTree is also applicable to the pom project method.
      MavenProject mavenProject = readMavenProject(force, monitor);
      if(mavenProject == null) {
        log.error("Unable to read maven project. Dependencies not updated."); //$NON-NLS-1$
        return null;
      }

      IMavenProjectFacade facade = null;
      if(pomFile != null && new Path(IMavenConstants.POM_FILE_NAME).equals(pomFile.getProjectRelativePath())) {
        facade = MavenPlugin.getMavenProjectRegistry().getProject(pomFile.getProject());
      }

      DependencyNode root = MavenPlugin.getMavenModelManager().readDependencyTree(facade, mavenProject, classpath,
          monitor);
      root.setData("LEVEL", "ROOT");
      for(DependencyNode nd : root.getChildren()) {
        nd.setData("LEVEL", "DIRECT");
      }
      rootNodes.put(classpath, root);
    }

    return rootNodes.get(classpath);
  }

  /**
   * this method is safer than readMavenProject for instances that shall return fast and don't mind not having the
   * MavenProject instance around.
   *
   * @return the cached MavenProject instance or null if not loaded.
   */
  public MavenProject getMavenProject() {
    return mavenProject;
  }

  /**
   * either returns the cached MavenProject instance or reads it, please note that if you want your method to always
   * return fast getMavenProject() is preferable please see <code>mavenProjectChanged()</code> for explanation why even
   * force==true might not give you the latest uptodate MavenProject instance matching the current saved file in some
   * circumstances.
   *
   * @param force
   * @param monitor
   * @return
   * @throws CoreException
   */
  public MavenProject readMavenProject(boolean force, IProgressMonitor monitor) throws CoreException {
    if(force || mavenProject == null) {

      IEditorInput input = getEditorInput();

      if(input instanceof IFileEditorInput fileInput) {
        pomFile = fileInput.getFile();
        pomFile.refreshLocal(1, null);
      }

      //never overwrite by null, rather keep old value than null..
      MavenProject prj = SelectionUtil.getMavenProject(input, monitor);
      if(prj != null) {
        mavenProject = prj;
      }
    }
    return mavenProject;
  }

  @Override
  public void dispose() {
    disposed = true;

    if(sourceDocument != null) {
      if(closeEditorOnDocumentDeletion != null && documentProvider != null) {
        documentProvider.removeElementStateListener(closeEditorOnDocumentDeletion);
      }
      if(documentListener != null) {
        sourceDocument.removeDocumentListener(documentListener);
      }
    }
    sourceDocument = null;
    // should properly dispose the document
    sourcePage.dispose();

    MavenPluginActivator.getDefault().getMavenProjectManager().removeMavenProjectChangedListener(this);

    if(structuredModel != null) { //#336331
      structuredModel.releaseFromEdit();
    }

    if(activationListener != null) {
      activationListener.dispose();
      activationListener = null;
    }

    ResourcesPlugin.getWorkspace().removeResourceChangeListener(MavenPomEditor.this);

    super.dispose();
  }

  /**
   * Saves structured editor XXX form model need to be synchronized
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
    resourceChangeEventSkip = true;
    try {
      sourcePage.doSave(monitor);
    } finally {
      resourceChangeEventSkip = false;
    }
  }

  @Override
  public void doSaveAs() {
    // IEditorPart editor = getEditor(0);
    // editor.doSaveAs();
    // setPageText(0, editor.getTitle());
    // setInput(editor.getEditorInput());
  }

  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  @Override
  public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {

    setPartName(editorInput.getToolTipText());
    // setContentDescription(name);
    super.init(site, editorInput);
    if(editorInput instanceof IFileEditorInput) {
      ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    reloadMavenProjectCache();
    IMavenProjectRegistry projectRegistry = MavenPluginActivator.getDefault().getMavenProjectManager();
    projectRegistry.addMavenProjectChangedListener(this);

    activationListener = new MavenPomActivationListener(site.getWorkbenchWindow().getPartService());
  }

  public void showInSourceEditor(EObject o) {
    IDOMElement element = getElement(o);
    if(element != null) {
      int start = element.getStartOffset();
      int lenght = element.getLength();
      setActivePage(sourcePageIndex);
      sourcePage.selectAndReveal(start, lenght);
    }
  }

  public IDOMElement getElement(EObject o) {
    for(Adapter adapter : o.eAdapters()) {
      if(adapter instanceof EMF2DOMSSEAdapter a) {
        if(a.getNode() instanceof IDOMElement element) {
          return element;
        }
        break;
      }
    }
    return null;
  }

  // IShowEditorInput

  @Override
  public void showEditorInput(IEditorInput editorInput) {
    // could activate different tabs based on the editor input
  }

  // IGotoMarker

  @Override
  public void gotoMarker(IMarker marker) {
    // TODO use selection to activate corresponding form page elements
    setActivePage(sourcePageIndex);
    IGotoMarker adapter = sourcePage.getAdapter(IGotoMarker.class);
    adapter.gotoMarker(marker);
  }

  // ISearchEditorAccess

  @Override
  public IDocument getDocument(Match match) {
    return sourcePage.getDocumentProvider().getDocument(getEditorInput());
  }

  @Override
  public IAnnotationModel getAnnotationModel(Match match) {
    return sourcePage.getDocumentProvider().getAnnotationModel(getEditorInput());
  }

  @Override
  public boolean isDirty() {
    return sourcePage.isDirty();
  }

  /**
   * returns only the pages that implement MavenPomEditorPage will not return the effective pom and xml editor page for
   * example..
   *
   * @return
   */
  public List<MavenPomEditorPage> getMavenPomEditorPages() {
    return mavenpomEditorPages;
  }

  public void showDependencyHierarchy(ArtifactKey artifactKey) {
    setActivePage(dependencyTreePage.getId());
    dependencyTreePage.selectDepedency(artifactKey);
  }

  private boolean checkedWritableStatus;

  private boolean readOnly;

  private IDocumentProvider documentProvider;

  private IElementStateListener closeEditorOnDocumentDeletion;

  /**
   * read/write check for read only pom files -- called when the file is opened and will validateEdit -- so files will
   * be checked out of src control, etc Note: this is actually done separately from isReadOnly() because there are 2
   * notions of 'read only' for a POM. The first is for a file downloaded from a repo, like maven central. That one is
   * never editable. The second is for a local file that is read only because its been marked that way by an SCM, etc.
   * This method will do a one-time check/validateEdit for the life of the POM editor.
   **/
  protected boolean checkReadOnly() {
    if(isCheckedWritableStatus()) {
      return readOnly;
    }
    setCheckedWritableStatus(true);
    if(getPomFile() != null && getPomFile().isReadOnly()) {
      IStatus validateEdit = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {getPomFile()},
          getEditorSite().getShell());
      if(!validateEdit.isOK()) {
        readOnly = true;
      } else {
        readOnly = isReadOnly();
      }
    } else {
      readOnly = isReadOnly();
    }
    return readOnly;
  }

  /**
   * Adapted from <code>org.eclipse.ui.texteditor.AbstractTextEditor.ActivationListener</code>
   */
  class MavenPomActivationListener implements IPartListener, IWindowListener {

    private IWorkbenchPart activePart;

    private boolean isHandlingActivation = false;

    public MavenPomActivationListener(IPartService partService) {
      partService.addPartListener(this);
      PlatformUI.getWorkbench().addWindowListener(this);
    }

    public void dispose() {
      getSite().getWorkbenchWindow().getPartService().removePartListener(this);
      PlatformUI.getWorkbench().removeWindowListener(this);
    }

    // IPartListener

    @Override
    public void partActivated(IWorkbenchPart part) {
      activePart = part;
      handleActivation();
      checkReadOnly();
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
      activePart = null;
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
    }

    // IWindowListener

    @Override
    public void windowActivated(IWorkbenchWindow window) {
      if(window == getEditorSite().getWorkbenchWindow()) {
        /*
         * Workaround for problem described in
         * http://dev.eclipse.org/bugs/show_bug.cgi?id=11731
         * Will be removed when SWT has solved the problem.
         */
        window.getShell().getDisplay().asyncExec(() -> handleActivation());
      }
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
    }

    /**
     * Handles the activation triggering a element state check in the editor.
     */
    void handleActivation() {
      if(isHandlingActivation) {
        return;
      }

      if(activePart == MavenPomEditor.this) {
        isHandlingActivation = true;
        try {

          if(sourcePage != null) {
            sourcePage.setFocus();
            //sourcePage.update();
          }

        } finally {
          isHandlingActivation = false;

        }
      }
    }
  }

  public ITextEditor getSourcePage() {
    return sourcePage;
  }

  @Override
  public IFormPage setActivePage(String pageId) {
    if(pageId == null) {
      setActivePage(sourcePageIndex);
    }
    return super.setActivePage(pageId);
  }

  @Override
  public <T> T getAdapter(Class<T> adapter) {
    if(MavenProject.class.equals(adapter)) {
      return adapter.cast(getMavenProject());
    }

    T result = super.getAdapter(adapter);
    if(result != null && Display.getCurrent() == null) {
      return result;
    }
    return adapter.cast(sourcePage.getAdapter(adapter));
  }

  public IFile getPomFile() {
    return pomFile;
  }

  private void reloadMavenProjectCache() {
    //reload the cached MavenProject instance here.
    Job jb = new Job("reload maven project") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          //we're not interested in the result, just want to get the MP instance cached.
          readMavenProject(true, monitor);
        } catch(CoreException e) {
          log.error("failed to load maven project for " + getEditorInput(), e);
        }
        return Status.OK_STATUS;
      }
    };
    jb.setSystem(true);
    jb.schedule();
  }

  /**
   * you may be asking why we have this method here.. sit back, relax and let me tell you the epic story of it.. 1. we
   * attempt to keep an instance of MavenProject instance around - to make queries from xml editor and elsewhere easy
   * and *fast*. 2. in init() we read it and store 3. however how do we update the value when stuff changes? 4. only
   * IFileEditorInputs are really update-able, everything else is a read-only editor. 5. so how do we listen on the
   * resource being changed and update the value accordingly? it appears that Selectionutil.getMavenProject(EditorInput)
   * relies on MavenProjectManager.create() which appears to have rather tricky behaviour. It either gives you the
   * cached instance or if not around creates one on the fly. 5a. The fly one is however not added to the cache. As a
   * consequence the fly ones are always recreated each time you ask. 5b. The cached ones react quite in opposite way,
   * no matter when you ask (even if you know the file has changed) you always get the cached value until the registry
   * gets updated. 6. so to keep our MavenProject instance uptodate for both 5a and 5b cases, we need to: 6a. listen on
   * workspace resources and try loading the MavenProject. for 5a it will load the new instance but for 5b it will keep
   * returning the old value. 6b. so we also listen on MavenProjectChangedEvents and for 5b cases get the correct, fresh
   * new MavenProject instance here. 7. please note that 6a comes before 6b and is done for both 5a and 5b as it's hard
   * to tell those IMavenprojectfacade instances apart.. Your storyteller for tonite was mkleint
   */

  @Override
  public void mavenProjectChanged(List<MavenProjectChangedEvent> events, IProgressMonitor monitor) {
    IEditorInput input = getEditorInput();
    if(input instanceof IFileEditorInput fileinput) {
      for(MavenProjectChangedEvent event : events) {
        if(fileinput.getFile().equals(event.getSource())) {
          IMavenProjectFacade facade = event.getMavenProject();
          if(facade != null) {
            MavenProject mp = facade.getMavenProject();
            if(mp != null) {
              mavenProject = mp;
              if(getContainer() != null && !getContainer().isDisposed())
                getContainer().getDisplay().asyncExec(() -> {
                  for(MavenPomEditorPage page : getMavenPomEditorPages()) {
                    page.mavenProjectHasChanged();
                  }
                });
            }
          }
        }
      }
    }
  }

  /*
   * @see org.eclipse.ui.part.MultiPageEditorPart#setPageText(int, java.lang.String)
   */
  @Override
  public void setPageText(int pageIndex, String text) {
    super.setPageText(pageIndex, text);
  }

  /**
   * @return Returns the checkedWritableStatus.
   */
  public boolean isCheckedWritableStatus() {
    return checkedWritableStatus;
  }

  /**
   * @param checkedWritableStatus The checkedWritableStatus to set.
   */
  public void setCheckedWritableStatus(boolean checkedWritableStatus) {
    this.checkedWritableStatus = checkedWritableStatus;
  }

  /**
   * @return Returns the effectivePomSourcePage.
   */
  public ITextEditor getEffectivePomSourcePage() {
    return effectivePomSourcePage;
  }

  /**
   * @param sourcePage The sourcePage to set.
   */
  public void setSourcePage(ITextEditor sourcePage) {
    this.sourcePage = sourcePage;
    this.sourcePageIndex = pages.indexOf(sourcePage);
  }

  /**
   * @param effectivePomSourcePage The effectivePomSourcePage to set.
   */
  public void setEffectivePomSourcePage(ITextEditor effectivePomSourcePage) {
    this.effectivePomSourcePage = effectivePomSourcePage;
  }

  @Override
  public void close(boolean save) {
    if(disposed) {
      return;
    }
    super.close(save);
  }
}
