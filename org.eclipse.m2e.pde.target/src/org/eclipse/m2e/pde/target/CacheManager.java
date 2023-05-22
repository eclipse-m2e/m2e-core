/*******************************************************************************
 * Copyright (c) 2020, 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.file.DeletingPathVisitor;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetHandle;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * The cache manager serves the following purpose:
 * <ul>
 * <li>adding a synchronization point for code working on the same (effective)
 * target model (e.g. active target in eclipse and a target editor opened)</li>
 * <li>prevent different processes/jvm to access the same file using
 * file-locks</li>
 * <li>providing a storage area for each target for example if different targets
 * reference the same maven artifact but with different properties</li>
 * <li>allows to remove target content that has not been used for a long
 * time</li>
 * </ul>
 */
class CacheManager {

	private static final String LASTACCESS_MARKER = ".lastaccess";

	private static Path baseDir;

	private static final Map<String, CacheManager> MANAGERS = new HashMap<>();

	private final Path folder;

	private CacheManager(String cacheKey) {
		this.folder = getCacheBaseDir().resolve(cacheKey);
		try {
			Files.writeString(folder.resolve(LASTACCESS_MARKER), ""); // touch last access
		} catch (IOException e) {
			// can't mark last access then...
		}
	}

	private static final Set<OpenOption> LOCK_FILE_OPEN_OPTIONS = Set.of(StandardOpenOption.CREATE,
			StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);

	/**
	 * Allows synchronized and locked access to the given artifact, the consumer is
	 * called with the file that represents the artifact at this cache location
	 * (what might not exits).
	 *
	 * @param <R>      the return value type
	 * @param artifact the artifact identifier to be used
	 * @param consumer the consumer that is handed over control of the file for the
	 *                 time of the invocation of the
	 *                 {@link CacheConsumer#consume(File)} method
	 * @return the value that is returned from the consumers call to
	 *         {@link CacheConsumer#consume(File)}
	 * @throws Exception if either there is an error while acquiring necessary
	 *                   system-resources locks or any exception thrown by the
	 *                   consumer itself
	 */
	public synchronized <R> R accessArtifactFile(Artifact artifact, CacheConsumer<R> consumer) throws Exception {
		Path gav = Path.of(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
		Path gavFolder = Files.createDirectories(folder.resolve(gav));
		Path file = gavFolder.resolve(artifact.getFile().getName());
		Path lockFile = gavFolder.resolve(artifact.getFile().getName() + ".lock");
		try (FileChannel channel = FileChannel.open(lockFile, LOCK_FILE_OPEN_OPTIONS); FileLock lock = channel.lock()) {
			return consumer.consume(file.toFile());
		}
	}

	/**
	 * Gives access to the {@link CacheManager} for the given {@link ITargetHandle},
	 * the handle must support the {@link ITargetHandle#getMemento()} for this to
	 * work
	 *
	 * @param handle the handle for what a {@link CacheManager} is requested
	 * @return the {@link CacheManager} that should be used to access files from
	 *         this location
	 * @throws CoreException if unable to generate a memento from the given
	 *                       {@link ITargetHandle}
	 */
	public static synchronized CacheManager forTargetHandle(ITargetHandle handle) throws CoreException {
		String targetId = DigestUtils.sha1Hex(handle.getMemento());
		return MANAGERS.computeIfAbsent(targetId, CacheManager::new);
	}

	/**
	 * Clears all inactive cache locations that are older than the given time
	 *
	 * @param timeout the duration since the last use after which a CacheManager is
	 *                deleted
	 */
	private static synchronized void clearFilesOlderThan(Path cacheBaseDir, Duration timeout) {
		FileTime deleteBefore = FileTime.from(Instant.now().minus(timeout));
		try (DirectoryStream<Path> folders = Files.newDirectoryStream(cacheBaseDir)) {
			for (Path folder : folders) {
				// Delete cache-folders last used before the specified timeout
				// and not used in any manager
				if (!MANAGERS.containsKey(folder.getFileName().toString())) {
					Path marker = folder.resolve(LASTACCESS_MARKER);
					if (Files.isRegularFile(marker) && Files.getLastModifiedTime(marker).compareTo(deleteBefore) < 0) {
						Files.walkFileTree(folder, DeletingPathVisitor.withLongCounters());
					}
				}
			}
		} catch (IOException e) { // ignore exceptions
			Platform.getLog(CacheManager.class).log(Status.error("Failed to clear Maven bundle cache", e));
		}
	}

	private static synchronized Path getCacheBaseDir() {
		if (baseDir == null) {
			Bundle bundle = FrameworkUtil.getBundle(CacheManager.class);
			if (bundle == null) {
				throw new IllegalStateException(CacheManager.class.getSimpleName() + " not loaded from a bundle");
			}
			baseDir = bundle.getDataFile("").toPath();
			// clear all locations older than 14 days, this can be improved by
			// 1) watch for changes in the workspace -> if target is deleted/removed from
			// workspace we can clear the cache
			// 2) we can add a preference page where the user can force clearing the cache
			// or set the cache days
			Runnable cleaner = () -> clearFilesOlderThan(baseDir, Duration.ofDays(14));
			new Thread(cleaner, "Wrapped bundles cache cleaner").start();
		}
		return baseDir;
	}

	/**
	 * Consumer for a cache location that allows to throw checked exceptions and
	 * return a value
	 *
	 * @param <T> the return type
	 */
	interface CacheConsumer<T> {
		T consume(File file) throws Exception;
	}

	public static boolean isOutdated(File cacheFile, File sourceFile) {
		if (cacheFile.exists()) {
			long sourceTimeStamp = sourceFile.lastModified();
			long cacheTimeStamp = cacheFile.lastModified();
			return sourceTimeStamp > cacheTimeStamp;
		}
		return true;
	}

}
