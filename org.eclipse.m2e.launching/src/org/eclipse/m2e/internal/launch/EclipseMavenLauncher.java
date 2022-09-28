/*******************************************************************************
 * Copyright (c) 2022, 2022 Christoph Läubrich
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.internal.launch.IMavenLauncher;


/**
 * EclipseMavenLauncher
 */
@Component(service = IMavenLauncher.class)
public class EclipseMavenLauncher implements IMavenLauncher {

  public CompletableFuture<?> runMaven(File basedir, String goals, Map<String, String> mavenProperties,
      boolean interactive) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
    ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null,
        launchManager.generateLaunchConfigurationName("mavenrun-" + UUID.randomUUID()));
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getAbsolutePath());
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goals);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_SAVE_BEFORE_LAUNCH, false);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_BATCH, !interactive);
    List<String> properties = new ArrayList<>();
    mavenProperties.forEach((propertyKey, propertyValue) -> {
      if(propertyKey != null && !propertyKey.isBlank()) {
        String prop = propertyKey.strip() + (propertyValue == null ? "" : "=" + propertyValue);
        properties.add(prop);
      }
    });
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROPERTIES, properties);
    workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
    CompletableFuture<ILaunch> run = new CompletableFuture<>();
    ILaunchConfiguration configuration = workingCopy.doSave();

    AtomicReference<List<String>> consolePrintOut = new AtomicReference<>();
    launchManager.addLaunchListener(new ILaunchesListener2() {

      public void launchesRemoved(ILaunch[] launches) {
        getMavenLaunch(launches).ifPresent(launch -> {
          launchManager.removeLaunchListener(this);
          if(!run.isDone()) {
            run.cancel(true);
          }
        });
      }

      public void launchesChanged(ILaunch[] launches) {
        getMavenLaunch(launches).ifPresent(launch -> {
          for(IProcess process : launch.getProcesses()) {
            IStreamsProxy streamsProxy = process.getStreamsProxy();
            List<String> printout = Collections.synchronizedList(new ArrayList<>());
            if(consolePrintOut.compareAndSet(null, printout)) {
              streamsProxy.getOutputStreamMonitor().addListener((text, m) -> printout.add(text));
              streamsProxy.getErrorStreamMonitor().addListener((text, m) -> printout.add(text));
            }
          }
        });
      }

      public void launchesAdded(ILaunch[] launches) {
      }

      public void launchesTerminated(ILaunch[] launches) {
        getMavenLaunch(launches).ifPresent(launch -> {
          launchManager.removeLaunchListener(this);
          for(IProcess process : launch.getProcesses()) {
            try {
              int exitValue = process.getExitValue();
              if(exitValue != 0) {
                String message = "Process exit value was " + exitValue;
                if(consolePrintOut.get() != null) {
                  message = message + ". Console printout:\n"
                      + "===============================================================\n"
                      + consolePrintOut.get().stream().collect(Collectors.joining())
                      + "\n\"===============================================================";
                }
                run.completeExceptionally(new IllegalStateException(message));
                return;
              }
            } catch(DebugException ex) {
              run.completeExceptionally(ex);
              return;
            }
          }
          if(!interactive) {
            launchManager.removeLaunch(launch);
          }
          run.completeAsync(() -> launch);
        });
      }

      private Optional<ILaunch> getMavenLaunch(ILaunch[] launches) {
        return Arrays.stream(launches).filter(l -> l.getLaunchConfiguration() == configuration).findFirst();
      }
    });
    DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
    return run;
  }

}
