/********************************************************************************
 * Copyright (c) 2022, 2024 Hannes Wellmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Hannes Wellmann - initial API and implementation
 *   Christoph Läubrich - factor out into dedicated component
 ********************************************************************************/
package org.eclipse.m2e.internal.maven.listener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.maven.project.MavenProject;

public final class MavenProjectBuildData {
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

	/**
	 * <p>
	 * This method is supposed to be called from M2E within the Eclipse-IDE JVM.
	 * </p>
	 * 
	 * @param dataSet the data-set to parse
	 * @return the {@link MavenProjectBuildData} parsed from the given string
	 */
	static MavenProjectBuildData parseMavenBuildProject(String dataSet) {
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

	static String serializeProjectData(MavenProject project) {
		StringJoiner data = new StringJoiner(",");
		add(data, "groupId", project.getGroupId());
		add(data, "artifactId", project.getArtifactId());
		add(data, "version", project.getVersion());
		add(data, "file", project.getFile());
		add(data, "basedir", project.getBasedir());
		add(data, "build.directory", project.getBuild().getDirectory());
		return data.toString();
	}

	private static void add(StringJoiner data, String key, Object value) {
		data.add(key + "=" + value);
	}
}