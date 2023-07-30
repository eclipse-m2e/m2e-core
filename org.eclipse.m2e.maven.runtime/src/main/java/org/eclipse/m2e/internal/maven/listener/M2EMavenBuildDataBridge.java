/********************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 ********************************************************************************/

package org.eclipse.m2e.internal.maven.listener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link EventSpy} listens to certain events within a Maven build JVM and
 * sends certain data (e.g. about projects being built) to the JVM of the
 * Eclipse IDE that launched the Maven build JVM.
 * 
 * @author Hannes Wellmann
 *
 */
@Named
@Singleton
public class M2EMavenBuildDataBridge implements EventSpy {

	private static final String SOCKET_FILE_PROPERTY_NAME = "m2e.build.project.data.socket.port";
	private static final String DATA_SET_SEPARATOR = ";;";

	private static final Logger LOGGER = LoggerFactory.getLogger(M2EMavenBuildDataBridge.class);

	private SocketChannel writeChannel;

	@Override
	public void init(Context context) throws IOException {
		String socketPort = System.getProperty(SOCKET_FILE_PROPERTY_NAME);
		if (socketPort != null) {
			try {
				// TODO: replace by the following once Java-17 is required by Maven:
				// SocketAddress address = UnixDomainSocketAddress.of(socketPort);
				int port = Integer.parseInt(socketPort);
				SocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
				this.writeChannel = SocketChannel.open(address);
			} catch (Exception e) {
				LOGGER.warn("Failed to establish connection to Eclipse-M2E", e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		writeChannel.close();
	}

	@Override
	public void onEvent(Object event) throws Exception {
		if (writeChannel != null && event instanceof ExecutionEvent
				&& ((ExecutionEvent) event).getType() == Type.ProjectStarted) {

			String message = serializeProjectData(((ExecutionEvent) event).getProject());

			ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
			synchronized (writeChannel) {
				while (buffer.hasRemaining()) {
					writeChannel.write(buffer);
				}
			}
		}
	}

	private static String serializeProjectData(MavenProject project) {
		StringJoiner data = new StringJoiner(",");
		add(data, "groupId", project.getGroupId());
		add(data, "artifactId", project.getArtifactId());
		add(data, "version", project.getVersion());
		add(data, "file", project.getFile());
		add(data, "basedir", project.getBasedir());
		add(data, "build.directory", project.getBuild().getDirectory());
		return data.toString() + DATA_SET_SEPARATOR;
	}

	private static void add(StringJoiner data, String key, Object value) {
		data.add(key + "=" + value);
	}

	/**
	 * <p>
	 * This method is supposed to be called from M2E within the Eclipse-IDE JVM.
	 * </p>
	 * 
	 * @param dataSet the data-set to parse
	 * @return the {@link MavenProjectBuildData} parsed from the given string
	 */
	private static MavenProjectBuildData parseMavenBuildProject(String dataSet) {
		Map<String, String> data = new HashMap<>(8);
		for (String entry : dataSet.split(",")) {
			String[] keyValue = entry.split("=");
			if (keyValue.length != 2) {
				throw new IllegalStateException("Invalid data-set format" + dataSet);
			}
			data.put(keyValue[0], keyValue[1]);
		}
		return new MavenProjectBuildData(data);
	}

	public static final class MavenProjectBuildData {
		public final String groupId;
		public final String artifactId;
		public final String version;
		public final Path projectBasedir;
		public final Path projectFile;
		public final Path projectBuildDirectory;

		MavenProjectBuildData(Map<String, String> data) {
			if (data.size() != 6) {
				throw new IllegalArgumentException();
			}
			this.groupId = Objects.requireNonNull(data.get("groupId"));
			this.artifactId = Objects.requireNonNull(data.get("artifactId"));
			this.version = Objects.requireNonNull(data.get("version"));
			this.projectBasedir = Paths.get(data.get("basedir"));
			this.projectFile = Paths.get(data.get("file"));
			this.projectBuildDirectory = Paths.get(data.get("build.directory"));
		}
	}

	/**
	 * Prepares the connection to a {@code Maven build JVM} to be launched and is
	 * intended to be called from the Eclipse IDE JVM.
	 * 
	 * @param label           the label of the listener thread
	 * @param datasetListener the listener, which is notified whenever a new
	 *                        {@link MavenProjectBuildData MavenProjectBuildDataSet}
	 *                        has arived from the Maven VM in the Eclipse-IDE VM.
	 * @return the preapre {@link MavenBuildConnection}
	 * @throws IOException
	 */
	public static MavenBuildConnection prepareConnection(String label, Consumer<MavenProjectBuildData> datasetListener)
			throws IOException {

//	    TODO: use UNIX domain socket once Java-17 is required by Maven
//	    Path socketFile = Files.createTempFile("m2e.maven.build.listener", ".socket");
//	    socketFile.toFile().deleteOnExit();
//	    Files.delete(socketFile); // file must not exist when the server-socket is bound

//	    ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
//	    server.bind(UnixDomainSocketAddress.of(socketFile));
		ServerSocketChannel server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

		MavenBuildConnection connection = new MavenBuildConnection(server);

		Thread reader = new Thread(() -> {
			try (ServerSocketChannel s = server; SocketChannel readChannel = server.accept()) {
				ByteBuffer buffer = ByteBuffer.allocate(512);
				StringBuilder message = new StringBuilder();
				while (readChannel.read(buffer) >= 0) {
					message.append(new String(buffer.array(), 0, buffer.position()));
					for (int terminatorIndex; (terminatorIndex = message.indexOf(DATA_SET_SEPARATOR)) >= 0;) {
						String dataSet = message.substring(0, terminatorIndex);
						message.delete(0, terminatorIndex + DATA_SET_SEPARATOR.length());

						MavenProjectBuildData buildData = parseMavenBuildProject(dataSet);
						datasetListener.accept(buildData);
					}
					// Explicit cast for compatibility with covariant return type on JDK 9's
					// ByteBuffer
					((Buffer) buffer).clear();
				}
			} catch (IOException ex) { // ignore, happens if Maven process is forcibly terminated
			} finally {
				connection.readCompleted.set(true);
			}
//	      try {
//	        Files.deleteIfExists(socketFile);
//	      } catch(IOException ex) { // ignore
//	      }
		}, "M2E Maven build <" + label + "> connection reader");
		reader.setDaemon(true);
		reader.start();
		return connection;
	}

	public static final class MavenBuildConnection {
		private final ServerSocketChannel server;
		private final AtomicBoolean readCompleted = new AtomicBoolean(false);

		MavenBuildConnection(ServerSocketChannel server) {
			this.server = server;
		}

		public String getMavenVMArguments() throws IOException {
			String port = Integer.toString(((InetSocketAddress) server.getLocalAddress()).getPort());
			return "-D" + SOCKET_FILE_PROPERTY_NAME + "=" + port;
		}

		public boolean isReadCompleted() {
			return readCompleted.get();
		}

		public void close() throws IOException {
			// Close the server to ensure the reader-thread does not wait forever for a
			// connection from the Maven-process in case something went wrong during
			// launching or while setting up the connection.
			server.close();
		}
	}

}
