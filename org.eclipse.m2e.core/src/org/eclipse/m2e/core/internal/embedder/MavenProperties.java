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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.shared.utils.StringUtils;


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

  private static final String MVN_MAVEN_CONFIG = ".mvn/maven.config";

  private static final Logger log = LoggerFactory.getLogger(MavenProperties.class);

  private static final String BUILD_VERSION_PROPERTY = "version";

  private static final String BUILD_VERSION_UNKNOWN_PROPERTY = "<version unknown>";

  private static String mavenVersion;

  private static String mavenBuildVersion;

  static {
    Properties properties = getMavenRuntimeProperties();
    mavenVersion = properties.getProperty(BUILD_VERSION_PROPERTY, BUILD_VERSION_UNKNOWN_PROPERTY);
    mavenBuildVersion = createMavenVersionString(properties);
  }

  private MavenProperties() {
    //prevent instanciation
  }

  static Properties getMavenRuntimeProperties() {
    Properties properties = new Properties();

    try (InputStream resourceAsStream = MavenCli.class
        .getResourceAsStream("/org/apache/maven/messages/build.properties")) {
      if(resourceAsStream != null) {
        properties.load(resourceAsStream);
      }
    } catch(IOException e) {
      log.error("Unable to read Maven properties from JAR file: {}", e.getMessage());
    }
    return properties;
  }

  /**
   * Create a human readable string containing the Maven version, buildnumber, and time of build
   *
   * @param buildProperties The build properties
   * @return Readable build info
   */
  static String createMavenVersionString(Properties buildProperties) {
    String timestamp = reduce(buildProperties.getProperty("timestamp"));
    String version = reduce(buildProperties.getProperty(BUILD_VERSION_PROPERTY));
    String rev = reduce(buildProperties.getProperty("buildNumber"));
    String distributionName = reduce(buildProperties.getProperty("distributionName"));

    String msg = distributionName + " ";
    msg += (version != null ? version : BUILD_VERSION_UNKNOWN_PROPERTY);
    if(rev != null || timestamp != null) {
      msg += " (";
      msg += (rev != null ? rev : "");
      if(StringUtils.isNotBlank(timestamp)) {
        String ts = formatTimestamp(Long.parseLong(timestamp));
        msg += (rev != null ? "; " : "") + ts;
      }
      msg += ")";
    }
    return msg;
  }

  private static String reduce(String s) {
    return (s != null ? (s.startsWith("${") && s.endsWith("}") ? null : s) : null);
  }

  private static String formatTimestamp(long timestamp) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    return sdf.format(new Date(timestamp));
  }

  public static String getMavenBuildVersion() {
    return mavenBuildVersion;
  }

  public static String getMavenVersion() {
    return mavenVersion;
  }

  /**
   * Add the "maven.version" and "maven.build.version" properties to the given properties
   *
   * @param properties
   */
  public static void setProperties(Properties properties) {
    if(properties != null) {
      properties.setProperty("maven.version", mavenVersion);
      properties.setProperty("maven.build.version", mavenBuildVersion);
    }
  }

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

  public static CommandLine getMavenArgs(File multiModuleProjectDirectory) throws IOException, ParseException {
    if(multiModuleProjectDirectory != null) {
      File configFile = new File(multiModuleProjectDirectory, MVN_MAVEN_CONFIG);
      if(configFile.isFile()) {
        List<String> args = new ArrayList<>();
        for(String arg : new String(Files.readAllBytes(configFile.toPath())).split("\\s+")) {
          if(!arg.isEmpty()) {
            args.add(arg);
          }
        }
        CLIManager manager = new CLIManager();
        return manager.parse(args.toArray(String[]::new));
      }
    }
    return null;
  }

  public static void getProfiles(CommandLine commandLine, Consumer<String> activeProfilesConsumer,
      Consumer<String> inactiveProfilesConsumer) {
    if(commandLine != null && commandLine.hasOption(CLIManager.ACTIVATE_PROFILES)) {
      String[] profileOptionValues = commandLine.getOptionValues(CLIManager.ACTIVATE_PROFILES);
      if(profileOptionValues != null) {
        for(String profileOptionValue : profileOptionValues) {
          StringTokenizer tokenizer = new StringTokenizer(profileOptionValue, ",");
          while(tokenizer.hasMoreTokens()) {
            getProfile(tokenizer.nextToken().trim(), activeProfilesConsumer, inactiveProfilesConsumer);
          }
        }
      }
    }
  }

  public static void getProfile(String profile, Consumer<String> activeProfilesConsumer,
      Consumer<String> inactiveProfilesConsumer) {
    if(profile.startsWith("-") || profile.startsWith("!")) {
      inactiveProfilesConsumer.accept(profile.substring(1));
    } else if(profile.startsWith("+")) {
      activeProfilesConsumer.accept(profile.substring(1));
    } else {
      activeProfilesConsumer.accept(profile);
    }
  }

  public static void getCliProperties(CommandLine commandLine, BiConsumer<String, String> consumer) {
    if(commandLine != null && commandLine.hasOption(CLIManager.SET_SYSTEM_PROPERTY)) {
      String[] defStrs = commandLine.getOptionValues(CLIManager.SET_SYSTEM_PROPERTY);
      if(defStrs != null) {
        for(String defStr : defStrs) {
          MavenProperties.getCliProperty(defStr, consumer);
        }
      }
    }
  }

  public static void getCliProperty(String property, BiConsumer<String, String> consumer) {
    int index = property.indexOf('=');
    if(index <= 0) {
      consumer.accept(property.trim(), "true");
    } else {
      consumer.accept(property.substring(0, index).trim(), property.substring(index + 1).trim());
    }
  }
}
