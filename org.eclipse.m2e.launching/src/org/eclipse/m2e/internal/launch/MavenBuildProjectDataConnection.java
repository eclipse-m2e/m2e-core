/********************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;
import org.eclipse.m2e.internal.maven.listener.MavenProjectBuildData;


public class MavenBuildProjectDataConnection {

  static {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(new ILaunchesListener2() {
      public void launchesRemoved(ILaunch[] launches) {
        cleanupConnections(launches);
      }

      public void launchesTerminated(ILaunch[] launches) {
        cleanupConnections(launches);
      }

      public void launchesAdded(ILaunch[] launches) { // ignore
      }

      public void launchesChanged(ILaunch[] launches) { // ignore
      }

      private void cleanupConnections(ILaunch[] launches) {
        Arrays.stream(launches).flatMap(launch -> getConnection(launch).stream()).forEach(con -> {
          con.terminate();
        });
      }
    });
  }

  static void openListenerConnection(ILaunch launch, VMArguments arguments) {
    try {
      if(MavenLaunchUtils.getMavenRuntime(launch.getLaunchConfiguration()) instanceof MavenEmbeddedRuntime) {
        getConnection(launch).ifPresent(existing -> {
          existing.terminate();
          throw new IllegalStateException(
              "Maven bridge already created for launch of" + launch.getLaunchConfiguration().getName());
        });
        MavenBuildConnectionProcess process = new MavenBuildConnectionProcess(launch);
        process.connect();
        arguments.append(process.getMavenVMArguments());
      }
    } catch(CoreException | IOException ex) { // ignore
    }
  }

  public static Optional<MavenBuildConnectionProcess> getConnection(ILaunch launch) {
    for(IProcess process : launch.getProcesses()) {
      if(process instanceof MavenBuildConnectionProcess connection) {
        return Optional.of(connection);
      }
    }
    return Optional.empty();
  }

  static MavenProjectBuildData getBuildProject(ILaunch launch, String groupId, String artifactId, String version) {
    Optional<MavenBuildConnectionProcess> connection = getConnection(launch);
    if(connection.isEmpty()) {
      return null;
    }
    MavenBuildConnectionProcess process = connection.get();
    Map<ArtifactKey, MavenProjectBuildData> projects = process.getProjects();
    ArtifactKey key = new ArtifactKey(groupId, artifactId, version, null);
    while(true) {
      MavenProjectBuildData buildProject = projects.get(key);
      if(buildProject != null || process.isTerminated()) {
        return buildProject;
      }
      Thread.onSpinWait(); // Await completion of project data read. It has to become available soon, since its GAV was printed on the console
    }
  }

}
