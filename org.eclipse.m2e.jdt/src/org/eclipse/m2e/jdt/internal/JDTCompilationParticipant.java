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

package org.eclipse.m2e.jdt.internal;

import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.core.embedder.IMavenConfiguration;
import org.eclipse.m2e.jdt.MavenJdtPlugin;


public class JDTCompilationParticipant extends CompilationParticipant {
  
  public static boolean DEBUG = MavenPlugin.getDefault().isDebugging()
      & Boolean.parseBoolean(Platform.getDebugOption(MavenJdtPlugin.PLUGIN_ID + "/debug/compilationParticipant")); //$NON-NLS-1$
  
  @Override
  public synchronized void cleanStarting(IJavaProject project) {
    if(DEBUG) {
      System.out.println("\nRequested clean for " + project.getProject() //$NON-NLS-1$
          + " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
    }

    MavenPlugin plugin = MavenPlugin.getDefault();
    IMavenConfiguration configuration = MavenPlugin.getDefault().getMavenConfiguration();
    if (configuration.isDebugOutput()) {
      MavenConsole console = plugin.getConsole();
      console.logMessage("Maven compilation participant: clean starting");
    }

    super.cleanStarting(project);

    try {
      plugin.getMavenProjectManager().requestFullMavenBuild(project.getProject());
    } catch(CoreException ex) {
      MavenLogger.log("Exception requesting full Maven build", ex);
    }
  }

  public void buildStarting(BuildContext[] files, boolean isBatch) {
  }
  
  // 3.4
  public void buildFinished(IJavaProject project) {
  }
  
  public boolean isActive(IJavaProject project) {
    return true;
  }
  
}
