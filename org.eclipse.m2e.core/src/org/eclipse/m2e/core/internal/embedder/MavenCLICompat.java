/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.eclipse.m2e.core.internal.embedder;

import java.io.File;


/**
 * Some constants copied here for backward compatibility
 */
public class MavenCLICompat {
  public static final String LOCAL_REPO_PROPERTY = "maven.repo.local";

  public static final String MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

  public static final String USER_HOME = System.getProperty("user.home");

  public static final File USER_MAVEN_CONFIGURATION_HOME = new File(USER_HOME, ".m2");

  public static final File DEFAULT_USER_TOOLCHAINS_FILE = new File(USER_MAVEN_CONFIGURATION_HOME, "toolchains.xml");

  public static final File DEFAULT_GLOBAL_TOOLCHAINS_FILE = new File(System.getProperty("maven.conf"),
      "toolchains.xml");
}
