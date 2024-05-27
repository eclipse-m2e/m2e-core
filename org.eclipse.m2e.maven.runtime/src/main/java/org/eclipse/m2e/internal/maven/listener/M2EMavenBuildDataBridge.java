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
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge between the remote running maven and the local m2e to exchange event
 * messages and information.
 */
@Named("m2e")
@Singleton
public class M2EMavenBuildDataBridge extends AbstractMavenLifecycleParticipant {

	private static final String SOCKET_FILE_PROPERTY_NAME = "m2e.build.project.data.socket.port";
	private static final String DATA_SET_SEPARATOR = ";;";

	private static final Logger LOGGER = LoggerFactory.getLogger(M2EMavenBuildDataBridge.class);

	private SocketChannel writeChannel;

	@Override
	public void afterSessionStart(MavenSession session) throws MavenExecutionException {
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
	public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
		try {
			writeChannel.close();
		} catch (IOException e) {
			// we can't do much here anyways...
		}
	}

	boolean isActive() {
		return writeChannel != null;
	}

	void sendMessage(String msg) {
		SocketChannel channel = writeChannel;
		if (channel != null) {
			ByteBuffer buffer = ByteBuffer.wrap((msg + DATA_SET_SEPARATOR).getBytes());
			synchronized (channel) {
				while (buffer.hasRemaining()) {
					try {
						channel.write(buffer);
					} catch (IOException e) {
						LOGGER.warn("Can't forward message to m2e: " + e);
					}
				}
			}
		}
	}






	/**
	 * Prepares the connection to a {@code Maven build JVM} to be launched and is
	 * intended to be called from the Eclipse IDE JVM.
	 * 
	 * @param label           the label of the listener thread
	 * @param buildListener the listener, which is notified whenever a new
	 *                        {@link MavenProjectBuildData MavenProjectBuildDataSet}
	 *                        has arived from the Maven VM in the Eclipse-IDE VM.
	 * @return the preapre {@link MavenBuildConnection}
	 * @throws IOException
	 */
	public static MavenBuildConnection prepareConnection(String label, MavenBuildListener buildListener)
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
						if (dataSet.startsWith(M2eEventSpy.PROJECT_START_EVENT)) {
							MavenProjectBuildData buildData = MavenProjectBuildData
									.parseMavenBuildProject(
									dataSet.substring(M2eEventSpy.PROJECT_START_EVENT.length()));
							buildListener.projectStarted(buildData);
						} else if (dataSet.startsWith(M2eMojoExecutionListener.TEST_START_EVENT)) {
							String path = dataSet.substring(M2eMojoExecutionListener.TEST_START_EVENT.length());
							buildListener.onTestEvent(new MavenTestEvent(Type.MojoStarted, Paths.get(path)));
						} else if (dataSet.startsWith(M2eMojoExecutionListener.TEST_END_EVENT)) {
							String path = dataSet.substring(M2eMojoExecutionListener.TEST_END_EVENT.length());
							buildListener.onTestEvent(new MavenTestEvent(Type.MojoSucceeded, Paths.get(path)));
						} else if (dataSet.startsWith(M2eMojoExecutionListener.TEST_END_FAILED_EVENT)) {
							String path = dataSet.substring(M2eMojoExecutionListener.TEST_END_EVENT.length());
							buildListener.onTestEvent(new MavenTestEvent(Type.MojoFailed, Paths.get(path)));
						}
					}
					// Explicit cast for compatibility with covariant return type on JDK 9's
					// ByteBuffer
					((Buffer) buffer).clear();
				}
			} catch (IOException ex) { // ignore, happens if Maven process is forcibly terminated
			} finally {
				connection.readCompleted.set(true);
				buildListener.close();
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
