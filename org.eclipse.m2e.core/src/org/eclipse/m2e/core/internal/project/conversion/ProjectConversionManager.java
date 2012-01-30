/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal.project.conversion;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;
import org.eclipse.m2e.core.project.conversion.IProjectConversionManager;


/**
 * Manages conversion of existing Eclipse projects into Maven ones. <br/>
 * Looks up for {@link AbstractProjectConversionParticipant} contributed by 3rd party eclipse plugins.
 * 
 * @author Fred Bricon
 */
public class ProjectConversionManager implements IProjectConversionManager {

  private static final String CONVERSION_PARTICIPANTS_EXTENSION_POINT = "org.eclipse.m2e.core.projectConversionParticipants";

  private static final Logger log = LoggerFactory.getLogger(ProjectConversionManager.class);

  private static List<AbstractProjectConversionParticipant> lookupConversionParticipants(IProject project) {
    List<AbstractProjectConversionParticipant> participants = new ArrayList<AbstractProjectConversionParticipant>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint conversionExtensionPoint = registry.getExtensionPoint(CONVERSION_PARTICIPANTS_EXTENSION_POINT);
    if(conversionExtensionPoint != null) {
      IExtension[] archetypesExtensions = conversionExtensionPoint.getExtensions();
      for(IExtension extension : archetypesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          try {
            if(project.hasNature(element.getAttribute("nature"))) {
              participants.add((AbstractProjectConversionParticipant) element.createExecutableExtension("class"));
            }
          } catch(CoreException ex) {
            log.debug("Can not load IProjectConversionParticipant", ex);
          }
        }
      }
    }
    return participants;
  }

  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if(model == null) {
      return;
    }
    List<AbstractProjectConversionParticipant> participants = getConversionParticipants(project);
    if(participants != null) {
      for(AbstractProjectConversionParticipant participant : participants) {
        participant.convert(project, model, monitor);
      }
    }
  }

  public List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project) throws CoreException {
    List<AbstractProjectConversionParticipant> allParticipants = lookupConversionParticipants(project);
    List<AbstractProjectConversionParticipant> participants = new ArrayList<AbstractProjectConversionParticipant>();
    if(allParticipants != null) {
      for(AbstractProjectConversionParticipant participant : allParticipants) {
        if(participant.accept(project)) {
          participants.add(participant);
        }
      }
    }
    return participants;
  }

}
