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
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.TargetBundle;

public class CacheManager {

	private static final String LASTACCESS_MARKER = ".lastaccess";

	private static File baseDir;

	private static final Map<String, CacheManager> MANAGERS = new HashMap<>();

	private volatile boolean invalidated;

	private File folder;

	public CacheManager(File folder) {
		this.folder = folder;
		try {
			FileUtils.touch(new File(LASTACCESS_MARKER));
		} catch (IOException e) {
			// can't mark last access then...
		}
	}

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

	public TargetBundle getTargetBundle(Artifact artifact, MissingMetadataMode metadataMode) {
		if (invalidated) {
			throw new IllegalStateException("invalidated location");
		}
		return new MavenTargetBundle(artifact, this, metadataMode);
	}

	public static synchronized CacheManager forTargetHandle(ITargetHandle handle) {
		if (baseDir == null) {
			throw new IllegalStateException("bundle not active");
		}
		String targetId;
		try {
			targetId = DigestUtils.sha1Hex(handle.getMemento());
		} catch (CoreException e) {
			throw new RuntimeException("creating CacheManager failed due to internal error", e);
		}
		return MANAGERS.computeIfAbsent(targetId, key -> {

			File folder = new File(baseDir, key);
			return new CacheManager(folder);
		});
	}

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

	public static synchronized void setBasedir(File baseDir) {
		CacheManager.baseDir = baseDir;
		for (CacheManager m : MANAGERS.values()) {
			m.invalidated = true;
		}
		MANAGERS.clear();
	}

	public static interface CacheConsumer<T> {
		T consume(File file) throws Exception;
	}

}
