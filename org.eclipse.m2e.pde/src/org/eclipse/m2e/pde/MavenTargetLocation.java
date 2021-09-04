/*******************************************************************************
 * Copyright (c) 2018, 2021 Christoph Läubrich
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

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
import org.eclipse.core.runtime.SubMonitor;
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
	public static final String ELEMENT_EXCLUDED = "exclude";
	public static final String ELEMENT_DEPENDENCY = "dependency";
	public static final String ELEMENT_DEPENDENCIES = "dependencies";
	public static final String ELEMENT_REPOSITORY = "repository";
	public static final String ELEMENT_REPOSITORY_ID = "id";
	public static final String ELEMENT_REPOSITORY_URL = "url";
	public static final String ELEMENT_REPOSITORIES = "repositories";

	public static final String ATTRIBUTE_INSTRUCTIONS_REFERENCE = "reference";
	public static final String ATTRIBUTE_DEPENDENCY_SCOPE = "includeDependencyScope";
	public static final String ATTRIBUTE_INCLUDE_SOURCE = "includeSource";
	public static final String ATTRIBUTE_MISSING_META_DATA = "missingManifest";
	public static final String DEFAULT_DEPENDENCY_SCOPE = "";
	public static final MissingMetadataMode DEFAULT_METADATA_MODE = MissingMetadataMode.GENERATE;
	public static final String DEFAULT_PACKAGE_TYPE = "jar";
	public static final String POM_PACKAGE_TYPE = "pom";
	public static final String DEPENDENCYNODE_PARENT = "dependencynode.parent";
	public static final String DEPENDENCYNODE_ROOT = "dependencynode.root";

	private final String dependencyScope;
	private final MissingMetadataMode metadataMode;
	private Map<Artifact, TargetBundle> targetBundles;
	private final Map<MavenTargetDependency, List<DependencyNode>> dependencyNodes = new ConcurrentHashMap<>();
	private final Set<Artifact> ignoredArtifacts = new HashSet<>();
	private final Set<String> excludedArtifacts = new HashSet<>();
	private final Set<Artifact> failedArtifacts = new HashSet<>();
	private final Map<String, BNDInstructions> instructionsMap = new LinkedHashMap<>();
	private final boolean includeSource;
	private final List<MavenTargetDependency> roots;
	private final List<MavenTargetRepository> extraRepositories;

	public MavenTargetLocation(Collection<MavenTargetDependency> rootDependecies,
			Collection<MavenTargetRepository> extraRepositories, MissingMetadataMode metadataMode,
			String dependencyScope, boolean includeSource, Collection<BNDInstructions> instructions,
			Collection<String> excludes) {
		this.roots = new ArrayList<MavenTargetDependency>(rootDependecies);
		this.extraRepositories = Collections.unmodifiableList(new ArrayList<>(extraRepositories));
		this.metadataMode = metadataMode;
		this.dependencyScope = dependencyScope;
		this.includeSource = includeSource;
		for (BNDInstructions instr : instructions) {
			instructionsMap.put(instr.getKey(), instr);
		}
		excludedArtifacts.addAll(excludes);
		for (MavenTargetDependency root : roots) {
			root.bind(this);
		}
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		if (targetBundles == null) {
			CacheManager cacheManager = CacheManager.forTargetHandle(definition.getHandle());
			ignoredArtifacts.clear();
			targetBundles = new HashMap<>();
			IMaven maven = MavenPlugin.getMaven();
			List<ArtifactRepository> repositories = new ArrayList<>(maven.getArtifactRepositories());
			for (MavenTargetRepository extraRepository : extraRepositories) {
				ArtifactRepository repository = maven.createArtifactRepository(extraRepository.getId(),
						extraRepository.getUrl());
				repositories.add(repository);
			}
			SubMonitor subMonitor = SubMonitor.convert(monitor, roots.size() * 100);
			for (MavenTargetDependency root : roots) {
				resolveDependency(root, maven, repositories, cacheManager, subMonitor.split(100));
			}
		}
		TargetBundle[] bundles = targetBundles.entrySet().stream().filter(e -> !isExcluded(e.getKey()))
				.map(Entry::getValue).toArray(TargetBundle[]::new);
		return bundles;
	}

	public List<MavenTargetRepository> getExtraRepositories() {
		return extraRepositories;
	}

	private void resolveDependency(MavenTargetDependency root, IMaven maven, List<ArtifactRepository> repositories,
			CacheManager cacheManager, IProgressMonitor monitor) throws CoreException {
		Artifact artifact = RepositoryUtils.toArtifact(maven.resolve(root.getGroupId(), root.getArtifactId(),
				root.getVersion(), root.getType(), root.getClassifier(), repositories, monitor));
		if (artifact != null) {
			boolean isPomType = POM_PACKAGE_TYPE.equals(artifact.getExtension());
			if (isPomType || (dependencyScope != null && !dependencyScope.isBlank())) {
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
							node.setData(DEPENDENCYNODE_PARENT, MavenTargetLocation.this);
							node.setData(DEPENDENCYNODE_ROOT, root);
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
				dependencyNodes.put(root, dependecies.getNodes());
			} else {
				addBundleForArtifact(artifact, cacheManager, maven);
			}
		}
	}

	private void addBundleForArtifact(Artifact artifact, CacheManager cacheManager, IMaven maven) {
		if (POM_PACKAGE_TYPE.equals(artifact.getExtension())) {
			// pom typed artifacts are not for bundeling --> TODO we should generate a
			// feature from them!
			return;
		}
		BNDInstructions bndInstructions = instructionsMap.get(getKey(artifact));
		if (bndInstructions == null) {
			// no specific instructions for this artifact, try using the location default
			// then
			bndInstructions = instructionsMap.get("");
		}
		TargetBundle bundle = cacheManager.getTargetBundle(artifact, bndInstructions, metadataMode);
		IStatus status = bundle.getStatus();
		if (status.isOK()) {
			targetBundles.put(artifact, bundle);
			if (includeSource) {
				try {
					Artifact resolve = RepositoryUtils.toArtifact(maven.resolve(artifact.getGroupId(),
							artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension(), "sources",
							maven.getArtifactRepositories(), new NullProgressMonitor()));
					targetBundles.put(resolve, new MavenSourceBundle(bundle.getBundleInfo(), resolve, cacheManager));
				} catch (Exception e) {
					// Source not available / usable
				}
			}
		} else if (status.matches(IStatus.CANCEL)) {
			ignoredArtifacts.add(artifact);
		} else {
			failedArtifacts.add(artifact);
			// failed ones must be added to the target as well to fail resolution of the TP
			targetBundles.put(artifact, bundle);
		}
	}

	public MavenTargetLocation update(IProgressMonitor monitor) throws CoreException {

		List<MavenTargetDependency> latest = new ArrayList<MavenTargetDependency>();
		int updated = 0;
		for (MavenTargetDependency dependency : roots) {
			Artifact artifact = new DefaultArtifact(
					dependency.getGroupId() + ":" + dependency.getArtifactId() + ":(0,]");
			IMaven maven = MavenPlugin.getMaven();
			RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
			IMavenExecutionContext context = maven.createExecutionContext();
			List<ArtifactRepository> repositories = new ArrayList<>(maven.getArtifactRepositories());
			for (MavenTargetRepository extraRepository : extraRepositories) {
				ArtifactRepository repository = maven.createArtifactRepository(extraRepository.getId(),
						extraRepository.getUrl());
				repositories.add(repository);
			}
			List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(repositories);
			VersionRangeRequest request = new VersionRangeRequest(artifact, remoteRepositories, null);
			VersionRangeResult result = context.execute(new ICallable<VersionRangeResult>() {

				@Override
				public VersionRangeResult call(IMavenExecutionContext context, IProgressMonitor monitor)
						throws CoreException {
					RepositorySystemSession session = context.getRepositorySession();
					try {
						return repoSystem.resolveVersionRange(session, request);
					} catch (VersionRangeResolutionException e) {
						throw new CoreException(
								new Status(IStatus.ERROR, MavenTargetLocation.class.getPackage().getName(),
										"Resolving latest version failed", e));
					}
				}
			}, monitor);
			Version highestVersion = result.getHighestVersion();
			if (highestVersion == null || highestVersion.toString().equals(dependency.getVersion())) {
				latest.add(dependency.copy());
			} else {
				latest.add(new MavenTargetDependency(dependency.getGroupId(), dependency.getArtifactId(),
						highestVersion.toString(), dependency.getType(), dependency.getClassifier()));
				updated++;
			}
		}
		if (updated == 0) {
			return null;
		}

		return new MavenTargetLocation(latest, extraRepositories, metadataMode, dependencyScope, includeSource,
				instructionsMap.values(), excludedArtifacts);

	}

	public List<MavenTargetDependency> getRoots() {
		return roots;
	}

	public MavenTargetLocation withInstructions(Collection<BNDInstructions> instructions) {
		return new MavenTargetLocation(roots, extraRepositories, metadataMode, dependencyScope, includeSource,
				instructions, excludedArtifacts);
	}

	public BNDInstructions getInstructions(Artifact artifact) {
		String key = getKey(artifact);
		BNDInstructions bnd = instructionsMap.get(key);
		if (bnd == null) {
			return new BNDInstructions(key, null);
		}
		return bnd;
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

	List<DependencyNode> getDependencyNodes(MavenTargetDependency dependency) {
		return dependencyNodes.get(dependency);
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
		return Objects.hash(roots, dependencyNodes, dependencyScope, failedArtifacts, ignoredArtifacts, metadataMode,
				targetBundles);
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
		return Objects.equals(roots, other.roots) && Objects.equals(dependencyNodes, other.dependencyNodes)
				&& Objects.equals(dependencyScope, other.dependencyScope)
				&& Objects.equals(failedArtifacts, other.failedArtifacts)
				&& Objects.equals(ignoredArtifacts, other.ignoredArtifacts) && metadataMode == other.metadataMode
				&& Objects.equals(targetBundles, other.targetBundles);
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
		if (!roots.isEmpty()) {
			xml.append("<" + ELEMENT_DEPENDENCIES + ">");
			roots.stream().sorted(Comparator.comparing(MavenTargetDependency::getKey)).forEach(dependency -> {
				xml.append("<" + ELEMENT_DEPENDENCY + ">");
				element(xml, ELEMENT_GROUP_ID, dependency.getGroupId());
				element(xml, ELEMENT_ARTIFACT_ID, dependency.getArtifactId());
				element(xml, ELEMENT_VERSION, dependency.getVersion());
				element(xml, ELEMENT_TYPE, dependency.getType());
				element(xml, ELEMENT_CLASSIFIER, dependency.getClassifier());
				xml.append("</" + ELEMENT_DEPENDENCY + ">");
			});
			xml.append("</" + ELEMENT_DEPENDENCIES + ">");
		}
		if (!extraRepositories.isEmpty()) {
			xml.append("<" + ELEMENT_REPOSITORIES + ">");
			extraRepositories.stream().sorted(Comparator.comparing(MavenTargetRepository::getUrl))
					.forEach(repository -> {
						xml.append("<" + ELEMENT_REPOSITORY + ">");
						element(xml, ELEMENT_REPOSITORY_ID, repository.getId());
						element(xml, ELEMENT_REPOSITORY_URL, repository.getUrl());
						xml.append("</" + ELEMENT_REPOSITORY + ">");
					});
			xml.append("</" + ELEMENT_REPOSITORIES + ">");
		}
		instructionsMap.values().stream().filter(Predicate.not(BNDInstructions::isEmpty))
				.sorted(Comparator.comparing(BNDInstructions::getKey)).forEach(bnd -> {
					String instructions = bnd.getInstructions();
					xml.append("<" + ELEMENT_INSTRUCTIONS);
					attribute(xml, ATTRIBUTE_INSTRUCTIONS_REFERENCE, bnd.getKey());
					xml.append("><![CDATA[\r\n");
					xml.append(instructions);
					xml.append("\r\n]]></" + ELEMENT_INSTRUCTIONS + ">");
				});
		excludedArtifacts.stream().sorted().forEach(ignored -> {
			element(xml, ELEMENT_EXCLUDED, ignored);
		});
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

	public MissingMetadataMode getMetadataMode() {
		if (metadataMode == null) {
			return DEFAULT_METADATA_MODE;
		}
		return metadataMode;
	}

	public void refresh() {
		dependencyNodes.clear();
		targetBundles = null;
		clearResolutionStatus();
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

	public boolean isExcluded(Artifact artifact) {
		return excludedArtifacts.contains(getKey(artifact));
	}

	public void setExcluded(Artifact artifact, boolean disabled) {
		if (disabled) {
			excludedArtifacts.add(getKey(artifact));
		} else {
			excludedArtifacts.remove(getKey(artifact));
		}
	}

	public MavenTargetBundle getMavenTargetBundle(Artifact artifact) {
		TargetBundle targetBundle = targetBundles.get(artifact);
		if (targetBundle instanceof MavenTargetBundle) {
			return (MavenTargetBundle) targetBundle;
		}
		return null;
	}

	public MavenTargetBundle getMavenTargetBundle(MavenTargetDependency dependency) {
		List<DependencyNode> list = dependencyNodes.get(dependency);
		if (list != null) {
			for (DependencyNode node : list) {
				if (node.getData().get(DEPENDENCYNODE_ROOT) == dependency) {
					return getMavenTargetBundle(node.getArtifact());
				}
			}
		}
		return null;
	}

	public Collection<String> getExcludes() {
		return Collections.unmodifiableCollection(excludedArtifacts);
	}

	public Collection<BNDInstructions> getInstructions() {
		return Collections.unmodifiableCollection(instructionsMap.values());
	}

}
