/*******************************************************************************
 * Copyright (c) 2013 Igor Fedorenko and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.ui.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;


/**
 * Helpers to query and manipulate workbench working sets.
 * 
 * @since 1.5
 */
public class WorkingSets {

  /**
   * Returns all visible workbench working sets.
   * 
   * @since 1.5
   */
  public static String[] getWorkingSets() {
    List<String> workingSets = new ArrayList<String>();
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      if(workingSet.isVisible()) {
        workingSets.add(workingSet.getName());
      }
    }
    return workingSets.toArray(new String[workingSets.size()]);
  }

  /**
   * Returns existing or creates new workbench working set with the given name
   * 
   * @since 1.5
   */
  public static IWorkingSet getOrCreateWorkingSet(String workingSetName) {
    IWorkingSetManager wsm = PlatformUI.getWorkbench().getWorkingSetManager();
    IWorkingSet workingSet = wsm.getWorkingSet(workingSetName);
    if(workingSet == null) {
      workingSet = wsm.createWorkingSet(workingSetName, new IAdaptable[0]);
      // TODO is there a constant we should be setting here?
      workingSet.setId("org.eclipse.ui.resourceWorkingSetPage");
      wsm.addWorkingSet(workingSet);
    }
    return workingSet;
  }

  /**
   * Adds given projects to workbench working set with the given name. Creates new working set if workbench working set
   * with given name does not already exist.
   * 
   * @since 1.5
   */
  public static void addToWorkingSet(IProject[] projects, String workingSetName) {
    IWorkingSet[] workingSets = new IWorkingSet[] {getOrCreateWorkingSet(workingSetName)};
    IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IProject project : projects) {
      manager.addToWorkingSets(project, workingSets);
    }
  }

  /**
   * Adds given projects to given workbench working sets.
   * 
   * @since 1.5
   */
  public static void addToWorkingSets(IProject[] projects, List<IWorkingSet> workingSets) {
    // PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(project, new IWorkingSet[] {workingSet});
    if(projects != null && projects.length > 0 && workingSets != null && !workingSets.isEmpty()) {
      for(IWorkingSet workingSet : workingSets) {
        if(workingSet != null) {
          IAdaptable[] adaptedProjects = workingSet.adaptElements(projects);
          IAdaptable[] oldElements = workingSet.getElements();

          Set<IAdaptable> newElements = new LinkedHashSet<>();
          Collections.addAll(newElements, oldElements);
          Collections.addAll(newElements, adaptedProjects);
          workingSet.setElements(newElements.toArray(new IAdaptable[newElements.size()]));
        }
      }
    }
  }

  /**
   * Adds given projects to given workbench working sets.
   * 
   * @since 1.5
   */
  public static void addToWorkingSets(Collection<IProject> projects, List<IWorkingSet> workingSets) {
    addToWorkingSets(projects.toArray(new IProject[projects.size()]), workingSets);
  }

  /**
   * Returns all projects that belong to workbench working sets.
   * 
   * @since 1.5
   */
  public static Set<IProject> getProjects() {
    Set<IProject> projects = new HashSet<IProject>();
    IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : manager.getAllWorkingSets()) {
      try {
        for(IAdaptable element : workingSet.getElements()) {
          IProject project = element.getAdapter(IProject.class);
          if(project != null) {
            projects.add(project);
          }
        }
      } catch(IllegalStateException ignored) {
        // ignore bad/misconfigured working sets
      }
    }
    return projects;
  }

  /**
   * Returns one of the working sets the element directly belongs to. Returns {@code null} if the element does not
   * belong to any working set.
   * 
   * @since 1.5
   */
  public static IWorkingSet getAssignedWorkingSet(IResource element) {
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      for(IAdaptable adaptable : workingSet.getElements()) {
        if(adaptable.getAdapter(IResource.class) == element) {
          return workingSet;
        }
      }
    }
    return null;
  }

  /**
   * Returns all working sets the element directly belongs to. Returns empty collection if the element does not belong
   * to any working set. The order of returned working sets is not specified.
   * 
   * @since 1.5
   */
  public static List<IWorkingSet> getAssignedWorkingSets(IResource element) {
    List<IWorkingSet> list = new ArrayList<IWorkingSet>();
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    for(IWorkingSet workingSet : workingSetManager.getWorkingSets()) {
      for(IAdaptable adaptable : workingSet.getElements()) {
        if(adaptable.getAdapter(IResource.class) == element) {
          list.add(workingSet);
        }
      }
    }
    return list;
  }

}
