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

package org.eclipse.m2e.internal.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Eugene Kuleshov
 */
public class MavenSourceLocator extends AbstractSourceLookupDirector {
  private static final Logger log = LoggerFactory.getLogger(MavenSourceLocator.class);

  public void initializeParticipants() {
    List<ISourceLookupParticipant> participants = new ArrayList<ISourceLookupParticipant>();

    // TODO is it possible to avoid unconditional activation of all registered participants?

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry.getExtensionPoint("org.eclipse.m2e.launching.sourceLookupParticipants");
    if(extensionPoint != null) {
      IExtension[] extensions = extensionPoint.getExtensions();
      for(IExtension extension : extensions) {
        IConfigurationElement[] elements = extension.getConfigurationElements();
        for(IConfigurationElement element : elements) {
          try {
            participants.add((ISourceLookupParticipant) element.createExecutableExtension("class"));
          } catch(CoreException ex) {
            log.debug("Problem with external extension point", ex);
          }
        }
      }
    }

    participants.add(new JavaSourceLookupParticipant());

    addParticipants(participants.toArray(new ISourceLookupParticipant[participants.size()]));
  }
}
