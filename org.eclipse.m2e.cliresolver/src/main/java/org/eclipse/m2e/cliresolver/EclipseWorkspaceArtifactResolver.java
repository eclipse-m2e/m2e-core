
package org.eclipse.m2e.cliresolver;

import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;

import org.eclipse.m2e.cli.WorkspaceState;


public class EclipseWorkspaceArtifactResolver extends DefaultArtifactResolver {
  public void resolve(Artifact artifact, List remoteRepositories, ArtifactRepository localRepository)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolve(artifact, remoteRepositories, localRepository);
    }
  }

  public void resolveAlways(Artifact artifact, List remoteRepositories, ArtifactRepository localRepository)
      throws ArtifactResolutionException, ArtifactNotFoundException {
    if(!resolveAsEclipseProject(artifact)) {
      super.resolveAlways(artifact, remoteRepositories, localRepository);
    }
  }

  private boolean resolveAsEclipseProject(Artifact artifact) {
    Properties state = WorkspaceState.getState();

    if(state == null) {
      return false;
    }

    if(artifact == null) {
      // according to the DefaultArtifactResolver source code, it looks
      // like artifact can be null
      return false;
    }

    return WorkspaceState.resolveArtifact(artifact);
  }

}
