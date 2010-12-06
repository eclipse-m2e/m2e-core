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

package org.eclipse.m2e.core.project;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import org.eclipse.m2e.core.core.IMavenConstants;
import org.eclipse.m2e.core.core.MavenConsole;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class LocalProjectScanner extends AbstractProjectScanner<MavenProjectInfo> {
  private final File workspaceRoot;
  private final List<String> folders;
  private final boolean basedirRemameRequired;

  private Set<File> scannedFolders = new HashSet<File>();
  private final MavenConsole console;
  private final MavenModelManager modelManager;

  public LocalProjectScanner(File workspaceRoot, String folder, boolean needsRename, MavenModelManager modelManager,
      MavenConsole console) {
    this(workspaceRoot, Collections.singletonList(folder), needsRename, modelManager, console);
  }

  public LocalProjectScanner(File workspaceRoot, List<String> folders, boolean basedirRemameRequired,
      MavenModelManager modelManager, MavenConsole console) {
    this.workspaceRoot = workspaceRoot;
    this.folders = folders;
    this.basedirRemameRequired = basedirRemameRequired;
    this.modelManager = modelManager;
    this.console = console;
  }

  public void run(IProgressMonitor monitor) throws InterruptedException {
    monitor.beginTask(Messages.LocalProjectScanner_task_scanning, IProgressMonitor.UNKNOWN);
    try {
      for(String folderName : folders) {
        try {
          File folder = new File(folderName).getCanonicalFile();
          scanFolder(folder, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
        } catch(IOException ex) {
          addError(ex);
        }
      }
    } finally {
      monitor.done();
    }
  }

  private void scanFolder(File baseDir, IProgressMonitor monitor) throws InterruptedException {
    if(monitor.isCanceled()) {
      throw new InterruptedException();
    }

    monitor.subTask(baseDir.toString());
    monitor.worked(1);

    // Don't scan the .metadata folder
    if(!baseDir.exists() || !baseDir.isDirectory() || IMavenConstants.METADATA_FOLDER.equals(baseDir.getName())) {
      return;
    }

    MavenProjectInfo projectInfo = readMavenProjectInfo(baseDir, "", null); //$NON-NLS-1$
    if(projectInfo != null) {
      addProject(projectInfo);
      return; // don't scan subfolders of the Maven project
    }

    File[] files = baseDir.listFiles();
    for(int i = 0; i < files.length; i++ ) {
      File file;
      try {
        file = files[i].getCanonicalFile();
        if(file.isDirectory()) {
          scanFolder(file, monitor);
        }
      } catch(IOException ex) {
        addError(ex);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(File baseDir, String modulePath, MavenProjectInfo parentInfo) {
    try {
      baseDir = baseDir.getCanonicalFile();
      
      File pomFile = new File(baseDir, IMavenConstants.POM_FILE_NAME);
      if(!pomFile.exists()) {
        return null;
      }
      
      Model model = modelManager.readMavenModel(pomFile);

      if (!scannedFolders.add(baseDir)) {
        return null; // we already know this project
      }

      String pomName = modulePath + "/" + IMavenConstants.POM_FILE_NAME; //$NON-NLS-1$

      MavenProjectInfo projectInfo = newMavenProjectInfo(pomName, pomFile, model, parentInfo);
      projectInfo.setBasedirRename(getBasedirRename(projectInfo));

      Map<String, Set<String>> modules = new LinkedHashMap<String, Set<String>>();
      for(String module : model.getModules()) {
        modules.put(module, new HashSet<String>());
      }

      for(Profile profile : model.getProfiles()) {
        for(String module : profile.getModules()) {
          Set<String> profiles = modules.get(module);
          if(profiles == null) {
            profiles = new HashSet<String>();
            modules.put(module, profiles);
          }
          profiles.add(profile.getId());
        }
      }

      for(Map.Entry<String, Set<String>> e : modules.entrySet()) {
        String module = e.getKey();
        Set<String> profiles = e.getValue();

        File moduleBaseDir = new File(baseDir, module);
        MavenProjectInfo moduleInfo = readMavenProjectInfo(moduleBaseDir, module, projectInfo);
        if(moduleInfo != null) {
          moduleInfo.addProfiles(profiles);
          projectInfo.add(moduleInfo);
        }
      }

      return projectInfo;

    } catch(CoreException ex) {
      addError(ex);
      console.logError("Unable to read model " + baseDir.getAbsolutePath());
    } catch(IOException ex) {
      addError(ex);
      console.logError("Unable to read model " + baseDir.getAbsolutePath());
    }

    return null;
  }

  protected MavenProjectInfo newMavenProjectInfo(String label, File pomFile, Model model, MavenProjectInfo parent) {
    return new MavenProjectInfo(label, pomFile, model, parent);
  }

  public String getDescription() {
    return folders.toString();
  }

  private int getBasedirRename(MavenProjectInfo mavenProjectInfo) throws IOException {
    File cannonical = mavenProjectInfo.getPomFile().getParentFile().getParentFile().getCanonicalFile();
    if (basedirRemameRequired && cannonical.equals(workspaceRoot.getCanonicalFile())) {
      return MavenProjectInfo.RENAME_REQUIRED;
    }
    return MavenProjectInfo.RENAME_NO;
  }
}
