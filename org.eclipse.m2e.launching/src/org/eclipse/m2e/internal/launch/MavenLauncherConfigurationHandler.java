/*******************************************************************************
 * Copyright (c) 2008-2018 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;

import org.eclipse.m2e.core.embedder.IMavenLauncherConfiguration;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 * MavenLauncherConfigurationHandler
 * 
 * @author Igor Fedorenko
 */
public class MavenLauncherConfigurationHandler implements IMavenLauncherConfiguration {

  private String mainType;

  private String mainRealm;

  private LinkedHashMap<String, List<String>> realms = new LinkedHashMap<String, List<String>>();

  private List<String> curEntries;

  public void addArchiveEntry(String entry) {
    if(curEntries == null) {
      throw new IllegalStateException();
    }
    curEntries.add(entry);
  }

  public void forceArchiveEntry(String entry) {
    List<String> realm = realms.get(mainRealm);
    if(realm == null) {
      throw new IllegalStateException();
    }
    realm.add(0, entry);
  }

  public void addProjectEntry(IMavenProjectFacade facade) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFolder output = root.getFolder(facade.getOutputLocation());
    if(output.isAccessible()) {
      addArchiveEntry(output.getLocation().toFile().getAbsolutePath());
    }
    // add custom classpath entries
    final IJavaProject javaProject = JavaCore.create(facade.getProject());
    try {
      for(IClasspathEntry cpe : javaProject.getRawClasspath()) {
        if(cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
          IResource resource = root.findMember(cpe.getPath());
          if(resource != null) {
            // workspace resource
            addArchiveEntry(resource.getLocation().toOSString());
          } else {
            // external 
            File file = cpe.getPath().toFile();
            if(file.exists()) {
              addArchiveEntry(file.getAbsolutePath());
            }
          }
        }
      }
    } catch(JavaModelException ex) {
      // XXX to do what to do about this
    }
  }

  public void addRealm(String realm) {
    if(!realms.containsKey(realm)) {
      curEntries = new ArrayList<String>();
      realms.put(realm, curEntries);
    }
  }

  public void setMainType(String type, String realm) {
    this.mainType = type;
    this.mainRealm = realm;
  }

  public void save(OutputStream os) throws IOException {
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    out.write(NLS.bind("main is {0} from {1}\n", mainType, mainRealm));
    for(Map.Entry<String, List<String>> realm : realms.entrySet()) {
      if(LAUNCHER_REALM.equals(realm.getKey())) {
        continue;
      }
      out.write(NLS.bind("[{0}]\n", realm.getKey()));
      for(String entry : realm.getValue()) {
        out.write(NLS.bind("load {0}\n", entry));
      }
    }
    out.flush();
  }

  public String getMainReal() {
    return mainRealm;
  }

  public List<String> getRealmEntries(String realm) {
    return realms.get(realm);
  }
}
