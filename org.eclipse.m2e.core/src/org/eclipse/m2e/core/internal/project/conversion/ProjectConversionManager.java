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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @since 1.1
 */
public class ProjectConversionManager implements IProjectConversionManager {

  private static final String CONVERSION_PARTICIPANTS_EXTENSION_POINT = "org.eclipse.m2e.core.projectConversionParticipants";

  private static final Logger log = LoggerFactory.getLogger(ProjectConversionManager.class);

  private static List<AbstractProjectConversionParticipant> lookupConversionParticipants(IProject project) {
    List<AbstractProjectConversionParticipant> participants = new ArrayList<AbstractProjectConversionParticipant>();

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint conversionExtensionPoint = registry.getExtensionPoint(CONVERSION_PARTICIPANTS_EXTENSION_POINT);
    Map<String, Set<String>> restrictedPackagings = new HashMap<String, Set<String>>();
    if(conversionExtensionPoint != null) {
      IExtension[] archetypesExtensions = conversionExtensionPoint.getExtensions();
      for(IExtension extension : archetypesExtensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          if("projectConversionParticipant".equals(element.getName())) {
            try {
              if(project.hasNature(element.getAttribute("nature"))) {
                AbstractProjectConversionParticipant projectConversionParticipant = (AbstractProjectConversionParticipant) element
                    .createExecutableExtension("class");
                participants.add(projectConversionParticipant);
              }
            } catch(CoreException ex) {
              log.debug("Can not load IProjectConversionParticipant", ex);
            }
          } else if("conversionParticipantConfiguration".equals(element.getName())) {
            setRestrictedPackagings(restrictedPackagings, element);
          }
        }
      }

      for(AbstractProjectConversionParticipant cp : participants) {
        Set<String> newPackagings = restrictedPackagings.get(cp.getId());
        if(newPackagings != null) {
          for(String p : newPackagings) {
            cp.addRestrictedPackaging(p);
          }
        }
      }
    }
    return participants;
  }

  private static void setRestrictedPackagings(Map<String, Set<String>> restrictedPackagings,
      IConfigurationElement element) {
    String pid = element.getAttribute("conversionParticipantId");
    String packagesAsString = element.getAttribute("compatiblePackagings");
    if(pid != null && packagesAsString != null) {
      try {
        String[] packagingsArray = packagesAsString.split(",");
        Set<String> packagings = new HashSet<String>(packagingsArray.length);
        for(String packaging : packagingsArray) {
          String p = packaging.trim();
          if(p.length() > 0) {
            packagings.add(p);
          }
        }

        Set<String> allPackages = restrictedPackagings.get(pid);
        if(allPackages == null) {
          allPackages = new HashSet<String>();
          restrictedPackagings.put(pid, allPackages);
        }

        allPackages.addAll(packagings);

      } catch(Exception e) {
        log.debug("Cannot parse restricted packagings ", e);
      }
    }
  }

  public void convert(IProject project, Model model, IProgressMonitor monitor) throws CoreException {
    if(model == null) {
      return;
    }
    List<AbstractProjectConversionParticipant> participants = getConversionParticipants(project, model.getPackaging());
    if(participants != null) {
      for(AbstractProjectConversionParticipant participant : participants) {
        participant.convert(project, model, monitor);
      }
    }
  }

  @Deprecated
  public List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project) throws CoreException {
    return getConversionParticipants(project, null);
  }

  public List<AbstractProjectConversionParticipant> getConversionParticipants(IProject project, String packaging)
      throws CoreException {
    List<AbstractProjectConversionParticipant> allParticipants = lookupConversionParticipants(project);
    List<AbstractProjectConversionParticipant> participants = new ArrayList<AbstractProjectConversionParticipant>();
    if(allParticipants != null) {
      for(AbstractProjectConversionParticipant participant : allParticipants) {
        if(packaging != null && !participant.isPackagingCompatible(packaging)) {
          continue;
        }
        if(participant.accept(project)) {
          participants.add(participant);
        }
      }
    }
    return Collections.unmodifiableList(participants);
  }

}
