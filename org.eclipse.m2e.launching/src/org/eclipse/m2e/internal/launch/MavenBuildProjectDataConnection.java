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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.internal.launch.MavenRuntimeLaunchSupport.VMArguments;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge.MavenBuildConnection;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge.MavenProjectBuildData;


public class MavenBuildProjectDataConnection {

  private static record MavenBuildConnectionData(Map<ArtifactKey, MavenProjectBuildData> projects,
      MavenBuildConnection connection) {
  }

  private static final Map<ILaunch, MavenBuildConnectionData> LAUNCH_PROJECT_DATA = new ConcurrentHashMap<>();

  static {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(new ILaunchesListener2() {
      public void launchesRemoved(ILaunch[] launches) {
        closeServers(Arrays.stream(launches).map(LAUNCH_PROJECT_DATA::remove));
      }

      public void launchesTerminated(ILaunch[] launches) {
        closeServers(Arrays.stream(launches).map(LAUNCH_PROJECT_DATA::get));
      }

      private static void closeServers(Stream<MavenBuildConnectionData> connectionData) {
        connectionData.filter(Objects::nonNull).forEach(c -> {
          try {
            c.connection().close();
          } catch(IOException ex) { // ignore
          }
        });
      }

      public void launchesAdded(ILaunch[] launches) { // ignore
      }

      public void launchesChanged(ILaunch[] launches) { // ignore
      }
    });
  }

  static void openListenerConnection(ILaunch launch, VMArguments arguments) {
    try {
      if(MavenLaunchUtils.getMavenRuntime(launch.getLaunchConfiguration()) instanceof MavenEmbeddedRuntime) {

        Map<ArtifactKey, MavenProjectBuildData> projects = new ConcurrentHashMap<>();

        MavenBuildConnection connection = M2EMavenBuildDataBridge.prepareConnection(
            launch.getLaunchConfiguration().getName(),
            d -> projects.put(new ArtifactKey(d.groupId, d.artifactId, d.version, null), d));

        if(LAUNCH_PROJECT_DATA.putIfAbsent(launch, new MavenBuildConnectionData(projects, connection)) != null) {
          connection.close();
          throw new IllegalStateException(
              "Maven bridge already created for launch of" + launch.getLaunchConfiguration().getName());
        }
        arguments.append(connection.getMavenVMArguments());
      }
    } catch(CoreException | IOException ex) { // ignore
    }
  }

  static MavenProjectBuildData getBuildProject(ILaunch launch, String groupId, String artifactId, String version) {
    MavenBuildConnectionData build = LAUNCH_PROJECT_DATA.get(launch);
    if(build == null) {
      return null;
    }
    ArtifactKey key = new ArtifactKey(groupId, artifactId, version, null);
    while(true) {
      MavenProjectBuildData buildProject = build.projects().get(key);
      if(buildProject != null || build.connection().isReadCompleted()) {
        return buildProject;
      }
      Thread.onSpinWait(); // Await completion of project data read. It has to become available soon, since its GAV was printed on the console
    }
  }

}
