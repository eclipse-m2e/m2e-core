/********************************************************************************
 * Copyright (c) 2024, 2024 Pascal Treilhes and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Pascal Treilhes - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.jdt.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.JavaRuntime;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;


/**
 * This class is used to support the launch configuration of JUnit and TestNG tests
 */
public class UnitTestSupport {

  /**
   * Feature flag to enable or disable the support
   */
  private static final boolean FEATURE_ENABLED = Boolean
      .parseBoolean(System.getProperty("m2e.process.test.configuration", "true"));

  private static final Logger LOG = LoggerFactory.getLogger(UnitTestSupport.class);

  /**
   * org.apache.maven.surefire.api.testset.TestListResolver method name to check if a test should run
   */
  private static final String SHOULD_RUN_METHOD = "shouldRun";

  /**
   * org.apache.maven.plugin.surefire.AbstractSurefireMojo method name to get the included and excluded tests
   */
  private static final String GET_INCLUDED_AND_EXCLUDED_TESTS_METHOD = "getIncludedAndExcludedTests";

  /**
   * Launch configuration attribute for the main class
   */
  private static final String LAUNCH_CONFIG_MAIN_CLASS = "org.eclipse.jdt.launching.MAIN_TYPE";

  /**
   * Launch configuration attribute for the project
   */
  private static final String LAUNCH_CONFIG_PROJECT = "org.eclipse.jdt.launching.PROJECT_ATTR";

  /**
   * Launch configuration attribute for the VM arguments
   */
  public static final String LAUNCH_CONFIG_VM_ARGUMENTS = "org.eclipse.jdt.launching.VM_ARGUMENTS";

  /**
   * Launch configuration attribute for the environment variables
   */
  public static final String LAUNCH_CONFIG_ENVIRONMENT_VARIABLES = "org.eclipse.debug.core.environmentVariables";

  /**
   * Launch configuration attribute for the system properties
   */
  private static final String LAUNCH_CONFIG_WORKING_DIRECTORY = "org.eclipse.jdt.launching.WORKING_DIRECTORY";

  /**
   * Launch configuration attribute for the working directory
   */
  private static final String PLUGIN_ARGLINE = "argLine";

  /**
   * surefire/failsafe mojo configuration element for the environment variables
   */
  private static final String PLUGIN_ENVIRONMENT_VARIABLES = "environmentVariables";

  /**
   * surefire/failsafe mojo configuration element for the system properties
   */
  private static final String PLUGIN_SYSPROP_VARIABLES = "systemPropertyVariables";

  /**
   * surefire/failsafe mojo configuration element for the working directory
   */
  private static final String PLUGIN_WORKING_DIRECTORY = "workingDirectory";

  /**
   * surefire/failsafe mojo configuration element for the enable assertions
   */
  private static final String PLUGIN_ENABLE_ASSERTIONS = "enableAssertions";

  /**
   * maven goal for the test execution
   */
  private static final String GOAL_TEST = "test";

  /**
   * maven goal for the integration test execution
   */
  private static final String GOAL_INTEGRATION_TEST = "integration-test";

  /**
   * maven artifact id for the surefire plugin
   */
  private static final String SUREFIRE_PLUGIN_ARTIFACT_ID = "maven-surefire-plugin";

  /**
   * maven artifact id for the failsafe plugin
   */
  private static final String FAILSAFE_PLUGIN_ARTIFACT_ID = "maven-failsafe-plugin";

  /**
   * deffered variable pattern
   */
  private static final Pattern DEFERRED_VAR_PATTERN = Pattern.compile("@\\{(.*?)\\}");

  /**
   * maven group id for the maven plugins
   */
  private static final String MAVEN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  private static final List<ExecutionId> TEST_EXECUTIONS = List.of(
      new ExecutionId(MAVEN_PLUGIN_GROUP_ID, SUREFIRE_PLUGIN_ARTIFACT_ID, GOAL_TEST),
      new ExecutionId(MAVEN_PLUGIN_GROUP_ID, FAILSAFE_PLUGIN_ARTIFACT_ID, GOAL_INTEGRATION_TEST));

  private static final Set<String> CONSIDERED_LAUNCH_TYPES = Set.of(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST,
      MavenRuntimeClasspathProvider.JDT_TESTNG_TEST);

  /**
   * Reset all launch configurations for the project
   */
  public static void resetLaunchConfigurations(IProject project) {
    if(FEATURE_ENABLED) {
      new ConfigurationManager().setupLaunchConfigurations(project);
    }
  }

  /**
   * Reset the launch configuration
   * 
   * @param configuration the configuration
   */
  public static void setupLaunchConfigurationFromMavenConfiguration(ILaunchConfiguration configuration) {
    if(FEATURE_ENABLED) {
      new ConfigurationManager().setupLaunchConfiguration(configuration);
    }
  }

  /**
   * Check if the type is supported
   * 
   * @param id the type id
   * @return true if supported
   */
  private static boolean isSupportedType(String id) {
    return id != null && CONSIDERED_LAUNCH_TYPES.contains(id);
  }

  private static class ConfigurationManager {

    /**
     * Reset all launch configurations for the project
     */
    public void setupLaunchConfigurations(IProject project) {

      if(project != null && project.exists()) {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        for(String launchTypeId : CONSIDERED_LAUNCH_TYPES) {
          try {
            // Get launch type
            ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(launchTypeId);
            if(type == null) {
              // unknown type, probably support not installed
              continue;
            }
            // Get all launch configurations for the type
            ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations(type);
            for(ILaunchConfiguration configuration : configurations) {
              // Check if the configuration is associated with the desired project and type 
              String configurationProjectName = configuration.getAttribute(LAUNCH_CONFIG_PROJECT, "");
              if(project.getName().equals(configurationProjectName)) {
                LOG.info("Reset launch configuration name: {}", configuration.getName());
                setupLaunchConfiguration(configuration);
              }
            }
          } catch(Exception e) {
            LOG.error(e.getMessage(), e);
          }
        }

      }
    }

    /**
     * Reset the launch configuration
     */
    public void setupLaunchConfiguration(ILaunchConfiguration configuration) {
      try {
        if(!isSupportedType(configuration.getType().getIdentifier())) {
          return;
        }
        IProject project = JavaRuntime.getJavaProject(configuration).getProject();

        if(MavenPlugin.isMavenProject(project)) {

          switch(configuration.getType().getIdentifier()) {
            case MavenRuntimeClasspathProvider.JDT_TESTNG_TEST:
            case MavenRuntimeClasspathProvider.JDT_JUNIT_TEST: {

              LOG.info("Updating {} from maven configuration", configuration.getName());

              IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);

              TestLaunchArguments args = getTestLaunchArguments(configuration, facade, null);

              // update the configuration only if arg extraction was successfull
              if(args != null) {
                defineConfigurationValues(project, configuration, args);
              }

            }
          }
        }
      } catch(CoreException ex) {
        LOG.error(ex.getMessage(), ex);
      }
    }

    /**
     * Define the configuration values
     */
    private void defineConfigurationValues(IProject project, ILaunchConfiguration configuration,
        TestLaunchArguments args) throws CoreException {

      ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();

      StringJoiner launchArguments = new StringJoiner("\n");
      if(args.enableAssertions()) {
        launchArguments.add("-ea");
      }
      if(args.argLine() != null) {
        launchArguments.add(args.argLine());
      }
      if(args.systemPropertyVariables() != null) {
        args.systemPropertyVariables().entrySet().stream() //
            .filter(e -> e.getKey() != null && e.getValue() != null)
            .forEach(e -> launchArguments.add("-D" + e.getKey() + "=" + escapeValue(e.getValue())));
      }
      copy.setAttribute(LAUNCH_CONFIG_VM_ARGUMENTS, launchArguments.toString());

      try {
        if(args.workingDirectory() != null
            && !Files.isSameFile(project.getLocation().toPath().toAbsolutePath(), args.workingDirectory().toPath())) {
          copy.setAttribute(LAUNCH_CONFIG_WORKING_DIRECTORY, args.workingDirectory().getAbsolutePath());
        } else {
          copy.setAttribute(LAUNCH_CONFIG_WORKING_DIRECTORY, (String) null);
        }
      } catch(IOException ex) {
        LOG.error(ex.getMessage(), ex);
      }

      if(args.environmentVariables() != null) {
        Map<String, String> filteredMap = args.environmentVariables().entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        copy.setAttribute(LAUNCH_CONFIG_ENVIRONMENT_VARIABLES, filteredMap);
      }

      copy.doSave();
    }

    private static String escapeValue(String raw) {
      if(raw.contains(" ") || raw.contains("\t") || raw.contains("\r") || raw.contains("\n")) {
        return "\"" + raw + "\"";
      }
      return raw;
    }

    private TestLaunchArguments getTestLaunchArguments(ILaunchConfiguration configuration, IMavenProjectFacade facade,
        IProgressMonitor monitor) throws CoreException {

      String testClass = configuration.getAttribute(LAUNCH_CONFIG_MAIN_CLASS, "");
      MavenProject mavenProject = facade.getMavenProject();

      // find test executions
      List<MojoExecution> executions = new ArrayList<>();
      for(ExecutionId id : TEST_EXECUTIONS) {
        executions.addAll(facade.getMojoExecutions(id.groupId(), id.artifactId(), monitor, id.goal()));
      }

      // find which plugin executions will launch the test
      List<MojoExecution> handlers = executions.stream()
          .filter(e -> isTestHandledByPlugin(facade, monitor, e, testClass)).toList();

      // only one execution is expected here but if more only the first one is used
      for(MojoExecution execution : handlers) {
        return getTestLaunchArguments(mavenProject, execution, monitor);
      }
      return null;
    }

    /**
     * Check if the test is handled by the plugin
     * 
     * @return true if the test is handled by the plugin
     */
    private boolean isTestHandledByPlugin(IMavenProjectFacade facade, IProgressMonitor monitor, MojoExecution execution,
        String testClass) {

      String testFile = testClass.replace(".", "/") + ".class";
      // get a configured mojo instance 
      Optional<Mojo> mojo = getMojoInstance(facade, execution, monitor);
      // get an instance of org.apache.maven.surefire.api.testset.TestListResolver directly from the plugin instance
      Optional<Object> testResolverInstance = mojo.map(o -> uncheckedInvoke(o, GET_INCLUDED_AND_EXCLUDED_TESTS_METHOD));
      // check if the test is handled by the plugin
      Boolean isTestHandled = testResolverInstance.map(o -> uncheckedInvoke(o, SHOULD_RUN_METHOD, testFile, ""))
          .map(Boolean.class::cast).orElse(false);

      return isTestHandled;
    }

    /**
     * Invoke a method without throwing checked exceptions
     * 
     * @param instance the instance on which to invoke the method
     * @param methodName the name of the method to invoke
     * @param arguments the arguments
     * @return the result of the invocation
     */
    private static Object uncheckedInvoke(Object instance, String methodName, Object... arguments) {
      Class<?> searchClass = instance.getClass();
      Class<?>[] parameters = Arrays.stream(arguments).map(p -> p.getClass()).toArray(Class[]::new);
      while(searchClass != null) {
        try {
          Method method = searchClass.getDeclaredMethod(methodName, parameters);
          if(!method.trySetAccessible()) {
            LOG.error("Cannot make accessible {}", method);
          } else {
            return method.invoke(instance, arguments);
          }
        } catch(NoSuchMethodException | SecurityException ex) {
          LOG.debug(ex.getMessage(), ex);
        } catch(IllegalAccessException | InvocationTargetException ex) {
          LOG.error(ex.getMessage(), ex);
        }
        searchClass = searchClass.getSuperclass();
      }
      return null;
    }

    /** Execution cache */
    private final Map<MojoExecution, Mojo> mojoCache = new HashMap<>();

    /**
     * Get a configured mojo instance
     */
    public Optional<Mojo> getMojoInstance(IMavenProjectFacade facade, MojoExecution execution,
        IProgressMonitor monitor) {
      return Optional.ofNullable(mojoCache.computeIfAbsent(execution, exe -> {
        try {
          return facade.createExecutionContext().execute(facade.getMavenProject(),
              (context, pm) -> MavenPlugin.getMaven().getConfiguredMojo(context.getSession(), exe, Mojo.class),
              monitor);
        } catch(CoreException ex) {
          LOG.error("Unable to instanciate mojo instance", ex);
          return null;
        }
      }));
    }

    /**
     * Get all the arguments provided to the plugin for the provided {@link MojoExecution}.
     * 
     * @param mavenProject the current maven project
     * @param execution the plugin execution
     * @param monitor the progress monitor
     * @return the arguments
     */
    @SuppressWarnings("unchecked")
    private TestLaunchArguments getTestLaunchArguments(MavenProject mavenProject, MojoExecution execution,
        IProgressMonitor monitor) {
      try {
        IMaven maven = MavenPlugin.getMaven();

        String argLine = maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ARGLINE, String.class, monitor);
        argLine = resolveDeferredVariables(mavenProject, argLine);

        return new TestLaunchArguments(argLine,
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_SYSPROP_VARIABLES, Map.class, monitor),
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ENVIRONMENT_VARIABLES, Map.class, monitor),
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_WORKING_DIRECTORY, File.class, monitor),
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ENABLE_ASSERTIONS, Boolean.class, monitor));
      } catch(Exception e) {
        LOG.error(e.getMessage(), e);
      }
      return null;
    }

  }

  /**
   * This method is used to resolve deferred variables introduced by failsafe/surefire plugins in a given string value.
   * Deferred variables are placeholders in the string that are replaced with actual values from the Maven project's
   * properties. The placeholders are in the format @{...}, where ... is the key of the property. If a placeholder's
   * corresponding property does not exist, the placeholder is left as is.
   *
   * @param mavenProject the Maven project from which to retrieve the properties
   * @param value the string containing the placeholders to be replaced
   * @return the string with all resolvable placeholders replaced with their corresponding property values
   */
  private static String resolveDeferredVariables(MavenProject mavenProject, String value) {
    Properties properties = mavenProject.getProperties();
    if(properties.isEmpty() || value == null) {
      return value;
    }
    return DEFERRED_VAR_PATTERN.matcher(value).replaceAll(match -> {
      String placeholder = match.group();
      String key = match.group(1);
      String replacement = properties.getProperty(key);
      return replacement != null ? replacement : placeholder;
    });
  }

  /**
   * Holder for the surefire/failsafe launch arguments
   */
  private static record TestLaunchArguments(String argLine, Map<String, String> systemPropertyVariables,
      Map<String, String> environmentVariables, File workingDirectory, boolean enableAssertions) {
  }

  /**
   * Holder for the execution id
   */
  private static record ExecutionId(String groupId, String artifactId, String goal) {
  }

}
