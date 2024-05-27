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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.MavenSession;
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
@Singleton
@Named("m2e")
public class M2EMavenBuildDataBridge extends AbstractMavenLifecycleParticipant {

	private static final String SOCKET_FILE_PROPERTY_NAME = "m2e.build.project.data.socket.port";
	private static final String DATA_SET_SEPARATOR = ";;";

	private static final Logger LOGGER = LoggerFactory.getLogger(M2EMavenBuildDataBridge.class);

	private SocketChannel writeChannel;

	@Override
	public synchronized void afterSessionStart(MavenSession session) throws MavenExecutionException {
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
		close();
	}

	private synchronized void close() {
		if (writeChannel != null) {
			try {
				writeChannel.close();
			} catch (IOException e) {
				// nothing we want to do here...
			}
		}
		writeChannel = null;
	}

	public synchronized boolean isActive() {
		return writeChannel != null;
	}

	public synchronized void sendMessage(String message) {
		if (writeChannel == null) {
			return;
		}
		ByteBuffer buffer = ByteBuffer.wrap((message + DATA_SET_SEPARATOR).getBytes());
		while (buffer.hasRemaining()) {
			try {
				writeChannel.write(buffer);
			} catch (IOException e) {
				// channel seems dead...
				close();
			}
		}
	}

	/**
	 * Prepares the connection to a {@code Maven build JVM} to be launched and is
	 * intended to be called from the Eclipse IDE JVM.
	 * 
	 * @param label    the label of the listener thread
	 * @param listener the listener, which is notified whenever a new
	 *                 {@link MavenProjectBuildData MavenProjectBuildDataSet} has
	 *                 arived from the Maven VM in the Eclipse-IDE VM.
	 * @return the maven arguments
	 * @throws IOException if init of connection failed
	 */
	public static String openConnection(String label, MavenBuildConnectionListener listener) throws IOException {

//	    TODO: use UNIX domain socket once Java-17 is required by Maven
//	    Path socketFile = Files.createTempFile("m2e.maven.build.listener", ".socket");
//	    socketFile.toFile().deleteOnExit();
//	    Files.delete(socketFile); // file must not exist when the server-socket is bound

//	    ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
//	    server.bind(UnixDomainSocketAddress.of(socketFile));
		ServerSocketChannel server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));

		MavenBuildConnection connection = new MavenBuildConnection(server, listener);

		Thread reader = new Thread(() -> {
			try (ServerSocketChannel s = server; SocketChannel readChannel = server.accept()) {
				ByteBuffer buffer = ByteBuffer.allocate(512);
				StringBuilder message = new StringBuilder();
				while (readChannel.read(buffer) >= 0) {
					message.append(new String(buffer.array(), 0, buffer.position()));
					for (int terminatorIndex; (terminatorIndex = message.indexOf(DATA_SET_SEPARATOR)) >= 0;) {
						String dataSet = message.substring(0, terminatorIndex);
						message.delete(0, terminatorIndex + DATA_SET_SEPARATOR.length());
						MavenProjectBuildData buildData = MavenProjectBuildData.parseMavenBuildProject(dataSet);
						listener.onData(buildData);
					}
					// Explicit cast for compatibility with covariant return type on JDK 9's
					// ByteBuffer
					((Buffer) buffer).clear();
				}
			} catch (IOException ex) { // ignore, happens if Maven process is forcibly terminated
			} finally {
				connection.close();
			}
//	      try {
//	        Files.deleteIfExists(socketFile);
//	      } catch(IOException ex) { // ignore
//	      }
		}, "M2E Maven build <" + label + "> connection reader");
		reader.setDaemon(true);
		reader.start();
		listener.onOpen(label, connection);
		String port = Integer.toString(((InetSocketAddress) server.getLocalAddress()).getPort());
		return "-D" + SOCKET_FILE_PROPERTY_NAME + "=" + port;
	}
}
