/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;
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

	MavenPomFeatureModel(Artifact artifact, TargetBundles bundles) {
		this.artifact = artifact;
		this.targetBundles = bundles;
	}

	@Override
	public IResource getUnderlyingResource() {
		return null;
	}

	@Override
	public void load() throws CoreException {
		editable = true;
		try (FileInputStream stream = new FileInputStream(artifact.getFile())) {
			Model model = MavenPlugin.getMaven().readModel(stream);

			IFeature f = getFeature();
			f.setId(model.getGroupId() + "." + model.getArtifactId() + "." + model.getPackaging());
			f.setVersion(TargetBundles.createOSGiVersion(model.getVersion()).toString());
			f.setLabel(model.getName());
			String description = model.getDescription();
			String url = model.getUrl();
			IFeatureInfo info = f.getModel().getFactory().createInfo(IFeature.INFO_DESCRIPTION);
			info.setDescription(description);
			info.setURL(url);
			f.setFeatureInfo(info, info.getIndex());

			List<License> licenses = model.getLicenses();
			if (!licenses.isEmpty()) {
				licenses.stream().map(license -> {
					return Stream.<String>builder().add(license.getName()).add(license.getUrl())
							.add(license.getComments()).build().filter(Objects::nonNull)
							.filter(Predicate.not(String::isBlank)).collect(Collectors.joining("\r\n"));
				}).collect(Collectors.joining("--------------------------------------------------\r\n"));
			}
			Optional<DependencyNode> dependencyNode = targetBundles.getDependencyNode(artifact);
			MavenTargetBundle[] dependencies = dependencyNode.map(node -> {
				PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
				node.accept(nlg);
				return nlg;
			}).map(nlg -> nlg.getArtifacts(true)).stream().flatMap(Collection::stream).filter(a -> a.getFile() != null)
					.flatMap(a -> targetBundles.getTargetBundle(a).stream()).toArray(MavenTargetBundle[]::new);
			IFeaturePlugin[] featurePlugins = new IFeaturePlugin[dependencies.length];
			for (int i = 0; i < featurePlugins.length; i++) {
				FeaturePlugin plugin = new MavenFeaturePlugin(dependencies[i], this);
				plugin.setParent(f);
				featurePlugins[i] = plugin;
			}
			f.addPlugins(featurePlugins);
			setEnabled(true);
			updateTimeStampWith(System.currentTimeMillis());
			if (DEBUG_FEATURE_XML) {
				File file = new File("/tmp/" + f.getId() + "/feature.xml");
				File installLocation = file.getParentFile();
				installLocation.mkdirs();
				try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
					f.write("    ", writer);
					writer.flush();
				}
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, getClass(), "failed to load pom file"));
		} finally {
			editable = false;

		}
		setLoaded(true);
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
			Feature f = new MavenPomFeature(this);
			this.feature = f;
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
