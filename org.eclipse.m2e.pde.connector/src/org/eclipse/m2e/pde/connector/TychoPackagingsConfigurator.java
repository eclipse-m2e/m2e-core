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
import org.eclipse.pde.ds.internal.annotations.DSAnnotationVersion;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class TychoPackagingsConfigurator extends AbstractProjectConfigurator {

    private static final String TYCHO_GROUP_ID = "org.eclipse.tycho";
    private static final String TYCHO_DS_PLUGIN_ARTIFACT_ID = "tycho-ds-plugin";
    private static final String GOAL_DECLARATIVE_SERVICES = "declarative-services";

    private static final Logger log = LoggerFactory.getLogger(TychoPackagingsConfigurator.class);
   
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
        }
    }

    void applyDsConfiguration(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
        List<MojoExecution> mojoExecutions = getTychoDsPluginMojoExecutions(request, monitor);
        if (mojoExecutions.isEmpty()) {
            return;
        }
        if (mojoExecutions.size() > 1) {
            log.error("Found more than one execution for plugin {}:{} and goal {}, only consider configuration of first one", TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID, GOAL_DECLARATIVE_SERVICES);
        }
        // first mojo execution is relevant
        Xpp3Dom dom = mojoExecutions.get(0).getConfiguration();
        // apply PDE configuration for DS
        IEclipsePreferences prefs = new ProjectScope(request.mavenProjectFacade().getProject())
                .getNode(org.eclipse.pde.ds.internal.annotations.Activator.PLUGIN_ID);
        Xpp3Dom dsEnabled = dom.getChild("enabled");
        final boolean isDsEnabled;
        if (dsEnabled != null && !dsEnabled.getValue().isEmpty()) {
            isDsEnabled = Boolean.valueOf(dsEnabled.getValue());
            prefs.putBoolean(org.eclipse.pde.ds.internal.annotations.Activator.PREF_ENABLED, isDsEnabled);
        } else {
            isDsEnabled = false;
        }
        if (isDsEnabled) {
            Xpp3Dom dsVersion = dom.getChild("dsVersion");
            if (containsExplicitConfigurationValue(dsVersion)) {
                DSAnnotationVersion version = parseVersion(dsVersion.getValue());
                if (version != null) {
                    prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_SPEC_VERSION, version.name());
                }
            }
            Xpp3Dom path = dom.getChild("path");
            if (containsExplicitConfigurationValue(path)) {
                prefs.put(org.eclipse.pde.ds.internal.annotations.Activator.PREF_PATH, path.getValue());
            }
        }
    }
    
    private boolean containsExplicitConfigurationValue(Xpp3Dom config) {
        if (config == null) {
            return false;
        }
        String value = config.getValue();
        // check if value is a property name
        if (value.startsWith("${")) {
            return false;
        }
        return true;
    }

    DSAnnotationVersion parseVersion(String version) {
        if (version.startsWith("V")) {
            version = version.substring(1).replace('_', '.');
        }
        Version osgiCompliantVersion = Version.parseVersion(version);
        if (osgiCompliantVersion.getMajor() == 1 && osgiCompliantVersion.getMinor() == 1) {
            return DSAnnotationVersion.V1_1;
        } else if (osgiCompliantVersion.getMajor() == 1 && osgiCompliantVersion.getMinor() == 2) {
            return DSAnnotationVersion.V1_2;
        } else if (osgiCompliantVersion.getMajor() == 1 && osgiCompliantVersion.getMinor() == 3) {
            return DSAnnotationVersion.V1_3;
        } else {
            log.error("Unsupported DS spec version {} found, using default instead", version );
        }
        return null;
    }

    protected List<MojoExecution> getTychoDsPluginMojoExecutions(ProjectConfigurationRequest request, IProgressMonitor monitor)
            throws CoreException {
        return request.mavenProjectFacade().getMojoExecutions(TYCHO_GROUP_ID, TYCHO_DS_PLUGIN_ARTIFACT_ID,
                monitor, GOAL_DECLARATIVE_SERVICES);
    }
}
