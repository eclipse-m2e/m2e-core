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

package org.eclipse.m2e.refactoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameJavaProjectProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectManager;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.PropertyElement;
import org.eclipse.m2e.refactoring.RefactoringModelResources.PropertyInfo;
import org.eclipse.m2e.refactoring.internal.Activator;
import org.eclipse.osgi.util.NLS;


/**
 * Base class for all pom.xml refactorings in workspace
 * 
 * @author Anton Kraev
 */
@SuppressWarnings("restriction")
public abstract class AbstractPomRefactoring extends Refactoring {

  protected static final String PROBLEMS_DURING_REFACTORING = Messages.AbstractPomRefactoring_error;

  // main file that is being refactored
  protected IFile file;

  // maven plugin
  protected MavenPlugin mavenPlugin;

  // editing domain
  protected AdapterFactoryEditingDomain editingDomain;

  private HashMap<String, RefactoringModelResources> models;

  public AbstractPomRefactoring(IFile file) {
    this.file = file;

    this.mavenPlugin = MavenPlugin.getDefault();

    List<AdapterFactoryImpl> factories = new ArrayList<AdapterFactoryImpl>();
    factories.add(new ResourceItemProviderAdapterFactory());
    factories.add(new ReflectiveItemProviderAdapterFactory());

    ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(factories);
    BasicCommandStack commandStack = new BasicCommandStack();
    this.editingDomain = new AdapterFactoryEditingDomain(adapterFactory, //
        commandStack, new HashMap<Resource, Boolean>());
  }

  // this gets actual refactoring visitor
  public abstract PomVisitor getVisitor();

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    return new RefactoringStatus();
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    CompositeChange res = new CompositeChange(getTitle());
    IMavenProjectFacade[] projects = mavenPlugin.getMavenProjectManager().getProjects();
    pm.beginTask(Messages.AbstractPomRefactoring_task, projects.length);

    models = new HashMap<String, RefactoringModelResources>();

    try {
      // load all models
      // XXX: assumption: artifactId is unique within workspace
      for(IMavenProjectFacade projectFacade : projects) {
        // skip "other" projects if not requested
        if(!scanAllArtifacts() && !projectFacade.getPom().equals(file)) {
          continue;
        }

        // skip closed projects
        if(!projectFacade.getProject().isAccessible() || !projectFacade.getPom().isAccessible()) {
          continue;
        }

        loadModel(projectFacade, pm);
      }

      // construct properties for all models
      for(IMavenProjectFacade projectFacade : projects) {
        RefactoringModelResources model = models.get(projectFacade.getArtifactKey().getArtifactId());
        if(model == null) {
          continue;
        }

        Map<String, PropertyInfo> properties = new HashMap<String, PropertyInfo>();

        // find all workspace parents
        List<RefactoringModelResources> workspaceParents = new ArrayList<RefactoringModelResources>();
        MavenProject current = model.getProject();
        // add itself
        workspaceParents.add(model);
        for(MavenProject parentProject = getParentProject(projectFacade, current, pm); parentProject != null;) {
          String id = parentProject.getArtifactId();
          RefactoringModelResources parent = models.get(id);
          if(parent != null) {
            workspaceParents.add(parent);
          } else {
            break;
          }
          parentProject = getParentProject(projectFacade, parentProject, pm);
        }

        //fill properties (from the root)
        for(int i = workspaceParents.size() - 1; i >= 0; i-- ) {
          RefactoringModelResources resource = workspaceParents.get(i);
          EList<PropertyElement> props = resource.getTmpModel().getProperties();
          if(props == null)
            continue;
          Iterator<?> it = props.iterator();
          while(it.hasNext()) {
            PropertyElement pair = (PropertyElement) it.next();
            String pName = pair.getName();
            PropertyInfo info = properties.get(pName);
            if(info == null) {
              info = new PropertyInfo();
              properties.put(pName, info);
            }
            info.setPair(pair);
            info.setResource(resource);
          }
        }

        model.setProperties(properties);
      }

      // calculate the list of affected models
      for(String artifact : models.keySet()) {
        RefactoringModelResources model = models.get(artifact);
        model.setCommand(getVisitor().applyChanges(model, pm));
      }

      // process all refactored properties, creating more commands
      for(String artifact : models.keySet()) {
        RefactoringModelResources model = models.get(artifact);

        if(model.getProperties() == null) {
          continue;
        }

        for(String pName : model.getProperties().keySet()) {
          PropertyInfo info = model.getProperties().get(pName);
          if(info.getNewValue() != null) {
            CompoundCommand command = info.getResource().getCommand();
            if(command == null) {
              command = new CompoundCommand();
              info.getResource().setCommand(command);
            }
            command.append(info.getNewValue());
          }
        }
      }

      // process the file itself first
      for(String artifact : models.keySet()) {
        RefactoringModelResources model = models.get(artifact);
        if(model.getPomFile().equals(file)) {
          processCommand(model, res);
          model.releaseAllResources();
          models.remove(artifact);
          break;
        }
      }

      // process others
      for(String artifact : models.keySet()) {
        processCommand(models.get(artifact), res);
      }

      // rename project if required
      // TODO probably should copy relevant classes from internal packages
      String newName = getNewProjectName();
      if(newName != null) {
        RenameJavaProjectProcessor processor = new RenameJavaProjectProcessor(JavaCore.create(file.getProject()));
        RenameRefactoring refactoring = new RenameRefactoring(processor);
        processor.setNewElementName(newName);
        RefactoringStatus tmp = new RefactoringStatus();
        tmp.merge(refactoring.checkInitialConditions(pm));
        if(!tmp.hasFatalError()) {
          tmp.merge(refactoring.checkFinalConditions(pm));
          if(!tmp.hasFatalError()) {
            res.add(refactoring.createChange(pm));
          }
        }
      }
    } catch(final PomRefactoringException ex) {
      return new Change() {
        public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
          return RefactoringStatus.createFatalErrorStatus(ex.getStatus().getMessage());
        }
        public Object getModifiedElement() {
          return null;
        }
        public String getName() {
          return ex.getStatus().getMessage();
        }
        public void initializeValidationData(IProgressMonitor pm) {
        }
        public Change perform(IProgressMonitor pm) throws CoreException {
          return null;
        }
        public boolean isEnabled() {
          return false;
        }
      };
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, PROBLEMS_DURING_REFACTORING, ex));
    } finally {
      for(String artifact : models.keySet()) {
        models.get(artifact).releaseAllResources();
      }
      RefactoringModelResources.cleanupTmpProject();
    }

    return res;
  }

  protected MavenProject getParentProject(IMavenProjectFacade project, MavenProject current, IProgressMonitor monitor)
      throws CoreException {
    IMaven maven = mavenPlugin.getMaven();
    MavenProjectManager projectManager = mavenPlugin.getMavenProjectManager();

    MavenExecutionRequest request = projectManager.createExecutionRequest(project.getPom(),
        project.getResolverConfiguration(), monitor);

    return maven.resolveParentProject(request, current, monitor);
  }

  // title for a composite change
  public abstract String getTitle();

  protected RefactoringModelResources loadModel(IMavenProjectFacade projectFacade, IProgressMonitor pm)
      throws CoreException, IOException {
    pm.setTaskName(NLS.bind(Messages.AbstractPomRefactoring_loading, projectFacade.getProject().getName()));
    RefactoringModelResources current = new RefactoringModelResources(projectFacade);
    models.put(current.effective.getArtifactId(), current);
    pm.worked(1);
    return current;
  }

  // this method determines whether all artifacts will be sent to visitor or only main one
  public abstract boolean scanAllArtifacts();

  protected void processCommand(RefactoringModelResources model, CompositeChange res) throws Exception {
    CompoundCommand command = model.getCommand();
    if(command == null) {
      return;
    }
    if(command.canExecute()) {
      // apply changes to temp file
      editingDomain.getCommandStack().execute(command);
      // create text change comparing temp file and real file
      TextFileChange change = new ChangeCreator(model.getPomFile(), model.getPomBuffer().getDocument(), model
          .getTmpBuffer().getDocument(), file.getParent().getName()).createChange();
      res.add(change);
    }
  }

  // returns new eclipse project name or null if no change
  public String getNewProjectName() {
    return null;
  }

  public Model createModel() {
    try {
      Resource resource = MavenPlugin.getDefault().getMavenModelManager().loadResource(file);
      return (Model) resource.getContents().get(0);
    } catch(CoreException ex) {
      MavenLogger.log(PROBLEMS_DURING_REFACTORING, ex);
      return null;
    }
  }
}
