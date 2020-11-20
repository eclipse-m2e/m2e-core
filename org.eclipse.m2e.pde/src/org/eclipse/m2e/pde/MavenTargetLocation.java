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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

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

	public static final String ELEMENT_TYPE = "type";
	public static final String ELEMENT_VERSION = "version";
	public static final String ELEMENT_ARTIFACT_ID = "artifactId";
	public static final String ELEMENT_GROUP_ID = "groupId";
	public static final String ELEMENT_INSTRUCTIONS = "instructions";
	public static final String ELEMENT_EXCLUDE = "exclude";
	public static final String ATTRIBUTE_INSTRUCTIONS_REFERENCE = "reference";
	public static final String ATTRIBUTE_DEPENDENCY_SCOPE = "includeDependencyScope";
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
	private final String dependencyScope;
	private final MissingMetadataMode metadataMode;
	private Map<Artifact, MavenTargetBundle> targetBundles;
	private List<DependencyNode> dependencyNodes;
	private Set<Artifact> ignoredArtifacts = new HashSet<>();

	private Set<Artifact> failedArtifacts = new HashSet<>();
	private Set<String> disabledArtifacts = new HashSet<>();
	private Map<String, BNDInstructions> instructionsMap = new LinkedHashMap<String, BNDInstructions>();

	public MavenTargetLocation(String groupId, String artifactId, String version, String artifactType,
			MissingMetadataMode metadataMode, String dependencyScope, Collection<BNDInstructions> instructions,
			Collection<String> disabledArtifacts) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.artifactType = artifactType;
		this.metadataMode = metadataMode;
		this.dependencyScope = dependencyScope;
		for (BNDInstructions instr : instructions) {
			if (instr.getKey().isEmpty()) {
				instructionsMap.put(instr.getKey(), instr);
			} else {
				instructionsMap.put(instr.getKey(), instr.withParent(() -> getDefaultInstructions()));
			}
		}
		this.disabledArtifacts.addAll(disabledArtifacts);
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		if (targetBundles == null) {
			CacheManager cacheManager = CacheManager.forTargetHandle(definition.getHandle());
			ignoredArtifacts.clear();
			targetBundles = new LinkedHashMap<Artifact, MavenTargetBundle>();
			IMaven maven = MavenPlugin.getMaven();
			List<ArtifactRepository> repositories = maven.getArtifactRepositories();
			Artifact artifact = RepositoryUtils.toArtifact(maven.resolve(getGroupId(), getArtifactId(), getVersion(),
					getArtifactType(), null, repositories, monitor));
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
						addBundleForArtifact(a, cacheManager);
					}
					dependencyNodes = dependecies.getNodes();
				} else {
					addBundleForArtifact(artifact, cacheManager);
				}
			}
		}
		return targetBundles.values().stream()
				.filter(Predicate.not(b -> disabledArtifacts.contains(getKey(b.getArtifact()))))
				.toArray(TargetBundle[]::new);
	}

	private void addBundleForArtifact(Artifact artifact, CacheManager cacheManager) {
		BNDInstructions bndInstructions = instructionsMap.get(getKey(artifact));
		if (bndInstructions == null || bndInstructions.isEmpty()) {
			// no specific instructions for this artifact, try using the location default
			// then
			bndInstructions = instructionsMap.get("");
		}
		MavenTargetBundle bundle = cacheManager.getTargetBundle(artifact, bndInstructions, metadataMode);
		IStatus status = bundle.getStatus();
		if (status.isOK()) {
			targetBundles.put(artifact, bundle);
		} else if (status.matches(IStatus.CANCEL)) {
			ignoredArtifacts.add(artifact);
		} else {
			failedArtifacts.add(artifact);
			// failed ones must be added to the target as well to fail resolution of the TP
			targetBundles.put(artifact, bundle);
		}
	}

	public MavenTargetBundle getTargetBundle(Artifact artifact) {
		return targetBundles.get(artifact);
	}

	public Set<String> getDisabledArtifacts() {
		return Collections.unmodifiableSet(disabledArtifacts);
	}

	public Collection<BNDInstructions> getAllInstructions() {
		return Collections.unmodifiableCollection(instructionsMap.values());
	}

	public BNDInstructions getDefaultInstructions() {
		return instructionsMap.getOrDefault("", BNDInstructions.EMPTY);
	}

	public BNDInstructions getInstructions(Artifact artifact) {
		if (artifact == null) {
			return getDefaultInstructions();
		}
		String key = getKey(artifact);
		return instructionsMap.getOrDefault(key, new BNDInstructions(key, null, () -> getDefaultInstructions()));
	}

	private static String getKey(Artifact artifact) {
		if (artifact == null) {
			return "";
		}
		String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
		String classifier = artifact.getClassifier();
		if (classifier != null && !classifier.isBlank()) {
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

	@Override
	public String serialize() {
		StringBuilder xml = new StringBuilder();
		xml.append("<location type=\"");
		xml.append(getType());
		xml.append("\" " + ATTRIBUTE_MISSING_META_DATA + "=\"");
		xml.append(metadataMode.name().toLowerCase());
		xml.append("\" " + ATTRIBUTE_DEPENDENCY_SCOPE + "=\"");
		if (dependencyScope != null && !dependencyScope.isBlank()) {
			xml.append(dependencyScope);
		}
		xml.append("\" >");
		stringElement(ELEMENT_GROUP_ID, groupId, xml);
		stringElement(ELEMENT_ARTIFACT_ID, artifactId, xml);
		stringElement(ELEMENT_VERSION, version, xml);
		stringElement(ELEMENT_TYPE, artifactType, xml);
		for (BNDInstructions bnd : instructionsMap.values()) {
			if (bnd.isEmpty()) {
				continue;
			}
			String instructions = bnd.getInstructions();
			xml.append("<" + ELEMENT_INSTRUCTIONS);
			String key = bnd.getKey();
			if (key != null && !key.isBlank()) {
				xml.append(" ");
				xml.append(ATTRIBUTE_INSTRUCTIONS_REFERENCE);
				xml.append("=\"");
				xml.append(key);
				xml.append("\"");
			}
			xml.append("><![CDATA[");
			xml.append(instructions);
			xml.append("]]></" + ELEMENT_INSTRUCTIONS + ">");
		}
		for (String artifact : disabledArtifacts) {
			stringElement(ELEMENT_EXCLUDE, artifact, xml);
		}
		xml.append("</location>");
		return xml.toString();
	}

	private static void stringElement(String elementName, String content, StringBuilder xml) {
		xml.append("<");
		xml.append(elementName);
		xml.append(">");
		xml.append(content);
		xml.append("</");
		xml.append(elementName);
		xml.append(">");
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

	public boolean isDisabled(Artifact artifact) {
		return disabledArtifacts.contains(getKey(artifact));
	}

	public void setDisabled(Artifact artifact, boolean disabled) {
		if (disabled) {
			disabledArtifacts.add(getKey(artifact));
		} else {
			disabledArtifacts.remove(getKey(artifact));
		}
	}

	public boolean setInstructions(Artifact artifact, BNDInstructions instructions) {
		if (instructions == null) {
			return instructionsMap.remove(getKey(artifact)) != null;
		}
		BNDInstructions replaced = instructionsMap.put(getKey(artifact), instructions);
		if (replaced != null) {
			return !replaced.equals(instructions);
		}
		return true;
	}
}
