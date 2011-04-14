/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.discovery.directory.tests;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.Catalog;
import org.eclipse.equinox.internal.p2.discovery.DiscoveryCore;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogCategory;
import org.eclipse.equinox.internal.p2.discovery.model.CatalogItem;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;

import org.eclipse.m2e.internal.discovery.MavenDiscovery;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogConfiguration;
import org.eclipse.m2e.internal.discovery.wizards.MavenCatalogViewer;


@SuppressWarnings("restriction")
public class DiscoveryDirectoryTest extends TestCase implements IShellProvider {

  private ResourceManager resourceManager;

  private Shell shell;

  private Catalog catalog;

  private MavenCatalogConfiguration configuration;

  private IProgressMonitor monitor;

  @Override
  public void setUp() throws Exception {
    catalog = new Catalog();
    catalog.setEnvironment(DiscoveryCore.createEnvironment());
    catalog.setVerifyUpdateSiteAvailability(false);
    catalog.getDiscoveryStrategies().add(new TestM2EBundleStrategy());

    // Build the list of tags to show in the Wizard header
    catalog.setTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));

    // Create configuration for the catalog
    configuration = new MavenCatalogConfiguration();
    configuration.setShowTagFilter(true);
    configuration.setSelectedTags(Collections.singletonList(MavenDiscovery.APPLICABLE_TAG));
    configuration.setShowInstalledFilter(false);
    configuration.setSelectedPackagingTypes(Collections.EMPTY_LIST);
    configuration.setSelectedMojos(Collections.EMPTY_LIST);
    configuration.setSelectedLifecycleIds(Collections.EMPTY_LIST);
    configuration.setSelectedConfigurators(Collections.EMPTY_LIST);
    shell = new Shell(Workbench.getInstance().getDisplay());
    this.resourceManager = new LocalResourceManager(JFaceResources.getResources(Workbench.getInstance().getDisplay()));
    monitor = new NullProgressMonitor();
  }

  public void tearDown() throws Exception {
    resourceManager.dispose();
    shell.dispose();
    resourceManager = null;
    shell = null;
  }

  /*
   * This is to ensure that each CatalogCategory has a valid icon associated with it.
   */
  public void testImagesPresent() throws Exception {
    updateMavenCatalog();

    assertTrue("Expected at least one category", catalog.getCategories().size() > 0);

    for(CatalogCategory category : catalog.getCategories()) {
      assertNotNull("Icon missing for catalog category: " + category.getId(),
          getIconImage(category.getSource(), category.getIcon(), 48, true));
    }
  }

  /*
   * This ensures each CatalogItem has a valid update site which has the IU available.
   */
  public void testHasIUs() throws Exception {
    updateMavenCatalog();
    IMetadataRepositoryManager mgr = getMetadataRepositoryManager();

    assertTrue("Expected at least one item", catalog.getItems().size() > 0);

    for(CatalogItem item : catalog.getItems()) {
      URI uri = getUri(item);
      if(uri.getHost().equals("localhost")) {
        // these should be tests entries, we don't care about them. 
        continue;
      }
      IMetadataRepository repo = mgr.loadRepository(uri, monitor);
      assertFalse("Has IUs: " + item.getId(), item.getInstallableUnits().isEmpty());
      if(repo == null) {
        fail("Unknown failure loading repository for item: " + item.getId());
        return;
      }
      for(String iuId : item.getInstallableUnits()) {
        IVersionedId iuVid = VersionedId.parse(iuId);
        IQuery<IInstallableUnit> q = QueryUtil.createIUQuery(iuVid);
        IQueryResult<IInstallableUnit> result = repo.query(q, monitor);
        assertFalse("CatalogItem " + item.getId() + " missing IU: " + iuId, result.isEmpty());
      }
    }
  }

  private URI getUri(CatalogItem item) {
    URI uri = null;
    try {
      uri = new URI(item.getSiteUrl());
      return uri;
    } catch(URISyntaxException ex) {
      throw new IllegalArgumentException("Failed to create URI for CatalogItem " + item.getId() + "\n"
          + item.getSiteUrl(), ex);
    }
  }

  private Image getIconImage(AbstractCatalogSource discoverySource, Icon icon, int dimension, boolean fallback) {
    String imagePath;
    switch(dimension) {
      case 64:
        imagePath = icon.getImage64();
        if(imagePath != null || !fallback) {
          break;
        }
      case 48:
        imagePath = icon.getImage48();
        if(imagePath != null || !fallback) {
          break;
        }
      case 32:
        imagePath = icon.getImage32();
        break;
      default:
        throw new IllegalArgumentException();
    }
    if(imagePath != null && imagePath.length() > 0) {
      URL resource = discoverySource.getResource(imagePath);
      if(resource != null) {
        ImageDescriptor descriptor = ImageDescriptor.createFromURL(resource);
        return resourceManager.createImage(descriptor);
      }
    }
    return null;
  }

  private IMetadataRepositoryManager getMetadataRepositoryManager() {
    IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(TestActivator.getDefault().getBundle()
        .getBundleContext(), IProvisioningAgent.SERVICE_NAME);
    return (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
  }

  private void updateMavenCatalog() {
    MavenCatalogViewer mcv = new MavenCatalogViewer(catalog, this, new RunnableContext(), configuration);
    mcv.createControl(shell);
    mcv.updateCatalog();
  }

  private static class RunnableContext implements IRunnableContext {
    public RunnableContext() {
    }

    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException,
        InterruptedException {
      runnable.run(new NullProgressMonitor());
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.window.IShellProvider#getShell()
   */
  public Shell getShell() {
    return shell;
  }
}
