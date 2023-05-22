/*******************************************************************************
 * Copyright (c) 2021, 2023 Christoph Läubrich and others
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenToolbox;
import org.eclipse.m2e.pde.target.shared.MavenBundleWrapper;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.feature.AbstractFeatureModel;
import org.eclipse.pde.internal.core.feature.Feature;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;

@SuppressWarnings("restriction")
class MavenPomFeatureModel extends AbstractFeatureModel {

	private static final boolean DEBUG_FEATURE_XML = false;

	private static final long serialVersionUID = 1L;
	private boolean editable;
	private Artifact artifact;
	private TargetBundles targetBundles;

	private boolean isSourceFeature;

	MavenPomFeatureModel(Artifact artifact, TargetBundles bundles, boolean isSourceFeature) {
		this.artifact = artifact;
		this.targetBundles = bundles;
		this.isSourceFeature = isSourceFeature;
	}

	@Override
	public IResource getUnderlyingResource() {
		return null;
	}

	@Override
	public void load() throws CoreException {
		editable = true;
		try (FileInputStream stream = new FileInputStream(artifact.getFile())) {
			Model model = IMavenToolbox.of(MavenPlugin.getMaven()).readModel(stream);

			IFeature f = getFeature();
			String id = model.getGroupId() + "." + model.getArtifactId() + "." + model.getPackaging();
			if (isSourceFeature) {
				id += ".source";
			}
			f.setId(id);
			f.setVersion(MavenBundleWrapper.createOSGiVersion(model.getVersion()).toString());
			String name = model.getName();
			if (isSourceFeature) {
				name += " (Source)";
			}
			f.setLabel(name);
			String description = model.getDescription();
			String url = model.getUrl();
			addFeatureInfo(f, IFeature.INFO_DESCRIPTION, description, url);

			List<License> licenses = model.getLicenses();
			if (!licenses.isEmpty()) {
				String newLine = System.lineSeparator();
				licenses.stream()
						.map(license -> Stream.of(license.getName(), license.getUrl(), license.getComments())
								.filter(Objects::nonNull).filter(Predicate.not(String::isBlank))
								.collect(Collectors.joining(newLine)))
						.collect(Collectors.joining("--------------------------------------------------" + newLine));
			}
			Optional<DependencyNode> dependencyNode = targetBundles.getDependencyNode(artifact);
			List<TargetBundle> dependencies = dependencyNode.map(node -> {
				PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
				node.accept(nlg);
				return nlg.getArtifacts(true);
			}).stream().flatMap(Collection::stream).filter(a -> a.getFile() != null)
					.flatMap(a -> targetBundles.getTargetBundle(a, isSourceFeature).stream()).toList();
			List<IFeaturePlugin> featurePlugins = new ArrayList<>();
			for (TargetBundle bundle : dependencies) {
				FeaturePlugin plugin = new MavenFeaturePlugin(bundle, this);
				plugin.setParent(f);
				featurePlugins.add(plugin);
			}
			f.addPlugins(featurePlugins.toArray(FeaturePlugin[]::new));
			setEnabled(true);
			updateTimeStampWith(System.currentTimeMillis());
			if (DEBUG_FEATURE_XML) {
				File file = new File("/tmp/" + f.getId() + "/feature.xml");
				file.getParentFile().mkdirs();
				try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
					f.write("    ", writer);
				}
			}
		} catch (IOException e) {
			throw new CoreException(Status.error("failed to load pom file"));
		} finally {
			editable = false;
		}
		setLoaded(true);
	}

	private void addFeatureInfo(IFeature feature, int infoIndex, String description, String url) throws CoreException {
		IFeatureInfo info = feature.getModel().getFactory().createInfo(infoIndex);
		info.setDescription(description);
		info.setURL(url);
		feature.setFeatureInfo(info, info.getIndex());
	}

	@Override
	public String getInstallLocation() {
		return null;
	}

	@Override
	public boolean isInSync() {
		return true;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public IFeature getFeature() {
		if (feature == null) {
			this.feature = new MavenPomFeature(this);
		}
		return feature;
	}

	private static final class MavenPomFeature extends Feature {

		private static final long serialVersionUID = 1L;

		MavenPomFeature(MavenPomFeatureModel model) {
			setModel(model);
		}

		@Override
		public boolean isValid() {
			return hasRequiredAttributes();
		}

	}

}
