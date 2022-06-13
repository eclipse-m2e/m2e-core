/*******************************************************************************
 * Copyright (c) 2018, 2022 Christoph Läubrich
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Christoph Läubrich - initial API and implementation
 *      Patrick Ziegler - Support contribution of Eclipse features via Maven repositories
 *******************************************************************************/
package org.eclipse.m2e.pde.target;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
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
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class MavenTargetLocation extends AbstractBundleContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MavenTargetLocation.class);
	private static final String SOURCE_SUFFIX = ".source";
	private static final String NOT_A_FEATURE = "not_a_feature";
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
	public static final String ELEMENT_FEATURE = "feature";

	public static final String ATTRIBUTE_LABEL = "label";
	public static final String ATTRIBUTE_INSTRUCTIONS_REFERENCE = "reference";

	public static final String ATTRIBUTE_DEPENDENCY_DEPTH = "includeDependencyDepth";

	public static final String ATTRIBUTE_DEPENDENCY_SCOPES = "includeDependencyScopes";
	public static final String ATTRIBUTE_INCLUDE_SOURCE = "includeSource";
	public static final String ATTRIBUTE_MISSING_META_DATA = "missingManifest";
	public static final List<String> DEFAULT_DEPENDENCY_SCOPES = List
			.of(org.apache.maven.artifact.Artifact.SCOPE_COMPILE);
	public static final MissingMetadataMode DEFAULT_METADATA_MODE = MissingMetadataMode.GENERATE;
	public static final String DEFAULT_PACKAGE_TYPE = "jar";
	public static final String POM_PACKAGE_TYPE = "pom";
	public static final String DEPENDENCYNODE_PARENT = "dependencynode.parent";
	public static final String DEPENDENCYNODE_ROOT = "dependencynode.root";
	public static final DependencyDepth DEFAULT_INCLUDE_MODE = DependencyDepth.NONE;

	private final Collection<String> dependencyScopes;
	private final MissingMetadataMode metadataMode;
	private TargetBundles targetBundles;

	private final Set<String> excludedArtifacts = new HashSet<>();
	private final Set<Artifact> failedArtifacts = new HashSet<>();
	private final Map<String, BNDInstructions> instructionsMap = new LinkedHashMap<>();
	private final boolean includeSource;
	private final List<MavenTargetDependency> roots;
	private final List<MavenTargetRepository> extraRepositories;
	private final IFeature featureTemplate;
	private String label;
	private DependencyDepth dependencyDepth;

	public MavenTargetLocation(String label, Collection<MavenTargetDependency> rootDependecies,
			Collection<MavenTargetRepository> extraRepositories, MissingMetadataMode metadataMode,
			DependencyDepth dependencyDepth, Collection<String> dependencyScopes, boolean includeSource,
			Collection<BNDInstructions> instructions, Collection<String> excludes, IFeature featureTemplate) {
		this.label = label;
		this.dependencyDepth = dependencyDepth;
		this.featureTemplate = featureTemplate;
		this.roots = new ArrayList<>(rootDependecies);
		this.extraRepositories = Collections.unmodifiableList(new ArrayList<>(extraRepositories));
		this.metadataMode = metadataMode;
		this.dependencyScopes = dependencyScopes;
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
		return resolveArtifacts(definition, monitor).stream().flatMap(tb -> tb.bundles.entrySet().stream())
				.filter(e -> !isExcluded(e.getKey())).map(Entry::getValue).toArray(TargetBundle[]::new);
	}

	private synchronized Optional<TargetBundles> resolveArtifacts(ITargetDefinition definition,
			IProgressMonitor monitor) throws CoreException {
		if (targetBundles == null && definition != null) {
			CacheManager cacheManager = CacheManager.forTargetHandle(definition.getHandle());
			TargetBundles bundles = new TargetBundles();
			IMaven maven = MavenPlugin.getMaven();
			List<ArtifactRepository> repositories = getAvailableArtifactRepositories(maven);
			SubMonitor subMonitor = SubMonitor.convert(monitor, roots.size() * 100);
			for (MavenTargetDependency root : roots) {
				if (subMonitor.isCanceled()) {
					break;
				}
				resolveDependency(root, maven, repositories, bundles, cacheManager, subMonitor.split(100));
			}
			if (featureTemplate != null) {
				generateFeature(bundles, false);
				if (includeSource) {
					generateFeature(bundles, true);
				}
			}
			Iterator<IModel> iterator = bundles.features.stream().map(tf -> tf.getFeatureModel()).iterator();
			while (iterator.hasNext()) {
				IModel model = iterator.next();
				model.load();
			}
			if (subMonitor.isCanceled()) {
				return Optional.empty();
			}
			targetBundles = bundles;
		}
		return Optional.ofNullable(targetBundles);
	}

	public String getLabel() {
		return label;
	}

	private void generateFeature(TargetBundles bundles, boolean source) throws CoreException {
		Predicate<TargetBundle> bundleFilter = TargetBundle::isSourceBundle;
		TemplateFeatureModel featureModel = new TemplateFeatureModel(featureTemplate);
		featureModel.load();
		IFeature feature = featureModel.getFeature();
		if (source) {
			feature.setId(feature.getId() + SOURCE_SUFFIX);
			String label = feature.getLabel();
			if (label != null && !label.isBlank()) {
				feature.setLabel(label + " (source)");
			}
			for (IFeaturePlugin plugin : feature.getPlugins()) {
				if (!plugin.getId().endsWith(SOURCE_SUFFIX)) {
					feature.removePlugins(new IFeaturePlugin[] { plugin });
				}
			}
		} else {
			bundleFilter = Predicate.not(bundleFilter);
		}
		Iterator<TargetBundle> featurePlugins = bundles.bundles.entrySet().stream() //
				.filter(e -> !isExcluded(e.getKey()) && !isIgnored(e.getKey()))//
				.map(Entry::getValue)//
				.filter(bundleFilter)//
				.sorted(Comparator.comparing(TargetBundle::getBundleInfo,
						Comparator.comparing(BundleInfo::getSymbolicName)))
				.iterator();
		while (featurePlugins.hasNext()) {
			TargetBundle targetBundle = featurePlugins.next();
			feature.addPlugins(new IFeaturePlugin[] { new MavenFeaturePlugin(targetBundle, featureModel) });
		}
		featureModel.makeReadOnly();
		bundles.features.add(new MavenTargetFeature(featureModel));
	}

	public List<MavenTargetRepository> getExtraRepositories() {
		return extraRepositories;
	}

	private Artifact resolveDependency(MavenTargetDependency root, IMaven maven, List<ArtifactRepository> repositories,
			TargetBundles targetBundles, CacheManager cacheManager, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
		IMavenProjectRegistry registry = MavenPlugin.getMavenProjectRegistry();
		IMavenProjectFacade workspaceProject = registry.getMavenProject(root.getGroupId(), root.getArtifactId(),
				root.getVersion());
		Artifact artifact;
		if (workspaceProject != null && workspaceProject.getPackaging().equals(root.getType())) {
			MavenProject mavenProject = workspaceProject.getMavenProject(subMonitor.split(80));
			artifact = new WorkspaceArtifact(RepositoryUtils.toArtifact(mavenProject.getArtifact()), workspaceProject);
		} else {
			artifact = RepositoryUtils.toArtifact(maven.resolve(root.getGroupId(), root.getArtifactId(),
					root.getVersion(), root.getType(), root.getClassifier(), repositories, subMonitor.split(80)));
		}
		if (artifact != null) {
			DependencyDepth depth = dependencyDepth;
			if (POM_PACKAGE_TYPE.equals(artifact.getExtension()) && depth == DependencyDepth.NONE) {
				// fetching only the pom but no dependencies does not makes much sense...
				depth = DependencyDepth.DIRECT;
			}
			if (depth == DependencyDepth.DIRECT || depth == DependencyDepth.INFINITE) {
				ICallable<PreorderNodeListGenerator> callable = new DependencyNodeGenerator(root, artifact, depth,
						dependencyScopes, repositories, this);
				PreorderNodeListGenerator dependecies;
				if (workspaceProject == null) {
					dependecies = maven.createExecutionContext().execute(callable, subMonitor);
				} else {
					dependecies = registry.execute(workspaceProject, callable, subMonitor);
				}
				for (Artifact a : dependecies.getArtifacts(true)) {
					if (a.getFile() == null) {
						// this is a filtered dependency
						continue;
					}
					addBundleForArtifact(a, cacheManager, maven, targetBundles);
				}
				targetBundles.dependencyNodes.put(root, dependecies.getNodes());
			} else {
				addBundleForArtifact(artifact, cacheManager, maven, targetBundles);
			}
		}

		return artifact;
	}

	private boolean isPomType(Artifact artifact) {
		return POM_PACKAGE_TYPE.equals(artifact.getExtension());
	}

	private File getFeatureFile(Artifact artifact, CacheManager cacheManager) {
		File baseFile = artifact.getFile();
		
		if(baseFile == null) {
			return null;
		} else if (baseFile.isDirectory()) {
			File featureFile = new File(baseFile, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			return featureFile.exists() ? featureFile : null;
		} else if (DEFAULT_PACKAGE_TYPE.equals(FilenameUtils.getExtension(baseFile.getName()))) {
			return unpackFeatureFile(artifact, cacheManager);
		}
		
		return null;
	}
	
	private File unpackFeatureFile(Artifact artifact, CacheManager cacheManager) {
		try {
			return cacheManager.accessArtifactFile(artifact, file -> {
				// Unpack feature.xml into the same directory as the jar
				File featureFile = new File(file.getParentFile(), ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				
				// May have already been unpacked -> reuse
				if(featureFile.exists()) {
					return featureFile;
				}
				
				File markerFile = new File(file.getParentFile(), NOT_A_FEATURE);
				
				// Artifact has already been checked during an earlier cycle
				if(markerFile.exists() && markerFile.lastModified() >= file.lastModified()) {
					return null;
				}
				
				try (JarFile jar = new JarFile(artifact.getFile())) {
					ZipEntry entry = jar.getEntry(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
					
					// feature.xml is missing -> not an Eclipse feature
					if(entry == null) {
						FileUtils.touch(markerFile);
						return null;
					}
					
					Files.copy(jar.getInputStream(entry), featureFile.toPath());
					
					return featureFile;
				} catch(IOException e) {
					LOGGER.error(e.getLocalizedMessage(), e);
					return null;
				}
			});
		} catch(Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	private void addBundleForArtifact(Artifact artifact, CacheManager cacheManager, IMaven maven,
			TargetBundles targetBundles) {
		File featureFile = getFeatureFile(artifact, cacheManager);
		
		if (isPomType(artifact)) {
			targetBundles.features
					.add(new MavenTargetFeature(new MavenPomFeatureModel(artifact, targetBundles, false)));
			if (includeSource) {
				targetBundles.features
						.add(new MavenTargetFeature(new MavenPomFeatureModel(artifact, targetBundles, true)));
			}
			return;
		} else if (featureFile != null) {
			try {
				targetBundles.features.add(new TargetFeature(featureFile));
			} catch (CoreException e) {
				failedArtifacts.add(artifact);
				LOGGER.error(e.getLocalizedMessage(), e);
			}
			return;
		}		
		BNDInstructions bndInstructions = instructionsMap.get(getKey(artifact));
		if (bndInstructions == null) {
			// no specific instructions for this artifact, try using the location default
			// then
			bndInstructions = instructionsMap.get("");
		}
		MavenTargetBundle bundle = cacheManager.getTargetBundle(artifact, bndInstructions, metadataMode);
		IStatus status = bundle.getStatus();
		if (status.isOK()) {
			targetBundles.bundles.put(artifact, bundle);
			if (includeSource) {
				try {
					List<ArtifactRepository> repositories = getAvailableArtifactRepositories(maven);
					Artifact resolve = RepositoryUtils.toArtifact(maven.resolve(artifact.getGroupId(),
							artifact.getArtifactId(), artifact.getBaseVersion(), artifact.getExtension(), "sources",
							repositories, new NullProgressMonitor()));
					MavenSourceBundle sourceBundle = new MavenSourceBundle(bundle.getBundleInfo(), resolve,
							cacheManager);
					targetBundles.bundles.put(resolve, sourceBundle);
					targetBundles.sourceBundles.put(artifact, sourceBundle);
				} catch (Exception e) {
					// Source not available / usable
				}
			}
		} else if (status.matches(IStatus.CANCEL)) {
			targetBundles.ignoredArtifacts.add(artifact);
		} else {
			failedArtifacts.add(artifact);
			// failed ones must be added to the target as well to fail resolution of the TP
			targetBundles.bundles.put(artifact, bundle);
		}
	}

	public MavenTargetLocation update(IProgressMonitor monitor) throws CoreException {

		List<MavenTargetDependency> latest = new ArrayList<>();
		int updated = 0;
		for (MavenTargetDependency dependency : roots) {
			Artifact artifact = new DefaultArtifact(
					dependency.getGroupId() + ":" + dependency.getArtifactId() + ":(0,]");
			IMaven maven = MavenPlugin.getMaven();
			RepositorySystem repoSystem = MavenPluginActivator.getDefault().getRepositorySystem();
			IMavenExecutionContext context = maven.createExecutionContext();
			List<ArtifactRepository> repositories = getAvailableArtifactRepositories(maven);
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

		return new MavenTargetLocation(label, latest, extraRepositories, metadataMode, dependencyDepth,
				dependencyScopes,
				includeSource, instructionsMap.values(), excludedArtifacts, featureTemplate);

	}

	private List<ArtifactRepository> getAvailableArtifactRepositories(IMaven maven) throws CoreException {
		List<ArtifactRepository> repositories = new ArrayList<>(maven.getArtifactRepositories());
		for (MavenTargetRepository repo : extraRepositories) {
			ArtifactRepository repository = maven.createArtifactRepository(repo.getId(), repo.getUrl());
			repositories.add(repository);
		}
		return repositories;
	}

	public List<MavenTargetDependency> getRoots() {
		return roots;
	}

	public IFeature getFeatureTemplate() {
		return featureTemplate;
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
		return targetBundles.bundles.size() - 1;
	}

	List<DependencyNode> getDependencyNodes(MavenTargetDependency dependency) {
		TargetBundles bundles = targetBundles;
		if (bundles == null) {
			return Collections.emptyList();
		}
		return bundles.dependencyNodes.get(dependency);
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		return resolveArtifacts(definition, monitor).stream().flatMap(tb -> tb.features.stream())
				.toArray(TargetFeature[]::new);
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
		return Objects.hash(roots, dependencyScopes, failedArtifacts, metadataMode);
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
		return Objects.equals(roots, other.roots) && Objects.equals(dependencyScopes, other.dependencyScopes)
				&& Objects.equals(failedArtifacts, other.failedArtifacts);
	}

	public boolean isIncludeSource() {
		return includeSource;
	}

	@Override
	public String serialize() {
		StringBuilder xml = new StringBuilder();
		xml.append("<location");
		attribute(xml, ATTRIBUTE_LABEL, label);
		attribute(xml, ATTRIBUTE_MISSING_META_DATA, metadataMode.name().toLowerCase());
		attribute(xml, ATTRIBUTE_DEPENDENCY_SCOPES, dependencyScopes.stream().collect(Collectors.joining(",")));
		attribute(xml, ATTRIBUTE_DEPENDENCY_DEPTH, dependencyDepth.name().toLowerCase());
		attribute(xml, ATTRIBUTE_INCLUDE_SOURCE, includeSource ? "true" : "");
		attribute(xml, "type", getType());
		xml.append(">");
		if (featureTemplate != null) {
			try (PrintWriter writer = new PrintWriter(new StringBuilderWriter(xml))) {
				featureTemplate.write("", writer);
				writer.flush();
			}
		}
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
		String string = xml.toString();
		return string;
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
		targetBundles = null;
		clearResolutionStatus();
	}

	public Collection<String> getDependencyScopes() {
		if (dependencyScopes.isEmpty()) {
			return DEFAULT_DEPENDENCY_SCOPES;
		}
		return dependencyScopes;
	}

	public DependencyDepth getDependencyDepth() {
		return dependencyDepth;
	}

	public boolean isIgnored(Artifact artifact) {
		TargetBundles bundles = targetBundles;
		if (bundles == null) {
			return false;
		}
		return bundles.ignoredArtifacts.contains(artifact);
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
		TargetBundles bundles = targetBundles;
		if (bundles != null) {
			return bundles.getMavenTargetBundle(artifact).orElse(null);
		}
		return null;
	}

	public MavenTargetBundle getMavenTargetBundle(MavenTargetDependency dependency) {
		TargetBundles bundles = targetBundles;
		if (bundles != null) {
			return bundles.getTargetBundle(dependency).orElse(null);
		}
		return null;
	}

	public Collection<String> getExcludes() {
		return Collections.unmodifiableCollection(excludedArtifacts);
	}

	public Collection<BNDInstructions> getInstructions() {
		return Collections.unmodifiableCollection(instructionsMap.values());
	}

	public MavenTargetLocation withInstructions(Collection<BNDInstructions> instructions) {
		return new MavenTargetLocation(label, roots.stream().map(MavenTargetDependency::copy).collect(Collectors.toList()), extraRepositories, metadataMode, dependencyDepth, dependencyScopes,
				includeSource, instructions, excludedArtifacts, featureTemplate);
	}

	public MavenTargetLocation withoutRoot(MavenTargetDependency toRemove) {
		return new MavenTargetLocation(label,
				roots.stream().filter(root -> root != toRemove).map(root -> root.copy()).collect(Collectors.toList()),
				extraRepositories,
				metadataMode, dependencyDepth, dependencyScopes, includeSource, instructionsMap.values(),
				excludedArtifacts, featureTemplate);
	}

}
