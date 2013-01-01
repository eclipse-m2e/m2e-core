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

package org.eclipse.m2e.scm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.scm.internal.Messages;
import org.eclipse.m2e.scm.internal.ScmHandlerFactory;
import org.eclipse.m2e.scm.spi.ScmHandler;


/**
 * Checkout operation
 * 
 * @author Eugene Kuleshov
 */
public class MavenCheckoutOperation {
  private static final Logger log = LoggerFactory.getLogger(MavenCheckoutOperation.class);

  private final File location;

  private final Collection<MavenProjectScmInfo> mavenProjects;

  private final List<String> locations = new ArrayList<String>();

  public MavenCheckoutOperation(File location, Collection<MavenProjectScmInfo> mavenProjects) {
    this.location = location;
    this.mavenProjects = mavenProjects;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException, CoreException {
    List<MavenProjectScmInfo> flatProjects = new ArrayList<MavenProjectScmInfo>();

    // sort nested projects
    for(MavenProjectScmInfo info : mavenProjects) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      String folderUrl = info.getFolderUrl();

      monitor
          .setTaskName(NLS.bind(Messages.MavenCheckoutOperation_task_scanning, info.getLabel(), info.getFolderUrl()));

      // XXX check if projects already exist
      boolean isNestedPath = false;
      for(MavenProjectScmInfo info2 : mavenProjects) {
        if(info != info2) {
          String path = info2.getFolderUrl();
          if(folderUrl.startsWith(path + "/")) { //$NON-NLS-1$
            isNestedPath = true;
            break;
          }
        }
      }
      if(!isNestedPath) {
        flatProjects.add(info);
      }
    }

    for(MavenProjectScmInfo info : flatProjects) {
      if(monitor.isCanceled()) {
        throw new InterruptedException();
      }

      monitor
          .setTaskName(NLS.bind(Messages.MavenCheckoutOperation_task_checking, info.getLabel(), info.getFolderUrl()));

      // XXX if location is pointing to workspace folder need to create unique dir too 
      File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
      File location = getUniqueDir(this.location == null ? workspaceRoot : this.location);

      ScmHandler handler = ScmHandlerFactory.getHandler(info.getFolderUrl());
      if(handler == null) {
        String msg = "SCM provider is not available for " + info.getFolderUrl();
        log.error(msg);
      } else {
        handler.checkoutProject(info, location, monitor);
        locations.add(location.getAbsolutePath());
      }
    }
  }

  protected File getUniqueDir(File baseDir) {
    long suffix = System.currentTimeMillis();
    while(true) {
      File tempDir = new File(baseDir, "maven." + suffix); //$NON-NLS-1$
      if(!tempDir.exists()) {
        return tempDir;
      }
      suffix++ ;
    }
  }

  /**
   * @return Returns collection of {@link MavenProjectScmInfo}
   */
  public Collection<MavenProjectScmInfo> getMavenProjects() {
    return this.mavenProjects;
  }

  /**
   * @return Returns list of <code>String</code> paths for the checked out locations
   */
  public List<String> getLocations() {
    return this.locations;
  }

}
