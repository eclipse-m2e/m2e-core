/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.pde;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.TargetBundle;

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
public class CacheManager {

	private static final String LASTACCESS_MARKER = ".lastaccess";

	private static File baseDir;

	private static final Map<String, CacheManager> MANAGERS = new HashMap<>();

	private volatile boolean invalidated;

	private final File folder;

	private CacheManager(File folder) {
		this.folder = folder;
		try {
			FileUtils.touch(new File(folder, LASTACCESS_MARKER));
		} catch (IOException e) {
			// can't mark last access then...
		}
	}

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
		File gavFolder = new File(folder,
				artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion());
		FileUtils.forceMkdir(gavFolder);
		File file = new File(gavFolder, artifact.getFile().getName());
		File lockFile = new File(gavFolder, artifact.getFile().getName() + ".lock");
		lockFile.deleteOnExit();
		try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw"); FileChannel channel = raf.getChannel()) {
			FileLock lock = channel.lock();
			try {
				return consumer.consume(file);
			} finally {
				lock.release();
			}
		}
	}

	/**
	 * Get the requested Artifact as a {@link TargetBundle}, might wrap the content
	 * as indicated by the {@link MissingMetadataMode}
	 *
	 * @param artifact        the artifact to acquire
	 * @param bndInstructions
	 * @param metadataMode    the mode to use if this artifact is not a bundle
	 * @return
	 */
	public MavenTargetBundle getTargetBundle(Artifact artifact, BNDInstructions bndInstructions,
			MissingMetadataMode metadataMode) {
		if (invalidated) {
			throw new IllegalStateException("invalidated location");
		}
		Properties prop;
		if (bndInstructions == null) {
			prop = BNDInstructions.getDefaultInstructions().asProperties();
		} else {
			prop = bndInstructions.asProperties();
		}
		return new MavenTargetBundle(artifact, prop, this, metadataMode);
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
		if (baseDir == null) {
			throw new IllegalStateException("bundle not active");
		}
		String targetId = DigestUtils.sha1Hex(handle.getMemento());
		return MANAGERS.computeIfAbsent(targetId, key -> {

			File folder = new File(baseDir, key);
			return new CacheManager(folder);
		});
	}

	/**
	 * Clears all inactive cache locations that are older than the given time
	 *
	 * @param amount the amount of time to use
	 * @param unit   the unit of time to use
	 */
	public static synchronized void clearFilesOlderThan(long amount, TimeUnit unit) {
		if (baseDir == null) {
			throw new IllegalStateException("bundle not active");
		}
		File[] listFiles = baseDir.listFiles();
		long deleteStamp = System.currentTimeMillis() - unit.toMillis(amount);
		if (listFiles != null) {
			for (File folder : listFiles) {
				if (folder.isDirectory() && !MANAGERS.containsKey(folder.getName())) {
					File marker = new File(folder, LASTACCESS_MARKER);
					if (marker.isFile() && FileUtils.isFileOlder(marker, deleteStamp)) {
						try {
							FileUtils.deleteDirectory(folder);
						} catch (IOException e) {
						}
					}
				}
			}
		}
	}

	/**
	 * Set the basedir to use, purge all existing managers and invalidate them
	 *
	 * @param baseDir
	 */
	static synchronized void setBasedir(File baseDir) {
		CacheManager.baseDir = baseDir;
		for (CacheManager m : MANAGERS.values()) {
			m.invalidated = true;
		}
		MANAGERS.clear();
	}

	/**
	 * Consumer for a cache location that allows to throw checked exceptions and
	 * return a value
	 *
	 * @param <T> the return type
	 */
	public interface CacheConsumer<T> {
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
