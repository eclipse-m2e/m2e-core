/********************************************************************************
 * Copyright (c) 2022, 2024 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *   Christoph Läubrich - refactor into {@link IMavenLaunchParticipant}
 ********************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

import org.eclipse.m2e.core.internal.launch.MavenEmbeddedRuntime;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge;


public class MavenBuildConnectionLaunchParticipant implements IMavenLaunchParticipant {

  static {
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(new ILaunchesListener2() {
      public void launchesRemoved(ILaunch[] launches) {
        ensureClosed(launches);
      }

      private void ensureClosed(ILaunch[] launches) {
        Arrays.stream(launches).flatMap(l -> MavenBuildConnectionProcess.get(l).stream())
            .forEach(MavenBuildConnectionProcess::terminate);
      }

      public void launchesTerminated(ILaunch[] launches) {
        ensureClosed(launches);
      }

      public void launchesAdded(ILaunch[] launches) { // ignore
      }

      public void launchesChanged(ILaunch[] launches) { // ignore
      }
    });
  }

  public String getProgramArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
    try {
      if(MavenLaunchUtils.getMavenRuntime(launch.getLaunchConfiguration()) instanceof MavenEmbeddedRuntime) {

        MavenBuildConnectionProcess process = new MavenBuildConnectionProcess(launch);
        return M2EMavenBuildDataBridge.openConnection(launch.getLaunchConfiguration().getName(), process);
      }
    } catch(CoreException | IOException ex) { // ignore
    }
    return null;
  }

  public String getVMArguments(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) {
    return null;
  }

  public List<ISourceLookupParticipant> getSourceLookupParticipants(ILaunchConfiguration configuration, ILaunch launch,
      IProgressMonitor monitor) {
    return List.of();
  }

}
