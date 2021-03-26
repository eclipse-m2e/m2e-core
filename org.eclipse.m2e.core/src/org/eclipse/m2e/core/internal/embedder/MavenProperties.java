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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String ts = formatTimestamp(Long.valueOf(timestamp));
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
}
