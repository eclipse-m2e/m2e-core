package org.eclipse.m2e.debug.runtime;

import static java.util.Map.entry;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.UnaryOperator;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DebugEventSpy extends AbstractEventSpy {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugEventSpy.class);

	private static final String DEBUG_PORT_VARIABLE = "${debugPort}";
	public static final String DEBUG_DATA_PROPERTY = "m2e.debug.support"; //$NON-NLS-1$

	public static String getEnableDebugProperty() {
		return "-D" + DebugEventSpy.DEBUG_DATA_PROPERTY + "=" + "true";
	}

	// TODO: name this entire fragment m2e.debug.extension to reflect that it is an
	// extension to the maven-runtime?

	// TODO: specify the debug setup of the supported plug-ins at a more suitable
	// location. Ideally within the Maven-plugins directly (like for lifecycle
	// mappings) LifecycleMappingFactory.readMavenPluginEmbeddedMetadata(), or with
	// life-cylce mappins/connectors
	// TODO: specify properties per Goal!
	private static final Map<String, Entry<String, UnaryOperator<String>>> ID_2_CONFIGURATION_INJECTOR = Map.of(
			"org.apache.maven.plugins:maven-surefire-plugin", //
			entry("debugForkedProcess",
					v -> "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + DEBUG_PORT_VARIABLE
							+ " -Xnoagent -Djava.compiler=NONE"), // TODO: use Java-9 approach: agentlib:jdwp ?
			"org.eclipse.tycho:tycho-surefire-plugin", entry("debugPort", v -> DEBUG_PORT_VARIABLE),
			"org.eclipse.tycho.extras:tycho-eclipserun-plugin", //
			entry("argLine", // TODO: use not deprecated approach with nested elements in jvmArgs
					v -> "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + DEBUG_PORT_VARIABLE
							+ " -Xnoagent")); // TODO: use Java-9 approach: agentlib:jdwp ?

	private boolean isDebugLaunch = false;

	@Override
	public void init(Context context) throws Exception {
		Map<String, Object> data = context.getData();
		Object userProperties = data.get("userProperties");
		if (userProperties instanceof Properties) {
			Properties properties = (Properties) userProperties;
			isDebugLaunch = Boolean.parseBoolean(properties.getProperty(DEBUG_DATA_PROPERTY, "false"));
		}
		if (isDebugLaunch) {
			LOGGER.debug("M2E - Automatic debugging of forked VMs enabled");
		}
	}

	@Override
	public void close() throws Exception {
		isDebugLaunch = false;
	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (isDebugLaunch && event instanceof ExecutionEvent) {
			ExecutionEvent executionEvent = (ExecutionEvent) event;
			if (executionEvent.getType() == Type.MojoStarted) {
				MojoExecution mojoExecution = executionEvent.getMojoExecution();
				String id = mojoExecution.getGroupId() + ":" + mojoExecution.getArtifactId();

				Entry<String, UnaryOperator<String>> injector = ID_2_CONFIGURATION_INJECTOR.get(id);
				if (injector != null) {
					Xpp3Dom configuration = mojoExecution.getConfiguration();
					int debugPort = getFreeDebugPort();
					String debugElementName = injector.getKey();
					Xpp3Dom debugElement = getOrCreateChild(configuration, debugElementName);
					injectDebugSetupIntoConfiguration(debugElement, injector.getValue(), debugPort);

					LOGGER.debug("M2E - Injected debug-port {} into configuration element <{}>", debugPort,
							debugElementName);
				}
			}
		}
	}

	private void injectDebugSetupIntoConfiguration(Xpp3Dom debugElement, UnaryOperator<String> valueComputer,
			int debugPort) {
		String enhancedValue = valueComputer.apply(debugElement.getValue());
		enhancedValue = enhancedValue.replace(DEBUG_PORT_VARIABLE, Integer.toString(debugPort));
		debugElement.setValue(enhancedValue);
		// TODO: handle other cases
		// - nested elements
		// - set(but do not overwrite),
		// - append (check if already contained?)
	}

	private static Xpp3Dom getOrCreateChild(Xpp3Dom configuration, String name) {
		Xpp3Dom child = configuration.getChild(name);
		if (child != null) {
			child.setInputLocation(null); // disconnect from actual source
		} else {
			child = new Xpp3Dom(name);
			configuration.addChild(child);
		}
		return child;
	}

	private static int getFreeDebugPort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			return 0;
		}
	}
}
