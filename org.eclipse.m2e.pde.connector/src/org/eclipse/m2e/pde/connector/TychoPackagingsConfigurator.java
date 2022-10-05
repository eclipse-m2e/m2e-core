/*******************************************************************************
 * Copyright (c) 2022 Konrad Windszus
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Konrad Windszus
 *******************************************************************************/
package org.eclipse.m2e.pde.connector;

import java.util.List;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class TychoPackagingsConfigurator extends AbstractProjectConfigurator {

    private static final String TYCHO_DS_PLUGIN_GROUP_ID = "org.eclipse.tycho";
    private static final String TYCHO_DS_PLUGIN_ARTIFACT_ID = "tycho-ds-plugin";
    private static final String GOAL_DECLARATIVE_SERVICES = "declarative-services";

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        MavenProject mavenProject = request.mavenProject();
        IProject project = request.mavenProjectFacade().getProject();

        String packaging = mavenProject.getPackaging();
        if ("eclipse-plugin".equals(packaging) || "eclipse-test-plugin".equals(packaging)) {
            PDEProjectHelper.configurePDEBundleProject(project, mavenProject, monitor);
            applyDsConfiguration(request, monitor);
        } else if ("eclipse-feature".equals(packaging)) {
            // see org.eclipse.pde.internal.ui.wizards.feature.AbstractCreateFeatureOperation
            if (!project.hasNature(PDE.FEATURE_NATURE)) {
                CoreUtility.addNatureToProject(project, PDE.FEATURE_NATURE, monitor);
            }
        } else if ("eclipse-repository".equals(packaging)) {
            // see org.eclipse.pde.internal.ui.wizards.site.NewSiteProjectCreationOperation
            if (!project.hasNature(PDE.SITE_NATURE)) {
                CoreUtility.addNatureToProject(project, PDE.SITE_NATURE, monitor);
            }
        }
    }

    void applyDsConfiguration(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        List<MojoExecution> mojoExecutions = getTychoDsPluginMojoExecutions(request, monitor);
        for (MojoExecution mojoExecution : mojoExecutions) {
            // apply PDE configuration for DS
            IEclipsePreferences prefs = new ProjectScope(request.mavenProjectFacade().getProject())
                    .getNode(org.eclipse.pde.ds.internal.annotations.Activator.PLUGIN_ID);
            Xpp3Dom dom = mojoExecution.getConfiguration();
            Xpp3Dom dsEnabled = dom.getChild("tycho.ds.enabled");
            if (dsEnabled != null && Boolean.valueOf(dsEnabled.getValue())) {
                prefs.putBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, true);
            } else {
                prefs.putBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, false);
            }
            Xpp3Dom dsVersion = dom.getChild("tycho.ds.version");
            if (dsVersion == null && !dsVersion.getValue().isEmpty()) {
                prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_SPEC_VERSION, dsVersion.getValue());
            }
        }
    }

    protected List<MojoExecution> getTychoDsPluginMojoExecutions(ProjectConfigurationRequest request, IProgressMonitor monitor)
            throws CoreException {
        return request.mavenProjectFacade().getMojoExecutions(TYCHO_DS_PLUGIN_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID,
                monitor, GOAL_DECLARATIVE_SERVICES);
    }
}
