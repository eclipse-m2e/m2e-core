/*******************************************************************************
 * Copyright (c) 2020, 2025 Christoph Läubrich and others.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;


@Component(service = URLStreamHandlerService.class, property = URLConstants.URL_HANDLER_PROTOCOL + "=mvn")
public class MvnProtocolHandlerService extends AbstractURLStreamHandlerService implements DebugOptionsListener {

  private static final String ID = "org.eclipse.m2e.core";

  private static final String OPTION = "/mvnProtocolHandler";

  private final IMaven maven;

  private volatile DebugTrace debugTrace;

  private final ServiceRegistration<DebugOptionsListener> serviceRegistration;

  @Activate
  public MvnProtocolHandlerService(@Reference IMaven maven, BundleContext bundleContext) {
    this.maven = maven;
    //We need to register this manually here to we are lazy as otherwise it would activate this component immediately 
    Hashtable<String, Object> hashtable = new Hashtable<>();
    hashtable.put(DebugOptions.LISTENER_SYMBOLICNAME, ID);
    serviceRegistration = bundleContext.registerService(DebugOptionsListener.class, this, hashtable);
  }

  @Deactivate
  void shutdown() {
    serviceRegistration.unregister();
  }
  @Override
  public void optionsChanged(DebugOptions options) {
    if(options.getBooleanOption(ID + OPTION, false)) {
      this.debugTrace = options.newDebugTrace(ID, MvnProtocolHandlerService.class);
    } else {
      this.debugTrace = null;
    }
  }

  @Override
  public URLConnection openConnection(URL url) {
    return new MavenURLConnection(url, maven, debugTrace);
  }

  private static final class MavenURLConnection extends URLConnection {

    private String subPath;

    private ArtifactResult artifactResult;

    private final IMaven maven;

    private final DebugTrace debugTrace;

    protected MavenURLConnection(URL url, IMaven maven, DebugTrace debugTrace) {
      super(url);
      this.maven = maven;
      this.debugTrace = debugTrace;
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
      if(debugTrace != null) {
        debugTrace.trace(OPTION, "connect to " + url);
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
        boolean isSnapshot = artifact.getBaseVersion().endsWith("-SNAPSHOT");
        if(isSnapshot && !context.getExecutionRequest().isUpdateSnapshots()) {
          //if a snapshot version is requested, always force an update!
          context.getExecutionRequest().setUpdateSnapshots(true);
        }
        List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
        List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);
        if(debugTrace != null) {
          debugTrace.trace(OPTION, "Fetching artifact " + artifact + " using "
              + remoteRepositories.stream().map(rp -> rp.getUrl()).collect(Collectors.joining(", "))
              + " with update snapshots = " + context.getExecutionRequest().isUpdateSnapshots());
        }
        artifactResult = context.execute(new ICallable<ArtifactResult>() {

          @Override
          public ArtifactResult call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
            ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remoteRepositories, null);
            RepositorySystemSession session = context.getRepositorySession();
            try {
              return repoSystem.resolveArtifact(session, artifactRequest);
            } catch(ArtifactResolutionException e) {
              if(debugTrace != null) {
                debugTrace.trace(OPTION, "resolving of artifact " + artifact + " failed!", e);
              }
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
        if(debugTrace != null) {
          debugTrace.trace(OPTION, "Open stream to artifact file " + location);
        }
        return new FileInputStream(location);
      }
      String urlSpec = "jar:" + location.toURI() + "!" + subPath;
      try {
        if(debugTrace != null) {
          debugTrace.trace(OPTION, "Open stream to subpath in artifact file " + urlSpec);
        }
        return new URI(urlSpec).toURL().openStream();
      } catch(URISyntaxException ex) {
        throw new IOException(ex);
      }
    }

    @Override
    public long getLastModified() {
      try {
        connect();
      } catch(IOException e) {
        return 0;
      }
      if(artifactResult == null || artifactResult.isMissing()) {
        return 0;
      }
      return artifactResult.getArtifact().getFile().lastModified();
    }
  }

}
