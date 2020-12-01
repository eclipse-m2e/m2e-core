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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.version.Version;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
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

	public static final String ELEMENT_CLASSIFIER = "classifier";
	public static final String ELEMENT_TYPE = "type";
	public static final String ELEMENT_VERSION = "version";
	public static final String ELEMENT_ARTIFACT_ID = "artifactId";
	public static final String ELEMENT_GROUP_ID = "groupId";
	public static final String ELEMENT_INSTRUCTIONS = "instructions";
	public static final String ATTRIBUTE_INSTRUCTIONS_REFERENCE = "reference";
	public static final String ATTRIBUTE_DEPENDENCY_SCOPE = "includeDependencyScope";
	public static final String ATTRIBUTE_INCLUDE_SOURCE = "includeSource";
	public static final String ATTRIBUTE_MISSING_META_DATA = "missingManifest";
	public static final String DEFAULT_DEPENDENCY_SCOPE = "";
	public static final MissingMetadataMode DEFAULT_METADATA_MODE = MissingMetadataMode.GENERATE;
	public static final String DEFAULT_PACKAGE_TYPE = "jar";
	public static final String DEPENDENCYNODE_IS_ROOT = "dependencynode.root";
	public static final String DEPENDENCYNODE_PARENT = "dependencynode.parent";

	private final String artifactId;
	private final String groupId;
	private final String version;
	private final String artifactType;
	private final String classifier;
	private final String dependencyScope;
	private final MissingMetadataMode metadataMode;
	private List<TargetBundle> targetBundles;
	private List<DependencyNode> dependencyNodes;
	private Set<Artifact> ignoredArtifacts = new HashSet<>();

	private Set<Artifact> failedArtifacts = new HashSet<>();
	private Map<String, BNDInstructions> instructionsMap = new LinkedHashMap<String, BNDInstructions>();
	private boolean includeSource;

	public MavenTargetLocation(String groupId, String artifactId, String version, String artifactType,
			String classifier, MissingMetadataMode metadataMode, String dependencyScope,
			Collection<BNDInstructions> instructions, boolean includeSource) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.artifactType = artifactType;
		this.classifier = classifier;
		this.metadataMode = metadataMode;
		this.dependencyScope = dependencyScope;
		this.includeSource = includeSource;
		for (BNDInstructions instr : instructions) {
			instructionsMap.put(instr.getKey(), instr);
		}
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		if (targetBundles == null) {
			CacheManager cacheManager = CacheManager.forTargetHandle(definition.getHandle());
			ignoredArtifacts.clear();
			targetBundles = new ArrayList<>();
			IMaven maven = MavenPlugin.getMaven();
			List<ArtifactRepository> repositories = maven.getArtifactRepositories();
			Artifact artifact = RepositoryUtils.toArtifact(maven.resolve(getGroupId(), getArtifactId(), getVersion(),
					getArtifactType(), getClassifier(), repositories, monitor));
			if (artifact != null) {
				if (dependencyScope != null && !dependencyScope.isBlank()) {
					IMavenExecutionContext context = maven.createExecutionContext();
					PreorderNodeListGenerator dependecies = context.execute(new ICallable<PreorderNodeListGenerator>() {

						@Override
						public PreorderNodeListGenerator call(IMavenExecutionContext context, IProgressMonitor monitor)
								throws CoreException {
							try {
								CollectRequest collectRequest = new CollectRequest();
								collectRequest.setRoot(new Dependency(artifact, dependencyScope));
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
								throw new CoreException(
										new Status(IStatus.ERROR, MavenTargetLocation.class.getPackage().getName(),
												"Resolving dependencies failed", e));
							} catch (RuntimeException e) {
								throw new CoreException(new Status(IStatus.ERROR,
										MavenTargetLocation.class.getPackage().getName(), "Internal error", e));
							}
						}
					}, monitor);

					for (Artifact a : dependecies.getArtifacts(true)) {
						addBundleForArtifact(a, cacheManager, maven);
					}
					dependencyNodes = dependecies.getNodes();
				} else {
					addBundleForArtifact(artifact, cacheManager, maven);
				}
			}
		}
		return targetBundles.toArray(new TargetBundle[0]);
	}

	private void addBundleForArtifact(Artifact artifact, CacheManager cacheManager, IMaven maven) {
		BNDInstructions bndInstructions = instructionsMap.get(getKey(artifact));
		if (bndInstructions == null) {
			// no specific instructions for this artifact, try using the location default
			// then
			bndInstructions = instructionsMap.get("");
		}
		TargetBundle bundle = cacheManager.getTargetBundle(artifact, bndInstructions, metadataMode);
		IStatus status = bundle.getStatus();
		if (status.isOK()) {
			targetBundles.add(bundle);
			if (includeSource) {
				try {
					Artifact resolve = RepositoryUtils.toArtifact(maven.resolve(artifact.getGroupId(),
							artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension(), "sources",
							maven.getArtifactRepositories(), new NullProgressMonitor()));
					targetBundles.add(new MavenSourceBundle(bundle.getBundleInfo(), resolve, cacheManager));
				} catch (Exception e) {
					// Source not available / usable
				}
			}
		} else if (status.matches(IStatus.CANCEL)) {
			ignoredArtifacts.add(artifact);
		} else {
			failedArtifacts.add(artifact);
			// failed ones must be added to the target as well to fail resolution of the TP
			targetBundles.add(bundle);
		}
	}

	public MavenTargetLocation update(IProgressMonitor monitor) throws CoreException {

		Artifact artifact = new DefaultArtifact(getGroupId() + ":" + getArtifactId() + ":(0,]");
		IMaven maven = MavenPlugin.getMaven();
		RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
		IMavenExecutionContext context = maven.createExecutionContext();
		List<ArtifactRepository> artifactRepositories = maven.getArtifactRepositories();
		List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);
		VersionRangeRequest request = new VersionRangeRequest(artifact, remoteRepositories, null);
		VersionRangeResult result = context.execute(new ICallable<VersionRangeResult>() {

			@Override
			public VersionRangeResult call(IMavenExecutionContext context, IProgressMonitor monitor)
					throws CoreException {
				RepositorySystemSession session = context.getRepositorySession();
				try {
					return repoSystem.resolveVersionRange(session, request);
				} catch (VersionRangeResolutionException e) {
					throw new CoreException(new Status(IStatus.ERROR, MavenTargetLocation.class.getPackage().getName(),
							"Resolving latest version failed", e));
				}
			}
		}, monitor);
		Version highestVersion = result.getHighestVersion();
		if (highestVersion == null || highestVersion.toString().equals(version)) {
			return null;
		}
		return new MavenTargetLocation(groupId, artifactId, highestVersion.toString(), artifactType, classifier,
				metadataMode, dependencyScope, instructionsMap.values(), includeSource);

	}

	public BNDInstructions getInstructions(Artifact artifact) {
		return instructionsMap.get(getKey(artifact));
	}

	private static String getKey(Artifact artifact) {
		if (artifact == null) {
			return "";
		}
		String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
		String classifier = artifact.getClassifier();
		if (classifier != null) {
			key += ":" + classifier;
		}
		key += ":" + artifact.getBaseVersion();
		return key;
	}

	public int getDependencyCount() {
		if (targetBundles == null) {
			return -1;
		}
		return targetBundles.size() - 1;
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
		return Objects.hash(artifactId, artifactType, dependencyNodes, dependencyScope, failedArtifacts, groupId,
				ignoredArtifacts, metadataMode, targetBundles, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MavenTargetLocation other = (MavenTargetLocation) obj;
		return Objects.equals(artifactId, other.artifactId) && Objects.equals(artifactType, other.artifactType)
				&& Objects.equals(dependencyNodes, other.dependencyNodes)
				&& Objects.equals(dependencyScope, other.dependencyScope)
				&& Objects.equals(failedArtifacts, other.failedArtifacts) && Objects.equals(groupId, other.groupId)
				&& Objects.equals(ignoredArtifacts, other.ignoredArtifacts) && metadataMode == other.metadataMode
				&& Objects.equals(targetBundles, other.targetBundles) && Objects.equals(version, other.version);
	}

	public boolean isIncludeSource() {
		return includeSource;
	}

	@Override
	public String serialize() {
		StringBuilder xml = new StringBuilder();
		xml.append("<location");
		attribute(xml, "type", getType());
		attribute(xml, ATTRIBUTE_MISSING_META_DATA, metadataMode.name().toLowerCase());
		attribute(xml, ATTRIBUTE_DEPENDENCY_SCOPE, dependencyScope);
		attribute(xml, ATTRIBUTE_INCLUDE_SOURCE, includeSource ? "true" : "");
		xml.append(">");
		element(xml, ELEMENT_GROUP_ID, groupId);
		element(xml, ELEMENT_ARTIFACT_ID, artifactId);
		element(xml, ELEMENT_VERSION, version);
		element(xml, ELEMENT_TYPE, artifactType);
		element(xml, ELEMENT_CLASSIFIER, classifier);
		for (BNDInstructions bnd : instructionsMap.values()) {
			String instructions = bnd.getInstructions();
			if (instructions == null || instructions.isBlank()) {
				continue;
			}
			xml.append("<" + ELEMENT_INSTRUCTIONS);
			attribute(xml, ATTRIBUTE_INSTRUCTIONS_REFERENCE, bnd.getKey());
			xml.append("><![CDATA[");
			xml.append(instructions);
			xml.append("]]></" + ELEMENT_INSTRUCTIONS + ">");
		}
		xml.append("</location>");
		return xml.toString();
	}

	private static void element(StringBuilder xml, String name, String value) {
		if (value != null && !value.isBlank()) {
			xml.append('<');
			xml.append(name);
			xml.append('>');
			xml.append(value);
			xml.append("</");
			xml.append(name);
			xml.append('>');
		}
	}

	private static void attribute(StringBuilder xml, String name, String value) {
		if (value != null && !value.isBlank()) {
			xml.append(' ');
			xml.append(name);
			xml.append('=');
			xml.append('"');
			xml.append(value);
			xml.append('"');
		}
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

	public String getClassifier() {
		return classifier;
	}

	public MissingMetadataMode getMetadataMode() {
		if (metadataMode == null) {
			return DEFAULT_METADATA_MODE;
		}
		return metadataMode;
	}

	public void refresh() {
		dependencyNodes = null;
		targetBundles = null;
		clearResolutionStatus();
	}

	public String getArtifactType() {
		if (artifactType != null && !artifactType.trim().isEmpty()) {
			return artifactType;
		}
		return DEFAULT_PACKAGE_TYPE;
	}

	public String getDependencyScope() {
		if (dependencyScope != null && !dependencyScope.trim().isEmpty()) {
			return dependencyScope;
		}
		return DEFAULT_DEPENDENCY_SCOPE;
	}

	public boolean isIgnored(Artifact artifact) {
		return ignoredArtifacts.contains(artifact);
	}

	public boolean isFailed(Artifact artifact) {
		return failedArtifacts.contains(artifact);
	}
}
