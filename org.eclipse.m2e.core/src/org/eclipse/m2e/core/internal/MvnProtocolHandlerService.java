/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.core.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.osgi.service.url.AbstractURLStreamHandlerService;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;

public class MvnProtocolHandlerService extends AbstractURLStreamHandlerService {

	public MvnProtocolHandlerService() {
	}

	@Override
	public URLConnection openConnection(URL url) {
		return new MavenURLConnection(url);
	}

	private static final class MavenURLConnection extends URLConnection {

		private String subPath;
		private ArtifactResult artifactResult;

		protected MavenURLConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
			if (artifactResult != null) {
				return;
			}
			String path = url.getPath();
			if (path == null) {
				throw new IOException("maven coordinates are missing");
			}
			int subPathIndex = path.indexOf('/');
			
			String[] coordinates;
            if (subPathIndex > -1) {
                subPath = path.substring(subPathIndex);
                coordinates = path.substring(0, subPathIndex).split(":");
            } else {
                coordinates = path.split(":");
            }
            if (coordinates.length < 3) {
                throw new IOException(
                        "required format is groupId:artifactId:version or groupId:artifactId:version:type");
            }
            String type;
            if (coordinates.length > 3) {
                type = coordinates[3];
            } else {
                type = "jar";
            }
			Artifact artifact = new DefaultArtifact(coordinates[0], coordinates[1], type, coordinates[2]);
			try {
				IMaven maven = MavenPlugin.getMaven();
				RepositorySystem repoSystem = org.eclipse.m2e.core.internal.MavenPluginActivator.getDefault()
						.getRepositorySystem();
				IMavenExecutionContext context = maven.createExecutionContext();
				List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
				List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);
				artifactResult = context.execute(new ICallable<ArtifactResult>() {

					@Override
					public ArtifactResult call(IMavenExecutionContext context, IProgressMonitor monitor)
							throws CoreException {
						ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remoteRepositories, null);
						RepositorySystemSession session = context.getRepositorySession();
						try {
							return repoSystem.resolveArtifact(session, artifactRequest);
						} catch (ArtifactResolutionException e) {
							throw new CoreException(
									new Status(IStatus.ERROR, MvnProtocolHandlerService.class.getPackage().getName(),
											"Resolving artifact failed", e));
						}
					}
				}, new NullProgressMonitor());
			} catch (CoreException e) {
				throw new IOException("resolving artifact " + artifact + " failed", e);
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			connect();
			if (artifactResult == null || artifactResult.isMissing()) {
				throw new FileNotFoundException();
			}
			File location = artifactResult.getArtifact().getFile();
			if (subPath == null) {
				return new FileInputStream(location);
			}
			String urlSpec = "jar:" + location.toURI() + "!" + subPath;
			return new URL(urlSpec).openStream();
		}
	}

}
