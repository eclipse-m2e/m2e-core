package org.eclipse.m2e.bnd.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.build.Project;

/**
 * Adapts eclipse projects managed by m2e to bnd projects
 */
@Component
@AdapterTypes(adaptableClass = IProject.class, adapterNames = Project.class)
public class BndPluginAdapter implements IAdapterFactory{

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IProject eclipseProject) {
			IMavenProjectFacade mavenProject = Adapters.adapt(eclipseProject, IMavenProjectFacade.class);
			if (isRelevantProject(mavenProject)) {
				System.out.println(eclipseProject.getName() + " uses bnd plugin!");
			}
		}
		return null;
	}

	private boolean isRelevantProject(IMavenProjectFacade mavenProject) {
		// TODO cache result inside IProject store
		if (mavenProject != null) {
			Map<MojoExecutionKey, List<IPluginExecutionMetadata>> mapping = mavenProject
					.getMojoExecutionMapping();
			for (MojoExecutionKey key : mapping.keySet()) {
				if ("biz.aQute.bnd".equals(key.groupId()) && "bnd-maven-plugin".equals(key.artifactId())) {
					return true;
				}
			}
		}
		return false;
	}

}
