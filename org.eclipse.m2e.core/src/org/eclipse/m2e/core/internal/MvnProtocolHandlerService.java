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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


@Component(service = URLStreamHandlerService.class, property = URLConstants.URL_HANDLER_PROTOCOL + "=mvn")
public class MvnProtocolHandlerService extends AbstractURLStreamHandlerService {

  @Reference
  IMaven maven;

  @Override
  public URLConnection openConnection(URL url) {
    //TODO replace reference with IMaven maven = MavenPlugin.getMaven();
    //this is required to make the component active even if m2e itself is not running at the moment
    //that will make the m2e protocol available from the start of eclipse
    //See https://github.com/eclipse-equinox/equinox/pull/290
    return new MavenURLConnection(url, maven);
  }

  private static final class MavenURLConnection extends URLConnection {

    private String subPath;

    private ArtifactResult artifactResult;

    private IMaven maven;

    protected MavenURLConnection(URL url, IMaven maven) {
      super(url);
      this.maven = maven;
    }

    @Override
    public void connect() throws IOException {
      if(artifactResult != null) {
        return;
      }
      String path = url.getPath();
      if(path == null) {
        throw new IOException("maven coordinates are missing");
      }
      int subPathIndex = path.indexOf('/');

      String[] coordinates;
      if(subPathIndex > -1) {
        subPath = path.substring(subPathIndex);
        coordinates = path.substring(0, subPathIndex).split(":");
      } else {
        coordinates = path.split(":");
      }
      if(coordinates.length < 3) {
        throw new IOException("required format is groupId:artifactId:version[:packaging[:classifier]]");
      }
      String type = coordinates.length > 3 ? coordinates[3] : "jar";
      String classifier = coordinates.length > 4 ? coordinates[4] : null;
      Artifact artifact = new DefaultArtifact(coordinates[0], coordinates[1], classifier, type, coordinates[2]);
      try {
        RepositorySystem repoSystem = maven.lookup(RepositorySystem.class);
        IMavenExecutionContext context = maven.createExecutionContext();
        List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
        List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);
        artifactResult = context.execute(new ICallable<ArtifactResult>() {

          @Override
          public ArtifactResult call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
            ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remoteRepositories, null);
            RepositorySystemSession session = context.getRepositorySession();
            try {
              return repoSystem.resolveArtifact(session, artifactRequest);
            } catch(ArtifactResolutionException e) {
              throw new CoreException(Status.error("Resolving artifact failed", e));
            }
          }
        }, new NullProgressMonitor());
      } catch(CoreException e) {
        throw new IOException("resolving artifact " + artifact + " failed", e);
      }
    }

    @Override
    public InputStream getInputStream() throws IOException {
      connect();
      if(artifactResult == null || artifactResult.isMissing()) {
        throw new FileNotFoundException();
      }
      File location = artifactResult.getArtifact().getFile();
      if(subPath == null) {
        return new FileInputStream(location);
      }
      String urlSpec = "jar:" + location.toURI() + "!" + subPath;
      return new URL(urlSpec).openStream();
    }

    @Override
    public long getLastModified() {
      try {
        connect();
      } catch (IOException e) {
        return 0;
      }
      if(artifactResult == null || artifactResult.isMissing()) {
        return 0;
      }
      return artifactResult.getArtifact().getFile().lastModified();
    }
  }

}
