/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * See https://github.com/apache/maven/blob/9ae008a67db18693d7debf99bf3742ab180cc016/maven-embedder/src/main/java/org/apache/maven/cli/CLIReportingUtils.java
 */

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;


/**
 * Holds Maven Runtime version properties.
 * <p>
 * Most of the code was copied from maven-embedder's <a href=
 * "https://github.com/apache/maven/blob/9ae008a67db18693d7debf99bf3742ab180cc016/maven-embedder/src/main/java/org/apache/maven/cli/CLIReportingUtils.java#L84-L131">CLIReportingUtils.java</a>
 * </p>
 *
 * @since 1.15
 */
public class MavenProperties {

  public static File computeMultiModuleProjectDirectory(IResource resource) {
    if(resource == null) {
      return null;
    }
    IPath location = resource.getLocation();
    if(location == null) {
      return null;
    }
    return computeMultiModuleProjectDirectory(location.toFile());
  }

  /**
   * @param file a base file or directory, may be <code>null</code>
   * @return the value for `maven.multiModuleProjectDirectory` as defined in Maven launcher
   */
  public static File computeMultiModuleProjectDirectory(File file) {
    if(file == null) {
      return null;
    }
    final File basedir = file.isDirectory() ? file : file.getParentFile();
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    File workspaceRoot = workspace.getRoot().getLocation().toFile();
  
    for(File root = basedir; root != null && !root.equals(workspaceRoot); root = root.getParentFile()) {
      if(new File(root, IMavenPlexusContainer.MVN_FOLDER).isDirectory()) {
        return root;
      }
    }
    return null;
  }
}
