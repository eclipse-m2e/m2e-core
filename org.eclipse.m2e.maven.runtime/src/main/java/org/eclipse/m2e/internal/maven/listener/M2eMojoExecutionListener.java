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
package org.eclipse.m2e.internal.maven.listener;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.google.inject.Inject;

@Named("m2e")
@Singleton
public class M2eMojoExecutionListener implements MojoExecutionListener {

	private final ConverterLookup converterLookup = new DefaultConverterLookup();
	static final String TEST_START_EVENT = "TEST_MOJO_START#";
	static final String TEST_END_EVENT = "TEST_MOJO_END_SUCCESS#";
	static final String TEST_END_FAILED_EVENT = "TEST_MOJO_END_FAILED#";
	private M2EMavenBuildDataBridge bridge;
	private Map<MojoExecution, Path> testExecutionPathMap = new ConcurrentHashMap<>();
	private BuildPluginManager buildPluginManager;

	@Inject
	public M2eMojoExecutionListener(M2EMavenBuildDataBridge bridge, BuildPluginManager buildPluginManager) {
		this.bridge = bridge;
		this.buildPluginManager = buildPluginManager;
	}

	@Override
	public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
		if (!bridge.isActive()) {
			return;
		}
		MojoExecution execution = event.getExecution();
		// it would be better if we have a common interface in plexus-build api that
		// mojos can implement instead then we just need to query for the interface
		boolean bndTest = isBndTest(execution);
		if (isSurefireTest(execution) || isFailsafeTest(execution) || bndTest || isTychoSurefire(execution)) {
			File reportsDirectory = getMojoParameterValue(execution, bndTest ? "reportsDir" : "reportsDirectory",
					File.class, event.getSession());
			if (reportsDirectory != null) {
				testExecutionPathMap.put(execution, reportsDirectory.toPath());
				bridge.sendMessage(TEST_START_EVENT + reportsDirectory.getAbsolutePath());
			}
		}
	}

	private static boolean isTychoSurefire(MojoExecution execution) {
		return execution.getPlugin().getId().startsWith("org.eclipse.tycho:tycho-surefire-plugin:")
				&& ("test".equals(execution.getGoal()) || "plugin-test".equals(execution.getGoal())
						|| "bnd-test".equals(execution.getGoal()));

	}

	private static boolean isFailsafeTest(MojoExecution execution) {
		return execution.getPlugin().getId().startsWith("org.apache.maven.plugins:maven-failsafe-plugin:")
				&& "integration-test".equals(execution.getGoal());
	}

	private static boolean isSurefireTest(MojoExecution execution) {
		return execution.getPlugin().getId().startsWith("org.apache.maven.plugins:maven-surefire-plugin:")
				&& "test".equals(execution.getGoal());
	}

	private static boolean isBndTest(MojoExecution execution) {
		return execution.getPlugin().getId().startsWith("biz.aQute.bnd:bnd-testing-maven-plugin:")
				&& "testing".equals(execution.getGoal());
	}

	private void afterMojoExecution(MojoExecutionEvent event, String type) {
		if (!bridge.isActive()) {
			return;
		}
		Path testPath = testExecutionPathMap.remove(event.getExecution());
		if (testPath != null) {
			bridge.sendMessage(type + testPath.toAbsolutePath().toString());
		}
	}

	private <T> T getMojoParameterValue(MojoExecution mojoExecution, String parameter, Class<T> asType,
			MavenSession session) {
		Xpp3Dom dom = mojoExecution.getConfiguration();
		if (dom == null) {
			return null;
		}
		PlexusConfiguration configuration = new XmlPlexusConfiguration(dom).getChild(parameter);
		if (configuration == null) {
			return null;
		}
		MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
		ClassRealm pluginRealm;
		try {
			pluginRealm = buildPluginManager.getPluginRealm(session, mojoDescriptor.getPluginDescriptor());
			ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
			ConfigurationConverter typeConverter = converterLookup.lookupConverterForType(asType);

			Object value = typeConverter.fromConfiguration(converterLookup, configuration, asType,
					mojoDescriptor.getImplementationClass(), pluginRealm, expressionEvaluator, null);
			return asType.cast(value);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
		afterMojoExecution(event, TEST_END_EVENT);
	}

	@Override
	public void afterExecutionFailure(MojoExecutionEvent event) {
		afterMojoExecution(event, TEST_END_FAILED_EVENT);

	}

}
