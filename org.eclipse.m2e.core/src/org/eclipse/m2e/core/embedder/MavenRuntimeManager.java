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

package org.eclipse.m2e.core.embedder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.launch.AbstractMavenRuntime;
import org.eclipse.m2e.core.internal.launch.MavenExternalRuntime;
import org.eclipse.m2e.core.internal.launch.MavenRuntimeManagerImpl;


/**
 * Maven runtime manager
 * 
 * @deprecated as of version 1.5, m2e does not provide API to access or configure Maven Installations
 * @author Eugene Kuleshov
 * @author Jason van Zyl
 */
@Deprecated
@SuppressWarnings("deprecation")
public class MavenRuntimeManager {

  public static final String DEFAULT = MavenRuntimeManagerImpl.DEFAULT;

  public static final String EMBEDDED = MavenRuntimeManagerImpl.EMBEDDED;

  public static final String WORKSPACE = MavenRuntimeManagerImpl.WORKSPACE;

  private final MavenRuntimeManagerImpl impl;

  public MavenRuntimeManager(MavenRuntimeManagerImpl impl) {
    this.impl = impl;
  }

  /**
   * @deprecated this method does nothing
   */
  @Deprecated
  public void setEmbeddedRuntime(MavenRuntime embeddedRuntime) {
  }

  /**
   * @deprecated this method does nothing
   */
  @Deprecated
  public void setWorkspaceRuntime(MavenRuntime workspaceRuntime) {
  }

  public MavenRuntime getDefaultRuntime() {
    return impl.getRuntime(DEFAULT);
  }

  public MavenRuntime getRuntime(String location) {
    if(location == null || location.length() == 0 || DEFAULT.equals(location)) {
      return getDefaultRuntime();
    }
    for(MavenRuntime runtime : impl.getRuntimes().values()) {
      if(location.equals(runtime.getLocation())) {
        return runtime;
      }
    }

    return null;
  }

  public List<MavenRuntime> getMavenRuntimes() {
    List<MavenRuntime> result = new ArrayList<>();
    for(AbstractMavenRuntime runtime : impl.getMavenRuntimes()) {
      result.add(runtime);
    }
    return result;
  }

  public void reset() {
    impl.reset();
  }

  public void setDefaultRuntime(MavenRuntime runtime) {
    impl.setDefaultRuntime((AbstractMavenRuntime) runtime);
  }

  public void setRuntimes(List<MavenRuntime> runtimes) {
    List<AbstractMavenRuntime> internal = new ArrayList<>();
    for(MavenRuntime runtime : runtimes) {
      internal.add((AbstractMavenRuntime) runtime);
    }
    impl.setRuntimes(internal);
  }

  public static MavenRuntime createExternalRuntime(String location) {
    return new MavenExternalRuntime(location);
  }

  public String getGlobalSettingsFile() {
    return MavenPluginActivator.getDefault().getMavenConfiguration().getGlobalSettingsFile();
  }

}
