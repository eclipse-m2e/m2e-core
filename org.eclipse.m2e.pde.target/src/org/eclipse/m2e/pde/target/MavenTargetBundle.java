/*******************************************************************************
 * Copyright (c) 2018, 2023 Christoph Läubrich and others
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
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.RepositoryUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.pde.target.shared.MavenBundleWrapper;
import org.eclipse.m2e.pde.target.shared.ProcessingMessage;
import org.eclipse.m2e.pde.target.shared.WrappedBundle;
import org.eclipse.pde.core.target.TargetBundle;

public class MavenTargetBundle extends TargetBundle {

	private static final ILog LOGGER = Platform.getLog(MavenTargetBundle.class);
	private TargetBundle bundle;
	private IStatus status;
	private final BundleInfo bundleInfo;
	private boolean isWrapped;
	private Artifact artifact;

	@Override
	public BundleInfo getBundleInfo() {
		if (bundle == null) {
			return bundleInfo;
		}
		return bundle.getBundleInfo();
	}

	@Override
	public boolean isSourceBundle() {
		return bundle != null && bundle.isSourceBundle();
	}

	@Override
	public BundleInfo getSourceTarget() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourceTarget();
	}

	@Override
	public boolean isFragment() {
		return bundle != null && bundle.isFragment();
	}

	@Override
	public String getSourcePath() {
		if (bundle == null) {
			return null;
		}
		return bundle.getSourcePath();
	}

	public MavenTargetBundle(Artifact artifact, MavenTargetLocation location, IProgressMonitor monitor) {
		this.artifact = artifact;
		File file = artifact.getFile();
		this.bundleInfo = new BundleInfo(artifact.getGroupId() + "." + artifact.getArtifactId(), artifact.getVersion(),
				file != null ? file.toURI() : null, -1, false);
		try {
			bundle = new TargetBundle(file);
		} catch (Exception ex) {
			MissingMetadataMode metadataMode = location.getMetadataMode();
			if (metadataMode == MissingMetadataMode.ERROR) {
				status = Status.error(artifact + " is not a bundle", ex);
				LOGGER.log(status);
			} else if (metadataMode == MissingMetadataMode.GENERATE) {
				try {
					bundle = getWrappedArtifact(artifact, location, monitor);
					isWrapped = true;
				} catch (Exception e) {
					// not possible then
					String message = artifact + " is not a bundle and cannot be automatically bundled as such ";
					if (e.getMessage() != null) {
						message += " (" + e.getMessage() + ")";
					}
					status = Status.error(message, e);
					LOGGER.log(status);
				}
			} else {
				status = Status.CANCEL_STATUS;
				LOGGER.log(status);
			}
		}
	}

	public Artifact getArtifact() {
		return artifact;
	}

	private static TargetBundle getWrappedArtifact(Artifact artifact, MavenTargetLocation location,
			IProgressMonitor monitor) throws Exception {
		IMaven maven = MavenPlugin.getMaven();
		List<RemoteRepository> repositories = RepositoryUtils.toRepos(location.getAvailableArtifactRepositories(maven));
		Function<DependencyNode, Properties> instructionsLookup = node -> {
			BNDInstructions instructions = location.getInstructionsForArtifact(node.getArtifact());
			return instructions == null ? BNDInstructions.getDefaultInstructionProperties()
					: instructions.asProperties();
		};
		IMavenExecutionContext exeContext = IMavenExecutionContext.getThreadContext()
				.orElseGet(maven::createExecutionContext);

		Path wrappedBundle = exeContext.execute((context, monitor1) -> {
			RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
			RepositorySystemSession repositorySession = context.getRepositorySession();
			try {
				WrappedBundle wrap = MavenBundleWrapper.getWrappedArtifact(artifact, instructionsLookup, repositories,
						repoSystem, repositorySession, context.getComponentLookup().lookup(SyncContextFactory.class));
				List<ProcessingMessage> errors = wrap.messages()
						.filter(msg -> msg.type() == ProcessingMessage.Type.ERROR).toList();
				if (errors.isEmpty()) {
					return wrap.getFile();
				}
				if (errors.size() == 1) {
					throw new CoreException(Status.error(errors.get(0).message()));
				}
				MultiStatus multiStatus = new MultiStatus(MavenTargetBundle.class, IStatus.ERROR,
						"wrapping artifact " + artifact.getArtifactId() + " failed!");
				for (ProcessingMessage message : errors) {
					multiStatus.add(Status.error(message.message()));
				}
				throw new CoreException(multiStatus);
			} catch (CoreException e) {
				throw e;
			} catch (Exception e) {
				throw new CoreException(Status.error("Can't collect dependencies!", e));
			}
		}, monitor);
		return new TargetBundle(wrappedBundle.toFile());

	}

	public boolean isWrapped() {
		return isWrapped;
	}

	@Override
	public IStatus getStatus() {
		if (bundle == null) {
			if (status == null) {
				return Status.OK_STATUS;
			}
			return status;
		}
		return bundle.getStatus();
	}

	@Override
	public int hashCode() {
		return getBundleInfo().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof MavenTargetBundle other && getBundleInfo().equals(other.getBundleInfo());
	}

}
