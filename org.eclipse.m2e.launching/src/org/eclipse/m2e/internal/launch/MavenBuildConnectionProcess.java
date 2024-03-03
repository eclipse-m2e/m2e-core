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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.internal.maven.listener.MavenBuildConnection;
import org.eclipse.m2e.internal.maven.listener.MavenBuildConnectionListener;
import org.eclipse.m2e.internal.maven.listener.MavenProjectBuildData;


/**
 * This is the representation of the MavenBuildConnection we use to communicate with the remote maven process.
 */
public class MavenBuildConnectionProcess implements IProcess, MavenBuildConnectionListener {

  Map<ArtifactKey, CompletableFuture<MavenProjectBuildData>> projects = new ConcurrentHashMap<>();

  private ILaunch launch;

  private MavenBuildConnection connection;

  private Map<String, String> attributes = new HashMap<>();

  private AtomicBoolean terminated = new AtomicBoolean();

  private String label;

  /**
   * @param launch
   */
  public MavenBuildConnectionProcess(ILaunch launch) {
    this.launch = launch;
  }

  public <T> T getAdapter(Class<T> adapter) {
    return null;
  }

  public boolean canTerminate() {
    return false;
  }

  public boolean isTerminated() {
    return connection != null && connection.isCompleted();
  }

  public void terminate() {
    if(connection != null && terminated.compareAndSet(false, true)) {
      connection.close();
      fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
      for(CompletableFuture<MavenProjectBuildData> future : projects.values()) {
        future.cancel(true);
      }
    }
  }

  public String getLabel() {
    // TODO fetch the maven version from the remove process like mvn -V ... 
    if(label != null) {
      return "Maven<" + label + ">";
    }
    return "Maven";
  }

  public ILaunch getLaunch() {
    return launch;
  }

  public IStreamsProxy getStreamsProxy() {
    return null;
  }

  public void setAttribute(String key, String value) {
    attributes.put(key, value);
    fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
  }

  public String getAttribute(String key) {
    return attributes.get(key);
  }

  public int getExitValue() {
    return 0;
  }

  private static void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();
    if(manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] {event});
    }
  }

  public static Optional<MavenBuildConnectionProcess> get(ILaunch launch) {
    for(IProcess process : launch.getProcesses()) {
      if(process instanceof MavenBuildConnectionProcess p) {
        return Optional.of(p);
      }
    }
    return Optional.empty();
  }

  /**
   * @param launch2
   * @param groupId
   * @param artifactId
   * @param version
   * @return
   */
  public CompletableFuture<MavenProjectBuildData> getBuildProject(String groupId, String artifactId, String version) {
    return projects.computeIfAbsent(new ArtifactKey(groupId, artifactId, version, null),
        x -> new CompletableFuture<>());
  }

  public void onOpen(String label, MavenBuildConnection connection) {
    this.label = label;
    this.connection = connection;
    getLaunch().addProcess(this);
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));

  }

  public void onClose() {
    terminate();
  }

  public void onData(MavenProjectBuildData buildData) {
    projects.computeIfAbsent(new ArtifactKey(buildData.groupId, buildData.artifactId, buildData.version, null), x -> new CompletableFuture<>())
        .complete(buildData);
  }

}
