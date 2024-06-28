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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.internal.launch.MavenRuntimeClasspathProvider;


/**
 * This class is used to support the launch configuration of JUnit and TestNG tests
 */
public class UnitTestSupport {

  /**
   * Feature flag to enable or disable the support
   */
  public static final boolean FEATURE_ENABLED = Boolean
      .parseBoolean(System.getProperty("m2e.process.test.configuration", "true"));

  /**
   * Logger
   */
  private static final Logger log = LoggerFactory.getLogger(UnitTestSupport.class);

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
  public static final String LAUNCH_CONFIG_MAIN_CLASS = "org.eclipse.jdt.launching.MAIN_TYPE";

  /**
   * Launch configuration attribute for the test name
   */
  public static final String LAUNCH_CONFIG_TESTNAME = "org.eclipse.jdt.junit.TESTNAME";

  /**
   * Launch configuration attribute for the project
   */
  public static final String LAUNCH_CONFIG_PROJECT = "org.eclipse.jdt.launching.PROJECT_ATTR";

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
  public static final String LAUNCH_CONFIG_WORKING_DIRECTORY = "org.eclipse.jdt.launching.WORKING_DIRECTORY";

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
   * maven goal for the properties execution
   */
  private static final String GOAL_PROPERTIES = "properties";

  /**
   * maven artifact id for the surefire plugin
   */
  private static final String SUREFIRE_PLUGIN_ARTIFACT_ID = "maven-surefire-plugin";

  /**
   * maven artifact id for the failsafe plugin
   */
  private static final String FAILSAFE_PLUGIN_ARTIFACT_ID = "maven-failsafe-plugin";

  /**
   * maven artifact id for the dependency plugin
   */
  private static final String DEPENDENCY_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";

  /**
   * maven group id for the maven plugins
   */
  private static final String MAVEN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

  /**
   * List of test executions
   */
  private static final List<ExecutionId> TEST_EXECUTIONS = List.of(
      new ExecutionId(MAVEN_PLUGIN_GROUP_ID, SUREFIRE_PLUGIN_ARTIFACT_ID, GOAL_TEST),
      new ExecutionId(MAVEN_PLUGIN_GROUP_ID, FAILSAFE_PLUGIN_ARTIFACT_ID, GOAL_INTEGRATION_TEST));

  /**
   * List of default prerequisite executions (needed to populate properties
   */
  private static final List<ExecutionId> PREREQ_EXECUTIONS = List
      .of(new ExecutionId(MAVEN_PLUGIN_GROUP_ID, DEPENDENCY_PLUGIN_ARTIFACT_ID, GOAL_PROPERTIES));

  /**
   * property to set to define a list of mojo executions to be executed before the test launch configuration is updated
   * The format is a list of execution gourpId:artifactId:goal separated by a comma<br/>
   * Ex: grp1:art1:goal1,grp2:art2:goal2
   */
  public static final String PREREQ_CUSTOM_EXECUTIONS_PROPERTY = "m2e.launch.configuration.prerequisites";

  /**
   * Supported launch types
   */
  private static final Set<String> CONSIDERED_LAUNCH_TYPES = Set.of(MavenRuntimeClasspathProvider.JDT_JUNIT_TEST, MavenRuntimeClasspathProvider.JDT_TESTNG_TEST)

  /**
   * Reset all launch configurations for the project
   * 
   * @param project the project
   */
  public static void resetLaunchConfigurations(IProject project) {

    if(!FEATURE_ENABLED) {
      return;
    }

    new ConfigurationManager().setupLaunchConfigurations(project);

  }

  /**
   * Reset the launch configuration
   * 
   * @param configuration the configuration
   */
  public static void setupLaunchConfigurationFromMavenConfiguration(ILaunchConfiguration configuration) {

    if(!FEATURE_ENABLED) {
      return;
    }

    new ConfigurationManager().setupLaunchConfiguration(configuration);

  }

  /**
   * Check if the type is supported
   * 
   * @param id the type id
   * @return true if supported
   */
  private static boolean isSupportedType(String id) {
    return id!=null && CONSIDERED_LAUNCH_TYPES .contains(id);
  }

  /**
   * Configuration manager
   */
  private static class ConfigurationManager {

    /**
     * Execution cache
     */
    private MavenCache cache = new MavenCache();

    /**
     * Reset all launch configurations for the project
     * 
     * @param project the project
     */
    public void setupLaunchConfigurations(IProject project) {

      if(project != null && project.exists()) {

        // Get the launch manager
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

        for(String launchTypeId : supportedTypes) {
          try {
            // Get launch type
            ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(launchTypeId);

            if(type == null) {
              // unknown type, probably support not installed
              continue;
            }

            // Get all launch configurations for the type
            ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations(type);

            // Process each launch configuration
            for(ILaunchConfiguration configuration : configurations) {

              // configuration project name
              String confProjectName = configuration.getAttribute(LAUNCH_CONFIG_PROJECT, "");
              String projectName = project.getName();

              // Check if the configuration is associated with the desired project and type
              if(projectName.equals(confProjectName)) {
                log.info("Reset launch configuration name: {}", configuration.getName());
                setupLaunchConfiguration(configuration);
              }
            }
          } catch(Exception e) {
            log.error(e.getMessage(), e);
          }
        }

      }
    }

    /**
     * Reset the launch configuration
     * 
     * @param configuration the configuration
     */
    public void setupLaunchConfiguration(ILaunchConfiguration configuration) {

      try {

        if(!isSupportedType(configuration.getType().getIdentifier())) {
          return;
        }

        IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
        IProject project = javaProject.getProject();

        // maven project if project has a maven classpath
        boolean isMavenProject = configuration
            .getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "")
            .equals(MavenRuntimeClasspathProvider.MAVEN_CLASSPATH_PROVIDER);

        if(project != null && project.hasNature(IMavenConstants.NATURE_ID) && isMavenProject) {

          switch(configuration.getType().getIdentifier()) {
            case MavenRuntimeClasspathProvider.JDT_TESTNG_TEST:
            case MavenRuntimeClasspathProvider.JDT_JUNIT_TEST: {

              log.info("Updating {} from maven configuration", configuration.getName());

              IMavenProjectFacade facade = getMavenProjectFacade(javaProject);

              loadPrerequisites(configuration, facade, null);

              TestLaunchArguments args = getTestLaunchArguments(configuration, facade, null);

              // update the configuration only if arg extraction was successfull
              if(args != null) {
                defineConfigurationValues(project, configuration, args);
              }

            }
          }
        }
      } catch(CoreException ex) {
        log.error(ex.getMessage(), ex);
      }
    }

    /**
     * Define the configuration values
     * 
     * @param project the project
     * @param configuration the configuration
     * @param args the arguments
     * @throws CoreException
     */
    private void defineConfigurationValues(IProject project, ILaunchConfiguration configuration,
        TestLaunchArguments args) throws CoreException {
      ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();

      StringBuilder launchArguments = new StringBuilder();

      if(args.isEnableAssertions()) {
        launchArguments.append("-ea").append("\n");
      }

      if(args.getArgLine() != null) {
        launchArguments.append(args.getArgLine()).append("\n");
      }
      if(args.getSystemPropertyVariables() != null) {
        args.getSystemPropertyVariables().entrySet().forEach(
            e -> launchArguments.append("-D").append(e.getKey()).append("=").append(e.getValue()).append("\n"));
      }

      copy.setAttribute(LAUNCH_CONFIG_VM_ARGUMENTS, launchArguments.toString());

      try {
        if(args.getWorkingDirectory() != null && !Files.isSameFile(project.getLocation().toPath().toAbsolutePath(),
            args.getWorkingDirectory().toPath())) {
          copy.setAttribute(LAUNCH_CONFIG_WORKING_DIRECTORY, args.getWorkingDirectory().getAbsolutePath());
        } else {
          copy.setAttribute(LAUNCH_CONFIG_WORKING_DIRECTORY, (String) null);
        }
      } catch(IOException ex) {
        log.error(ex.getMessage(), ex);
      }

      if(args.getEnvironmentVariables() != null) {
        copy.setAttribute(LAUNCH_CONFIG_ENVIRONMENT_VARIABLES, args.getEnvironmentVariables());
      }

      copy.doSave();
    }

    private static IMavenProjectFacade getMavenProjectFacade(IJavaProject project) {
      return MavenPlugin.getMavenProjectRegistry().getProject(project.getProject());
    }

    private TestLaunchArguments getTestLaunchArguments(ILaunchConfiguration configuration, IMavenProjectFacade facade,
        IProgressMonitor monitor) throws CoreException {

      String testClass = configuration.getAttribute(LAUNCH_CONFIG_MAIN_CLASS, "");

      MavenProject mavenProject = facade.getMavenProject();

      // find test executions
      List<MojoExecution> executions = new ArrayList<>();
      for(ExecutionId id : TEST_EXECUTIONS) {
        executions.addAll(facade.getMojoExecutions(id.getGroupId(), id.getArtifactId(), monitor, id.getGoal()));
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
     * Execute the prerequisites mojos
     * 
     * @param configuration the configuration
     * @param facade the maven project facade
     * @param monitor the progress monitor
     */
    private void loadPrerequisites(ILaunchConfiguration configuration, IMavenProjectFacade facade,
        IProgressMonitor monitor) {

      String customExecutions = facade.getMavenProject().getProperties().getProperty(PREREQ_CUSTOM_EXECUTIONS_PROPERTY);

      List<ExecutionId> executionIds = customExecutions != null ? parseExecutionIds(customExecutions)
          : PREREQ_EXECUTIONS;

      // find prereq executions
      List<MojoExecution> executions = new ArrayList<>();
      for(ExecutionId id : executionIds) {
        try {
          executions.addAll(facade.getMojoExecutions(id.getGroupId(), id.getArtifactId(), monitor, id.getGoal()));
        } catch(CoreException ex) {
          log.error(ex.getMessage(), ex);
        }
      }

      // executing prerequisites mojo (mainly for properties)
      for(MojoExecution execution : executions) {
        try {
          cache.executeMojo(facade, execution, monitor);
        } catch(MojoExecutionException | MojoFailureException ex) {
          log.error(ex.getMessage(), ex);
        }
      }

    }

    /**
     * Parse the custom execution ids
     * 
     * @param customExecutions the custom executions
     * @return the list of execution ids
     */
    private List<ExecutionId> parseExecutionIds(String customExecutions) {
      List<ExecutionId> executionIds = new ArrayList<>();
      String[] customExecutionsArray = customExecutions.split(",");
      for(String customExecution : customExecutionsArray) {
        String[] customExecutionArray = customExecution.split(":");
        if(customExecutionArray.length == 3) {
          executionIds.add(new ExecutionId(customExecutionArray[0], customExecutionArray[1], customExecutionArray[2]));
        }
      }
      return executionIds;
    }

    /**
     * Check if the test is handled by the plugin
     * 
     * @param facade the maven project facade
     * @param monitor the progress monitor
     * @param execution the plugin execution
     * @param testClass the test class
     * @return true if the test is handled by the plugin
     */

    private boolean isTestHandledByPlugin(IMavenProjectFacade facade, IProgressMonitor monitor, MojoExecution execution,
        String testClass) {

      String testFile = testClass.replace(".", "/") + ".class";
      // get a configured mojo instance 
      Mojo mojo = cache.getMojoInstance(facade, execution, monitor);

      // get an instance of org.apache.maven.surefire.api.testset.TestListResolver directly from the plugin instance
      Optional<Object> testResolverInstance = Optional.of(mojo)
          .map(o -> findMethod(o, GET_INCLUDED_AND_EXCLUDED_TESTS_METHOD)).map(m -> {
            m.setAccessible(true);
            return m;
          }).map(m -> uncheckedInvoke(m, mojo));

      // check if the test is handled by the plugin
      Boolean isTestHandled = testResolverInstance.filter(Objects::nonNull)
          .map(o -> findMethod(o, SHOULD_RUN_METHOD, String.class, String.class)).map(m -> {
            m.setAccessible(true);
            return m;
          }).map(m -> uncheckedInvoke(m, testResolverInstance.get(), testFile, "")).map(Boolean.class::cast)
          .orElse(false);

      return isTestHandled;
    }

    /**
     * Lookup method in class and ancestors
     * 
     * @param instance the instance on which we search the method
     * @return a method or null if not found
     */
    private Method findMethod(Object instance, String methodName, Class<?>... parameters) {
      Method method = null;
      Class<?> searchClass = instance.getClass();

      while(method == null && searchClass != null) {
        try {
          method = searchClass.getDeclaredMethod(methodName, parameters);
        } catch(NoSuchMethodException | SecurityException ex) {
          log.debug(ex.getMessage(), ex);
        }
        searchClass = searchClass.getSuperclass();
      }

      return method;
    }

    /**
     * Invoke a method without throwing checked exceptions
     * 
     * @param method the method to invoke
     * @param instance the instance on which to invoke the method
     * @param parameters the parameters
     * @return the result of the invocation
     */
    private static Object uncheckedInvoke(Method method, Object instance, Object... parameters) {
      try {
        return method.invoke(instance, parameters);
      } catch(IllegalAccessException | InvocationTargetException ex) {
        log.error(ex.getMessage(), ex);
      }
      return null;
    }

    /**
     * get all the arguments provided to the plugin for the provided {@link MojoExecution}
     * 
     * @param mavenProject the current maven project
     * @param execution the plugin execution
     * @param monitor the progress monitor
     * @return the arguments
     */
    @SuppressWarnings("unchecked")
    private TestLaunchArguments getTestLaunchArguments(MavenProject mavenProject, MojoExecution execution,
        IProgressMonitor monitor) {

      IMaven maven = MavenPlugin.getMaven();

      try {
        TestLaunchArguments arguments = new TestLaunchArguments();
        arguments
            .setArgLine(maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ARGLINE, String.class, monitor));
        arguments.setWorkingDirectory(
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_WORKING_DIRECTORY, File.class, monitor));
        arguments.setEnableAssertions(
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ENABLE_ASSERTIONS, Boolean.class, monitor));
        arguments.setEnvironmentVariables(
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_ENVIRONMENT_VARIABLES, Map.class, monitor));
        arguments.setSystemPropertyVariables(
            maven.getMojoParameterValue(mavenProject, execution, PLUGIN_SYSPROP_VARIABLES, Map.class, monitor));
        return arguments;
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
      return null;
    }

  }

  /**
   * Cache for maven executions
   */
  private static class MavenCache {

    /**
     * Cache for the mojos
     */
    Map<MojoExecution, Mojo> mojoCache = new HashMap<>();

    /**
     * List of executed mojos
     */
    List<Mojo> executed = new ArrayList<>();

    /**
     * Execute a mojo if not already executed
     * 
     * @param facade the maven project facade
     * @param mojoExecution the mojo execution
     * @param monitor the progress monitor
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void executeMojo(IMavenProjectFacade facade, MojoExecution mojoExecution, IProgressMonitor monitor)
        throws MojoExecutionException, MojoFailureException {
      Mojo mojo = getMojoInstance(facade, mojoExecution, monitor);

      if(mojo != null && !executed.contains(mojo)) {
        mojo.execute();
        executed.add(mojo);
      }
    }

    /**
     * Get a configured mojo instance
     * 
     * @param facade the maven project facade
     * @param mojoExecution the mojo execution
     * @param monitor the progress monitor
     * @return the mojo instance
     */
    public Mojo getMojoInstance(IMavenProjectFacade facade, MojoExecution mojoExecution, IProgressMonitor monitor) {

      if(mojoCache.containsKey(mojoExecution)) {
        return mojoCache.get(mojoExecution);
      }

      IMaven maven = MavenPlugin.getMaven();

      try {
        Mojo mojo = facade.createExecutionContext().execute(facade.getMavenProject(), (context, pm) -> {
          return maven.getConfiguredMojo(context.getSession(), mojoExecution, Mojo.class);
        }, monitor);

        mojoCache.put(mojoExecution, mojo);

        return mojo;

      } catch(CoreException ex) {
        log.error("Unable to instanciate mojo instance", ex);
      }
      return null;
    }
  }

  /**
   * Holder for the surefire/failsafe launch arguments
   */
  private static class TestLaunchArguments {
    /**
     * The argLine element
     */
    String argLine;

    /**
     * The systemPropertyVariables element
     */
    Map<String, String> systemPropertyVariables;

    /**
     * The environmentVariables element
     */
    Map<String, String> environmentVariables;

    /**
     * The workingDirectory element
     */
    File workingDirectory;

    /**
     * The enableAssertions element
     */
    boolean enableAssertions;

    public String getArgLine() {
      return this.argLine;
    }

    public void setArgLine(String argLine) {
      this.argLine = argLine;
    }

    public Map<String, String> getEnvironmentVariables() {
      return this.environmentVariables;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
      this.environmentVariables = environmentVariables;
    }

    public File getWorkingDirectory() {
      return this.workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
      this.workingDirectory = workingDirectory;
    }

    public boolean isEnableAssertions() {
      return this.enableAssertions;
    }

    public void setEnableAssertions(boolean enableAssertions) {
      this.enableAssertions = enableAssertions;
    }

    public Map<String, String> getSystemPropertyVariables() {
      return this.systemPropertyVariables;
    }

    public void setSystemPropertyVariables(Map<String, String> systemPropertyVariables) {
      this.systemPropertyVariables = systemPropertyVariables;
    }

  }

  /**
   * Holder for the execution id
   */
  private static class ExecutionId {

    String groupId;

    String artifactId;

    String goal;

    /**
     * @param groupId
     * @param artifactId
     * @param goal
     */
    public ExecutionId(String groupId, String artifactId, String goal) {
      super();
      this.groupId = groupId.trim();
      this.artifactId = artifactId.trim();
      this.goal = goal.trim();
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
      return this.groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
      return this.artifactId;
    }

    /**
     * @return the goal
     */
    public String getGoal() {
      return this.goal;
    }

  }
}
