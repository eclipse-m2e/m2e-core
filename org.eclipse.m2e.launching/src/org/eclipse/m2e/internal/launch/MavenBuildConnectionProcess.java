/********************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.launch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge;
import org.eclipse.m2e.internal.maven.listener.M2EMavenBuildDataBridge.MavenBuildConnection;
import org.eclipse.m2e.internal.maven.listener.MavenBuildListener;
import org.eclipse.m2e.internal.maven.listener.MavenProjectBuildData;
import org.eclipse.m2e.internal.maven.listener.MavenTestEvent;


/**
 * A process that represents the remote connection to the maven process
 */
public class MavenBuildConnectionProcess implements IProcess {

  private ILaunch launch;

  private Map<String, String> attributes = new HashMap<>();

  private Map<ArtifactKey, MavenProjectBuildData> projects = new ConcurrentHashMap<>();

  private MavenBuildConnection connection;

  private List<MavenBuildListener> buildListeners = new CopyOnWriteArrayList<>();

  private AtomicBoolean terminated = new AtomicBoolean();

  public MavenBuildConnectionProcess(ILaunch launch) {
    this.launch = launch;
    launch.addProcess(this);
    attributes.put(IProcess.ATTR_PROCESS_TYPE, "m2e-build-endpoint");
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  public boolean canTerminate() {
    return !isTerminated();
  }

  public boolean isTerminated() {
    return terminated.get() || connection == null;
  }

  public void terminate() {
    if(terminated.compareAndSet(false, true)) {
      if(connection != null) {
        try {
          connection.close();
        } catch(IOException ex) {
        }
        connection = null;
      }
      for(MavenBuildListener mavenBuildListener : buildListeners) {
        mavenBuildListener.close();
      }
      buildListeners.clear();
      fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }
  }

  public String getLabel() {
    return "M2E Build Listener";
  }

  public ILaunch getLaunch() {
    return launch;
  }

  public IStreamsProxy getStreamsProxy() {
    return null;
  }

  @Override
  public void setAttribute(String key, String value) {
    attributes.put(key, value);
    fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
  }

  @Override
  public String getAttribute(String key) {
    return attributes.get(key);
  }

  public void addMavenBuildListener(MavenBuildListener listener) {
    buildListeners.add(listener);
  }

  /**
   * @param mavenTestRunnerClient
   * @return
   */
  public void removeMavenBuildListener(MavenBuildListener listener) {
    buildListeners.remove(listener);
  }

  @Override
  public int getExitValue() {
    return 0;
  }

  /**
   * @return the projects
   */
  public Map<ArtifactKey, MavenProjectBuildData> getProjects() {
    return this.projects;
  }

  void connect() throws IOException {
    connection = M2EMavenBuildDataBridge.prepareConnection(launch.getLaunchConfiguration().getName(),
        new MavenBuildListener() {

          @Override
          public void projectStarted(MavenProjectBuildData data) {
            projects.put(new ArtifactKey(data.groupId, data.artifactId, data.version, null), data);
            for(MavenBuildListener mavenBuildListener : buildListeners) {
              mavenBuildListener.projectStarted(data);
            }
          }

          @Override
          public void onTestEvent(MavenTestEvent mavenTestEvent) {
            for(MavenBuildListener mavenBuildListener : buildListeners) {
              mavenBuildListener.onTestEvent(mavenTestEvent);
            }
          }

          public void close() {
            MavenBuildConnectionProcess.this.terminate();
          }
        });
  }

  String getMavenVMArguments() throws IOException {
    return connection.getMavenVMArguments();
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();
    if(manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] {event});
    }
  }

}
