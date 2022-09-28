/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
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

package org.eclipse.m2e.core.project;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.internal.Messages;


/**
 * @author Eugene Kuleshov
 */
public class LocalProjectScanner extends AbstractProjectScanner<MavenProjectInfo> {
  private final List<String> folders;

  private final boolean basedirRemameRequired;

  private final Set<File> scannedFolders = new HashSet<>();

  private final MavenModelManager modelManager;


  public LocalProjectScanner(List<String> folders, boolean basedirRemameRequired,
      MavenModelManager modelManager) {
    this.folders = folders;
    this.basedirRemameRequired = basedirRemameRequired;
    this.modelManager = modelManager;
  }

  @Override
  public void run(IProgressMonitor monitor) throws InterruptedException {
    SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.LocalProjectScanner_task_scanning, folders.size());
    try {
      for(String folderName : folders) {
        try {
          File folder = new File(folderName).getCanonicalFile();
          scanFolder(folder, "", subMonitor.split(1)); //$NON-NLS-1$
        } catch(IOException ex) {
          addError(ex);
        }
      }
    } finally {
      subMonitor.done();
    }
  }

  private void scanFolder(File baseDir, String rootRelPath, IProgressMonitor m) throws InterruptedException {
    SubMonitor monitor = SubMonitor.convert(m, baseDir.toString(), 1);

    // Don't scan the .metadata folder
    if(!baseDir.isDirectory() || IMavenConstants.METADATA_FOLDER.equals(baseDir.getName())) {
      monitor.done();
      return;
    }

    try {
      if(scannedFolders.contains(baseDir.getCanonicalFile())) {
        monitor.done();
        return;
      }
    } catch(IOException ex1) {
      addError(ex1);
      return;
    }

    MavenProjectInfo projectInfo = readMavenProjectInfo(baseDir, rootRelPath, null);
    if(projectInfo != null) {
      addProject(projectInfo);
      monitor.done();
      return; // don't scan subfolders of the Maven project
    }

    File[] files = baseDir.listFiles();
    if(files == null) {
      addError(new Exception(NLS.bind(Messages.LocalProjectScanner_accessDeniedFromFolder, baseDir.getAbsolutePath())));
      return;
    }
    monitor.setWorkRemaining(files.length);
    for(File file : files) {
      try {
        file = file.getCanonicalFile();
        scanFolder(file, rootRelPath + "/" + file.getName(), monitor.split(1)); //$NON-NLS-1$
      } catch(IOException ex) {
        addError(ex);
      }
    }
  }

  private MavenProjectInfo readMavenProjectInfo(File baseDir, String modulePath, MavenProjectInfo parentInfo) {
    try {
      baseDir = baseDir.getCanonicalFile();

      if(!scannedFolders.add(baseDir)) {
        return null; // we already know this project
        //mkleint: well, if the project is first scanned standalone and later scanned via parent reference, the parent ref gets thrown away??
      }
      Model model = modelManager.readMavenModel(new File(baseDir, IMavenConstants.POM_FILE_NAME));
      if(model == null) {
        return null;
      }
      String pomName = modulePath + "/" + model.getPomFile().getName(); //$NON-NLS-1$
      if(model.getArtifactId() == null) {
        throw new CoreException(Status.error(NLS.bind(Messages.LocalProjectScanner_missingArtifactId, pomName)));
      }

      MavenProjectInfo projectInfo = newMavenProjectInfo(pomName, model.getPomFile(), model, parentInfo);
      //We only want to optionally rename the base directory not any sub directory
      if(parentInfo == null) {
        projectInfo.setBasedirRename(getBasedirRename(projectInfo));
      }

      Map<String, Set<String>> modules = new LinkedHashMap<>();
      for(String module : model.getModules()) {
        if(module.endsWith("/pom.xml")) { //$NON-NLS-1$
          module = module.substring(0, module.length() - "/pom.xml".length()); //$NON-NLS-1$
        }
        modules.put(module, new HashSet<>());
      }

      for(Profile profile : model.getProfiles()) {
        for(String module : profile.getModules()) {
          if(module.endsWith("/pom.xml")) { //$NON-NLS-1$
            module = module.substring(0, module.length() - "/pom.xml".length()); //$NON-NLS-1$
          }
          Set<String> profiles = modules.get(module);
          if(profiles == null) {
            profiles = new HashSet<>();
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

    } catch(CoreException | IOException ex) {
      addError(ex);
    }

    return null;
  }

  protected MavenProjectInfo newMavenProjectInfo(String label, File pomFile, Model model, MavenProjectInfo parent) {
    return new MavenProjectInfo(label, pomFile, model, parent);
  }

  @Override
  public String getDescription() {
    return folders.toString();
  }

  private int getBasedirRename(MavenProjectInfo mavenProjectInfo) {

    if(basedirRemameRequired) {
      return MavenProjectInfo.RENAME_REQUIRED;
    }
    return MavenProjectInfo.RENAME_NO;
  }
}
