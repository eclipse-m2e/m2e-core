/*******************************************************************************
 * Copyright (c) Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.internal.launch.IMavenLauncher;


/**
 * EclipseMavenLauncher
 */
@Component(service = IMavenLauncher.class)
public class EclipseMavenLauncher implements IMavenLauncher {

  public CompletableFuture<?> runMaven(File basedir, String goals, Properties mavenProperties) throws CoreException {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    ILaunchConfigurationType launchConfigurationType = launchManager
        .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);
    ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null,
        launchManager.generateLaunchConfigurationName("mavenrun-" + UUID.randomUUID()));
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getAbsolutePath());
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goals);
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_SAVE_BEFORE_LAUNCH, false);
    List<String> properties = new ArrayList<>();
    for(String propertyKey : mavenProperties.stringPropertyNames()) {
      String propertyValue = mavenProperties.getProperty(propertyKey);
      if(propertyKey != null && propertyKey.trim().length() > 0) {
        String prop = propertyKey.trim() + (propertyValue == null ? "" : "=" + propertyValue);
        properties.add(prop);
      }
    }
    workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROPERTIES, properties);
    workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
    CompletableFuture<ILaunch> run = new CompletableFuture<>();
    ILaunchConfiguration configuration = workingCopy.doSave();
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
                run.completeExceptionally(new RuntimeException("Process exit value was " + exitValue));
                return;
              }
            } catch(DebugException ex) {
              run.completeExceptionally(ex);
              return;
            }
          }
          launchManager.removeLaunch(launch);
          run.completeAsync(() -> launch);
        });
      }

      private Optional<ILaunch> getMavenLaunch(ILaunch[] launches) {
        for(ILaunch launch : launches) {
          if(launch.getLaunchConfiguration() == configuration) {
            return Optional.of(launch);
          }
        }
        return Optional.empty();
      }
    });
    DebugUITools.launch(configuration, ILaunchManager.RUN_MODE);
    return run;
  }

}
