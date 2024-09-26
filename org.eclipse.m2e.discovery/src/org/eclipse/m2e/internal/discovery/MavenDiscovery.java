/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.internal.discovery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Tag;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.eclipse.m2e.core.internal.lifecyclemapping.LifecycleMappingFactory;
import org.eclipse.m2e.core.internal.lifecyclemapping.model.LifecycleMappingMetadataSource;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.internal.discovery.strategy.M2ERemoteBundleDiscoveryStrategy;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.internal.discovery.wizards.MavenDiscoveryWizard;


@SuppressWarnings("restriction")
public class MavenDiscovery {
  private static final Logger log = LoggerFactory.getLogger(MavenDiscovery.class);

  public static final Tag NO_RESTART_TAG = new Tag("norestart", "norestart"); //$NON-NLS-1$//$NON-NLS-2$

  public static final Tag APPLICABLE_TAG = new Tag("applicable", Messages.MavenDiscovery_Wizard_Applicable_Tag); //$NON-NLS-1$

  private static final Tag EXTRAS_TAG = new Tag("extras", Messages.MavenDiscovery_Wizard_ExtrasTag); //$NON-NLS-1$

  private static final Tag LIFECYCLES_TAG = new Tag("lifecycles", Messages.MavenDiscovery_Wizard_LifecyclesTag); //$NON-NLS-1$

  private static final Tag MAVEN_TAG = new Tag("maven", Messages.MavenDiscovery_Wizard_MavenTag); //$NON-NLS-1$

  private static final String DEFAULT_BASEURL = "https://github.com/eclipse-m2e/m2e-discovery-catalog/releases/download/2.x/"; //$NON-NLS-1$

  private static final String DEFAULT_FILENAME = "catalog-2.x.xml"; //$NON-NLS-1$

  public static final String DEFAULT_URL = DEFAULT_BASEURL + DEFAULT_FILENAME;

  private static final String CONFIGURED_URL = System.getProperty("m2e.discovery.url"); //$NON-NLS-1$

  private static final String BASEURL = System.getProperty("m2e.discovery.baseurl", DEFAULT_BASEURL); //$NON-NLS-1$

  public static final String PATH;

  public static final String LIFECYCLE_PATH = "lifecycle/"; //$NON-NLS-1$

  public static final String LIFECYCLE_EXT = ".xml"; //$NON-NLS-1$

  public static final String PLUGINXML_EXT = ".pluginxml"; //$NON-NLS-1$

  static {
    PATH = CONFIGURED_URL != null ? CONFIGURED_URL : BASEURL + DEFAULT_FILENAME;
  }

  public static void launchWizard(Shell shell) {
    launchWizard(shell, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList());
  }

  public static void launchWizard(final Collection<String> packagingTypes, final Collection<MojoExecutionKey> mojos,
      final Collection<String> lifecycleIds, final Collection<String> configuratorIds) {
    final Display display = Workbench.getInstance().getDisplay();
    display
        .asyncExec(() -> launchWizard(display.getActiveShell(), packagingTypes, mojos, lifecycleIds, configuratorIds));
  }

  public static void launchWizard(Shell shell, Collection<String> packagingTypes, Collection<MojoExecutionKey> mojos,
      Collection<String> lifecycleIds, Collection<String> configuratorIds) {
    Catalog catalog = getCatalog();

    // Build the list of tags to show in the Wizard header
    List<Tag> tags = new ArrayList<>(3);
    if(!packagingTypes.isEmpty() || !mojos.isEmpty() || !configuratorIds.isEmpty() || !lifecycleIds.isEmpty()) {
      tags.add(APPLICABLE_TAG);
    }
    tags.add(EXTRAS_TAG);
    tags.add(LIFECYCLES_TAG);
    tags.add(MAVEN_TAG);
    catalog.setTags(tags);

    // Create configuration for the catalog
    MavenCatalogConfiguration configuration = new MavenCatalogConfiguration();
    configuration.setShowTagFilter(true);
    if(!packagingTypes.isEmpty() || !mojos.isEmpty() || !configuratorIds.isEmpty() || !lifecycleIds.isEmpty()) {
      tags = new ArrayList<>(1);
      tags.add(APPLICABLE_TAG);
      configuration.setSelectedTags(tags);
    } else {
      configuration.setSelectedTags(tags);
    }
    configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(packagingTypes);
    configuration.setSelectedMojos(mojos);
    configuration.setSelectedLifecycleIds(lifecycleIds);
    configuration.setSelectedConfigurators(configuratorIds);

    MavenDiscoveryWizard wizard = new MavenDiscoveryWizard(catalog, configuration);
    WizardDialog dialog = new WizardDialog(shell, wizard);
    dialog.open();
  }

  public static Catalog getCatalog() {
    Catalog catalog = new Catalog();
    catalog.setEnvironment(DiscoveryCore.createEnvironment());
    catalog.setVerifyUpdateSiteAvailability(false);

    // look for remote descriptor
    M2ERemoteBundleDiscoveryStrategy remoteDiscoveryStrategy = new M2ERemoteBundleDiscoveryStrategy();
    remoteDiscoveryStrategy.setDirectoryUrl(PATH);
    catalog.getDiscoveryStrategies().add(remoteDiscoveryStrategy);
    return catalog;
  }

  public static LifecycleMappingMetadataSource getLifecycleMappingMetadataSource(CatalogItem ci) {
    URL url = null;
    try {
      url = getLifecycleMappingMetadataSourceURL(ci);
      if(url == null) {
        return null;
      }
      // To ensure we can delete the temporary file we need to prevent caching, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4386865
      URLConnection conn = url.openConnection();
      if(conn instanceof JarURLConnection jarConn) {
        jarConn.setDefaultUseCaches(false);
      }
      try (InputStream is = conn.getInputStream()) {
        return LifecycleMappingFactory.createLifecycleMappingMetadataSource(is);
      }
    } catch(FileNotFoundException e) {
      // CatalogItem does not contain lifecycle mapping
      log.info("No lifecyle mapping found at " + url);
    } catch(Exception e) {
      log.warn(NLS.bind(Messages.MavenCatalogViewer_Error_loading_lifecycle, ci.getId()), e);
    }
    return null;
  }

  /*
   * Restart is required when one or more CatalogItem lacks the norestart tag.
   */
  public static boolean requireRestart(Iterable<CatalogItem> catalogItems) {
    for(CatalogItem item : catalogItems) {
      if(!item.hasTag(NO_RESTART_TAG)) {
        return true;
      }
    }
    return false;
  }

  public static void getProvidedProjectConfigurators(CatalogItem ci, List<String> projectConfigurators,
      List<String> mappingStrategies) {
    try {
      URL url = ci.getSource().getResource(LIFECYCLE_PATH + ci.getId() + PLUGINXML_EXT);
      if(url != null) {
        InputStream is = url.openStream();
        parsePluginXml(is, projectConfigurators, mappingStrategies);
      }
    } catch(FileNotFoundException e) {
      log.warn("CatalogItem {} does not contain lifecycle mapping metadata", ci.getId()); //$NON-NLS-1$
    } catch(Exception e) {
      log.warn(NLS.bind(Messages.MavenCatalogViewer_Error_loading_lifecycle, ci.getId()), e);
    }
  }

  public static void parsePluginXml(InputStream is, List<String> configurators, List<String> mappingStrategies)
      throws XmlPullParserException, IOException {
    Xpp3Dom plugin = Xpp3DomBuilder.build(is, "UTF-8"); //$NON-NLS-1$
    Xpp3Dom[] extensions = plugin.getChildren("extension"); //$NON-NLS-1$
    for(Xpp3Dom extension : extensions) {
      String extensionPoint = extension.getAttribute("point"); //$NON-NLS-1$
      if(LifecycleMappingFactory.EXTENSION_PROJECT_CONFIGURATORS.equals(extensionPoint)) {
        Xpp3Dom[] configuratorsDom = extension.getChildren("configurator"); //$NON-NLS-1$
        for(Xpp3Dom configurator : configuratorsDom) {
          String id = configurator.getAttribute("id"); //$NON-NLS-1$
          if(id != null) {
            configurators.add(id);
          }
        }
      } else if(LifecycleMappingFactory.EXTENSION_LIFECYCLE_MAPPINGS.equals(extensionPoint)) {
        Xpp3Dom[] lifecycleMappingsDom = extension.getChildren("lifecycleMapping"); //$NON-NLS-1$
        for(Xpp3Dom lifecycleMapping : lifecycleMappingsDom) {
          String id = lifecycleMapping.getAttribute("id"); //$NON-NLS-1$
          if(id != null) {
            mappingStrategies.add(id);
          }
        }
      }
    }
  }

  public static URL getLifecycleMappingMetadataSourceURL(CatalogItem ci) {
    return ci.getSource().getResource(LIFECYCLE_PATH + ci.getId() + LIFECYCLE_EXT);
  }
}
