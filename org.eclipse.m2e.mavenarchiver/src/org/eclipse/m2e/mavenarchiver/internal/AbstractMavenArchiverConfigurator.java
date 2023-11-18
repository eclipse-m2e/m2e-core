/*******************************************************************************
 * Copyright (c) 2008-2022 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.m2e.mavenarchiver.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.IntStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.ReflectionUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * This configurator is used to generate files as per the org.apache.maven
 * maven-archiver configuration.<br/>
 * During an eclipse build, it :<br/>
 * <ul>
 * <li>Generates pom.properties and pom.xml files under the
 * ${outputDir}/META-INF/maven/${project.groupId}/${project.artifactId} folder
 * <br/>
 * In maven, this behaviour is implemented by
 * org.apache.maven.archiver.PomPropertiesUtil from org.apache.maven
 * maven-archiver.</li>
 * <li>Generates the MANIFEST.MF under the ${outputDir}/META-INF/ folder</li>
 * </ul>
 * All mojos that use MavenArchiver (jar mojo, ejb mojo and so on) produce these
 * files during cli build. In order to reproduce this behaviour in eclipse, a
 * dedicated configurator must be created for each of these Mojos (JarMojo,
 * EjbMojo ...).
 *
 * @see https://svn.apache.org/repos/asf/maven/shared/trunk/maven-archiver/src/main/java/org/apache/maven/archiver/
 *      PomPropertiesUtil.java
 * @see http://maven.apache.org/shared/maven-archiver/index.html
 * @author igor
 * @author Fred Bricon
 */
public abstract class AbstractMavenArchiverConfigurator extends AbstractProjectConfigurator {

	@SuppressWarnings("restriction")
	private static final String POM_XML = org.eclipse.m2e.core.internal.IMavenConstants.POM_FILE_NAME;

	private static final String GET_MANIFEST = "getManifest";

	private static final String MANIFEST_ENTRIES_NODE = "manifestEntries";

	private static final String ARCHIVE_NODE = "archive";

	private static final String CREATED_BY_ENTRY = "Created-By";

	private static final String MAVEN_ARCHIVER_CLASS = "org.apache.maven.archiver.MavenArchiver";

	private static final String M2E = "Maven Integration for Eclipse";

	private static final String GENERATED_BY_M2E = "Generated by " + M2E;

	private static final boolean JDT_SUPPORTS_MODULES;

	static {
		boolean isModuleSupportAvailable = false;
		try {
			Class.forName("org.eclipse.jdt.core.IModuleDescription");
			isModuleSupportAvailable = true;
		} catch (Throwable ignored) {
		}
		JDT_SUPPORTS_MODULES = isModuleSupportAvailable;
	}

	@Override
	public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
		// Nothing to configure
	}

	/**
	 * Gets the MojoExecutionKey from which to retrieve the maven archiver instance.
	 * 
	 * @return the MojoExecutionKey from which to retrieve the maven archiver
	 *         instance.
	 */
	protected abstract MojoExecutionKey getExecutionKey();

	@Override
	public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
			IPluginExecutionMetadata executionMetadata) {

		MojoExecutionKey key = getExecutionKey();
		if (execution.getArtifactId().equals(key.artifactId()) && execution.getGoal().equals(key.goal())) {

			return new AbstractBuildParticipant() {
				@Override
				public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
					IResourceDelta delta = getDelta(projectFacade.getProject());

					boolean forceManifest = false;
					if (delta != null) {
						ManifestDeltaVisitor visitor = new ManifestDeltaVisitor();
						delta.accept(visitor);
						forceManifest = visitor.foundManifest;
					}

					// this will be true for full builds too
					boolean forcePom = getBuildContext().hasDelta(POM_XML);

					// The manifest will be (re)generated if it doesn't exist or an existing
					// manifest is modified
					mavenProjectChanged(projectFacade, null, forceManifest || forcePom, monitor);

					if (!forcePom) {
						forcePom = !getOutputPomXML(projectFacade).exists();
					}
					if (forcePom) {
						writePom(projectFacade, monitor);
					}
					return Collections.emptySet();
				}
			};
		}
		return null;
	}

	private class ManifestDeltaVisitor implements IResourceDeltaVisitor {

		private static final String MANIFEST = "MANIFEST.MF";

		boolean foundManifest;

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta.getResource() instanceof IFile && MANIFEST.equals(delta.getResource().getName())) {
				foundManifest = true;
			}
			return !foundManifest;
		}
	}

	/**
	 * Generates the project manifest if necessary, that is if the project manifest
	 * configuration has changed or if the dependencies have changed.
	 */
	@Override
	public void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {

		IMavenProjectFacade oldFacade = event.getOldMavenProject();
		IMavenProjectFacade newFacade = event.getMavenProject();
		if (oldFacade == null && newFacade == null) {
			return;
		}
		mavenProjectChanged(newFacade, oldFacade, false, monitor);
	}

	protected void mavenProjectChanged(IMavenProjectFacade newFacade, IMavenProjectFacade oldFacade,
			boolean forceGeneration, IProgressMonitor monitor) throws CoreException {
		IContainer outputdir = getBuildOutputDir(newFacade);
		IFile manifest = outputdir.getFile(IPath.forPosix(JarFile.MANIFEST_NAME));
		if (forceGeneration || needsNewManifest(manifest, oldFacade, newFacade)) {
			generateManifest(newFacade, manifest, monitor);
			refresh(outputdir, monitor);
		}
	}

	/**
	 * Gets the output directory in which the files will be generated
	 * 
	 * @param facade the maven project facade to get the output directory from.
	 * @return the full workspace path to the output directory
	 */
	protected abstract IPath getOutputDir(IMavenProjectFacade facade);

	/**
	 * Refreshes the output directory of the maven project after file
	 * generation.<br/>
	 * Implementations can override this method to add some post processing.
	 * 
	 * @param outputdir the output directory to refresh
	 * @param monitor   the progress monitor
	 * @throws CoreException
	 */
	private void refresh(IContainer outputdir, IProgressMonitor monitor) throws CoreException {
		// refresh the target folder
		if (outputdir.exists()) {
			try {
				outputdir.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			} catch (Exception e) {
				e.printStackTrace();
				// random java.lang.IllegalArgumentException: Element not found:
				// /parent/project/target/classes/META-INF.
				// occur when refreshing the folder on project import / creation
				// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=244315
			}
		}
	}

	/**
	 * Checks if the MANIFEST.MF needs to be regenerated. That is if :
	 * <ul>
	 * <li>it doesn't already exist</li>
	 * <li>the maven project configuration changed</li>
	 * <li>the maven project dependencies changed</li>
	 * </ul>
	 * Implementations can override this method to add pre-conditions.
	 * 
	 * @param manifest  the MANIFEST.MF file to control
	 * @param oldFacade the old maven facade project configuration
	 * @param newFacade the new maven facade project configuration
	 * @param monitor   the progress monitor
	 * @return true if the MANIFEST.MF needs to be regenerated
	 */
	protected boolean needsNewManifest(IFile manifest, IMavenProjectFacade oldFacade, IMavenProjectFacade newFacade) {

		if (!manifest.exists()) {
			return true;
		}
		// Can't compare to a previous state, so assuming it's unchanged
		// This situation actually occurs during incremental builds,
		// when called from the buildParticipant
		if (oldFacade == null || oldFacade.getMavenProject() == null) {
			return false;
		}

		MavenProject newProject = newFacade.getMavenProject();
		MavenProject oldProject = oldFacade.getMavenProject();

		// Assume Sets of artifacts are actually ordered
		if (dependenciesChanged(oldProject.getArtifacts(), newProject.getArtifacts())) {
			return true;
		}

		Xpp3Dom oldArchiveConfig = getArchiveConfiguration(oldProject);
		Xpp3Dom newArchiveConfig = getArchiveConfiguration(newProject);

		if (!Objects.equals(newArchiveConfig, oldArchiveConfig)) {
			return true;
		}

		// Name always not null
		if (!newProject.getName().equals(oldProject.getName())) {
			return true;
		}
		Optional<String> oldOrgaName = Optional.ofNullable(oldProject.getOrganization()).map(Organization::getName);
		Optional<String> newOrgaName = Optional.ofNullable(newProject.getOrganization()).map(Organization::getName);
		return !newOrgaName.equals(oldOrgaName);
	}

	/**
	 * Compare 2 lists of Artifacts for change
	 * 
	 * @param artifacts the reference artifact list
	 * @param others    the artifacts to compare to
	 * @return true if the 2 artifact lists are different
	 */
	private boolean dependenciesChanged(Collection<Artifact> artifacts, Collection<Artifact> others) {
		if (artifacts == others || artifacts == null || others == null) {
			return false;
		}
		if (artifacts.size() != others.size()) {
			return true;
		}
		Iterator<Artifact> otherIterator = others.iterator();
		return artifacts.stream().anyMatch(a -> !areEqual(a, otherIterator.next()));
	}

	private boolean areEqual(Artifact dep, Artifact other) {
		if (dep == other) {
			return true;
		}
		if (dep == null || other == null) {
			return false;
		}
		// So both artifacts are not null here.
		// Fast (to type) and easy way to compare artifacts.
		// Proper solution would not rely on internal implementation of toString
		return dep.toString().equals(other.toString()) && dep.isOptional() == other.isOptional();
	}

	private Xpp3Dom getArchiveConfiguration(MavenProject mavenProject) {
		Plugin plugin = mavenProject.getPlugin(getPluginKey());
		if (plugin == null)
			return null;

		Xpp3Dom pluginConfig = (Xpp3Dom) plugin.getConfiguration();
		if (pluginConfig == null) {
			return null;
		}
		return pluginConfig.getChild(ARCHIVE_NODE);
	}

	@SuppressWarnings("restriction")
	public void generateManifest(IMavenProjectFacade mavenFacade, IFile manifest, IProgressMonitor monitor)
			throws CoreException {

		MavenProject mavenProject = mavenFacade.getMavenProject();
		Set<Artifact> originalArtifacts = mavenProject.getArtifacts();
		boolean parentHierarchyLoaded = loadParentHierarchy(mavenFacade, monitor);
		try {
			markerManager.deleteMarkers(mavenFacade.getPom(), MavenArchiverConstants.MAVENARCHIVER_MARKER_ERROR);

			IMavenExecutionContext context = mavenFacade.createExecutionContext();
			context.getExecutionRequest().setOffline(MavenPlugin.getMavenConfiguration().isOffline());
			context.execute((innerContext, innerMonitor) -> {
				// Find the mojoExecution

				ClassLoader originalTCL = Thread.currentThread().getContextClassLoader();
				try {
					ClassRealm projectRealm = mavenProject.getClassRealm();
					if (projectRealm != null && projectRealm != originalTCL) {
						Thread.currentThread().setContextClassLoader(projectRealm);
					}
					MavenExecutionPlan executionPlan = mavenFacade.setupExecutionPlan(List.of("package"), monitor);
					MojoExecution mojoExecution = getExecution(executionPlan, getExecutionKey());
					if (mojoExecution == null) {
						return null;
					}

					// Get the target manifest file
					IFolder destinationFolder = (IFolder) manifest.getParent();
					org.eclipse.m2e.core.internal.M2EUtils.createFolder(destinationFolder, true, monitor);

					// Workspace project artifacts don't have a valid getFile(), so won't appear in
					// the manifest
					// We need to workaround the issue by creating fake files for such artifacts.
					// We could also use a custom File implementation having "public boolean
					// exists(){return true;}"
					mavenProject.setArtifacts(fixArtifactFileNames(mavenFacade));

					// Invoke the manifest generation API via reflection
					reflectManifestGeneration(mavenFacade, mojoExecution, innerContext.getSession(),
							new File(manifest.getLocation().toOSString()));
				} catch (Exception e) {
					throw new CoreException(Status.error("Something goes wrong!", e));
				} finally {
					Thread.currentThread().setContextClassLoader(originalTCL);
				}

				return null;
			}, monitor);

		} catch (Exception ex) {
			markerManager.addErrorMarkers(mavenFacade.getPom(), MavenArchiverConstants.MAVENARCHIVER_MARKER_ERROR, ex);

		} finally {
			// Restore the project state
			mavenProject.setArtifacts(originalArtifacts);
			if (parentHierarchyLoaded) {
				mavenProject.setParent(null);
			}
		}

	}

	private void reflectManifestGeneration(IMavenProjectFacade facade, MojoExecution mojoExecution,
			MavenSession session, File manifestFile) throws CoreException, ReflectiveOperationException, IOException {

		ClassLoader loader = null;
		Class<? extends Mojo> mojoClass;
		Mojo mojo = null;

		Xpp3Dom originalConfig = mojoExecution.getConfiguration();
		Xpp3Dom customConfig = Xpp3DomUtils.mergeXpp3Dom(new Xpp3Dom("configuration"), originalConfig);

		MavenProject mavenProject = facade.getMavenProject();
		IProject project = facade.getProject();
		// Add custom manifest entries
		customizeManifest(customConfig);

		mojoExecution.setConfiguration(customConfig);

		mojo = maven.getConfiguredMojo(session, mojoExecution, Mojo.class);
		mojoClass = mojo.getClass();
		loader = mojoClass.getClassLoader();
		try {
			Object archiver = getArchiverInstance(mojoClass, mojo, project);
			if (archiver != null) {
				Field archiveConfigurationField = findField(getArchiveConfigurationFieldName(), mojoClass);
				Object archiveConfiguration = getValue(mojo, archiveConfigurationField);
				Object mavenArchiver = getMavenArchiver(archiver, manifestFile, loader);

				Object manifest = getManifest(session, mavenProject, archiveConfiguration, mavenArchiver);

				// Get the user provided manifest, if it exists
				Object userManifest = getProvidedManifest(manifest.getClass(), archiveConfiguration);

				// Merge both manifests, the user provided manifest data takes precedence
				mergeManifests(manifest, userManifest);

				// Serialize the Manifest instance to an actual file
				writeManifest(manifestFile, manifest);
			}
		} finally {
			mojoExecution.setConfiguration(originalConfig);

			maven.releaseMojo(mojo, mojoExecution);
		}
	}

	private void writeManifest(File manifestFile, Object manifest) throws UnsupportedEncodingException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		Method write = getWriteMethod(manifest);
		if (write != null) {
			try (PrintWriter printWriter = new PrintWriter(
					WriterFactory.newWriter(manifestFile, WriterFactory.UTF_8))) {
				write.invoke(manifest, printWriter);
			}
		}
	}

	private Object getManifest(MavenSession session, MavenProject mavenProject, Object archiveConfiguration,
			Object mavenArchiver) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object manifest = null;
		Class<?> archiveConfigClass = archiveConfiguration.getClass();
		try {
			Method getManifest = mavenArchiver.getClass().getMethod(GET_MANIFEST, MavenSession.class,
					MavenProject.class, archiveConfigClass);

			// Create the Manifest instance
			manifest = getManifest.invoke(mavenArchiver, session, mavenProject, archiveConfiguration);

		} catch (NoSuchMethodException nsme) {
			// Fall back to legacy invocation
			Method getManifest = mavenArchiver.getClass().getMethod(GET_MANIFEST, MavenProject.class,
					archiveConfigClass);

			// Create the Manifest instance
			manifest = getManifest.invoke(mavenArchiver, mavenProject, archiveConfiguration);
		}
		return manifest;
	}

	private Object getArchiverInstance(Class<? extends Mojo> mojoClass, Mojo mojo, IProject project)
			throws IllegalAccessException {
		Field archiverField = findField(getArchiverFieldName(), mojoClass);
		if (archiverField == null) {
			// Since maven-jar-plugin 3.1.2, the field doesn't exist anymore, search for an
			// archiver map instead.
			Field archiversField = findField("archivers", mojoClass);
			if (archiversField != null) {
				Map<String, Object> archivers = getValue(mojo, archiversField);
				String key = isModular(project) ? "mjar" : "jar";
				return archivers.get(key);
			}
		} else {
			return getValue(mojo, archiverField);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getValue(Object obj, Field field) throws IllegalAccessException {
		field.setAccessible(true);
		return (T) field.get(obj);
	}

	private boolean isModular(IProject project) {
		try {
			if (JDT_SUPPORTS_MODULES && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(project);
				return jp.getModuleDescription() != null;
			}
		} catch (Exception ignoreMe) { // ignore
		}
		return false;
	}

	private Method getWriteMethod(Object manifest) {
		for (Method m : manifest.getClass().getMethods()) {
			if ("write".equals(m.getName())) {
				Class<?>[] params = m.getParameterTypes();
				if (params.length == 1 && Writer.class.isAssignableFrom(params[0])) {
					return m;
				}
			}
		}
		return null;
	}

	/**
	 * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=356725. Loads
	 * the parent project hierarchy if needed.
	 * 
	 * @param facade
	 * @param monitor
	 * @return true if parent projects had to be loaded.
	 * @throws CoreException
	 */
	private boolean loadParentHierarchy(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
		// TODO why is this not handled by m2e?
		MavenProject mavenProject = facade.getMavenProject();
		try {
			if (mavenProject.getModel().getParent() == null || mavenProject.getParent() != null) {
				// If the getParent() method is called without error,
				// we can assume the project has been fully loaded, no need to continue.
				return false;
			}
		} catch (IllegalStateException e) {
			// The parent can not be loaded properly
		}
		return facade.createExecutionContext().execute(mavenProject, (context, monitor2) -> {
			boolean loadedParent = false;
			MavenProject current = mavenProject;
			while (current != null && current.getModel().getParent() != null) {
				if (monitor.isCanceled()) {
					break;
				}
				String parentPath = current.getModel().getParent().getRelativePath();
				MavenProject parentProject = maven.readProject(new File(current.getBasedir(), parentPath), monitor);
				if (parentProject != null) {
					current.setParent(parentProject);
					loadedParent = true;
				}
				current = parentProject;
			}
			return loadedParent;
		}, monitor);
	}

	private Object getProvidedManifest(Class<?> manifestClass, Object archiveConfiguration) throws SecurityException,
			IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		try {
			Method getManifestFile = archiveConfiguration.getClass().getMethod("getManifestFile");
			File manifestFile = (File) getManifestFile.invoke(archiveConfiguration);

			if (manifestFile == null || !manifestFile.exists() || !manifestFile.canRead()) {
				return null;
			}
			try (Reader reader = new FileReader(manifestFile)) {
				return manifestClass.getConstructor(Reader.class).newInstance(reader);
			}
		} catch (IOException | NoSuchMethodException ex) {
			// ignore, this is not supported by this archiver version
		}
		return null;
	}

	private void mergeManifests(Object manifest, Object sourceManifest) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (sourceManifest == null) {
			return;
		}
		if (manifest instanceof Manifest mani && sourceManifest instanceof Manifest sourceMani) {
			merge(mani, sourceMani, false);
		} else {
			// keep backward compatibility with old plexus-archiver versions prior to 2.1
			Method merge = manifest.getClass().getMethod("merge", sourceManifest.getClass());
			merge.invoke(manifest, sourceManifest);
		}
	}

	/**
	 * @see org.codehaus.plexus.archiver.jar.JdkManifestFactory#merge()
	 */
	private void merge(Manifest target, Manifest other, boolean overwriteMain) {
		if (other != null) {
			Attributes mainAttributes = target.getMainAttributes();
			if (overwriteMain) {
				mainAttributes.clear();
				mainAttributes.putAll(other.getMainAttributes());
			} else {
				mainAttributes.putAll(other.getMainAttributes()); // the merge file always wins
			}
			other.getEntries().forEach((key, otherSection) -> {
				Attributes ourSection = target.getAttributes(key);
				if (ourSection == null) {
					if (otherSection != null) {
						target.getEntries().put(key, (Attributes) otherSection.clone());
					}
				} else {
					ourSection.putAll(otherSection); // the merge file always wins
				}
			});
		}
	}

	/**
	 * Get the Mojo's maven archiver field name.
	 * 
	 * @return the Mojo's maven archiver field name.
	 */
	protected abstract String getArchiverFieldName();

	/**
	 * Get the Mojo's archive configuration field name.
	 * 
	 * @return the Mojo's archive configuration field name.
	 */
	protected String getArchiveConfigurationFieldName() {
		return "archive";
	}

	private Set<Artifact> fixArtifactFileNames(IMavenProjectFacade facade) throws IOException, CoreException {
		Set<Artifact> artifacts = facade.getMavenProject().getArtifacts();
		if (artifacts == null) {
			return null;
		}
		Set<Artifact> newArtifacts = new LinkedHashSet<>(artifacts.size());

		ArtifactRepository localRepo = MavenPlugin.getMaven().getLocalRepository();

		for (Artifact a : artifacts) {
			Artifact artifact;
			if (a.getFile().isDirectory() || "pom.xml".equals(a.getFile().getName())) {
				// Workaround Driven Development : Create a dummy file associated with an
				// Artifact,
				// so this artifact won't be ignored during the resolution of the Class-Path
				// entry in the Manifest
				artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), a.getScope(),
						a.getType(), a.getClassifier(), a.getArtifactHandler());
				artifact.setFile(fakeFile(localRepo, a));
			} else {
				artifact = a;
			}

			newArtifacts.add(artifact);
		}
		return newArtifacts;
	}

	private void customizeManifest(Xpp3Dom customConfig) {
		if (customConfig == null) {
			return;
		}
		Xpp3Dom archiveNode = customConfig.getChild(ARCHIVE_NODE);
		if (archiveNode == null) {
			archiveNode = new Xpp3Dom(ARCHIVE_NODE);
			customConfig.addChild(archiveNode);
		}

		Xpp3Dom manifestEntriesNode = archiveNode.getChild(MANIFEST_ENTRIES_NODE);
		if (manifestEntriesNode == null) {
			manifestEntriesNode = new Xpp3Dom(MANIFEST_ENTRIES_NODE);
			archiveNode.addChild(manifestEntriesNode);
		}

		Xpp3Dom createdByNode = manifestEntriesNode.getChild(CREATED_BY_ENTRY);
		// Add a default "Created-By: Maven Integration for Eclipse", because it's cool
		if (createdByNode == null) {
			createdByNode = new Xpp3Dom(CREATED_BY_ENTRY);
			createdByNode.setValue(M2E);
			manifestEntriesNode.addChild(createdByNode);
		}
	}

	private Field findField(String name, Class<?> clazz) {
		return ReflectionUtils.getFieldByNameIncludingSuperclasses(name, clazz);
	}

	private Object getMavenArchiver(Object archiver, File manifestFile, ClassLoader loader)
			throws ReflectiveOperationException {
		Class<?> mavenArchiverClass = Class.forName(MAVEN_ARCHIVER_CLASS, false, loader);
		Object mavenArchiver = mavenArchiverClass.getDeclaredConstructor().newInstance();

		Method setArchiver = findMethodForArgumentTypes(mavenArchiverClass, "setArchiver", archiver.getClass());
		setArchiver.invoke(mavenArchiver, archiver);
		Method setOutputFile = mavenArchiverClass.getMethod("setOutputFile", File.class);
		setOutputFile.invoke(mavenArchiver, manifestFile);
		return mavenArchiver;
	}

	private static Method findMethodForArgumentTypes(Class<?> clazz, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException, SecurityException {
		return Arrays.stream(clazz.getMethods())
				.filter(m -> methodName.equals(m.getName()) && m.getParameterCount() == parameterTypes.length)
				.filter(m -> IntStream.range(0, parameterTypes.length)
						.mapToObj(i -> m.getParameterTypes()[i].isAssignableFrom(parameterTypes[i])).allMatch(b -> b))
				.findAny().orElseThrow(() -> new NoSuchMethodException("Method " + methodName + "() not found"));
	}

	private String getPluginKey() {
		MojoExecutionKey execution = getExecutionKey();
		return execution.groupId() + ":" + execution.artifactId();
	}

	private MojoExecution getExecution(MavenExecutionPlan executionPlan, MojoExecutionKey key) {
		for (MojoExecution execution : executionPlan.getMojoExecutions()) {
			if (key.artifactId().equals(execution.getArtifactId()) && key.groupId().equals(execution.getGroupId())
					&& key.goal().equals(execution.getGoal())) {
				return execution;
			}
		}
		return null;
	}

	/**
	 * Generates a temporary file in the system temporary folder for a given
	 * artifact
	 * 
	 * @param localRepo the local repository used to compute the file path
	 * @param artifact  the artifact to generate a temporary file for
	 * @return a temporary file sitting under
	 *         ${"java.io.tmpdir"}/fakerepo/${groupid}/{artifactid}/${version}/
	 * @throws IOException if the file could not be created
	 */
	private File fakeFile(ArtifactRepository localRepo, Artifact artifact) throws IOException {
		Path fakeRepo = getFakeDir();
		Path fakeFile = fakeRepo.resolve(localRepo.pathOf(artifact));
		Files.createDirectories(fakeFile.getParent());
		if (!Files.exists(fakeFile)) {
			Files.createFile(fakeFile);
		}
		return fakeFile.toFile();
	}

	private Path getFakeDir() throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(AbstractMavenArchiverConfigurator.class);
		if (bundle != null) {
			File dataFile = bundle.getDataFile("fakerepo");
			if (dataFile != null) {
				return dataFile.toPath();
			}
		}
		return Files.createTempDirectory("fakerepo");
	}

	@SuppressWarnings("restriction")
	private void writePom(IMavenProjectFacade facade, IProgressMonitor monitor) throws CoreException {
		IProject project = facade.getProject();
		ArtifactKey mavenProject = facade.getArtifactKey();

		IFile pom = getOutputPomXML(facade);
		IFolder output = (IFolder) pom.getParent();
		org.eclipse.m2e.core.internal.M2EUtils.createFolder(output, true, monitor);

		Properties properties = new Properties();
		properties.put("groupId", mavenProject.groupId());
		properties.put("artifactId", mavenProject.artifactId());
		properties.put("version", mavenProject.version());
		properties.put("m2e.projectName", project.getName());
		properties.put("m2e.projectLocation", project.getLocation().toOSString());

		IFile pomProperties = output.getFile("pom.properties");
		try (OutputStream out = Files.newOutputStream(getLocationPath(pomProperties))) {
			properties.store(out, GENERATED_BY_M2E);
		} catch (IOException ex) { // ignore
		}
		pomProperties.refreshLocal(IResource.DEPTH_ZERO, monitor);

		try (InputStream is = facade.getPom().getContents()) {
			Files.copy(is, getLocationPath(pom), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) { // ignore
		}
		pom.refreshLocal(IResource.DEPTH_ZERO, monitor);
	}

	private static Path getLocationPath(IResource pom) {
		return Path.of(pom.getLocationURI());
	}

	private IContainer getBuildOutputDir(IMavenProjectFacade facade) {
		IWorkspaceRoot root = facade.getProject().getWorkspace().getRoot();
		IPath outputDir = getOutputDir(facade);
		return outputDir.segmentCount() == 1 //
				? root.getProject(outputDir.segment(0))
				: root.getFolder(outputDir);
	}

	private IFile getOutputPomXML(IMavenProjectFacade facade) {
		ArtifactKey project = facade.getArtifactKey();
		return getBuildOutputDir(facade).getFile(
				IPath.forPosix("META-INF/maven/" + project.groupId() + "/" + project.artifactId() + "/" + POM_XML));
	}

}
