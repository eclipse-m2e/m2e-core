/*******************************************************************************
 * Copyright (c) 2018, 2020 Christoph Läubrich
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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;

@SuppressWarnings("restriction")
public class MavenTargetLocation extends AbstractBundleContainer {

	public static final String DEPENDENCYNODE_IS_ROOT = "dependencynode.root";
	public static final String DEPENDENCYNODE_PARENT = "dependencynode.parent";

	private final String artifactId;
	private final String groupId;
	private final String version;
	private final boolean includeDependencies;
	private final String artifactType;
	private final String dependencyScope;
	private final MissingMetadataMode metadataMode;
	private List<TargetBundle> targetBundles;
	private List<DependencyNode> dependencyNodes;

	public MavenTargetLocation(String groupId, String artifactId, String version, String artifactType,
			MissingMetadataMode metadataMode, boolean includeDependencies, String dependencyScope) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.artifactType = artifactType;
		this.metadataMode = metadataMode;
		this.includeDependencies = includeDependencies;
		this.dependencyScope = dependencyScope;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		if (targetBundles == null) {
			targetBundles = new ArrayList<>();
			IMaven maven = MavenPlugin.getMaven();
			List<ArtifactRepository> repositories = maven.getArtifactRepositories();
			Artifact artifact = RepositoryUtils.toArtifact(maven.resolve(getGroupId(), getArtifactId(), getVersion(),
					getArtifactType(), null, repositories, monitor));
			if (artifact != null) {
				if (includeDependencies) {
					IMavenExecutionContext context = maven.createExecutionContext();
					PreorderNodeListGenerator dependecies = context.execute(new ICallable<PreorderNodeListGenerator>() {

						@Override
						public PreorderNodeListGenerator call(IMavenExecutionContext context, IProgressMonitor monitor)
								throws CoreException {
							try {
								CollectRequest collectRequest = new CollectRequest();
								collectRequest.setRoot(new Dependency(artifact, getDependencyScope()));
								collectRequest.setRepositories(RepositoryUtils.toRepos(repositories));

								RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
								DependencyNode node = repoSystem
										.collectDependencies(context.getRepositorySession(), collectRequest).getRoot();
								node.setData(DEPENDENCYNODE_IS_ROOT, true);
								node.setData(DEPENDENCYNODE_PARENT, MavenTargetLocation.this);
								DependencyRequest dependencyRequest = new DependencyRequest();
								dependencyRequest.setRoot(node);
								repoSystem.resolveDependencies(context.getRepositorySession(), dependencyRequest);
								PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
								node.accept(nlg);
								return nlg;
							} catch (RepositoryException e) {
								e.printStackTrace();
								throw new CoreException(
										new Status(IStatus.ERROR, MavenTargetLocation.class.getPackage().getName(),
												"Resolving dependencies failed", e));
							} catch (RuntimeException e) {
								e.printStackTrace();
								throw new CoreException(new Status(IStatus.ERROR,
										MavenTargetLocation.class.getPackage().getName(), "Internal error", e));
							}
						}
					}, monitor);

					for (Artifact a : dependecies.getArtifacts(true)) {
						TargetBundle bundle = createTargetBundle(a);
						if (!bundle.getStatus().matches(IStatus.CANCEL)) {
							targetBundles.add(bundle);
						}
					}
					dependencyNodes = dependecies.getNodes();
				} else {
					TargetBundle bundle = createTargetBundle(artifact);
					if (!bundle.getStatus().matches(IStatus.CANCEL)) {
						targetBundles.add(bundle);
					}
				}
			}
		}
		return targetBundles.toArray(new TargetBundle[0]);
	}

	public int getDependencyCount() {
		if (targetBundles == null) {
			return -1;
		}
		return targetBundles.size() - 1;
	}

	private TargetBundle createTargetBundle(Artifact artifact) {
		File file = artifact.getFile();
		BundleInfo bundleInfo = new BundleInfo(artifact.getGroupId() + "." + artifact.getArtifactId(),
				artifact.getVersion(), file != null ? file.toURI() : null, -1, false);
		return new MavenTargetBundle(bundleInfo, file, metadataMode);
	}

	public List<DependencyNode> getDependencyNodes() {
		return dependencyNodes;
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		// XXX it would be possible to deploy features as maven artifacts, are there any
		// examples?
		return new TargetFeature[] {};
	}

	@Override
	public String getType() {
		return "Maven";
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		return System.getProperty("java.io.tmpdir");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + (includeDependencies ? 1231 : 1237);
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MavenTargetLocation other = (MavenTargetLocation) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (includeDependencies != other.includeDependencies)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String serialize() {
		StringBuilder xml = new StringBuilder();
		xml.append("<location type=\"");
		xml.append(getType());
		xml.append("\" missingMetaData=\"");
		xml.append(metadataMode.name().toLowerCase());
		xml.append("\" includeDependencies=\"");
		xml.append(includeDependencies);
		xml.append("\" dependencyScope=\"");
		xml.append(dependencyScope);
		xml.append("\" >");
		xml.append("<groupId>");
		xml.append(groupId);
		xml.append("</groupId>");
		xml.append("<artifactId>");
		xml.append(artifactId);
		xml.append("</artifactId>");
		xml.append("<version>");
		xml.append(version);
		xml.append("</version>");
		xml.append("<type>");
		xml.append(artifactType);
		xml.append("</type>");
		xml.append("</location>");
		return xml.toString();
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getVersion() {
		return version;
	}

	public boolean isIncludeDependencies() {
		return includeDependencies;
	}

	public MissingMetadataMode getMetadataMode() {
		if (metadataMode == null) {
			return MissingMetadataMode.AUTOMATED;
		}
		return metadataMode;
	}

	public void refresh() {
		dependencyNodes = null;
		targetBundles = null;
	}

	public String getArtifactType() {
		if (artifactType != null && !artifactType.trim().isEmpty()) {
			return artifactType;
		}
		return "jar";
	}

	public String getDependencyScope() {
		if (dependencyScope != null && !dependencyScope.trim().isEmpty()) {
			return dependencyScope;
		}
		return "compile";
	}
}
